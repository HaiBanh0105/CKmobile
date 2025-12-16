package com.example.banking.Adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.banking.databinding.ViewholderFlightBinding;
import com.example.banking.model.Flight;
import com.example.banking.viewHolder.FlightViewHolder;

import java.util.ArrayList;
import java.util.List;

public class FlightAdapter extends RecyclerView.Adapter<FlightViewHolder> {

    private final List<Flight> flightList = new ArrayList<>();
    private Flight selectedFlight;
    private OnFlightClickListener listener;

    public void setData(List<Flight> data) {
        flightList.clear();
        flightList.addAll(data);
        notifyDataSetChanged();
    }

    public void setOnClick(OnFlightClickListener listener) {
        this.listener = listener;
    }

    public Flight getSelectedFlight() {
        return selectedFlight;
    }

    @NonNull
    @Override
    public FlightViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewholderFlightBinding binding = ViewholderFlightBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new FlightViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull FlightViewHolder holder,
            int position) {

        Flight flight = flightList.get(position);
        holder.bind(flight);

        // highlight item được chọn
        holder.itemView.setSelected(flight == selectedFlight);

        holder.itemView.setOnClickListener(v -> {
            selectedFlight = flight;
            notifyDataSetChanged(); // refresh để highlight
            listener.onFlightClick(flight);
        });
    }

    @Override
    public int getItemCount() {
        return flightList.size();
    }

}
