package com.example.banking.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.banking.Adapter.FlightAdapter;
import com.example.banking.R;
import com.example.banking.databinding.ActivitySearchFlightBinding;
import com.example.banking.model.Flight;
import com.example.banking.model.FlightLocation;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.banking.Adapter.FlightAdapter;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class SearchFlight extends AppCompatActivity {
    private ActivitySearchFlightBinding binding;
    private FirebaseFirestore db;
    private FlightAdapter adapter;
    private FlightLocation fromLocation, toLocation;
    private String departDate, returnDate, selectedClassSeat;
    private long departTs, returnTs;
    private boolean isRoundTrip;
    private int adult, child, infant;
    private Flight selectedFlight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivitySearchFlightBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        db = FirebaseFirestore.getInstance();
        getIntentData();
        binding.txtDepartLocation.setText(fromLocation.getCity());
        binding.txtArrivalLocation.setText(toLocation.getCity());
        setupBackButton();
        setupRecyclerView();
        loadFlights();
    }

    private void loadFlights() {

        if (fromLocation == null || toLocation == null || departTs <= 0) {
            Toast.makeText(this, "Thiếu dữ liệu tìm kiếm", Toast.LENGTH_SHORT).show();
            return;
        }

        Timestamp start = getStartOfDay(departTs);
        Timestamp end = getEndOfDay(departTs);

        Query departQuery = db.collection("Flights")
                .whereEqualTo("origin", fromLocation.getCode())
                .whereEqualTo("destination", toLocation.getCode())
                .whereGreaterThanOrEqualTo("departureTime", start)
                .whereLessThanOrEqualTo("departureTime", end);

        departQuery.get()
                .addOnSuccessListener(snapshot -> {
                    ArrayList<Flight> departFlights = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Flight flight = doc.toObject(Flight.class);
                        if (flight != null) {
                            flight.setId(doc.getId());
                            flight.setSelectedSeatClassKey(selectedClassSeat);
                            departFlights.add(flight);
                        }
                    }
                    adapter.setData(departFlights);

                    if (departFlights.isEmpty()) {
                        Toast.makeText(this, "Không có chuyến bay chiều đi", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải chuyến bay", Toast.LENGTH_SHORT).show()
                );


        // ===== Round trip =====
        if (isRoundTrip && returnTs > 0) {
            loadReturnFlights();
        }
    }


    private void loadReturnFlights() {

        Timestamp start = getStartOfDay(returnTs);
        Timestamp end = getEndOfDay(returnTs);

        FirebaseFirestore.getInstance()
                .collection("flights")
                .whereEqualTo("origin", toLocation.getCode())
                .whereEqualTo("destination", fromLocation.getCode())
                .whereGreaterThanOrEqualTo("departureTime", start)
                .whereLessThanOrEqualTo("departureTime", end)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Flight> returnFlights = snapshot.toObjects(Flight.class);
                    // TODO: show dialog / tab chọn chuyến về
                });
    }

    private Timestamp getStartOfDay(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return new Timestamp(cal.getTime());
    }

    private Timestamp getEndOfDay(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return new Timestamp(cal.getTime());
    }


    public void toast(String content) {
        Toast.makeText(this,
                content,
                Toast.LENGTH_SHORT).show();
    }

    private void getIntentData() {
        Intent intent = getIntent();

        fromLocation = (FlightLocation)
                intent.getSerializableExtra("FROM_LOCATION");

        toLocation = (FlightLocation)
                intent.getSerializableExtra("TO_LOCATION");

        departDate = intent.getStringExtra("DEPART_DATE");
        returnDate = intent.getStringExtra("RETURN_DATE");
        selectedClassSeat = intent.getStringExtra("CLASS_SEAT");

        adult = intent.getIntExtra("ADULT", 0);
        child = intent.getIntExtra("CHILD", 0);
        infant = intent.getIntExtra("INFANT", 0);

        departTs = getIntent().getLongExtra("DEPART_TS", -1);
        returnTs = getIntent().getLongExtra("RETURN_TS", -1);

        isRoundTrip = intent.getBooleanExtra("IS_ROUND_TRIP", false);
    }


    private void setupRecyclerView() {

        adapter = new FlightAdapter();

        adapter.setOnClick(flight -> {

            selectedFlight = flight;

            Intent intent =
                    new Intent(this, FlightConfirmation.class);

            // chuyến bay được chọn
            intent.putExtra("FLIGHT_ID", flight.getId());

            // hành khách
            intent.putExtra("ADULT", adult);
            intent.putExtra("CHILD", child);
            intent.putExtra("INFANT", infant);
            intent.putExtra("SEAT_CLASS", selectedClassSeat);

            // giữ lại thông tin hành trình (nên có)
            intent.putExtra("FROM_LOCATION", fromLocation);
            intent.putExtra("TO_LOCATION", toLocation);
            intent.putExtra("DEPART_TS", departTs);
            intent.putExtra("RETURN_TS", returnTs);
            intent.putExtra("IS_ROUND_TRIP", isRoundTrip);

            startActivity(intent);
        });

        binding.recyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );
        binding.recyclerView.setAdapter(adapter);
    }


    private void setupBackButton() {
        binding.btnBack.setOnClickListener(v -> finish());
    }
}