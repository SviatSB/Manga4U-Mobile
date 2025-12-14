package com.example.mangaapp.adapters;

import android.content.res.Resources;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.mangaapp.R;
import com.example.mangaapp.models.Comment;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter uses a flat list of DisplayItem objects.
 * Root comments + inserted reply items (isReply = true). Replies carry depth (indent).
 * Provides methods insertReplies / collapseRepliesForParent to be called by fragment after fetching replies.
 */
public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {
    private final List<DisplayItem> items = new ArrayList<>();
    private long currentUserId;
    private OnCommentClickListener listener;

    public interface OnCommentClickListener {
        void onReplyClick(Comment comment);                 // prefill reply
        void onDeleteClick(Comment comment);
        void onToggleReplies(Comment comment, int position); // request load/expand/collapse replies
    }

    public CommentAdapter(List<Comment> comments, long currentUserId, OnCommentClickListener listener) {
        this.currentUserId = currentUserId;
        this.listener = listener;
        setRootComments(comments);
    }

    public void setRootComments(List<Comment> rootComments) {
        items.clear();
        if (rootComments != null) {
            for (Comment c : rootComments) {
                items.add(DisplayItem.forRoot(c));
            }
        }
        notifyDataSetChanged();
    }

    public void updateList(List<Comment> newComments) {
        setRootComments(newComments);
    }

    // Insert replies under parentAdapterPosition. repliesDepth = parentDepth + 1.
    public int insertReplies(int parentAdapterPosition, List<Comment> replies) {
        if (parentAdapterPosition < 0 || parentAdapterPosition >= items.size() || replies == null || replies.isEmpty())
            return 0;
        DisplayItem parent = items.get(parentAdapterPosition);
        int depth = parent.depth + 1;
        int insertPos = parentAdapterPosition + 1;
        List<DisplayItem> toInsert = new ArrayList<>();
        for (Comment r : replies) {
            DisplayItem di = DisplayItem.forReply(r, depth, parent.comment.getId());
            toInsert.add(di);
        }
        items.addAll(insertPos, toInsert);
        notifyItemRangeInserted(insertPos, toInsert.size());
        return toInsert.size();
    }

    // Collapse all replies for given parentCommentId (removes contiguous block after parent)
    public int collapseRepliesForParent(long parentCommentId) {
        int parentPos = findPositionByCommentId(parentCommentId);
        if (parentPos == -1) return 0;
        int removeStart = parentPos + 1;
        int count = 0;
        while (removeStart < items.size()) {
            DisplayItem di = items.get(removeStart);
            if (di.isReply && di.parentId == parentCommentId) {
                items.remove(removeStart);
                count++;
            } else break;
        }
        if (count > 0) notifyItemRangeRemoved(parentPos + 1, count);
        return count;
    }

    // Utility: find adapter position for comment id
    public int findPositionByCommentId(long commentId) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).comment.getId() == commentId) return i;
        }
        return -1;
    }

    // Method to get comment at position
    public Comment getCommentAtPosition(int position) {
        if (position >= 0 && position < items.size()) {
            return items.get(position).comment;
        }
        return null;
    }

    public void setCurrentUserId(long id) {
        this.currentUserId = id;
        notifyDataSetChanged();
    }

    public void setOnCommentClickListener(OnCommentClickListener l) {
        this.listener = l;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        DisplayItem di = items.get(position);
        holder.bind(di);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class CommentViewHolder extends RecyclerView.ViewHolder {
        ImageView avatar;
        TextView username, text, date, replyCount;
        ImageButton deleteBtn;
        ImageView btnExpand;

        CommentViewHolder(View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.avatar_image);
            username = itemView.findViewById(R.id.username_text);
            text = itemView.findViewById(R.id.comment_text);
            date = itemView.findViewById(R.id.date_text);
            replyCount = itemView.findViewById(R.id.reply_count_text);
            deleteBtn = itemView.findViewById(R.id.btn_delete);
            btnExpand = itemView.findViewById(R.id.btn_expand);
        }

        void bind(DisplayItem di) {
            Comment comment = di.comment;
            username.setText(comment.getUserNickname());
            text.setText(comment.getText());
            if (comment.getCreationTime() != null) {
                date.setText(comment.getCreationTime().split("T")[0]);
            } else date.setText("");

            if (comment.getUserAvatarUrl() != null && !comment.getUserAvatarUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(comment.getUserAvatarUrl())
                        .placeholder(R.drawable.ic_profile)
                        .circleCrop()
                        .into(avatar);
            } else {
                avatar.setImageResource(R.drawable.ic_profile);
            }

            if (comment.getReplyCount() > 0) {
                replyCount.setText("Відповіді (" + comment.getReplyCount() + ")");
                btnExpand.setVisibility(View.VISIBLE);
            } else {
                replyCount.setText("Відповісти");
                btnExpand.setVisibility(View.GONE);
            }

            // indent according to depth
            int leftPaddingDp = di.depth * 12; // 12dp per depth
            int leftPx = dpToPx(leftPaddingDp);
            itemView.setPadding(leftPx, itemView.getPaddingTop(), itemView.getPaddingRight(), itemView.getPaddingBottom());

            replyCount.setOnClickListener(v -> {
                if (listener != null) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        if (comment.getReplyCount() > 0) {
                            listener.onToggleReplies(comment, pos);
                        } else {
                            listener.onReplyClick(comment);
                        }
                    }
                }
            });

            btnExpand.setOnClickListener(v -> {
                if (listener != null) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        listener.onToggleReplies(comment, pos);
                    }
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) listener.onReplyClick(comment);
                return true;
            });

            if (comment.getUserId() == currentUserId) {
                deleteBtn.setVisibility(View.VISIBLE);
                deleteBtn.setOnClickListener(v -> {
                    if (listener != null) listener.onDeleteClick(comment);
                });
            } else {
                deleteBtn.setVisibility(View.GONE);
            }

            // subtle appear animation
            itemView.setAlpha(0f);
            itemView.setTranslationY(8f);
            itemView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(200)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }

        private int dpToPx(int dp) {
            return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem().getDisplayMetrics());
        }
    }

    // internal flat display item
    private static class DisplayItem {
        final Comment comment;
        final boolean isReply;
        final int depth; // 0 for root, increasing for nested replies
        final long parentId; // valid for replies

        private DisplayItem(Comment comment, boolean isReply, int depth, long parentId) {
            this.comment = comment;
            this.isReply = isReply;
            this.depth = depth;
            this.parentId = parentId;
        }

        static DisplayItem forRoot(Comment c) {
            return new DisplayItem(c, false, 0, -1);
        }

        static DisplayItem forReply(Comment c, int depth, long parentId) {
            return new DisplayItem(c, true, depth, parentId);
        }
    }
}