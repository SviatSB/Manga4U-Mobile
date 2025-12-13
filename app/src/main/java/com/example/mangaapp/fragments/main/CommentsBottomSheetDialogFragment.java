package com.example.mangaapp.fragments.main;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mangaapp.R;
import com.example.mangaapp.adapters.CommentAdapter;
import com.example.mangaapp.api.AccountApiClient;
import com.example.mangaapp.api.CommentApiService;
import com.example.mangaapp.models.Comment;
import com.example.mangaapp.utils.AuthManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CommentsBottomSheetDialogFragment extends BottomSheetDialogFragment {
    private static final String ARG_CHAPTER_ID = "chapter_id";
    private static final String ARG_PARENT_ID = "parent_id"; // For replies view

    private String chapterId;
    private Long parentCommentId; // If showing replies

    private RecyclerView recyclerView;
    private CommentAdapter adapter;
    private ProgressBar progressBar;
    private EditText commentInput;
    private ImageButton sendButton;
    private TextView titleView;
    private AuthManager authManager;
    private long currentUserId = -1;

    public static CommentsBottomSheetDialogFragment newInstance(String chapterId) {
        CommentsBottomSheetDialogFragment fragment = new CommentsBottomSheetDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CHAPTER_ID, chapterId);
        fragment.setArguments(args);
        return fragment;
    }

    public static CommentsBottomSheetDialogFragment newInstanceForReplies(String chapterId, Long parentCommentId) {
        CommentsBottomSheetDialogFragment fragment = new CommentsBottomSheetDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CHAPTER_ID, chapterId);
        args.putLong(ARG_PARENT_ID, parentCommentId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            chapterId = getArguments().getString(ARG_CHAPTER_ID);
            if (getArguments().containsKey(ARG_PARENT_ID)) {
                parentCommentId = getArguments().getLong(ARG_PARENT_ID);
            }
        }
        authManager = AuthManager.getInstance(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_comments_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        recyclerView = view.findViewById(R.id.comments_recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        commentInput = view.findViewById(R.id.comment_input);
        sendButton = view.findViewById(R.id.send_button);
        titleView = view.findViewById(R.id.comments_title);

        if (parentCommentId != null) {
            titleView.setText("Відповіді");
        }

        if (authManager.isLoggedIn()) {
             fetchCurrentUser();
        } else {
             setupRecyclerView();
             loadComments();
        }

        sendButton.setOnClickListener(v -> sendComment());
    }

    private void fetchCurrentUser() {
        com.example.mangaapp.api.AccountApiService accountService = AccountApiClient.getClient().create(com.example.mangaapp.api.AccountApiService.class);
        accountService.getMe("Bearer " + authManager.getAuthToken()).enqueue(new Callback<com.example.mangaapp.api.UserDto>() {
            @Override
            public void onResponse(Call<com.example.mangaapp.api.UserDto> call, Response<com.example.mangaapp.api.UserDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentUserId = response.body().getId();
                }
                setupRecyclerView();
                loadComments();
            }

            @Override
            public void onFailure(Call<com.example.mangaapp.api.UserDto> call, Throwable t) {
                setupRecyclerView();
                loadComments();
            }
        });
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CommentAdapter(new ArrayList<>(), currentUserId, new CommentAdapter.OnCommentClickListener() {
            @Override
            public void onReplyClick(Comment comment) {
                if (parentCommentId == null) {
                    // Open replies sheet
                    CommentsBottomSheetDialogFragment repliesFragment = 
                        CommentsBottomSheetDialogFragment.newInstanceForReplies(chapterId, comment.getId());
                    repliesFragment.show(getParentFragmentManager(), "RepliesSheet");
                } else {
                    // Nested replies not supported deeply, or just set text like "@User"
                    commentInput.setText("@" + comment.getUserNickname() + " ");
                    commentInput.setSelection(commentInput.getText().length());
                }
            }

            @Override
            public void onDeleteClick(Comment comment) {
                deleteComment(comment);
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void loadComments() {
        progressBar.setVisibility(View.VISIBLE);
        CommentApiService service = AccountApiClient.getClient().create(CommentApiService.class);

        Call<CommentApiService.CommentPagedDto> call;
        if (parentCommentId != null) {
            call = service.getReplies(parentCommentId, 0, 50);
        } else {
            call = service.getRootComments(chapterId, 0, 50);
        }

        call.enqueue(new Callback<CommentApiService.CommentPagedDto>() {
            @Override
            public void onResponse(Call<CommentApiService.CommentPagedDto> call, Response<CommentApiService.CommentPagedDto> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<Comment> items = response.body().getItems();
                    adapter.updateList(items);
                    if (items.isEmpty()) {
                        // Toast.makeText(getContext(), "Немає коментарів", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Помилка завантаження", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<CommentApiService.CommentPagedDto> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Помилка мережі", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendComment() {
        if (!authManager.isLoggedIn()) {
            Toast.makeText(getContext(), "Увійдіть в акаунт", Toast.LENGTH_SHORT).show();
            return;
        }

        String text = commentInput.getText().toString().trim();
        if (text.isEmpty()) return;

        progressBar.setVisibility(View.VISIBLE);
        CommentApiService service = AccountApiClient.getClient().create(CommentApiService.class);
        String token = "Bearer " + authManager.getAuthToken();

        CommentApiService.CreateCommentRequest request = 
            new CommentApiService.CreateCommentRequest(chapterId, text, parentCommentId);

        service.createComment(token, request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    commentInput.setText("");
                    Toast.makeText(getContext(), "Коментар додано", Toast.LENGTH_SHORT).show();
                    loadComments();
                } else {
                    progressBar.setVisibility(View.GONE);
                    String msg = "Помилка відправки";
                    try { if(response.errorBody()!=null) msg = response.errorBody().string(); } catch(Exception e){}
                    Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Помилка мережі", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteComment(Comment comment) {
        new AlertDialog.Builder(getContext())
            .setTitle("Видалити коментар?")
            .setPositiveButton("Так", (dialog, which) -> {
                progressBar.setVisibility(View.VISIBLE);
                CommentApiService service = AccountApiClient.getClient().create(CommentApiService.class);
                String token = "Bearer " + authManager.getAuthToken();

                service.deleteComment(token, comment.getId()).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(getContext(), "Видалено", Toast.LENGTH_SHORT).show();
                            loadComments();
                        } else {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(getContext(), "Помилка видалення", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Помилка мережі", Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("Ні", null)
            .show();
    }
}
