package com.example.banking.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.banking.R;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private final List<String> categories;

    public CategoryAdapter(List<String> categories) {
        this.categories = categories;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.viewholder_movie_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        holder.txtCategory.setText(categories.get(position));
    }

    @Override
    public int getItemCount() {
        return categories == null ? 0 : categories.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {

        TextView txtCategory;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            txtCategory = itemView.findViewById(R.id.txtCategory);
        }
    }
}
