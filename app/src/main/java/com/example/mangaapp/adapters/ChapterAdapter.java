package com.example.mangaapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mangaapp.API_MangaDex.ChapterFeedResponse;
import com.example.mangaapp.R;

import java.util.List;

public class ChapterAdapter extends RecyclerView.Adapter<ChapterAdapter.ChapterViewHolder> {
    private List<ChapterFeedResponse.Result> chapters;
    private OnChapterClickListener listener;

    public interface OnChapterClickListener {
        void onChapterClick(ChapterFeedResponse.Result chapter);
    }

    public ChapterAdapter(List<ChapterFeedResponse.Result> chapters, OnChapterClickListener listener) {
        this.chapters = chapters != null ? chapters : new java.util.ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChapterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chapter, parent, false);
        return new ChapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChapterViewHolder holder, int position) {
        ChapterFeedResponse.Result chapter = chapters.get(position);
        if (chapter.getAttributes() != null) {
            String title = chapter.getAttributes().getTitle();
            String number = chapter.getAttributes().getChapter();
            holder.title.setText(title != null && !title.isEmpty() ? title : "Глава " + number);
            holder.number.setText(number != null ? "Chapter " + number : "");
        } else {
            holder.title.setText("Без назви");
            holder.number.setText("");
        }
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onChapterClick(chapter);
            }
        });
    }

    @Override
    public int getItemCount() {
        return chapters.size();
    }

    static class ChapterViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView number;

        public ChapterViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.chapter_title);
            number = itemView.findViewById(R.id.chapter_number);
        }
    }
}