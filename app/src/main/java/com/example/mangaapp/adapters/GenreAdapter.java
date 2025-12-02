package com.example.mangaapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.List;

public class GenreAdapter extends RecyclerView.Adapter<GenreAdapter.GenreViewHolder> {
    private List<GenreItem> genres = new ArrayList<>();
    private List<String> selectedGenreIds = new ArrayList<>();
    private OnGenreSelectionListener listener;

    public interface OnGenreSelectionListener {
        void onGenresSelected(List<String> genreIds);
    }

    public static class GenreItem {
        private String id;
        private String name;
        private String group;

        public GenreItem(String id, String name, String group) {
            this.id = id;
            this.name = name;
            this.group = group;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getGroup() { return group; }
    }

    public class GenreViewHolder extends RecyclerView.ViewHolder {
        Chip chip;

        public GenreViewHolder(@NonNull View itemView) {
            super(itemView);
            chip = (Chip) itemView;
        }
    }

    public void setGenres(List<GenreItem> genres) {
        this.genres = genres;
        notifyDataSetChanged();
    }

    public void setSelectedGenreIds(List<String> selectedIds) {
        this.selectedGenreIds.clear();
        if (selectedIds != null) {
            this.selectedGenreIds.addAll(selectedIds);
        }
        notifyDataSetChanged();
    }

    public void setOnGenreSelectionListener(OnGenreSelectionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public GenreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(com.example.mangaapp.R.layout.item_genre, parent, false);
        return new GenreViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GenreViewHolder holder, int position) {
        GenreItem genre = genres.get(position);
        holder.chip.setText(genre.getName());
        
        // Видаляємо попередній listener, щоб уникнути конфліктів
        holder.chip.setOnCheckedChangeListener(null);
        
        // Встановлюємо стан checked
        boolean isSelected = selectedGenreIds.contains(genre.getId());
        holder.chip.setChecked(isSelected);
        
        // Змінюємо колір фону в залежності від стану
        updateChipAppearance(holder.chip, isSelected);
        
        // Додаємо listener після встановлення стану
        holder.chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String genreId = genre.getId();
            if (isChecked) {
                if (!selectedGenreIds.contains(genreId)) {
                    selectedGenreIds.add(genreId);
                }
            } else {
                selectedGenreIds.remove(genreId);
            }
            // Оновлюємо вигляд при зміні стану
            updateChipAppearance(holder.chip, isChecked);
        });
    }

    private void updateChipAppearance(Chip chip, boolean isSelected) {
        if (isSelected) {
            // Вибраний стан - фіолетовий фон
            chip.setChipBackgroundColorResource(com.example.mangaapp.R.color.purple_accent);
            chip.setTextColor(android.graphics.Color.WHITE);
        } else {
            // Нормальний стан - темний фон
            chip.setChipBackgroundColorResource(com.example.mangaapp.R.color.dark_background);
            chip.setTextColor(android.graphics.Color.WHITE);
        }
    }

    @Override
    public int getItemCount() {
        return genres.size();
    }

    public List<String> getSelectedGenreIds() {
        return new ArrayList<>(selectedGenreIds);
    }

    public void clearSelection() {
        selectedGenreIds.clear();
        notifyDataSetChanged();
    }
}

