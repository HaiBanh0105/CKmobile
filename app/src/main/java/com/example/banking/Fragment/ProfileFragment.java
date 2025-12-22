package com.example.banking.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.banking.util.FirestoreHelper;
import com.example.banking.R;
import com.example.banking.model.SessionManager;
import com.example.banking.change_password;
import com.example.banking.change_pin;
import com.example.banking.customer_infor;
import com.example.banking.login;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

public class ProfileFragment extends Fragment {
    private ImageView infor,changePass, changePin;

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

        avt = root.findViewById(R.id.imgProfile);

        if (cachedUrl != null && !cachedUrl.isEmpty()) {
            Glide.with(requireContext())
                    .load(cachedUrl)
                    .placeholder(R.drawable.men)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .skipMemoryCache(false)
                    .centerCrop()
                    .into(avt);
        }
        loadCustomerInfor(userId);


        infor = root.findViewById(R.id.btnInfor);
        changePass = root.findViewById(R.id.changePass);
        changePin = root.findViewById(R.id.changePin);
        Username = root.findViewById(R.id.tvCustomerName);
        logout = root.findViewById(R.id.btnLogout);
        LnInfor = root.findViewById(R.id.itemPersonalInfo);
        LnPassword = root.findViewById(R.id.itemSecurity);
        LnPin = root.findViewById(R.id.itemChangepin);


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
                            .placeholder(R.drawable.men)
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







}
