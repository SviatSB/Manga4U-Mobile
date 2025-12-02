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
import java.util.Random;

import com.example.mangaapp.api.ApiClient;
import com.example.mangaapp.MangaApiService;
import com.example.mangaapp.API_MangaDex.MangaDetail;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
        // Display only one main genre (if available). Hide the view when no genre.
        String type = manga.getType();
        if (type != null) {
            type = type.trim();
        }
        if (type == null || type.isEmpty() || "Manga".equalsIgnoreCase(type)) {
            holder.type.setVisibility(View.GONE);

            // If type is missing, try to fetch manga details to get tags and pick a random genre
            try {
                // Fall back to the tag list endpoint for this manga: /manga/{id}/tag
                MangaApiService api = ApiClient.getClient().create(MangaApiService.class);
                String tagPath = "manga/" + manga.getId() + "/tag";
                Call<com.example.mangaapp.API_MangaDex.TagResponse> tagCall = api.getTags(tagPath);
                final String mangaIdForCallback = manga.getId();
                tagCall.enqueue(new Callback<com.example.mangaapp.API_MangaDex.TagResponse>() {
                    @Override
                    public void onResponse(Call<com.example.mangaapp.API_MangaDex.TagResponse> call, Response<com.example.mangaapp.API_MangaDex.TagResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            try {
                                java.util.List<com.example.mangaapp.API_MangaDex.TagResponse.Data> tagData = response.body().getData();
                                if (tagData != null && !tagData.isEmpty()) {
                                    // pick a random tag's name (prefer 'en' then first available)
                                    Random rnd = new Random();
                                    int idx = rnd.nextInt(tagData.size());
                                    com.example.mangaapp.API_MangaDex.TagResponse.Data td = tagData.get(idx);
                                    String tagName = null;
                                    if (td.getAttributes() != null && td.getAttributes().getName() != null) {
                                        java.util.Map<String, String> nameMap = td.getAttributes().getName();
                                        if (nameMap != null) {
                                            tagName = nameMap.get("en");
                                            if (tagName == null) {
                                                for (String v : nameMap.values()) {
                                                    if (v != null && !v.isEmpty()) { tagName = v; break; }
                                                }
                                            }
                                        }
                                    }
                                    if (tagName != null && !tagName.isEmpty()) {
                                        for (int i = 0; i < mangaList.size(); i++) {
                                            if (mangaList.get(i).getId().equals(mangaIdForCallback)) {
                                                mangaList.get(i).setType(tagName);
                                                try { notifyItemChanged(i); } catch (Exception ignore) {}
                                                break;
                                            }
                                        }
                                    }
                                }
                            } catch (Exception ex) {
                                Log.e("MangaAdapter", "Error parsing tag response", ex);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<com.example.mangaapp.API_MangaDex.TagResponse> call, Throwable t) {
                        Log.e("MangaAdapter", "Failed to load tags for manga", t);
                    }
                });
            } catch (Exception e) {
                Log.e("MangaAdapter", "Error requesting manga tags", e);
            }
        } else {
            // If multiple genres are present in the string (comma-separated), show the first one
            String mainGenre = type;
            if (type.contains(",")) {
                mainGenre = type.split(",")[0].trim();
            }
            holder.type.setText(mainGenre);
            holder.type.setVisibility(View.VISIBLE);
        }
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
