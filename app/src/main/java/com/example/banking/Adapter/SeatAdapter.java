package com.example.banking.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.banking.R;
import com.example.banking.model.Seat;

import java.util.List;

public class SeatAdapter extends RecyclerView.Adapter<SeatAdapter.Holder> {

    private List<Seat> seats;

    public SeatAdapter(List<Seat> seats) {
        this.seats = seats;
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_seat, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(Holder h, int i) {
        Seat seat = seats.get(i);
        h.txt.setText(seat.id);

        if (seat.booked) {
            h.txt.setBackgroundResource(R.drawable.unavailable_seat);
            h.txt.setEnabled(false);
        } else if (seat.selected) {
            h.txt.setBackgroundResource(R.drawable.selected_seat);
            h.txt.setEnabled(true);
        } else {
            h.txt.setBackgroundResource(R.drawable.available_seat);
            h.txt.setEnabled(true);
        }

        h.txt.setOnClickListener(v -> {
            if (seat.booked) return;
            seat.selected = !seat.selected;
            notifyItemChanged(i);
        });
    }

    @Override
    public int getItemCount() {
        return seats.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView txt;
        Holder(View v) {
            super(v);
            txt = v.findViewById(R.id.txtSeat);
        }
    }
}

