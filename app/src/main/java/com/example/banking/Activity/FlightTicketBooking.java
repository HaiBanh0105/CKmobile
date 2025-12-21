package com.example.banking.Activity;

import android.content.Intent;
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
import com.example.banking.util.ClickEffectUtil;
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
    private String selectedClassSeat;

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
        initCalendarPicker();
        initClassSeat();
        setupSpinnerListeners();
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSearchFlight.setOnClickListener(v -> {

            // 1️⃣ Kiểm tra location
            if (fromLocation == null || toLocation == null) {
                toast("Vui lòng chọn điểm đi và điểm đến");
                return;
            }

            if (fromLocation.getCode().equals(toLocation.getCode())) {
                toast("Điểm đi và điểm đến không được trùng nhau");
                return;
            }

            // 2️⃣ Kiểm tra ngày đi
            if (binding.departureDate.getText().toString().equals("--/--/----")) {
                toast("Vui lòng chọn ngày khởi hành");
                return;
            }

            Intent intent = new Intent(this, SearchFlight.class);

            intent.putExtra("FROM_LOCATION", fromLocation);
            intent.putExtra("TO_LOCATION", toLocation);

            intent.putExtra("ADULT", adult);
            intent.putExtra("CHILD", child);
            intent.putExtra("INFANT", infant);

            intent.putExtra("DEPART_DATE",
                    binding.departureDate.getText().toString());

            intent.putExtra("CLASS_SEAT", selectedClassSeat);

            intent.putExtra("DEPART_TS", departCalendar.getTimeInMillis());
            startActivity(intent);
        });
    }

    private void initClickEffect() {
        ClickEffectUtil.apply(binding.btnSearchFlight);
        ClickEffectUtil.apply(binding.btnAdultPlus);
        ClickEffectUtil.apply(binding.btnAdultMinus);
        ClickEffectUtil.apply(binding.btnChildMinus);
        ClickEffectUtil.apply(binding.btnChildPlus);
        ClickEffectUtil.apply(binding.btnInfantMinus);
        ClickEffectUtil.apply(binding.btnInfantPlus);
        ClickEffectUtil.apply(binding.linearDepDate);
    }

    private void initCalendarPicker() {

        // Ngày khởi hành
        binding.linearDepDate.setOnClickListener(v -> {
            showDatePicker(true);
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

                    } else {
                        if (calendar.before(departCalendar)) {
                            toast("Ngày trở về phải sau ngày khởi hành");
                            return;
                        }
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

    private void initClassSeat() {
        // 🔹 ĐANG LOAD (giữ lại cho đồng bộ UI)
        binding.classSpinner.setEnabled(false);
        binding.classProgress.setVisibility(View.VISIBLE);

        // Dữ liệu cố định
        List<String> classSeats = new ArrayList<>();
        classSeats.add("ECONOMY");
        classSeats.add("BUSINESS");

        setupClassSeatSpinner(classSeats);

        // 🔹 LOAD XONG
        binding.classProgress.setVisibility(View.GONE);
        binding.classSpinner.setEnabled(true);
    }

    private void setupClassSeatSpinner(List<String> classSeats) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                classSeats
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        binding.classSpinner.setAdapter(adapter);

        // Mặc định chọn Economy
        binding.classSpinner.setSelection(0);
        selectedClassSeat = classSeats.get(0);

        binding.classSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                selectedClassSeat = classSeats.get(pos);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
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
        setLoadingState(true);

        db.collection("FlightLocations")
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(query -> {
                    locations.clear();
                    for (DocumentSnapshot doc : query) {
                        FlightLocation loc = doc.toObject(FlightLocation.class);
                        if (loc != null) locations.add(loc);
                    }

                    // Kiểm tra dữ liệu trước khi truy cập index 0, 1
                    if (locations.size() >= 2) {
                        setupSpinners();

                        // Khởi tạo giá trị mặc định
                        fromLocation = locations.get(0);
                        toLocation = locations.get(1);

                        // Cập nhật vị trí chọn trên Spinner (nếu cần)
                        binding.fromSpinner.setSelection(0);
                        binding.toSpinner.setSelection(1);
                    } else if (!locations.isEmpty()) {
                        setupSpinners();
                        fromLocation = locations.get(0);
                        binding.fromSpinner.setSelection(0);
                    }

                    setLoadingState(false);
                })
                .addOnFailureListener(e -> {
                    setLoadingState(false);
                    Toast.makeText(this, "Lỗi kết nối: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Hàm phụ để code sạch hơn (Clean Code)
    private void setLoadingState(boolean isLoading) {
        int visibility = isLoading ? View.VISIBLE : View.GONE;
        binding.fromProgress.setVisibility(visibility);
        binding.toProgress.setVisibility(visibility);

        binding.fromSpinner.setEnabled(!isLoading);
        binding.toSpinner.setEnabled(!isLoading);
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
    }

    private void setupSpinnerListeners() {
        // 1. Xử lý cho Điểm đi (fromSpinner)
        binding.fromSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                FlightLocation selected = locations.get(pos);

                // Kiểm tra trùng với Điểm đến
                if (toLocation != null && selected.getCode().equals(toLocation.getCode())) {
                    showErrorToast();
                    int oldPos = locations.indexOf(fromLocation);
                    binding.fromSpinner.setSelection(oldPos != -1 ? oldPos : (pos == 0 ? 1 : 0));
                    return;
                }
                fromLocation = selected;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 2. Xử lý cho Điểm đến (toSpinner)
        binding.toSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                FlightLocation selected = locations.get(pos);

                // Kiểm tra trùng với Điểm đi
                if (fromLocation != null && selected.getCode().equals(fromLocation.getCode())) {
                    showErrorToast();
                    int oldPos = locations.indexOf(toLocation);
                    binding.toSpinner.setSelection(oldPos != -1 ? oldPos : (pos == 0 ? 1 : 0));
                    return;
                }
                toLocation = selected;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // Hàm phụ phải nằm ngoài hàm setupSpinnerListeners
    private void showErrorToast() {
        Toast.makeText(this, "Điểm đi và điểm đến không được trùng nhau", Toast.LENGTH_SHORT).show();
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
