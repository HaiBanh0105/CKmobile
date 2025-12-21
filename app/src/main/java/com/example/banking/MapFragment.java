package com.example.banking;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
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

    LatLng myLatLng, selectedATM, lat;



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
                    lat = new LatLng(10.732163295241257, 106.69932033795564); // địa chỉ ĐH TDT
                    suggestNearestATM(myLatLng);

                } else {
                    Log.e("MapFragment", "Location is null. Check Emulator settings.");
                }
            });
        }
    }



    private void suggestNearestATM(LatLng myLatLng) {
        if (mMap == null || myLatLng == null) return;

        // Danh sách địa chỉ thật quanh Tôn Đức Thắng
        String[] addresses = {
                "1058 Nguyễn Văn Linh, Tân Phong, Quận 7",   // SC VivoCity
                "101 Tôn Dật Tiên, Tân Phú, Quận 7",        // Crescent Mall
                "469 Nguyễn Hữu Thọ, Tân Hưng, Quận 7",     // Lotte Mart
                "2-4 Đường số 9, Tân Hưng, Quận 7",         // Artinus 3D Gallery
                "Nguyễn Lương Bằng, Phú Mỹ Hưng, Quận 7",   // Sakura Park
                "63 Nguyễn Thị Thập, Tân Phú, Quận 7",      // Jump Arena
                "2-4 Phú Mỹ Hưng, Quận 7",                  // Vietopia
                "Cầu Ánh Sao, Tôn Dật Tiên, Quận 7",        // Cầu Ánh Sao
                "Cầu Phú Mỹ, Nguyễn Văn Linh, Quận 7",      // Cầu Phú Mỹ
                "107 Khánh Hội, phường 3, Quận 4"      // Đại học Tôn Đức Thắng
        };

        // Tọa độ tương ứng
        LatLng[] atms = {
                new LatLng(10.730558873096253, 106.70337445648383), // SC VivoCity
                new LatLng(10.728735473977917, 106.71874679562649), // Crescent Mall
                new LatLng(10.741271082812466, 106.70181464112719), // Lotte Mart
                new LatLng(10.743239179277857, 106.69493995396807), // Artinus 3D Gallery
                new LatLng(10.722016583520034, 106.72644165467958), // Sakura Park
                new LatLng(10.73765617609127, 106.72744323168207), // Jump Arena
                new LatLng(10.71665363898424, 106.73155272201866), // Vietopia
                new LatLng(10.725161114873131, 106.7198589220605), // Cầu Ánh Sao
                new LatLng(10.732949462537649, 106.72047401154215), // Cầu Phú Mỹ
                new LatLng(10.755670833566942, 106.7015042783351)
        };

        int nearestIndex = -1;
        float minDistance = Float.MAX_VALUE;
        float[] results = new float[1];

// Bước 1: tìm ATM gần nhất
        for (int i = 0; i < atms.length; i++) {
            Location.distanceBetween(myLatLng.latitude, myLatLng.longitude,
                    atms[i].latitude, atms[i].longitude, results);

            if (results[0] < minDistance) {
                minDistance = results[0];
                nearestIndex = i;
            }
        }

    // Bước 2: thêm tất cả ATM, chỉ ATM gần nhất mới màu xanh
        for (int i = 0; i < atms.length; i++) {
            float hue = (i == nearestIndex)
                    ? BitmapDescriptorFactory.HUE_GREEN
                    : BitmapDescriptorFactory.HUE_RED;

            mMap.addMarker(new MarkerOptions()
                    .position(atms[i])
                    .title("West Bank")
                    .snippet(addresses[i])
                    .icon(BitmapDescriptorFactory.defaultMarker(hue)));

            Log.d("ATM_LOG", "ATM #" + (i+1) + " | " + addresses[i]
                    + " | Lat: " + atms[i].latitude
                    + ", Lng: " + atms[i].longitude);
        }

    // Làm nổi bật ATM gần nhất trên UI
        if (nearestIndex != -1) {
            LatLng nearestATM = atms[nearestIndex];
            String nearestAddress = addresses[nearestIndex];

            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(nearestATM, 15));

            tvBranchName.setText("ATM gần nhất");
            tvBranchAddress.setText(nearestAddress);
            cardLocationDetails.setVisibility(View.VISIBLE);

            selectedATM = nearestATM;
        }

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
