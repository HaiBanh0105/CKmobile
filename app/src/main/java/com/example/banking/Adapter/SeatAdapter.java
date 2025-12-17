package com.example.banking.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.banking.R;
import com.example.banking.model.Seat;

import java.util.List;

public class SeatAdapter extends RecyclerView.Adapter<SeatAdapter.SeatViewHolder> {

    private List<Seat> seatList;
    private OnSeatClickListener listener;

    public interface OnSeatClickListener {
        void onSeatClick(int position);
    }

    public SeatAdapter(List<Seat> seatList, OnSeatClickListener listener) {
        this.seatList = seatList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SeatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_seat, parent, false);
        return new SeatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SeatViewHolder holder, int position) {
        Seat seat = seatList.get(position);

        holder.txtSeat.setText(seat.getId());

        if (seat.isBooked()) {
            holder.txtSeat.setBackgroundResource(R.drawable.unavailable_seat);
        } else if (seat.isSelected()) {
            holder.txtSeat.setBackgroundResource(R.drawable.selected_seat);
        } else {
            holder.txtSeat.setBackgroundResource(R.drawable.available_seat);
        }

        holder.itemView.setOnClickListener(v -> {
            if (!seat.isBooked()) {
                listener.onSeatClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return seatList.size();
    }

    static class SeatViewHolder extends RecyclerView.ViewHolder {
        TextView txtSeat;

        public SeatViewHolder(@NonNull View itemView) {
            super(itemView);
            txtSeat = itemView.findViewById(R.id.txtSeat);
        }
    }
}
