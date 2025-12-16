package com.example.banking.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.banking.R;
import com.example.banking.model.Banner;

import java.util.List;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {

    private Context context;
    private List<Banner> banners;

    public BannerAdapter(Context context, List<Banner> banners) {
        this.context = context;
        this.banners = banners;
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_banner, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        Banner banner = banners.get(position);

        Glide.with(context)
                .load(banner.getImage())
                .into(holder.imgBanner);

        holder.txtName.setText(banner.getName());
        holder.txtInfo.setText(
                banner.getGenre() + " • " + banner.getTime() + " • " + banner.getAge()
        );
    }

    @Override
    public int getItemCount() {
        return banners == null ? 0 : banners.size();
    }


    static class BannerViewHolder extends RecyclerView.ViewHolder {
        ImageView imgBanner;
        TextView txtName, txtInfo;

        BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            imgBanner = itemView.findViewById(R.id.imgBanner);
            txtName = itemView.findViewById(R.id.txtName);
            txtInfo = itemView.findViewById(R.id.txtInfo);
        }
    }
}
