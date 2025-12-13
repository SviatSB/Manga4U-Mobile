package com.example.mangaapp.fragments.main;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mangaapp.R;
import com.example.mangaapp.adapters.ReviewAdapter;
import com.example.mangaapp.api.AccountApiClient;
import com.example.mangaapp.api.ReviewApiService;
import com.example.mangaapp.utils.AuthManager;
import com.example.mangaapp.models.Review;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReviewsFragment extends Fragment {
    private static final String ARG_MANGA_ID = "manga_id";
    private String mangaId;
    private RecyclerView recyclerView;
    private ReviewAdapter adapter;
    private ProgressBar progressBar;
    private TextView averageRatingText;
    private ExtendedFloatingActionButton addReviewFab;
    private AuthManager authManager;
    private long currentUserId = -1;

    public static ReviewsFragment newInstance(String mangaId) {
        ReviewsFragment fragment = new ReviewsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MANGA_ID, mangaId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mangaId = getArguments().getString(ARG_MANGA_ID);
        }
        authManager = AuthManager.getInstance(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reviews, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        recyclerView = view.findViewById(R.id.reviews_recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        averageRatingText = view.findViewById(R.id.average_rating_text);
        addReviewFab = view.findViewById(R.id.add_review_fab);

        // Get current user ID if logged in
        if (authManager.isLoggedIn()) {
             // We can get user ID from stored info or fetch it. For now assume we might fetch it or have it.
             // Simplification: We will fetch "me" or rely on cached info if available. 
             // To make it simple, let's fetch 'me' to be sure about ID for deletion logic.
             fetchCurrentUser();
        } else {
             setupRecyclerView();
             loadReviews();
        }

        addReviewFab.setOnClickListener(v -> showAddReviewDialog());
    }

    private void fetchCurrentUser() {
        // Fetch current user ID to allow deleting own reviews
        com.example.mangaapp.api.AccountApiService accountService = AccountApiClient.getClient().create(com.example.mangaapp.api.AccountApiService.class);
        accountService.getMe("Bearer " + authManager.getAuthToken()).enqueue(new Callback<com.example.mangaapp.api.UserDto>() {
            @Override
            public void onResponse(Call<com.example.mangaapp.api.UserDto> call, Response<com.example.mangaapp.api.UserDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentUserId = response.body().getId();
                }
                setupRecyclerView();
                loadReviews();
            }

            @Override
            public void onFailure(Call<com.example.mangaapp.api.UserDto> call, Throwable t) {
                setupRecyclerView();
                loadReviews();
            }
        });
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ReviewAdapter(new ArrayList<>(), currentUserId, this::deleteReview);
        recyclerView.setAdapter(adapter);
    }

    private void loadReviews() {
        progressBar.setVisibility(View.VISIBLE);
        ReviewApiService service = AccountApiClient.getClient().create(ReviewApiService.class);
        
        // Load average rating
        service.getAverageRating(mangaId).enqueue(new Callback<ReviewApiService.AverageRatingResponse>() {
            @Override
            public void onResponse(Call<ReviewApiService.AverageRatingResponse> call, Response<ReviewApiService.AverageRatingResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    double avg = response.body().getAverage();
                    averageRatingText.setText(String.format("Середній рейтинг: %.1f ⭐", avg));
                }
            }
            @Override
            public void onFailure(Call<ReviewApiService.AverageRatingResponse> call, Throwable t) {}
        });

        // Load reviews list
        service.getReviews(mangaId, 0, 100).enqueue(new Callback<ReviewApiService.ReviewPagedDto>() {
            @Override
            public void onResponse(Call<ReviewApiService.ReviewPagedDto> call, Response<ReviewApiService.ReviewPagedDto> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<Review> items = response.body().getItems();
                    adapter.updateList(items);
                    
                    // Check if user already reviewed (optional logic could be added here to hide FAB)
                }
            }

            @Override
            public void onFailure(Call<ReviewApiService.ReviewPagedDto> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Помилка завантаження відгуків", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddReviewDialog() {
        if (!authManager.isLoggedIn()) {
            Toast.makeText(getContext(), "Будь ласка, увійдіть в акаунт", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_review, null);
        
        RatingBar ratingBar = view.findViewById(R.id.rating_bar);
        EditText reviewText = view.findViewById(R.id.review_text_input);
        
        builder.setView(view)
                .setPositiveButton("Надіслати", (dialog, which) -> {
                    int stars = (int) ratingBar.getRating();
                    if (stars < 1) {
                        Toast.makeText(getContext(), "Поставте оцінку від 1 до 5", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    submitReview(stars, reviewText.getText().toString());
                })
                .setNegativeButton("Скасувати", null)
                .show();
    }

    private void submitReview(int stars, String text) {
        progressBar.setVisibility(View.VISIBLE);
        ReviewApiService service = AccountApiClient.getClient().create(ReviewApiService.class);
        String token = "Bearer " + authManager.getAuthToken();
        
        ReviewApiService.AddReviewRequest request = new ReviewApiService.AddReviewRequest(mangaId, stars, text);
        
        service.addReview(token, request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Відгук додано!", Toast.LENGTH_SHORT).show();
                    loadReviews(); // Reload list
                } else {
                    String msg = "Помилка: " + response.code();
                    try { if(response.errorBody() != null) msg = response.errorBody().string(); } catch(Exception e){}
                    Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Помилка мережі", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteReview(Review review) {
        new AlertDialog.Builder(getContext())
                .setTitle("Видалити відгук?")
                .setPositiveButton("Так", (dialog, which) -> {
                     progressBar.setVisibility(View.VISIBLE);
                     ReviewApiService service = AccountApiClient.getClient().create(ReviewApiService.class);
                     String token = "Bearer " + authManager.getAuthToken();
                     
                     service.deleteReview(token, review.getId()).enqueue(new Callback<Void>() {
                         @Override
                         public void onResponse(Call<Void> call, Response<Void> response) {
                             if (response.isSuccessful()) {
                                 Toast.makeText(getContext(), "Відгук видалено", Toast.LENGTH_SHORT).show();
                                 loadReviews();
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
