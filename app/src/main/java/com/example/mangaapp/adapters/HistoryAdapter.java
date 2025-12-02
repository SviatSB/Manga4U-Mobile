package com.example.mangaapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mangaapp.R;
import com.example.mangaapp.models.HistoryItem;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<HistoryItem> historyList;
    private OnHistoryItemClickListener listener;

    public interface OnHistoryItemClickListener {
        void onHistoryItemClick(HistoryItem item);
    }

    public HistoryAdapter(List<HistoryItem> historyList, OnHistoryItemClickListener listener) {
        this.historyList = historyList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryItem item = historyList.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView mangaTitle;
        private TextView chapterInfo;
        private TextView updatedAt;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mangaTitle = itemView.findViewById(R.id.history_manga_title);
            chapterInfo = itemView.findViewById(R.id.history_chapter_info);
            updatedAt = itemView.findViewById(R.id.history_updated_at);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onHistoryItemClick(historyList.get(position));
                }
            });
        }

        public void bind(HistoryItem item) {
            mangaTitle.setText(item.getMangaName());
            
            String chapterText = "Глава " + item.getLastChapterNumber();
            if (item.getLastChapterTitle() != null && !item.getLastChapterTitle().isEmpty()) {
                chapterText += ": " + item.getLastChapterTitle();
            }
            chapterInfo.setText(chapterText);
            
            // Форматуємо дату
            if (item.getUpdatedAt() != null && !item.getUpdatedAt().isEmpty()) {
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                    Date date = inputFormat.parse(item.getUpdatedAt());
                    if (date != null) {
                        SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
                        updatedAt.setText(outputFormat.format(date));
                    } else {
                        updatedAt.setText(item.getUpdatedAt());
                    }
                } catch (ParseException e) {
                    updatedAt.setText(item.getUpdatedAt());
                }
            } else {
                updatedAt.setText("Недавно");
            }
        }
    }
}

