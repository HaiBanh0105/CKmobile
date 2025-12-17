package com.example.banking.Adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.banking.databinding.ViewholderActorBinding;
import com.example.banking.model.Movie;
import java.util.List;

public class ActorAdapter extends RecyclerView.Adapter<ActorAdapter.ViewHolder> {
    private List<Movie.Cast> castList;

    public ActorAdapter(List<Movie.Cast> castList) {
        this.castList = castList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Giả sử tệp XML layout của bạn tên là viewholder_cast.xml
        ViewholderActorBinding binding = ViewholderActorBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Movie.Cast cast = castList.get(position);

        holder.binding.textView2.setText(cast.getActor());

        // Load ảnh diễn viên bằng Glide
        Glide.with(holder.itemView.getContext())
                .load(cast.getPicUrl())
                .into(holder.binding.imageView3);
    }

    @Override
    public int getItemCount() {
        return castList != null ? castList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ViewholderActorBinding binding;
        public ViewHolder(ViewholderActorBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
