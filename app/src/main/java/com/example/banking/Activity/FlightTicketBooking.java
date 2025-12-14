package com.example.banking.Activity;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.banking.databinding.ActivityFlightTicketBookingBinding;
import com.example.banking.model.FlightLocation;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import android.app.DatePickerDialog;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;


public class FlightTicketBooking extends AppCompatActivity {

    private ActivityFlightTicketBookingBinding binding;
    private FirebaseFirestore db;
    private final Calendar departCalendar = Calendar.getInstance();
    private final Calendar returnCalendar = Calendar.getInstance();

    private final SimpleDateFormat vnDateFormat =
            new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN"));


    // Passenger
    private int adult = 1;
    private int child = 0;
    private int infant = 0;

    // Location
    private final List<FlightLocation> locations = new ArrayList<>();
    private FlightLocation fromLocation;
    private FlightLocation toLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityFlightTicketBookingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        initPassenger();
        loadLocations();
        initSearch();
        initRadioGroup();
        initCalendarPicker();
        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void initCalendarPicker() {

        // Ngày khởi hành
        binding.departureDate.setOnClickListener(v -> {
            showDatePicker(true);
        });

        // Ngày trở về
        binding.returnDate.setOnClickListener(v -> {
            if (!binding.radioRoundTrip.isChecked()) return;
            showDatePicker(false);
        });
    }

    private void showDatePicker(boolean isDeparture) {

        Calendar calendar = isDeparture ? departCalendar : returnCalendar;

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {

                    calendar.set(year, month, dayOfMonth);

                    if (isDeparture) {
                        binding.departureDate.setText(
                                vnDateFormat.format(calendar.getTime())
                        );

                        // Nếu là khứ hồi mà ngày về < ngày đi → reset
                        if (binding.radioRoundTrip.isChecked()
                                && returnCalendar.before(departCalendar)) {
                            binding.returnDate.setText("---");
                        }
                    } else {
                        if (calendar.before(departCalendar)) {
                            toast("Ngày trở về phải sau ngày khởi hành");
                            return;
                        }

                        binding.returnDate.setText(
                                vnDateFormat.format(calendar.getTime())
                        );
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        // Không cho chọn ngày trong quá khứ
        dialog.getDatePicker().setMinDate(System.currentTimeMillis());

        dialog.show();
    }

    public void toast(String content) {
        Toast.makeText(this,
                content,
                Toast.LENGTH_SHORT).show();
    }

    public void initRadioGroup() {
        binding.radioOneWay.setChecked(true);
        disableReturnDate();

        binding.radioOneWay.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                disableReturnDate();
            }
        });

        binding.radioRoundTrip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                enableReturnDate();
            }
        });
    }

    private void disableReturnDate() {
        binding.returnDate.setEnabled(false);
        binding.returnDate.setAlpha(0.4f);
    }

    private void enableReturnDate() {
        binding.returnDate.setEnabled(true);
        binding.returnDate.setAlpha(1f);
    }


    // ================= PASSENGER =================
    private void initPassenger() {
        updatePassengerUI();

        binding.btnAdultPlus.setOnClickListener(v -> {
            adult++;
            updatePassengerUI();
        });

        binding.btnAdultMinus.setOnClickListener(v -> {
            if (adult > 1) adult--;
            if (infant > adult) infant = adult;
            updatePassengerUI();
        });

        binding.btnChildPlus.setOnClickListener(v -> {
            child++;
            updatePassengerUI();
        });

        binding.btnChildMinus.setOnClickListener(v -> {
            if (child > 0) child--;
            updatePassengerUI();
        });

        binding.btnInfantPlus.setOnClickListener(v -> {
            if (infant < adult) {
                infant++;
                updatePassengerUI();
            } else {
                Toast.makeText(this,
                        "Em bé không vượt quá người lớn",
                        Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnInfantMinus.setOnClickListener(v -> {
            if (infant > 0) infant--;
            updatePassengerUI();
        });
    }

    private void updatePassengerUI() {
        binding.txtAdultCount.setText(String.valueOf(adult));
        binding.txtChildCount.setText(String.valueOf(child));
        binding.txtInfantCount.setText(String.valueOf(infant));
    }

    // ================= LOCATION =================
    private void loadLocations() {
        // 🔹 ĐANG LOAD
        binding.fromSpinner.setEnabled(false);
        binding.toSpinner.setEnabled(false);

        binding.fromProgress.setVisibility(View.VISIBLE);
        binding.toProgress.setVisibility(View.VISIBLE);

        db.collection("FlightLocations")
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(query -> {
                    locations.clear();
                    for (DocumentSnapshot doc : query) {
                        locations.add(doc.toObject(FlightLocation.class));
                    }

                    setupSpinners();

                    // 🔹 LOAD XONG
                    binding.fromProgress.setVisibility(View.GONE);
                    binding.toProgress.setVisibility(View.GONE);

                    binding.fromSpinner.setEnabled(true);
                    binding.toSpinner.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    binding.fromProgress.setVisibility(View.GONE);
                    binding.toProgress.setVisibility(View.GONE);
                    Toast.makeText(this, "Không tải được địa điểm", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupSpinners() {
        ArrayAdapter<FlightLocation> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                locations
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        binding.fromSpinner.setAdapter(adapter);
        binding.toSpinner.setAdapter(adapter);

        if (locations.size() > 1) {
            binding.fromSpinner.setSelection(0);
            binding.toSpinner.setSelection(1);
            fromLocation = locations.get(0);
            toLocation = locations.get(1);
        }

        binding.fromSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                fromLocation = locations.get(pos);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        binding.toSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                FlightLocation selected = locations.get(pos);

                if (fromLocation != null &&
                        selected.getCode().equals(fromLocation.getCode())) {

                    Toast.makeText(
                            FlightTicketBooking.this,
                            "Điểm đến không được trùng điểm đi",
                            Toast.LENGTH_SHORT
                    ).show();

                    // Quay về vị trí khác an toàn
                    binding.toSpinner.setSelection(
                            pos == 0 && locations.size() > 1 ? 1 : 0
                    );
                    return;
                }

                toLocation = selected;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void initSearch() {
        binding.btnSearchFlight.setOnClickListener(v -> {
            if (fromLocation == null || toLocation == null) {
                Toast.makeText(this,
                        "Vui lòng chọn điểm đi và điểm đến",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
