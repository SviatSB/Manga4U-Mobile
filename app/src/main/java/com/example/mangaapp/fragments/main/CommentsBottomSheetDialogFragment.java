package com.example.mangaapp.fragments.main;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mangaapp.R;
import com.example.mangaapp.adapters.CommentAdapter;
import com.example.mangaapp.api.AccountApiClient;
import com.example.mangaapp.api.CommentApiService;
import com.example.mangaapp.models.Comment;
import com.example.mangaapp.utils.AuthManager;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Bottom sheet with inline expandable replies (loads 3 at a time).
 */
public class CommentsBottomSheetDialogFragment extends BottomSheetDialogFragment {
    private static final String ARG_CHAPTER_ID = "chapter_id";
    private static final String ARG_PAGE_INDEX = "page_index";
    private static final int REPLIES_PAGE_SIZE = 3;

    private String chapterId;
    private Integer pageIndex;

    private RecyclerView recyclerView;
    private CommentAdapter adapter;
    private ProgressBar progressBar;
    private EditText commentInput;
    private ImageButton sendButton;
    private TextView titleView;
    private AuthManager authManager;
    private long currentUserId = -1;

    // per-parent tracking: how many replies already loaded and whether expanded
    private final Map<Long, Integer> repliesLoadedCount = new HashMap<>();
    private final Map<Long, Boolean> repliesExpanded = new HashMap<>();

    private final CommentAdapter.OnCommentClickListener commentClickListener = new CommentAdapter.OnCommentClickListener() {
        @Override
        public void onReplyClick(Comment comment) {
            commentInput.setText("@" + comment.getUserNickname() + " ");
            commentInput.setSelection(commentInput.getText().length());
            commentInput.requestFocus();

            // Показуємо клавіатуру
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(commentInput, InputMethodManager.SHOW_IMPLICIT);
            }
        }

        @Override
        public void onDeleteClick(Comment comment) {
            deleteComment(comment);
        }

