package com.example.banking.Activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.banking.Adapter.SeatAdapter;
import com.example.banking.databinding.ActivityChooseMovieSeatBinding;
import com.example.banking.model.Cinema;
import com.example.banking.model.Movie;
import com.example.banking.model.Seat;
import com.example.banking.model.Showtime;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.text.NumberFormat;
import java.util.Locale;

public class ChooseMovieSeat extends AppCompatActivity {

    private ActivityChooseMovieSeatBinding binding;

    private Movie movie;
    private Cinema cinema;
    private String date, time;

    private Showtime showtime;
    private List<Seat> seatList = new ArrayList<>();
    private SeatAdapter seatAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChooseMovieSeatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getDataFromIntent();
        setupHeader();
        setupSeatRecyclerView();
        loadShowtimeFromFirestore();

        binding.btnBack.setOnClickListener(v -> finish());
    }

    // ===== Nhận dữ liệu =====
    private void getDataFromIntent() {
        movie = (Movie) getIntent().getSerializableExtra("movie");
        cinema = (Cinema) getIntent().getSerializableExtra("cinema");
        date = getIntent().getStringExtra("date");
        time = getIntent().getStringExtra("time");
    }

    // ===== Header =====
    private void setupHeader() {
        binding.txtMovieTitle.setText(movie.getTitle());
        binding.txtCinemaName.setText(
                cinema.getName()
        );
        binding.txtCinemaAddress.setText(
                cinema.getAddress()
        );
        binding.txtDatetime.setText(date + " • " + time);
    }

    // ===== RecyclerView ghế =====
    private void setupSeatRecyclerView() {
        binding.seatMap.setLayoutManager(new GridLayoutManager(this, 6));

        seatAdapter = new SeatAdapter(seatList, position -> {
            Seat seat = seatList.get(position);
            seat.setSelected(!seat.isSelected());
            seatAdapter.notifyItemChanged(position);
            updateSeatInfo();
        });

        binding.seatMap.setAdapter(seatAdapter);
    }

    // ===== Load Showtime =====
    private void loadShowtimeFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Showtimes")
                .whereEqualTo("movieId", movie.getMovieId())
                .whereEqualTo("cinemaId", cinema.getCinemaId())
                .whereEqualTo("date", date)
                .whereEqualTo("time", time)
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) return;

                    showtime = query.getDocuments().get(0).toObject(Showtime.class);
                    createSeatMap(showtime);
                });
    }

    // ===== Tạo seatMap =====
    private void createSeatMap(Showtime showtime) {
        seatList.clear();

        List<String> bookedSeats = showtime.getBookedSeats();
        if (bookedSeats == null) bookedSeats = new ArrayList<>();

        char row = 'A';
        int rows = 7;
        int cols = 6;

        for (int i = 0; i < rows; i++) {
            for (int j = 1; j <= cols; j++) {
                String seatId = row + String.valueOf(j);
                boolean isBooked = bookedSeats.contains(seatId);
                seatList.add(new Seat(seatId, isBooked));
            }
            row++;
        }

        seatAdapter.notifyDataSetChanged();
    }

    // ===== Cập nhật số ghế & tiền =====
    private void updateSeatInfo() {
        int count = 0;
        int totalPrice = 0;
        StringBuilder seatNames = new StringBuilder();

        for (Seat s : seatList) {
            if (s.isSelected()) {
                count++;
                seatNames.append(s.getId()).append(", ");
                if (count % 5 == 0) {
                    seatNames.append("\n");
                }
            }
        }

        if (count > 0) {
            seatNames.setLength(seatNames.length() - 2);
            totalPrice = count * showtime.getPrice();
        }

        binding.txtSelectedNumber.setText("Đã chọn " + count + " ghế");

        binding.txtSelectedSeats.setText(
                count > 0 ? seatNames.toString() : "---"
        );

        binding.txtPrice.setText(formatVND(totalPrice));
    }

    private String formatVND(int amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(
                new Locale("vi", "VN")
        );
        return formatter.format(amount);
    }


}
