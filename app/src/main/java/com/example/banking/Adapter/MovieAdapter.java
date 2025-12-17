package com.example.banking.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.banking.Activity.MovieBookingDetail;
import com.example.banking.R;
import com.example.banking.databinding.ViewholderMovieBinding;
import com.example.banking.model.Movie;

import java.util.List;

public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.MovieViewHolder> {

    private Context context;
    private List<Movie> movieList;

    public MovieAdapter(Context context, List<Movie> movieList) {
        this.context = context;
        this.movieList = movieList;
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewholderMovieBinding binding = ViewholderMovieBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
        );
        return new MovieViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        Movie movie = movieList.get(position);

        // 🎬 Tên phim
        holder.binding.txtMovieName.setText(movie.getTitle());

        // 🎭 Thể loại (ghép toàn bộ genre)
        String genreText = "N/A";

        if (movie.getGenre() != null && !movie.getGenre().isEmpty()) {
            StringBuilder builder = new StringBuilder();

            for (int i = 0; i < movie.getGenre().size(); i++) {
                builder.append(movie.getGenre().get(i));
                if (i < movie.getGenre().size() - 1) {
                    builder.append(" • ");
                }
            }
            genreText = builder.toString();
        }

        holder.binding.txtMovieInfo.setText(genreText);


        // 🔞 Độ tuổi
        holder.binding.txtMovieAge.setText("Tuổi: " + movie.getAge());

        // ⏱ Thời lượng
        holder.binding.txtMovieDuration.setText(movie.getTime());

        // 🖼 Poster
        Glide.with(context)
                .load(movie.getPoster())
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_placeholder)
                .into(holder.binding.imgPoster);

        // 🎟 Đặt vé
        holder.binding.btnBook.setOnClickListener(v -> {
             Intent intent = new Intent(context, MovieBookingDetail.class);
             intent.putExtra("movie", movie);
             context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return movieList.size();
    }

    static class MovieViewHolder extends RecyclerView.ViewHolder {
        ViewholderMovieBinding binding;

        public MovieViewHolder(ViewholderMovieBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
