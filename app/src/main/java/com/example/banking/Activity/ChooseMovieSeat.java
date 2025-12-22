package com.example.banking.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.banking.Adapter.SeatAdapter;
import com.example.banking.model.SessionManager;
import com.example.banking.databinding.ActivityChooseMovieSeatBinding;
import com.example.banking.model.AccountTransaction;
import com.example.banking.model.Cinema;
import com.example.banking.model.Movie;
import com.example.banking.model.Seat;
import com.example.banking.model.ServiceBooking;
import com.example.banking.model.Showtime;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChooseMovieSeat extends BaseSecureActivity {

    private ActivityChooseMovieSeatBinding binding;

    private Movie movie;
    private Cinema cinema;
    private String date, time;

    private Showtime showtime;
    private final List<Seat> seatList = new ArrayList<>();
    private SeatAdapter seatAdapter;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChooseMovieSeatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initLoading(binding.getRoot());
        db = FirebaseFirestore.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        getDataFromIntent();
        setupHeader();
        setupSeatRecyclerView();
        loadShowtimeFromFirestore();

        binding.btnBack.setOnClickListener(v -> finish());

        binding.confirmBtn.setOnClickListener(v -> {
            double total = getSelectedSeatCount() * showtime.getPrice();
            if (total <= 0) {
                toast("Vui lòng chọn ghế");
                return;
            }
            showLoading(true);
            checkBalanceAndProceed(total);
        });
    }

    // ===== Intent =====
    private void getDataFromIntent() {
        movie = (Movie) getIntent().getSerializableExtra("movie");
        cinema = (Cinema) getIntent().getSerializableExtra("cinema");
        date = getIntent().getStringExtra("date");
        time = getIntent().getStringExtra("time");
    }

    // ===== Header =====
    private void setupHeader() {
        binding.txtMovieTitle.setText(movie.getTitle());
        binding.txtCinemaName.setText(cinema.getName());
        binding.txtCinemaAddress.setText(cinema.getAddress());
        binding.txtDatetime.setText(date + " • " + time);
    }

    // ===== Seat Recycler =====
    private void setupSeatRecyclerView() {
        binding.seatMap.setLayoutManager(new GridLayoutManager(this, 6));

        seatAdapter = new SeatAdapter(seatList, position -> {
            Seat seat = seatList.get(position);
            if (seat.isBooked()) return;

            seat.setSelected(!seat.isSelected());
            seatAdapter.notifyItemChanged(position);
            updateSeatInfo();
        });

        binding.seatMap.setAdapter(seatAdapter);
    }

    // ===== Load Showtime =====
    private void loadShowtimeFromFirestore() {
        db.collection("Showtimes")
                .whereEqualTo("movieId", movie.getMovieId())
                .whereEqualTo("cinemaId", cinema.getCinemaId())
                .whereEqualTo("date", date)
                .whereEqualTo("time", time)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) return;
                    showtime = query.getDocuments().get(0).toObject(Showtime.class);
                    createSeatMap();
                });
    }

    // ===== Seat Map =====
    private void createSeatMap() {
        seatList.clear();

        List<String> bookedSeats = showtime.getBookedSeats();
        if (bookedSeats == null) bookedSeats = new ArrayList<>();

        char row = 'A';
        int rows = 7, cols = 6;

        for (int i = 0; i < rows; i++) {
            for (int j = 1; j <= cols; j++) {
                String seatId = row + String.valueOf(j);
                seatList.add(new Seat(seatId, bookedSeats.contains(seatId)));
            }
            row++;
        }
        seatAdapter.notifyDataSetChanged();
    }

    // ===== UI Update =====
    private void updateSeatInfo() {
        int count = 0;
        StringBuilder names = new StringBuilder();

        for (Seat s : seatList) {
            if (s.isSelected()) {
                count++;
                names.append(s.getId()).append(", ");
            }
        }

        if (count > 0) names.setLength(names.length() - 2);

        binding.txtSelectedNumber.setText("Đã chọn " + count + " ghế");
        binding.txtSelectedSeats.setText(count > 0 ? names.toString() : "---");
        binding.txtPrice.setText(formatVND(count * showtime.getPrice()));
    }

    private int getSelectedSeatCount() {
        int count = 0;
        for (Seat s : seatList) if (s.isSelected()) count++;
        return count;
    }

    private String formatVND(int amount) {
        return NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(amount);
    }

    // ===== BALANCE CHECK =====
    private void checkBalanceAndProceed(double totalAmount) {
        String userId = SessionManager.getInstance().getUserId();

        db.collection("Accounts")
                .whereEqualTo("user_id", userId)
                .whereEqualTo("account_type", "checking")
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        showLoading(false);
                        toast("Không tìm thấy tài khoản");
                        return;
                    }

                    Double balance = snapshot.getDocuments().get(0).getDouble("balance");
                    if (balance == null || balance < totalAmount) {
                        showLoading(false);
                        toast("Số dư không đủ");
                        return;
                    }

                    createPendingTransaction(totalAmount);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    toast("Lỗi kiểm tra số dư");
                });
    }

    // ===== TRANSACTION =====
    private void createPendingTransaction(double totalAmount) {
        String userId = SessionManager.getInstance().getUserId();
        String transactionId = db.collection("AccountTransactions").document().getId();

        AccountTransaction tx = new AccountTransaction();
        tx.setTransactionId(transactionId);
        tx.setUserId(userId);
        tx.setType("SERVICE");
        tx.setCategory("MOVIE");
        tx.setAmount(totalAmount);
        tx.setReceiverName(cinema.getName());
        tx.setDescription("Thanh toán vé xem phim - Phim: " + movie.getTitle());
        tx.setTimestamp(Timestamp.now());
        tx.setStatus("PENDING");
        tx.setBiometricRequired(totalAmount >= 5_000_000);

        db.collection("AccountTransactions")
                .document(transactionId)
                .set(tx)
                .addOnSuccessListener(unused -> {
                    createServiceBooking(transactionId, totalAmount);
                    showLoading(false);

                    Intent intent = new Intent(this, AccountTransactionActivity.class);
                    intent.putExtra("TRANSACTION_ID", transactionId);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    toast("Lỗi tạo giao dịch");
                });
    }

    // ===== BOOKING =====
    private void createServiceBooking(String transactionId, double totalAmount) {
        String bookingId = db.collection("ServiceBookings").document().getId();

        ServiceBooking booking = new ServiceBooking();
        booking.setBookingId(bookingId);
        booking.setUserId(SessionManager.getInstance().getUserId());
        booking.setServiceType("MOVIE");
        booking.setStatus("PENDING_PAYMENT");
        booking.setTotalAmount(totalAmount);
        booking.setBookingTime(Timestamp.now());
        booking.setTransactionId(transactionId);

        Map<String, Object> details = new HashMap<>();
        details.put("movieId", movie.getMovieId());
        details.put("movieTitle", movie.getTitle());
        details.put("cinemaId", cinema.getCinemaId());
        details.put("cinemaName", cinema.getName());
        details.put("showtimeId", showtime.getShowtimeId());
        details.put("date", date);
        details.put("time", time);
        details.put("seats", getSelectedSeats());
        details.put("pricePerSeat", showtime.getPrice());
        booking.setServiceDetails(details);

        db.collection("ServiceBookings")
                .document(bookingId)
                .set(booking);
    }

    private List<String> getSelectedSeats() {
        List<String> result = new ArrayList<>();
        for (Seat s : seatList) if (s.isSelected()) result.add(s.getId());
        return result;
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
