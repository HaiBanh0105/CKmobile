package com.example.banking.Activity;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ScrollView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.banking.Adapter.CinemaAdapter;
import com.example.banking.Adapter.ShowtimeDateAdapter;
import com.example.banking.Adapter.ShowtimeTimeAdapter;
import com.example.banking.Adapter.ActorAdapter;
import com.example.banking.Adapter.CategoryAdapter;
import com.example.banking.databinding.ActivityMovieBookingDetailBinding;
import com.example.banking.model.Cinema;
import com.example.banking.model.Movie;
import com.example.banking.model.ShowtimeDate;
import com.example.banking.model.ShowtimeTime;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import eightbitlab.com.blurview.RenderScriptBlur;

public class MovieBookingDetail extends AppCompatActivity {

    private ActivityMovieBookingDetailBinding binding;
    private Movie movie;
    private List<Cinema> cinemaList = new ArrayList<>();
    private List<ShowtimeDate> dateList = new ArrayList<>();
    private Cinema selectedCinema;
    private ShowtimeDateAdapter DateAdapter;
    private ShowtimeTimeAdapter TimeAdapter;
    ShowtimeDate selectedDate;
    ShowtimeTime selectedTime;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMovieBookingDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        movie = (Movie) getIntent().getSerializableExtra("movie");

        if (movie != null) {
            setupBlurEffect();
            setMovieData();
        }

        loadCinemasFromFirestore();
        setupDateRecyclerView();

        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void setMovieData() {
        binding.txtTitle.setText(movie.getTitle());
        binding.txtYear.setText(String.valueOf(movie.getYear()));
        binding.txtDuration.setText(movie.getTime());
        binding.txtDescription.setText(movie.getDescription());

        Glide.with(this)
                .load(movie.getPoster())
                .centerCrop()
                .into(binding.imgMovie);

        if (movie.getGenre() != null && !movie.getGenre().isEmpty()) {
            binding.listCategory.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            binding.listCategory.setAdapter(new CategoryAdapter(movie.getGenre()));
        }

        if (movie.getCasts() != null && !movie.getCasts().isEmpty()) {
            binding.listActor.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            binding.listActor.setAdapter(new ActorAdapter(movie.getCasts()));
        }
    }

    private void setupBlurEffect() {
        float radius = 10f;
        ViewGroup rootView = (ViewGroup) binding.getRoot();
        Drawable windowBackground = getWindow().getDecorView().getBackground();

        binding.blurView
                .setupWith(rootView)
                .setBlurAlgorithm(new RenderScriptBlur(this))
                .setFrameClearDrawable(windowBackground)
                .setBlurRadius(radius);

        binding.blurView.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        binding.blurView.setClipToOutline(true);
    }

    private void setupDateRecyclerView() {
        dateList = getNext7Days();

        binding.rvDate.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );
        DateAdapter = new ShowtimeDateAdapter(dateList, position -> {
            // Cập nhật trạng thái chọn
            for (ShowtimeDate d : dateList) d.setSelected(false);
            dateList.get(position).setSelected(true);
            DateAdapter.notifyDataSetChanged();

            // Cập nhật txtDate
            ShowtimeDate selectedDate = dateList.get(position);
            binding.txtDate.setText("Ngày chiếu: " + selectedDate.getFullDate());

            // Load showtimes nếu đã chọn cinema
            if (selectedCinema != null) {
                loadShowtimesFromFirestore(selectedCinema.getCinemaId(), selectedDate.getFullDate());
            }
        });

        binding.rvDate.setAdapter(DateAdapter);

        // Mặc định chọn ngày đầu tiên
        binding.txtDate.setText("Ngày chiếu: " + dateList.get(0).getFullDate());
    }

    private List<ShowtimeDate> getNext7Days() {
        List<ShowtimeDate> dates = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();

        SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
        SimpleDateFormat fullFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat dayFormat = new SimpleDateFormat("E", Locale.getDefault());

        for (int i = 0; i < 7; i++) {
            String display = displayFormat.format(calendar.getTime());
            String full = fullFormat.format(calendar.getTime());
            String day = dayFormat.format(calendar.getTime());
            dates.add(new ShowtimeDate(display, full, day));
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        dates.get(0).setSelected(true); // mặc định ngày đầu tiên chọn
        return dates;
    }

    private void loadCinemasFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Cinemas")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    cinemaList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Cinema c = doc.toObject(Cinema.class);
                        cinemaList.add(c);
                    }
                    setupCinemaRecyclerView();
                });
    }

    private void setupCinemaRecyclerView() {
        binding.rvCinema.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );

        selectedCinema = cinemaList.get(0);

        // Cập nhật UI rạp
        binding.txtCinemaName.setText(selectedCinema.getName());
        binding.txtCinemaAddress.setText(selectedCinema.getAddress());

        // Lấy ngày đang được chọn (mặc định là ngày đầu)
        for (ShowtimeDate d : dateList) {
            if (d.isSelected()) {
                selectedDate = d;
                break;
            }
        }

        if (selectedDate != null) {
            loadShowtimesFromFirestore(
                    selectedCinema.getCinemaId(),
                    selectedDate.getFullDate()
            );
        }

        CinemaAdapter adapter = new CinemaAdapter(cinemaList, position -> {
            selectedCinema = cinemaList.get(position);

            binding.txtCinemaName.setText(selectedCinema.getName());
            binding.txtCinemaAddress.setText(selectedCinema.getAddress());

            for (ShowtimeDate d : dateList) {
                if (d.isSelected()) {
                    selectedDate = d;
                    break;
                }
            }

            if (selectedDate != null) {
                loadShowtimesFromFirestore(
                        selectedCinema.getCinemaId(),
                        selectedDate.getFullDate()
                );
            }
        });

        binding.rvCinema.setAdapter(adapter);
    }


    private void loadShowtimesFromFirestore(String cinemaId, String date) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Showtimes")
                .whereEqualTo("movieId", movie.getMovieId())
                .whereEqualTo("cinemaId", cinemaId)
                .whereEqualTo("date", date)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    List<ShowtimeTime> timeItems = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String time = doc.getString("time");
                        timeItems.add(new ShowtimeTime(time));
                    }

                    if (timeItems.isEmpty()) {
                        binding.rvTime.setVisibility(View.GONE);
                        binding.txtChooseTime.setText("Không có xuất chiếu");
                        return;
                    }

                    binding.rvTime.setVisibility(View.VISIBLE);
                    binding.txtChooseTime.setText("Chọn giờ chiếu");

                    LinearLayoutManager layoutManager =
                            new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
                    binding.rvTime.setLayoutManager(layoutManager);

                    // Mặc định chọn giờ đầu tiên
                    timeItems.get(0).setSelected(true);
                    selectedTime = timeItems.get(0);

                    TimeAdapter =
                            new ShowtimeTimeAdapter(timeItems, position -> {
                                for (ShowtimeTime t : timeItems) t.setSelected(false);
                                timeItems.get(position).setSelected(true);
                                selectedTime = timeItems.get(position);
                                TimeAdapter.notifyDataSetChanged();
                            });

                    binding.rvTime.setAdapter(TimeAdapter);

                    // Scroll mượt xuống khu vực giờ chiếu
                    scrollToView(binding.rvTime);
                })
                .addOnFailureListener(e -> {
                    binding.rvTime.setVisibility(View.GONE);
                    binding.txtChooseTime.setText("Không có xuất chiếu");
                });
    }

    private void scrollToView(View target) {
        binding.scrollView.post(() ->
                binding.scrollView.smoothScrollTo(0, target.getTop())
        );
    }
}
