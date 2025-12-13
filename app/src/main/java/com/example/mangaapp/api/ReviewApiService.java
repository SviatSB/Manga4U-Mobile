package com.example.mangaapp.api;

import com.example.mangaapp.models.Review;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ReviewApiService {
    @GET("api/Review/manga/{mangaId}")
    Call<ReviewPagedDto> getReviews(
        @Path("mangaId") String mangaId,
        @Query("skip") int skip,
        @Query("take") int take
    );

    @GET("api/Review/manga/{mangaId}/avg")
    Call<AverageRatingResponse> getAverageRating(@Path("mangaId") String mangaId);

    @POST("api/Review")
    Call<Void> addReview(@Header("Authorization") String token, @Body AddReviewRequest request);

    @DELETE("api/Review/{reviewId}")
    Call<Void> deleteReview(@Header("Authorization") String token, @Path("reviewId") long reviewId);

    class ReviewPagedDto {
        private int totalCount;
        private List<Review> items;

        public int getTotalCount() { return totalCount; }
        public List<Review> getItems() { return items; }
    }

    class AverageRatingResponse {
        private double average;
        public double getAverage() { return average; }
    }

    class AddReviewRequest {
        private String mangaExternalId;
        private int stars;
        private String text;

        public AddReviewRequest(String mangaExternalId, int stars, String text) {
            this.mangaExternalId = mangaExternalId;
            this.stars = stars;
            this.text = text;
        }
    }
}
