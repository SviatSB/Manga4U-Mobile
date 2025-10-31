package com.example.mangaapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mangaapp.R;
import com.example.mangaapp.models.RecentManga;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.List;

public class RecentMangaAdapter extends RecyclerView.Adapter<RecentMangaAdapter.ViewHolder> {

    private List<RecentManga> mangaList;
    private OnMangaClickListener listener;

    public interface OnMangaClickListener {
        void onMangaClick(RecentManga manga);
    }

    public RecentMangaAdapter(List<RecentManga> mangaList, OnMangaClickListener listener) {
        this.mangaList = mangaList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_manga, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecentManga manga = mangaList.get(position);
        holder.bind(manga);
    }

    @Override
    public int getItemCount() {
        return mangaList.size();
    }

    public void updateData(List<RecentManga> newMangaList) {
        this.mangaList = newMangaList;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private ShapeableImageView mangaCover;
        private TextView mangaTitle;
        private TextView mangaChapter;
        private TextView mangaProgress;
        private LinearProgressIndicator progressBar;
        private TextView mangaTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mangaCover = itemView.findViewById(R.id.manga_cover);
            mangaTitle = itemView.findViewById(R.id.manga_title);
            mangaChapter = itemView.findViewById(R.id.manga_chapter);
            mangaProgress = itemView.findViewById(R.id.manga_progress);
            progressBar = itemView.findViewById(R.id.manga_progress_bar);
            mangaTime = itemView.findViewById(R.id.manga_time);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onMangaClick(mangaList.get(position));
                }
            });
        }

        public void bind(RecentManga manga) {
            // Назва манги
            mangaTitle.setText(manga.getTitle());

            // Інформація про главу
            mangaChapter.setText(manga.getChapterInfo());

            // Прогрес читання
            mangaProgress.setText(manga.getProgressText());

            // Прогрес бар
            if (manga.getTotalPages() > 0) {
                int progress = (manga.getCurrentPage() * 100) / manga.getTotalPages();
                progressBar.setProgress(progress);
            } else {
                progressBar.setProgress(0);
            }

            // Час останнього читання
            mangaTime.setText(manga.getLastReadTimeFormatted());

            // Завантаження обкладинки
            if (manga.getCoverUrl() != null && !manga.getCoverUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(manga.getCoverUrl())
                        .placeholder(R.drawable.ic_manga_placeholder)
                        .error(R.drawable.ic_manga_placeholder)
                        .into(mangaCover);
            } else {
                mangaCover.setImageResource(R.drawable.ic_manga_placeholder);
            }
        }
    }
}