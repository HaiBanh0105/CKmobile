package com.example.banking.Adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.banking.R;
import com.example.banking.model.ShowtimeDate;

import java.util.List;

public class ShowtimeDateAdapter extends RecyclerView.Adapter<ShowtimeDateAdapter.DateViewHolder> {

    private List<ShowtimeDate> dateList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public ShowtimeDateAdapter(List<ShowtimeDate> dateList, OnItemClickListener listener) {
        this.dateList = dateList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_showtime_date, parent, false);
        return new DateViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DateViewHolder holder, int position) {
        ShowtimeDate item = dateList.get(position);
        holder.txtDate.setText(item.getDisplayDate());
        holder.txtDay.setText(item.getDay());

        if (item.isSelected()) {
            holder.itemView.setBackgroundResource(R.drawable.rounded_blue_bg);
            holder.txtDate.setTextColor(Color.WHITE);
            holder.txtDay.setTextColor(Color.WHITE);
        } else {
            holder.itemView.setBackgroundResource(R.drawable.rounded_gray_bg);
            holder.txtDate.setTextColor(Color.GRAY);
            holder.txtDay.setTextColor(Color.BLACK);
        }

        holder.itemView.setOnClickListener(v -> {
            for (ShowtimeDate d : dateList) d.setSelected(false);
            item.setSelected(true);
            notifyDataSetChanged();
            listener.onItemClick(position);
        });
    }

    @Override
    public int getItemCount() {
        return dateList.size();
    }

    static class DateViewHolder extends RecyclerView.ViewHolder {
        TextView txtDate, txtDay;

        public DateViewHolder(@NonNull View itemView) {
            super(itemView);
            txtDate = itemView.findViewById(R.id.txtDate);
            txtDay = itemView.findViewById(R.id.txtDay);
        }
    }
}
