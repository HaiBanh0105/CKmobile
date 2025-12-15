package com.example.banking;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.banking.Activity.FlightTicketBooking;
import com.example.banking.databinding.FragmentUtilitiesBinding;

public class UtilitiesFragment extends Fragment {

    // Khai báo một biến để lưu trữ instance của lớp binding
    private FragmentUtilitiesBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentUtilitiesBinding.inflate(inflater, container, false);

        View rootView = binding.getRoot();

        binding.btnFlightBooking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleBookingClick();
            }
        });

        binding.btnTopUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        return rootView;
    }

    private void handleBookingClick() {
        Intent intent = new Intent(getActivity(), FlightTicketBooking.class);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
