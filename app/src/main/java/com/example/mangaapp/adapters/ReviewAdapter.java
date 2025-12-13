package com.example.mangaapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.mangaapp.R;
import com.example.mangaapp.models.Review;
import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {
    private List<Review> reviews;
    private long currentUserId;
    private OnDeleteClickListener deleteListener;

    public interface OnDeleteClickListener {
        void onDeleteClick(Review review);
    }

    public ReviewAdapter(List<Review> reviews, long currentUserId, OnDeleteClickListener deleteListener) {
        this.reviews = reviews;
        this.currentUserId = currentUserId;
        this.deleteListener = deleteListener;
    }

    public void updateList(List<Review> newReviews) {
        this.reviews = newReviews;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviews.get(position);
        holder.bind(review);
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    class ReviewViewHolder extends RecyclerView.ViewHolder {
        ImageView avatar;
        TextView username, text, date;
        RatingBar ratingBar;
        ImageButton deleteBtn;

        ReviewViewHolder(View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.avatar_image);
            username = itemView.findViewById(R.id.username_text);
            text = itemView.findViewById(R.id.review_text);
            date = itemView.findViewById(R.id.date_text);
            ratingBar = itemView.findViewById(R.id.rating_bar);
            deleteBtn = itemView.findViewById(R.id.btn_delete);
        }

        void bind(Review review) {
            username.setText(review.getUserNickname());
            text.setText(review.getText() != null ? review.getText() : "");
            date.setText(review.getCreationTime().split("T")[0]); // Simple date formatting
            ratingBar.setRating(review.getStars());

            if (review.getUserAvatarUrl() != null && !review.getUserAvatarUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(review.getUserAvatarUrl())
                        .placeholder(R.drawable.ic_profile)
                        .circleCrop()
                        .into(avatar);
            } else {
                avatar.setImageResource(R.drawable.ic_profile);
            }

            // Show delete button if review belongs to current user
            if (review.getUserId() == currentUserId) {
                deleteBtn.setVisibility(View.VISIBLE);
                deleteBtn.setOnClickListener(v -> {
                    if (deleteListener != null) deleteListener.onDeleteClick(review);
                });
            } else {
                deleteBtn.setVisibility(View.GONE);
            }
        }
    }
}
