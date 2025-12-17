package com.example.banking.Adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.banking.R;
import com.example.banking.model.ShowtimeTime;

import java.util.List;

public class ShowtimeTimeAdapter extends RecyclerView.Adapter<ShowtimeTimeAdapter.TimeViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    private List<ShowtimeTime> items;
    private OnItemClickListener listener;

    public ShowtimeTimeAdapter(List<ShowtimeTime> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TimeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_showtime_time, parent, false);
        return new TimeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimeViewHolder holder, int position) {
        ShowtimeTime item = items.get(position);
        holder.txtCategory.setText(item.getTime());

        // đổi màu theo isSelected
        if (item.isSelected()) {
            holder.txtCategory.setTextColor(Color.WHITE);
            holder.txtCategory.setBackgroundResource(R.drawable.rounded_blue_bg);
        } else {
            holder.txtCategory.setTextColor(Color.BLACK);
            holder.txtCategory.setBackgroundResource(R.drawable.rounded_white_bg);
        }

        holder.itemView.setOnClickListener(v -> {
            // bỏ chọn tất cả
            for (ShowtimeTime t : items) t.setSelected(false);
            item.setSelected(true);
            notifyDataSetChanged();

            if (listener != null) listener.onItemClick(position);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class TimeViewHolder extends RecyclerView.ViewHolder {
        TextView txtCategory;
        public TimeViewHolder(@NonNull View itemView) {
            super(itemView);
            txtCategory = itemView.findViewById(R.id.txtCategory);
        }
    }
}