        @Override
        public void onToggleReplies(Comment comment, int position) {
            handleToggleReplies(comment, position);
        }
    };

    public static CommentsBottomSheetDialogFragment newInstance(String chapterId, Integer pageIndex) {
        CommentsBottomSheetDialogFragment fragment = new CommentsBottomSheetDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CHAPTER_ID, chapterId);
        if (pageIndex != null) args.putInt(ARG_PAGE_INDEX, pageIndex);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            chapterId = getArguments().getString(ARG_CHAPTER_ID);
            if (getArguments().containsKey(ARG_PAGE_INDEX)) {
                pageIndex = getArguments().getInt(ARG_PAGE_INDEX);
            }
        }
        authManager = AuthManager.getInstance(requireContext());
    }

    @Override
    public void onStart() {
        super.onStart();
        // Configure bottom sheet to expand to full height while allowing collapse
        if (getDialog() != null) {
            View bottomSheet = getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
                behavior.setHideable(true);

                int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
                behavior.setPeekHeight(screenHeight * 2 / 3); // 2/3 висоти екрану

                // Prevent dismissing when clicking outside
                behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                    @Override
                    public void onStateChanged(@NonNull View bottomSheet, int newState) {
                        if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                            dismiss();
                        }
                    }

                    @Override
                    public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                        // Optional: handle slide events
                    }
                });
            }

            // Set dialog properties
            getDialog().setCanceledOnTouchOutside(false);
            getDialog().setCancelable(true);

            // Make dialog full width
            if (getDialog().getWindow() != null) {
                getDialog().getWindow().setLayout(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                );
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
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

        // Додати кнопку "Назад"
        ImageButton btnBack = view.findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> {
            dismiss();
        });

        // Встановлюємо заголовок - ВИПРАВЛЕНО
        titleView.setText("Коментарі до глави");

        // Налаштовуємо RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setNestedScrollingEnabled(true);
        recyclerView.setHasFixedSize(false);

        adapter = new CommentAdapter(new ArrayList<>(), currentUserId, commentClickListener);
        recyclerView.setAdapter(adapter);

        // Завантажуємо дані
        if (authManager.isLoggedIn()) {
            fetchCurrentUser();
        } else {
            loadComments();
        }

        // Налаштовуємо поле вводу
        commentInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // Прокручуємо донизу при фокусі на полі вводу
                recyclerView.postDelayed(() -> {
                    if (adapter.getItemCount() > 0) {
                        recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
                    }
                }, 100);
            }
        });

        // Відкриття клавіатури при відкритті
        commentInput.postDelayed(() -> {
            commentInput.requestFocus();
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(commentInput, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 200);

        sendButton.setOnClickListener(v -> sendComment());

        // Обробка натискання Enter для відправки
        commentInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendComment();
                return true;
            }
            return false;
        });
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        // Ховаємо клавіатуру при закритті
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && commentInput != null) {
            imm.hideSoftInputFromWindow(commentInput.getWindowToken(), 0);
        }
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
                adapter.setCurrentUserId(currentUserId);
                adapter.setOnCommentClickListener(commentClickListener);
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
        Call<CommentApiService.CommentPagedDto> call = service.getRootComments(chapterId, pageIndex, 0, 50);
        call.enqueue(new Callback<CommentApiService.CommentPagedDto>() {
            @Override
            public void onResponse(Call<CommentApiService.CommentPagedDto> call, Response<CommentApiService.CommentPagedDto> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<Comment> comments = response.body().getItems();
                    adapter.updateList(comments);
                    repliesLoadedCount.clear();
                    repliesExpanded.clear();

                    // Прокручуємо донизу після завантаження
                    recyclerView.postDelayed(() -> {
                        if (comments.size() > 0) {
                            recyclerView.smoothScrollToPosition(comments.size() - 1);
                        }
                    }, 300);
                } else {
                    Toast.makeText(getContext(), "Помилка завантаження коментарів", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<CommentApiService.CommentPagedDto> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Помилка мережі", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleToggleReplies(Comment parentComment, int parentAdapterPosition) {
        long parentId = parentComment.getId();
        boolean expanded = repliesExpanded.getOrDefault(parentId, false);

        if (!expanded) {
            int alreadyLoaded = repliesLoadedCount.getOrDefault(parentId, 0);
            CommentApiService service = AccountApiClient.getClient().create(CommentApiService.class);
            progressBar.setVisibility(View.VISIBLE);
            service.getReplies(parentId, alreadyLoaded, REPLIES_PAGE_SIZE).enqueue(new Callback<CommentApiService.CommentPagedDto>() {
                @Override
                public void onResponse(Call<CommentApiService.CommentPagedDto> call, Response<CommentApiService.CommentPagedDto> response) {
                    progressBar.setVisibility(View.GONE);
                    if (response.isSuccessful() && response.body() != null) {
                        List<Comment> replies = response.body().getItems();
                        if (!replies.isEmpty()) {
                            adapter.insertReplies(parentAdapterPosition, replies);
                            repliesLoadedCount.put(parentId, alreadyLoaded + replies.size());
                            repliesExpanded.put(parentId, true);

                            // Прокручуємо до завантажених відповідей
                            recyclerView.postDelayed(() -> {
                                recyclerView.smoothScrollToPosition(parentAdapterPosition + replies.size());
                            }, 100);
                        }
                    } else {
                        Toast.makeText(getContext(), "Помилка завантаження відповідей", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<CommentApiService.CommentPagedDto> call, Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Помилка мережі", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            int removed = adapter.collapseRepliesForParent(parentId);
            repliesExpanded.put(parentId, false);
            repliesLoadedCount.put(parentId, Math.max(0, repliesLoadedCount.getOrDefault(parentId, 0) - REPLIES_PAGE_SIZE));
        }
    }

    private void sendComment() {
        if (!authManager.isLoggedIn()) {
            Toast.makeText(getContext(), "Увійдіть в акаунт", Toast.LENGTH_SHORT).show();
            return;
        }

        String text = commentInput.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(getContext(), "Введіть текст коментаря", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        CommentApiService service = AccountApiClient.getClient().create(CommentApiService.class);
        String token = "Bearer " + authManager.getAuthToken();

        // Перевіряємо, чи це відповідь на інший коментар
        Long parentId = null;
        String cleanedText = text;

        if (text.startsWith("@")) {
            // Шукаємо коментар, на який відповідаємо
            for (int i = 0; i < adapter.getItemCount(); i++) {
                Comment comment = adapter.getCommentAtPosition(i);
                if (comment != null && text.startsWith("@" + comment.getUserNickname() + " ")) {
                    parentId = comment.getId();
                    // Видаляємо префікс з тексту
                    cleanedText = text.substring(comment.getUserNickname().length() + 2).trim();
                    break;
                }
            }
        }

        CommentApiService.CreateCommentRequest request = new CommentApiService.CreateCommentRequest(
                chapterId, cleanedText, parentId, pageIndex
        );

        service.createComment(token, request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    commentInput.setText("");
                    loadComments();

                    // Ховаємо клавіатуру після відправки
                    InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(commentInput.getWindowToken(), 0);
                    }

                    Toast.makeText(getContext(), "Коментар опубліковано", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Помилка відправки", Toast.LENGTH_SHORT).show();
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
                .setMessage("Ви дійсно хочете видалити цей коментар? Цю дію не можна скасувати.")
                .setPositiveButton("Видалити", (dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);
                    CommentApiService service = AccountApiClient.getClient().create(CommentApiService.class);
                    String token = "Bearer " + authManager.getAuthToken();
                    service.deleteComment(token, comment.getId()).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            progressBar.setVisibility(View.GONE);
                            if (response.isSuccessful()) {
                                loadComments();
                                Toast.makeText(getContext(), "Коментар видалено", Toast.LENGTH_SHORT).show();
                            } else {
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
                .setNegativeButton("Скасувати", null)
                .show();
    }
}