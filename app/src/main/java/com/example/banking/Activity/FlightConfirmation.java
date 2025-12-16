package com.example.banking.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.banking.R;
import com.example.banking.SessionManager;
import com.example.banking.databinding.ActivityFlightConfirmationBinding;
import com.example.banking.model.Flight;
import com.example.banking.model.Passenger;
import com.example.banking.util.SimpleTextWatcher;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FlightConfirmation extends AppCompatActivity {

    private ActivityFlightConfirmationBinding binding;
    private FirebaseFirestore db;
    private Flight selectedFlight;

    private int adult, child, infant;

    // 🔹 DANH SÁCH HÀNH KHÁCH
    private final List<Passenger> passengerList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityFlightConfirmationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        getIntentData();
        setupActions();
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    // ================= INTENT =================
    private void getIntentData() {
        Intent intent = getIntent();

        adult = intent.getIntExtra("ADULT", 1);
        child = intent.getIntExtra("CHILD", 0);
        infant = intent.getIntExtra("INFANT", 0);

        String flightId = intent.getStringExtra("FLIGHT_ID");
        String seatClassKey = intent.getStringExtra("SEAT_CLASS");

        db.collection("Flights")
                .document(flightId)
                .get()
                .addOnSuccessListener(doc -> {
                    selectedFlight = doc.toObject(Flight.class);
                    if (selectedFlight != null) {
                        selectedFlight.setId(doc.getId());
                        selectedFlight.setSelectedSeatClassKey(seatClassKey);
                        bindFlightData();
                        bindPriceDetail();
                        addPassengers();
                    }
                });
    }

    // ================= UI =================
    private void bindFlightData() {
        binding.txtAirlineName.setText(selectedFlight.getAirline());
        binding.txtFlightNumber.setText(selectedFlight.getFlightNumber());

        binding.txtDepDate.setText(formatDate(selectedFlight.getDepartureTime()));
        binding.txtDepTimeAndCode.setText(
                selectedFlight.getOrigin() + " - " + formatTime(selectedFlight.getDepartureTime())
        );

        binding.txtArrDate.setText(formatDate(selectedFlight.getArrivalTime()));
        binding.txtArrTimeAndCode.setText(
                selectedFlight.getDestination() + " - " + formatTime(selectedFlight.getArrivalTime())
        );

        binding.txtClass.setText("Hạng: " + selectedFlight.getSelectedSeatClassKey());
        binding.txtFinalAmount.setText(formatPrice(calcTotalPrice()));
    }

    private void setupActions() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnAutoFill.setOnClickListener(v -> {
            SessionManager session = SessionManager.getInstance();

            if (session.getPhone() != null)
                binding.edtPhone.setText(session.getPhone());

            if (session.getEmail() != null)
                binding.edtEmail.setText(session.getEmail());

        });

        binding.btnConfirmPayment.setOnClickListener(v -> {
            for (Passenger p : passengerList) {
                if (p.fullName == null || p.fullName.isEmpty()) {
                    toast("Vui lòng nhập đủ thông tin cho " + p.title);
                    return;
                }
            }

            // 👉 passengerList đã đầy đủ
            toast("Dữ liệu hợp lệ, sẵn sàng thanh toán");
        });
    }

    // ================= PASSENGERS =================
    private void addPassengers() {
        for (int i = 0; i < adult; i++)
            addPassengerView("Người lớn " + (i + 1));

        for (int i = 0; i < child; i++)
            addPassengerView("Trẻ em " + (i + 1));

        for (int i = 0; i < infant; i++)
            addPassengerView("Em bé " + (i + 1));
    }

    private void addPassengerView(String title) {
        Passenger passenger = new Passenger(title);
        passengerList.add(passenger);

        View view = LayoutInflater.from(this)
                .inflate(R.layout.item_passenger, binding.passengerContainer, false);

        TextView txtTitle = view.findViewById(R.id.txtPassengerTitle);
        LinearLayout header = view.findViewById(R.id.layoutHeader);
        LinearLayout body = view.findViewById(R.id.layoutBody);
        ImageView toggle = view.findViewById(R.id.imgToggle);

        TextInputEditText edtName = view.findViewById(R.id.edtFullName);
        TextInputEditText edtId = view.findViewById(R.id.edtIdCard);
        TextInputEditText edtDob = view.findViewById(R.id.edtDob);

        txtTitle.setText(title);

        header.setOnClickListener(v -> {
            boolean expand = body.getVisibility() == View.GONE;
            body.setVisibility(expand ? View.VISIBLE : View.GONE);
            toggle.setRotation(expand ? 180 : 0);
        });

        // 🔹 LƯU DATA
        edtName.addTextChangedListener(SimpleTextWatcher.after(s -> passenger.fullName = s));
        edtId.addTextChangedListener(SimpleTextWatcher.after(s -> passenger.idCard = s));

        edtDob.setOnClickListener(v ->
                showMaterialDatePicker(date -> {
                    passenger.dob = date;
                    edtDob.setText(date);
                })
        );

        binding.passengerContainer.addView(view);
    }

    // ================= DATE PICKER =================
    private void showMaterialDatePicker(OnDateSelected callback) {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Chọn ngày sinh")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        picker.addOnPositiveButtonClickListener(ms -> {
            String date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    .format(new Date(ms));
            callback.onSelect(date);
        });

        picker.show(getSupportFragmentManager(), "DOB_PICKER");
    }

    interface OnDateSelected {
        void onSelect(String date);
    }

    // ================= HELPERS =================
    private String formatDate(Timestamp ts) {
        return ts == null ? "--/--/----" :
                new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(ts.toDate());
    }

    private String formatTime(Timestamp ts) {
        return ts == null ? "--:--" :
                new SimpleDateFormat("HH:mm", Locale.getDefault()).format(ts.toDate());
    }

    private double calcTotalPrice() {
        double base = selectedFlight.getSelectedSeatClass().get("price");
        return adult * base + child * base * 0.75 + infant * base * 0.1;
    }

    private String formatPrice(double price) {
        return NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(price);
    }

    private void bindPriceDetail() {
        var seat = selectedFlight.getSelectedSeatClass();
        if (seat == null || !seat.containsKey("price"))
            return; double base = seat.get("price");
        if (adult > 0) {
            binding.txtAdultPrice.setText( "Người lớn (" + adult + "): " + formatPrice(base) + " x " + adult );
        } else {
            binding.txtAdultPrice.setVisibility(View.GONE);
        } if (child > 0) {
            binding.txtChildPrice.setText( "Trẻ em (" + child + "): " + formatPrice(base * 0.75) + " x " + child );
        } else {
            binding.txtChildPrice.setVisibility(View.GONE);
        }
        if (infant > 0) { binding.txtInfantPrice.setText( "Em bé (" + infant + "): " + formatPrice(base * 0.1) + " x " + infant );
        } else {
            binding.txtInfantPrice.setVisibility(View.GONE);
        }
    }
}
