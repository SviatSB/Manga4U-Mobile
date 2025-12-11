package com.example.mangaapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mangaapp.R;
import com.example.mangaapp.models.Collection;
import java.util.List;

public class CollectionAdapter extends RecyclerView.Adapter<CollectionAdapter.ViewHolder> {
    private List<Collection> collections;
    private final OnCollectionClickListener listener;
    private final OnDeleteClickListener deleteListener;

    public interface OnCollectionClickListener {
        void onCollectionClick(Collection collection);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(Collection collection);
    }

    public CollectionAdapter(List<Collection> collections, OnCollectionClickListener listener, OnDeleteClickListener deleteListener) {
        this.collections = collections;
        this.listener = listener;
        this.deleteListener = deleteListener;
    }

    public void updateList(List<Collection> newCollections) {
        this.collections = newCollections;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_collection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Collection collection = collections.get(position);
        holder.name.setText(collection.getName());
        
        // System collections cannot be deleted
        if (collection.isSystem()) {
            holder.deleteButton.setVisibility(View.GONE);
            holder.info.setText("Системна колекція");
        } else {
            holder.deleteButton.setVisibility(View.VISIBLE);
            holder.info.setText("Створено користувачем");
        }
        
        holder.itemView.setOnClickListener(v -> listener.onCollectionClick(collection));
        
        holder.deleteButton.setOnClickListener(v -> deleteListener.onDeleteClick(collection));
    }

    @Override
    public int getItemCount() {
        return collections != null ? collections.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView info;
        ImageButton deleteButton;

        ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.collection_name);
            info = itemView.findViewById(R.id.collection_info);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }
    }
}
