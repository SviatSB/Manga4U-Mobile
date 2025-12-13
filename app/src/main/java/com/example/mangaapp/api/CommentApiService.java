package com.example.mangaapp.api;

import com.example.mangaapp.models.Comment;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface CommentApiService {
    @GET("api/Comment/root")
    Call<CommentPagedDto> getRootComments(
        @Query("chapterId") String chapterId,
        @Query("skip") int skip,
        @Query("take") int take
    );

    @GET("api/Comment/{commentId}/replies")
    Call<CommentPagedDto> getReplies(
        @Path("commentId") long commentId,
        @Query("skip") int skip,
        @Query("take") int take
    );

    @POST("api/Comment")
    Call<Void> createComment(@Header("Authorization") String token, @Body CreateCommentRequest request);

    @DELETE("api/Comment/{commentId}")
    Call<Void> deleteComment(@Header("Authorization") String token, @Path("commentId") long commentId);

    class CommentPagedDto {
        private int totalCount;
        private int replyCount;
        private List<Comment> items;

        public int getTotalCount() { return totalCount; }
        public int getReplyCount() { return replyCount; }
        public List<Comment> getItems() { return items; }
    }

    class CreateCommentRequest {
        private String mangaChapterExternalId;
        private String text;
        private Long parentCommentId;

        public CreateCommentRequest(String mangaChapterExternalId, String text, Long parentCommentId) {
            this.mangaChapterExternalId = mangaChapterExternalId;
            this.text = text;
            this.parentCommentId = parentCommentId;
        }
    }
}
