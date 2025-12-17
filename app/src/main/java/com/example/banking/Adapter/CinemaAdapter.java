package com.example.banking.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.banking.R;
import com.example.banking.model.Cinema;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

public class CinemaAdapter extends RecyclerView.Adapter<CinemaAdapter.CinemaViewHolder> {

    private List<Cinema> cinemaList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public CinemaAdapter(List<Cinema> cinemaList, OnItemClickListener listener) {
        this.cinemaList = cinemaList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CinemaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cinema, parent, false);
        return new CinemaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CinemaViewHolder holder, int position) {
        Cinema cinema = cinemaList.get(position);
        Glide.with(holder.itemView.getContext())
                .load(cinema.getLogo())
                .centerCrop()
                .into(holder.imgCinema);

        holder.itemView.setOnClickListener(v -> listener.onItemClick(position));
    }

    @Override
    public int getItemCount() {
        return cinemaList.size();
    }

    static class CinemaViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView imgCinema;

        public CinemaViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCinema = itemView.findViewById(R.id.imgCinema);
        }
    }
}
