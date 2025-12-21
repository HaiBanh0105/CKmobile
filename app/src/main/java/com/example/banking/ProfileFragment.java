package com.example.banking;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ProfileFragment extends Fragment {
    private ImageView infor,changePass, changePin, btnUpdateAvatar;

    LinearLayout LnInfor, LnPassword, LnPin;

    MaterialButton logout;

    private TextView Username;

    private ShapeableImageView avt;

    String userId = SessionManager.getInstance().getUserId();

    String cachedUrl = SessionManager.getInstance().getAvatarUrl();
    private ActivityResultLauncher<Intent> imagePickerLauncher;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_profile, container, false);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            avt.setImageURI(imageUri); // hiển thị ảnh mới
                            uploadAvatarToFirebase(imageUri); // tải lên Firebase
                        }
                    }
                }
        );

        avt = root.findViewById(R.id.imgProfile);

        if (cachedUrl != null && !cachedUrl.isEmpty()) {
            Glide.with(requireContext())
                    .load(cachedUrl)
                    .placeholder(R.drawable.ic_person)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .skipMemoryCache(false)
                    .centerCrop()
                    .into(avt);
        }
        loadCustomerInfor(userId);

        btnUpdateAvatar = root.findViewById(R.id.btnUpdateAvatar);
        infor = root.findViewById(R.id.btnInfor);
        changePass = root.findViewById(R.id.changePass);
        changePin = root.findViewById(R.id.changePin);
        Username = root.findViewById(R.id.tvCustomerName);
        logout = root.findViewById(R.id.btnLogout);
        LnInfor = root.findViewById(R.id.itemPersonalInfo);
        LnPassword = root.findViewById(R.id.itemSecurity);
        LnPin = root.findViewById(R.id.itemChangepin);

        btnUpdateAvatar.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 1);
            } else {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }

            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });


        LnInfor.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), customer_infor.class);
            intent.putExtra("role", "customer_update");
            startActivity(intent);
        });

        infor.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), customer_infor.class);
            intent.putExtra("role", "customer_update");
            startActivity(intent);
        });

        LnPassword.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), change_password.class);
            startActivity(intent);
        });

        changePass.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), change_password.class);
            startActivity(intent);
        });

        LnPin.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), change_pin.class);
            startActivity(intent);
        });

        changePin.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), change_pin.class);
            startActivity(intent);
        });


        logout.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), login.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            requireActivity().finish();

        });

        return  root;
    }

    private void loadCustomerInfor(String userId) {
        FirestoreHelper helper = new FirestoreHelper();
        helper.loadCustomerInfor(userId, new FirestoreHelper.CustomerCallback() {
            @Override
            public void onSuccess(String name, String phone, String email, String address, String id, String avatarUrl) {
                Username.setText(name);

                SessionManager.getInstance().setAvatarUrl(avatarUrl);

                if (isAdded()) {
                    Glide.with(requireContext())
                            .load(avatarUrl)
                            .placeholder(R.drawable.ic_person)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .skipMemoryCache(false)
                            .centerCrop()
                            .into(avt);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }



    private void uploadAvatarToFirebase(Uri imageUri) {
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(getContext(), "User ID không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        StorageReference storageRef = FirebaseStorage.getInstance()
                .getReference("avatars/" + userId + ".jpg");

        Log.d("Upload", "userId=" + userId);
        Log.d("Upload", "imageUri=" + imageUri);

        storageRef.putFile(imageUri)
                .addOnProgressListener(snapshot -> {
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    Log.d("Upload", "Progress: " + progress + "%");
                })
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String avatarUrl = uri.toString();
                        updateAvatarInFirestore(avatarUrl);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("Upload", "Error: ", e);
                    Toast.makeText(getContext(), "Lỗi tải ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    private void updateAvatarInFirestore(String avatarUrl) {
        FirebaseFirestore.getInstance()
                .collection("Users")
                .document(userId)
                .update("avatar", avatarUrl)
                .addOnSuccessListener(unused -> {
                    SessionManager.getInstance().setAvatarUrl(avatarUrl);

                    if (isAdded()) {
                        Glide.with(requireContext())
                                .load(avatarUrl)
                                .placeholder(R.drawable.ic_person)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .skipMemoryCache(false)
                                .centerCrop()
                                .into(avt);

                        Toast.makeText(requireContext(), "Cập nhật ảnh đại diện thành công", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Lỗi cập nhật database: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }



}
