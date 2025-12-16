package com.example.banking.viewHolder;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.banking.R;
import com.example.banking.databinding.ViewholderFlightBinding;
import com.example.banking.model.Flight;
import com.google.firebase.Timestamp;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;

public class FlightViewHolder extends RecyclerView.ViewHolder {

    private final ViewholderFlightBinding binding;

    public FlightViewHolder(ViewholderFlightBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    public void bind(Flight flight) {

        // Logo hãng bay
        binding.logo.setImageResource(
                getAirlineLogo(flight.getLogoUrl())
        );

        // Tên hãng
        binding.txtAirline.setText(flight.getAirline() + " • " + flight.getFlightNumber()
        );

        binding.txtDepartDate.setText(
                formatDate(flight.getDepartureTime())
        );

        binding.txtDepartTime.setText(
                formatTimeAmPm(flight.getDepartureTime())
        );

        binding.txtArrivalDate.setText(
                formatDate(flight.getArrivalTime())
        );

        binding.txtArrivalTime.setText(
                formatTimeAmPm(flight.getArrivalTime())
        );

        // Thời gian bay
        binding.txtDuration.setText(
                calcDuration(
                        flight.getDepartureTime(),
                        flight.getArrivalTime()
                )
        );

        binding.txtClassSeat.setText(
                flight.getSelectedSeatClassKey()
        );

        // Giá vé
        Map<String, Integer> seat = flight.getSelectedSeatClass();

        if (seat != null && seat.containsKey("price")) {
            binding.txtPrice.setText(
                    formatPrice(seat.get("price"))
            );
        }
    }

    // ===== Helpers =====
    private int getAirlineLogo(String key) {
        switch (key) {
            case "vn_airlines":
                return R.drawable.vn_airlines;
//            case "vietjet":
//                return R.drawable.vietjet;
//            case "bamboo":
//                return R.drawable.bamboo;
//            case "jetstar":
//                return R.drawable.jetstar;
            default:
                return R.drawable.vn_airlines;
        }
    }

    private String formatDate(Timestamp ts) {
        if (ts == null) return "--/--/----";

        SimpleDateFormat sdf =
                new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        return sdf.format(ts.toDate());
    }

    private String formatTimeAmPm(Timestamp ts) {
        if (ts == null) return "--:--";

        SimpleDateFormat sdf =
                new SimpleDateFormat("hh:mm a", Locale.US);
        // Locale.US để chắc chắn hiển thị AM / PM

        return sdf.format(ts.toDate());
    }

    private String calcDuration(Timestamp start, Timestamp end) {
        if (start == null || end == null) return "";

        long diff =
                end.toDate().getTime() -
                        start.toDate().getTime();

        long h = diff / (1000 * 60 * 60);
        long m = (diff / (1000 * 60)) % 60;

        return h + "h " + m + "m";
    }

    private String formatPrice(double price) {
        NumberFormat nf =
                NumberFormat.getCurrencyInstance(
                        new Locale("vi", "VN")
                );
        return nf.format(price);
    }
}
