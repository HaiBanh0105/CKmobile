package com.example.banking;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ProfileFragment extends Fragment {
    private ImageView infor;
    private ImageView changePass;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_profile, container, false);

        infor = root.findViewById(R.id.btnInfor);
        changePass = root.findViewById(R.id.changePass);

        infor.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), edit_customer.class);
            intent.putExtra("role", "customer_update");
            startActivity(intent);
        });

        changePass.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), change_password.class);
            startActivity(intent);
        });

        return  root;
    }


}
