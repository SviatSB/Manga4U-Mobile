package com.example.mangaapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.mangaapp.R;
import com.example.mangaapp.models.Comment;
import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {
    private List<Comment> comments;
    private long currentUserId;
    private OnCommentClickListener listener;

    public interface OnCommentClickListener {
        void onReplyClick(Comment comment);
        void onDeleteClick(Comment comment);
    }

    public CommentAdapter(List<Comment> comments, long currentUserId, OnCommentClickListener listener) {
        this.comments = comments;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void updateList(List<Comment> newComments) {
        this.comments = newComments;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);
        holder.bind(comment);
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    class CommentViewHolder extends RecyclerView.ViewHolder {
        ImageView avatar;
        TextView username, text, date, replyCount;
        ImageButton deleteBtn;

        CommentViewHolder(View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.avatar_image);
            username = itemView.findViewById(R.id.username_text);
            text = itemView.findViewById(R.id.comment_text);
            date = itemView.findViewById(R.id.date_text);
            replyCount = itemView.findViewById(R.id.reply_count_text);
            deleteBtn = itemView.findViewById(R.id.btn_delete);
        }

        void bind(Comment comment) {
            username.setText(comment.getUserNickname());
            text.setText(comment.getText());
            if (comment.getCreationTime() != null) {
                date.setText(comment.getCreationTime().split("T")[0]);
            }
            
            if (comment.getUserAvatarUrl() != null && !comment.getUserAvatarUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(comment.getUserAvatarUrl())
                        .placeholder(R.drawable.ic_profile)
                        .circleCrop()
                        .into(avatar);
            } else {
                avatar.setImageResource(R.drawable.ic_profile);
            }

            // Show reply count or "Reply" text
            if (comment.getReplyCount() > 0) {
                replyCount.setText("Відповіді (" + comment.getReplyCount() + ")");
            } else {
                replyCount.setText("Відповісти");
            }

            replyCount.setOnClickListener(v -> {
                if (listener != null) listener.onReplyClick(comment);
            });

            if (comment.getUserId() == currentUserId) {
                deleteBtn.setVisibility(View.VISIBLE);
                deleteBtn.setOnClickListener(v -> {
                    if (listener != null) listener.onDeleteClick(comment);
                });
            } else {
                deleteBtn.setVisibility(View.GONE);
            }
        }
    }
}
