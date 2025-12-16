package com.example.banking;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.CircularBounds;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.SearchNearbyRequest;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MapFragment extends Fragment {
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private PlacesClient placesClient;
    FloatingActionButton location;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_map, container, false);

        location = root.findViewById(R.id.fabMyLocation);

        location.setOnClickListener(v -> {
            moveToCurrentLocationAndSearch();
        });
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);

        // Khởi tạo Places API với API key
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), "AIzaSyAisLpwRasU5mLuGIq-ccICakGPNbGVLSo");
        }
        placesClient = Places.createClient(requireContext());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(googleMap -> {
                mMap = googleMap;


                enableMyLocationAndSearch();
            });
        }

    }

    private void enableMyLocationAndSearch() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
        } else {
            mMap.setMyLocationEnabled(true);
            moveToCurrentLocationAndSearch();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableMyLocationAndSearch();
        } else {
            Toast.makeText(requireContext(), "Cần quyền vị trí để tìm ngân hàng gần bạn", Toast.LENGTH_SHORT).show();
        }
    }


    // Hàm di chuyển camera và gọi tìm kiếm
    private void moveToCurrentLocationAndSearch() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.getLastLocation().addOnSuccessListener(loc -> {
                if (loc != null && mMap != null) {
                    LatLng myLatLng = new LatLng(loc.getLatitude(), loc.getLongitude());

                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 15));

                    searchNearbyPlaces(myLatLng);
                } else {
                    Log.e("MapFragment", "Location is null. Check Emulator settings.");
                }
            });
        }
    }

    private void searchNearbyPlaces(LatLng centerLocation) {
        List<Place.Field> placeFields = Arrays.asList(
                Place.Field.ID,
                Place.Field.DISPLAY_NAME,
                Place.Field.LAT_LNG,
                Place.Field.FORMATTED_ADDRESS
        );

        // Bán kính 2000m (2km) là hợp lý cho nội thành
        CircularBounds circle = CircularBounds.newInstance(centerLocation, 20000.0);

        // --- CẤU HÌNH GIẢ LẬP: TÌM SIÊU THỊ ---
        // Khi nào làm thật thì đổi thành Arrays.asList("bank", "atm");
        List<String> includedTypes = Arrays.asList("supermarket", "grocery_or_supermarket");

        SearchNearbyRequest request = SearchNearbyRequest.builder(circle, placeFields)
                .setIncludedTypes(includedTypes)
                .setMaxResultCount(20)
                .build();

        placesClient.searchNearby(request).addOnSuccessListener(response -> {
            mMap.clear(); // Xóa marker cũ để tránh trùng lặp

            for (Place place : response.getPlaces()) {
                LatLng latLng = place.getLatLng();
                String name = place.getDisplayName();
                String address = place.getFormattedAddress();

                if (latLng != null && name != null) {
                    String lowerName = name.toLowerCase(Locale.ROOT);

                    // --- LOGIC GIẢ LẬP: COI BÁCH HÓA XANH LÀ NGÂN HÀNG CỦA TÔI ---
                    // Kiểm tra cả có dấu và không dấu
                    boolean isTargetBank = lowerName.contains("bach hoa xanh") ||
                            lowerName.contains("bách hóa xanh") ||
                            lowerName.contains("bachhoaxanh");

                    MarkerOptions markerOptions = new MarkerOptions()
                            .position(latLng)
                            .title(name)
                            .snippet(address);

                    if (isTargetBank) {
                        // Màu Xanh Lá = Ngân hàng của tôi (Giả lập là BHX)
                        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                        // Z-Index cao hơn để marker này nổi lên trên marker khác
                        markerOptions.zIndex(1.0f);
                    } else {
                        // Màu Đỏ = Ngân hàng khác / Siêu thị khác
                        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                    }

                    mMap.addMarker(markerOptions);
                }
            }
        }).addOnFailureListener(exception -> {
            Log.e("PlacesAPI", "Lỗi tìm kiếm: " + exception.getMessage());
        });
    }



}
