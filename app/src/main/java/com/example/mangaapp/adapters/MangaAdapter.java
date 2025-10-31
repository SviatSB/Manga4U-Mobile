package com.example.mangaapp.adapters;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.mangaapp.Manga;
import com.example.mangaapp.R;

import java.util.List;

public class MangaAdapter extends RecyclerView.Adapter<MangaAdapter.MangaViewHolder> {
    private List<Manga> mangaList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Manga manga);
    }

    public MangaAdapter(List<Manga> mangaList, OnItemClickListener listener) {
        this.mangaList = mangaList;
        this.listener = listener;
    }

    public static class MangaViewHolder extends RecyclerView.ViewHolder {
        ImageView cover;
        TextView title;
        TextView type;
        TextView description;

        public MangaViewHolder(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.manga_cover);
            title = itemView.findViewById(R.id.manga_title);
            type = itemView.findViewById(R.id.manga_type);
            description = itemView.findViewById(R.id.manga_description);
        }
    }

    @NonNull
    @Override
    public MangaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.manga_item, parent, false);
        return new MangaViewHolder(view);
    }


    @Override
    public int getItemCount() {
        return mangaList.size();
    }
    @Override
    public void onBindViewHolder(@NonNull MangaViewHolder holder, int position) {
        Manga manga = mangaList.get(position);
        holder.title.setText(manga.getTitle());
        holder.type.setText(manga.getType());
        holder.description.setText(manga.getDescription());

        // Завантаження обкладинки
        if (manga.getCoverUrl() != null && !manga.getCoverUrl().isEmpty()) {
            Log.e("MangaApp", "Glide loading: " + manga.getCoverUrl());
            RequestOptions requestOptions = new RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.placeholder_manga)
                    .error(R.drawable.placeholder_manga);

            Glide.with(holder.itemView.getContext())
                    .load(manga.getCoverUrl())
                    .apply(requestOptions)
                    .into(holder.cover);
        } else {
            holder.cover.setImageResource(R.drawable.placeholder_manga);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(manga);
            }
        });
    }
}
