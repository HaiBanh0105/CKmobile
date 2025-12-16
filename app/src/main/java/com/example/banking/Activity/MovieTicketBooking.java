package com.example.banking.Activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewpager2.widget.ViewPager2;

import com.example.banking.Adapter.BannerAdapter;
import com.example.banking.Adapter.MovieAdapter;
import com.example.banking.R;
import com.example.banking.databinding.ActivityMovieTicketBookingBinding;
import com.example.banking.model.Banner;
import com.example.banking.model.Movie;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.List;

public class MovieTicketBooking extends AppCompatActivity {

    private ActivityMovieTicketBookingBinding binding;
    private FirebaseStorage storage;
    private FirebaseFirestore firestore;

    /* ================= MOVIES ================= */
    private MovieAdapter movieAdapter;
    private List<Movie> movieList;

    /* ================= BANNERS ================= */
    private BannerAdapter bannerAdapter;
    private List<Banner> bannerOriginList;     // list gốc
    private List<Banner> bannerInfiniteList;   // list nhân 3

    private final Handler sliderHandler = new Handler(Looper.getMainLooper());

    private final Runnable sliderRunnable = () -> {
        int next = binding.bannerPager.getCurrentItem() + 1;
        binding.bannerPager.setCurrentItem(next, true);
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMovieTicketBookingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    systemBars.bottom
            );
            return insets;
        });

        storage = FirebaseStorage.getInstance();
        firestore = FirebaseFirestore.getInstance();

        setupActions();
        setupRecyclerView();
        setupBannerPager();

        loadMovies();
        loadBanners();
    }

    /* ================= ACTIONS ================= */

    private void setupActions() {
        binding.btnBack.setOnClickListener(v -> finish());
    }

    /* ================= MOVIE LIST ================= */

    private void setupRecyclerView() {
        movieList = new ArrayList<>();
        movieAdapter = new MovieAdapter(this, movieList);

        binding.movieList.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        );
        binding.movieList.setAdapter(movieAdapter);
        binding.movieList.setHasFixedSize(true);
    }

    private void loadMovies() {
        firestore.collection("Movies")
                .get()
                .addOnSuccessListener(query -> {

                    if (isFinishing() || isDestroyed()) return;

                    movieList.clear();

                    for (DocumentSnapshot doc : query.getDocuments()) {
                        Movie movie = doc.toObject(Movie.class);
                        if (movie != null) {
                            movieList.add(movie);
                        }
                    }

                    movieAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(Throwable::printStackTrace);
    }

    /* ================= BANNER SLIDER ================= */

    private void setupBannerPager() {
        bannerOriginList = new ArrayList<>();
        bannerInfiniteList = new ArrayList<>();

        bannerAdapter = new BannerAdapter(this, bannerInfiniteList);
        binding.bannerPager.setAdapter(bannerAdapter);

        binding.bannerPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        binding.bannerPager.setOffscreenPageLimit(3);

        // 1 lớn – 2 nhỏ
        binding.bannerPager.setPageTransformer((page, position) -> {
            float scale = 0.88f;
            float alpha = 0.7f;

            page.setScaleX(scale + (1 - Math.abs(position)) * 0.12f);
            page.setScaleY(scale + (1 - Math.abs(position)) * 0.12f);
            page.setAlpha(alpha + (1 - Math.abs(position)) * 0.3f);
        });

        setupInfiniteScroll();
        setupAutoSlide();
    }

    private void loadBanners() {
        binding.progressBar2.setVisibility(View.VISIBLE);

        firestore.collection("Banners")
                .get()
                .addOnSuccessListener(query -> {

                    bannerOriginList.clear();

                    for (DocumentSnapshot doc : query.getDocuments()) {
                        Banner banner = doc.toObject(Banner.class);
                        if (banner != null) {
                            bannerOriginList.add(banner);
                        }
                    }

                    setupInfiniteData();
                    binding.progressBar2.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    binding.progressBar2.setVisibility(View.GONE);
                    e.printStackTrace();
                });
    }

    /* ================= INFINITE LOGIC ================= */

    private void setupInfiniteData() {
        bannerInfiniteList.clear();

        if (bannerOriginList.size() < 2) {
            bannerInfiniteList.addAll(bannerOriginList);
        } else {
            bannerInfiniteList.addAll(bannerOriginList);
            bannerInfiniteList.addAll(bannerOriginList);
            bannerInfiniteList.addAll(bannerOriginList);
        }

        bannerAdapter.notifyDataSetChanged();

        int middle = bannerInfiniteList.size() / 3;
        binding.bannerPager.setCurrentItem(middle, false);
    }

    private void setupInfiniteScroll() {
        binding.bannerPager.registerOnPageChangeCallback(
                new ViewPager2.OnPageChangeCallback() {

                    @Override
                    public void onPageScrollStateChanged(int state) {

                        if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                            // 👉 User đang chạm / kéo → PAUSE
                            sliderHandler.removeCallbacks(sliderRunnable);
                        }

                        if (state == ViewPager2.SCROLL_STATE_IDLE) {
                            // 👉 User buông tay → RESUME auto slide
                            sliderHandler.postDelayed(sliderRunnable, 3000);

                            int position = binding.bannerPager.getCurrentItem();
                            int size = bannerInfiniteList.size() / 3;

                            if (size == 0) return;

                            if (position < size) {
                                binding.bannerPager.setCurrentItem(position + size, false);
                            } else if (position >= size * 2) {
                                binding.bannerPager.setCurrentItem(position - size, false);
                            }
                        }
                    }
                }
        );
    }


    /* ================= AUTO SLIDE ================= */

    private void setupAutoSlide() {
        sliderHandler.postDelayed(sliderRunnable, 3000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sliderHandler.removeCallbacks(sliderRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sliderHandler.postDelayed(sliderRunnable, 3000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sliderHandler.removeCallbacksAndMessages(null);
    }
}
