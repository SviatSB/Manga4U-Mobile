package com.example.mangaapp.fragments.main;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mangaapp.R;
import com.example.mangaapp.adapters.CommentAdapter;
import com.example.mangaapp.api.AccountApiClient;
import com.example.mangaapp.api.CommentApiService;
import com.example.mangaapp.models.Comment;
import com.example.mangaapp.utils.AuthManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Full-screen comments view — opens like Reviews but without rating.
 * Reuses the comments layout (field + recycler). Keeps keyboard behaviour and focus.
 */
public class CommentsFullScreenFragment extends Fragment {
    private static final String ARG_CHAPTER_ID = "chapter_id";
    private static final String ARG_PAGE_INDEX = "page_index";
    private static final String ARG_PARENT_ID = "parent_id";

    private String chapterId;
    private Integer pageIndex;
    private Long parentCommentId;

    private RecyclerView recyclerView;
    private CommentAdapter adapter;
    private CommentAdapter.OnCommentClickListener commentClickListener;
    private ProgressBar progressBar;
    private EditText commentInput;
    private ImageButton sendButton;
    private TextView titleView;
    private AuthManager authManager;
    private long currentUserId = -1;

    public static CommentsFullScreenFragment newInstance(String chapterId, Integer pageIndex) {
        CommentsFullScreenFragment f = new CommentsFullScreenFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CHAPTER_ID, chapterId);
        if (pageIndex != null) args.putInt(ARG_PAGE_INDEX, pageIndex);
        f.setArguments(args);
        return f;
    }

    public static CommentsFullScreenFragment newInstanceForReplies(String chapterId, Integer pageIndex, Long parentCommentId) {
        CommentsFullScreenFragment f = new CommentsFullScreenFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CHAPTER_ID, chapterId);
        if (pageIndex != null) args.putInt(ARG_PAGE_INDEX, pageIndex);
        if (parentCommentId != null) args.putLong(ARG_PARENT_ID, parentCommentId);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            chapterId = getArguments().getString(ARG_CHAPTER_ID);
            if (getArguments().containsKey(ARG_PAGE_INDEX)) pageIndex = getArguments().getInt(ARG_PAGE_INDEX);
            if (getArguments().containsKey(ARG_PARENT_ID)) parentCommentId = getArguments().getLong(ARG_PARENT_ID);
        }
        authManager = AuthManager.getInstance(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // reuse the existing comments layout (field + recycler). It's full-screen now.
        return inflater.inflate(R.layout.fragment_comments_bottom_sheet, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        // ensure keyboard causes resize (so input stays visible)
        if (requireActivity().getWindow() != null) {
            requireActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
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
        } else if (pageIndex != null) {
            titleView.setText("Коментарі — сторінка " + (pageIndex + 1));
        } else {
            titleView.setText("Коментарі");
        }

        setupRecyclerView();

        if (authManager.isLoggedIn()) {
            fetchCurrentUser();
        } else {
            loadComments();
        }

        // request focus + show keyboard
        commentInput.requestFocus();
        commentInput.post(() -> {
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(commentInput, InputMethodManager.SHOW_IMPLICIT);
        });

        sendButton.setOnClickListener(v -> sendComment());
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setNestedScrollingEnabled(true);
        recyclerView.setFocusable(false);

        commentClickListener = new CommentAdapter.OnCommentClickListener() {
            @Override
            public void onReplyClick(Comment comment) {
                // prefill reply text
                commentInput.setText("@" + comment.getUserNickname() + " ");
                commentInput.setSelection(commentInput.getText().length());
                commentInput.requestFocus();
            }

            @Override
            public void onDeleteClick(Comment comment) {
                deleteComment(comment);
            }

            @Override
            public void onToggleReplies(Comment comment, int position) {
                // For full-screen we open nested replies as a new full-screen fragment (keeps back stack)
                CommentsFullScreenFragment replies = CommentsFullScreenFragment.newInstanceForReplies(chapterId, pageIndex, comment.getId());
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(com.example.mangaapp.R.id.nav_host_fragment_content_main, replies)
                        .addToBackStack("CommentsReplies")
                        .commit();
            }
        };

        adapter = new CommentAdapter(new ArrayList<>(), currentUserId, commentClickListener);
        recyclerView.setAdapter(adapter);
    }

    private void fetchCurrentUser() {
        com.example.mangaapp.api.AccountApiService accountService =
                AccountApiClient.getClient().create(com.example.mangaapp.api.AccountApiService.class);
        accountService.getMe("Bearer " + authManager.getAuthToken()).enqueue(new Callback<com.example.mangaapp.api.UserDto>() {
            @Override
            public void onResponse(Call<com.example.mangaapp.api.UserDto> call, Response<com.example.mangaapp.api.UserDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentUserId = response.body().getId();
                }
                adapter = new CommentAdapter(new ArrayList<>(), currentUserId, commentClickListener);
                recyclerView.setAdapter(adapter);
                loadComments();
            }

            @Override
            public void onFailure(Call<com.example.mangaapp.api.UserDto> call, Throwable t) {
                loadComments();
            }
        });
    }

    private void loadComments() {
        progressBar.setVisibility(View.VISIBLE);
        CommentApiService service = AccountApiClient.getClient().create(CommentApiService.class);

        Call<CommentApiService.CommentPagedDto> call;
        if (parentCommentId != null) {
            call = service.getReplies(parentCommentId, 0, 50);
        } else {
            call = service.getRootComments(chapterId, pageIndex, 0, 50);
        }

        call.enqueue(new Callback<CommentApiService.CommentPagedDto>() {
            @Override
            public void onResponse(Call<CommentApiService.CommentPagedDto> call, Response<CommentApiService.CommentPagedDto> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<Comment> items = response.body().getItems();
                    adapter.updateList(items);

                    // Keep focus and keyboard
                    commentInput.post(() -> {
                        commentInput.requestFocus();
                        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) imm.showSoftInput(commentInput, InputMethodManager.SHOW_IMPLICIT);
                    });
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

        CommentApiService.CreateCommentRequest request;
        if (pageIndex != null) {
            request = new CommentApiService.CreateCommentRequest(chapterId, text, parentCommentId, pageIndex);
        } else {
            request = new CommentApiService.CreateCommentRequest(chapterId, text, parentCommentId);
        }

        service.createComment(token, request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    commentInput.setText("");
                    loadComments();
                } else {
                    String msg = "Помилка відправки";
                    try { if (response.errorBody() != null) msg = response.errorBody().string(); } catch (Exception ignored) {}
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
        new AlertDialog.Builder(requireContext())
                .setTitle("Видалити коментар?")
                .setPositiveButton("Так", (dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);
                    CommentApiService service = AccountApiClient.getClient().create(CommentApiService.class);
                    String token = "Bearer " + authManager.getAuthToken();

                    service.deleteComment(token, comment.getId()).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            progressBar.setVisibility(View.GONE);
                            if (response.isSuccessful()) loadComments();
                            else Toast.makeText(getContext(), "Помилка видалення", Toast.LENGTH_SHORT).show();
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