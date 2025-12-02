package com.example.mangaapp.fragments.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.navigation.Navigation;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import retrofit2.Callback;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;

import com.example.mangaapp.api.ApiClient;
import com.example.mangaapp.adapters.ChapterAdapter;
import com.example.mangaapp.MangaApiService;
import com.example.mangaapp.R;
import com.example.mangaapp.api.AccountApiService;
import com.example.mangaapp.api.AccountApiClient;
import com.example.mangaapp.utils.AuthManager;
import okhttp3.ResponseBody;

import java.util.List;
import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChapterListFragment extends Fragment {
    private RecyclerView chaptersRecyclerView;
    private ProgressBar progressBar;
    private ChapterAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_chapter_list, container, false);

        chaptersRecyclerView = root.findViewById(R.id.chapters_list);
        progressBar = root.findViewById(R.id.progress_bar);

        chaptersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        String mangaId = getArguments() != null ? getArguments().getString("manga_id") : "";
        ArrayList<String> selectedLangs = getArguments() != null ? getArguments().getStringArrayList("selected_languages") : null;
        Log.e("MangaApp", "[ChapterListFragment] Loading chapters for mangaId: " + mangaId);

        if (!mangaId.isEmpty()) {
            loadChapters(mangaId, selectedLangs);
        }

        return root;
    }

    private void loadChapters(String mangaId, ArrayList<String> selectedLangs) {
        progressBar.setVisibility(View.VISIBLE);

        MangaApiService apiService = ApiClient.getClient().create(MangaApiService.class);
        List<String> langs;
        if (selectedLangs != null && !selectedLangs.isEmpty()) {
            langs = selectedLangs;
        } else {
            langs = new ArrayList<>();
            langs.add("en");
            langs.add("uk");
            langs.add("ru");
            langs.add("ja");
        }
        Call<com.example.mangaapp.API_MangaDex.ChapterFeedResponse> call = apiService.getMangaChapters("manga/" + mangaId + "/feed", 100, 0, "asc", langs);

        call.enqueue(new Callback<com.example.mangaapp.API_MangaDex.ChapterFeedResponse>() {
            @Override
            public void onResponse(Call<com.example.mangaapp.API_MangaDex.ChapterFeedResponse> call, Response<com.example.mangaapp.API_MangaDex.ChapterFeedResponse> response) {
                progressBar.setVisibility(View.GONE);
                Log.e("MangaApp", "[ChapterListFragment] onResponse: " + response.toString());
                if (response.errorBody() != null) {
                    try {
                        Log.e("MangaApp", "[ChapterListFragment] Error body: " + response.errorBody().string());
                    } catch (Exception e) {
                        Log.e("MangaApp", "[ChapterListFragment] Error reading errorBody", e);
                    }
                }
                if (response.isSuccessful() && response.body() != null) {
                    List<com.example.mangaapp.API_MangaDex.ChapterFeedResponse.Result> chapters = response.body().getData();
                    if (chapters == null) chapters = new ArrayList<>();
                    adapter = new ChapterAdapter(chapters, chapter -> {
                        Log.e("MangaApp", "[ChapterListFragment] Chapter clicked: " + chapter.getId());
                        Log.d("ChapterListFragment", "MangaId: " + mangaId + ", ChapterId: " + chapter.getId());
                        
                        // Записуємо історію при кліку на главу
                        updateHistory(mangaId, chapter);
                        
                        Bundle bundle = new Bundle();
                        bundle.putString("chapterId", chapter.getId());
                        bundle.putString("mangaId", mangaId);
                        try {
                            Navigation.findNavController(requireView())
                                    .navigate(R.id.action_chapterListFragment_to_readerFragment, bundle);
                        } catch (Exception e) {
                            Log.e("MangaApp", "[ChapterListFragment] Navigation error: ", e);
                        }
                    });
                    chaptersRecyclerView.setAdapter(adapter);
                } else {
                    Log.e("MangaApp", "[ChapterListFragment] Response not successful or body null");
                    Toast.makeText(getContext(), "Failed to load chapters", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<com.example.mangaapp.API_MangaDex.ChapterFeedResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Log.e("MangaApp", "[ChapterListFragment] Failure: " + t.getMessage(), t);
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void updateHistory(String mangaId, com.example.mangaapp.API_MangaDex.ChapterFeedResponse.Result chapter) {
        Log.d("ChapterListFragment", "updateHistory called for mangaId: " + mangaId);
        AuthManager authManager = AuthManager.getInstance(requireContext());
        if (!authManager.isLoggedIn()) {
            Log.d("ChapterListFragment", "User not logged in, skipping history update");
            return;
        }
        
        String token = authManager.getAuthToken();
        if (token == null) {
            Log.e("ChapterListFragment", "Auth token is null, cannot update history");
            return;
        }
        
        Log.d("ChapterListFragment", "User is logged in, proceeding with history update");
        
        // Отримуємо мову з SharedPreferences
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("manga_prefs", android.content.Context.MODE_PRIVATE);
        String language = prefs.getString("selected_language", "en");
        
        // Отримуємо інформацію про главу
        String chapterId = chapter.getId();
        String chapterTitle = "Перегляд манги"; // Значення за замовчуванням
        int chapterNumber = 1; // Значення за замовчуванням (валідатор вимагає > 0)
        
        if (chapter.getAttributes() != null) {
            if (chapter.getAttributes().getTitle() != null && !chapter.getAttributes().getTitle().isEmpty()) {
                chapterTitle = chapter.getAttributes().getTitle();
            }
            String chapterNumStr = chapter.getAttributes().getChapter();
            if (chapterNumStr != null && !chapterNumStr.isEmpty()) {
                try {
                    String[] parts = chapterNumStr.split("\\.");
                    float num = Float.parseFloat(parts[0]);
                    chapterNumber = (int) num;
                    if (chapterNumber <= 0) {
                        chapterNumber = 1; // Якщо отримали 0 або менше, використовуємо 1
                    }
                } catch (NumberFormatException e) {
                    Log.e("ChapterListFragment", "Error parsing chapter number: " + chapterNumStr, e);
                    chapterNumber = 1; // Якщо не вдалося розпарсити, використовуємо 1
                }
            }
        }
        
        // Перевірка на null/порожні значення
        if (mangaId == null || mangaId.isEmpty()) {
            Log.e("ChapterListFragment", "MangaId is null or empty, cannot update history");
            return;
        }
        if (chapterId == null || chapterId.isEmpty()) {
            Log.e("ChapterListFragment", "ChapterId is null or empty, cannot update history");
            return;
        }
        if (language == null || language.isEmpty()) {
            language = "en"; // Значення за замовчуванням
        }
        
        AccountApiService apiService = AccountApiClient.getClient().create(AccountApiService.class);
        AccountApiService.UpdateHistoryRequest request = new AccountApiService.UpdateHistoryRequest(
            mangaId,
            chapterId,
            language,
            chapterTitle,
            chapterNumber
        );
        
        Log.d("ChapterListFragment", "Sending history update request:");
        Log.d("ChapterListFragment", "  MangaId: " + mangaId);
        Log.d("ChapterListFragment", "  ChapterId: " + chapterId);
        Log.d("ChapterListFragment", "  Language: " + language);
        Log.d("ChapterListFragment", "  ChapterTitle: " + chapterTitle);
        Log.d("ChapterListFragment", "  ChapterNumber: " + chapterNumber);
        
        Call<ResponseBody> call = apiService.updateHistory("Bearer " + token, request);
        call.enqueue(new retrofit2.Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<ResponseBody> call, @NonNull retrofit2.Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d("ChapterListFragment", "History updated successfully - Status: " + response.code());
                } else {
                    Log.e("ChapterListFragment", "Error updating history: " + response.code());
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            Log.e("ChapterListFragment", "Error body: " + errorBody);
                        } catch (Exception e) {
                            Log.e("ChapterListFragment", "Error reading error body", e);
                        }
                    }
                }
            }
            
            @Override
            public void onFailure(@NonNull retrofit2.Call<ResponseBody> call, @NonNull Throwable t) {
                Log.e("ChapterListFragment", "Error updating history", t);
                Log.e("ChapterListFragment", "Failure message: " + t.getMessage());
            }
        });
    }
}