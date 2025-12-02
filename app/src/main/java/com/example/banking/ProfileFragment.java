package com.example.banking;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

public class ProfileFragment extends Fragment {
    private ImageView infor,changePass;

    MaterialButton logout;

    private TextView Username;

    private ShapeableImageView avt;

    String userId = SessionManager.getInstance().getUserId();


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_profile, container, false);

        loadCustomerInfor(userId);

        infor = root.findViewById(R.id.btnInfor);
        changePass = root.findViewById(R.id.changePass);
        Username = root.findViewById(R.id.tvCustomerName);
        logout = root.findViewById(R.id.btnLogout);
        avt = root.findViewById(R.id.imgProfile);

        infor.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), customer_infor.class);
            intent.putExtra("role", "customer_update");
            startActivity(intent);
        });

        changePass.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), change_password.class);
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
            public void onSuccess(String name, String phone, String email, String address, String id) {
                Username.setText(name);
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });

    }


}
