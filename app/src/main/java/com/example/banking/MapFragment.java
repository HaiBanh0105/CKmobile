package com.example.banking;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MapFragment extends Fragment {
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private PlacesClient placesClient;
    FloatingActionButton location;

    MaterialCardView cardLocationDetails;
    TextView tvBranchName, tvBranchAddress;
    MaterialButton btnDirections;

    LatLng myLatLng, selectedATM;;



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_map, container, false);

        location = root.findViewById(R.id.fabMyLocation);
        cardLocationDetails = root.findViewById(R.id.cardLocationDetails);
        tvBranchName = root.findViewById(R.id.tvBranchName);
        tvBranchAddress = root.findViewById(R.id.tvBranchAddress);
        btnDirections = root.findViewById(R.id.btnDirections);

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

                // Chỉ nên gọi ở đây khi mMap đã sẵn sàng
                enableMyLocationAndSearch();

                mMap.setOnMarkerClickListener(marker -> {
                    if (!"Vị trí của tôi".equals(marker.getTitle())) {
                        // cập nhật CardView
                        tvBranchName.setText(marker.getTitle());
                        tvBranchAddress.setText(marker.getSnippet());
                        cardLocationDetails.setVisibility(View.VISIBLE);

                        // lưu ATM được chọn để khi bấm nút "Chỉ đường" mới gọi Directions API
                        selectedATM = marker.getPosition();
                    }
                    return false;
                });
            });
        }


        btnDirections.setOnClickListener(v -> {
            // Chỉ cần kiểm tra đã chọn ATM chưa.
            // Google Maps sẽ tự lấy vị trí hiện tại của người dùng làm điểm xuất phát.
            if (selectedATM != null) {
                openGoogleMapsNavigation(selectedATM);
            } else {
                Toast.makeText(requireContext(), "Vui lòng chọn một địa điểm ATM trên bản đồ", Toast.LENGTH_SHORT).show();
            }
        });

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
                    myLatLng = new LatLng(loc.getLatitude(), loc.getLongitude());

                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 15));
                    addFakeATMs(myLatLng);

                } else {
                    Log.e("MapFragment", "Location is null. Check Emulator settings.");
                }
            });
        }
    }

    // Hàm nhận vào LatLng myLatLng và thêm vài ATM giả định quanh đó
    private void addFakeATMs(LatLng myLatLng) {
        if (mMap == null || myLatLng == null) return;

        LatLng atm1 = new LatLng(myLatLng.latitude + 0.001, myLatLng.longitude + 0.001);

        LatLng atm2 = new LatLng(myLatLng.latitude - 0.001, myLatLng.longitude - 0.001);

        LatLng atm3 = new LatLng(myLatLng.latitude - 0.001, myLatLng.longitude + 0.001);

        // Thêm marker cho từng ATM
        mMap.addMarker(new com.google.android.gms.maps.model.MarkerOptions()
                .position(atm1)
                .title("ATM Ngân hàng ACB")
                .snippet("123 Nguyễn Hữu Thọ, Tân Phong, Q.7"));

        mMap.addMarker(new com.google.android.gms.maps.model.MarkerOptions()
                .position(atm2)
                .title("ATM Ngân hàng ACB")
                .snippet("654 Nguyễn Văn Linh, Tân Phong, Q.7"));

        mMap.addMarker(new com.google.android.gms.maps.model.MarkerOptions()
                .position(atm3)
                .title("ATM Ngân hàng ACB")
                .snippet("444 Lê Văn Lương, Tân Phong, Q.1"));
    }

    //Mở google map chỉ đường
    private void openGoogleMapsNavigation(LatLng dest) {
        Uri gmmIntentUri = Uri.parse("http://maps.google.com/maps?daddr=" + dest.latitude + "," + dest.longitude + "&dirflg=w");

        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        try {
            startActivity(mapIntent);
        } catch (ActivityNotFoundException e) {
            // Nếu chưa cài App Maps thì mở trình duyệt
            try {
                Intent unrestrictedIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                startActivity(unrestrictedIntent);
            } catch (ActivityNotFoundException innerE) {
                Toast.makeText(requireContext(), "Vui lòng cài đặt Google Maps", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
