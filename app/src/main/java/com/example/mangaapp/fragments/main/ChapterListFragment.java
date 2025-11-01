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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;

import com.example.mangaapp.api.ApiClient;
import com.example.mangaapp.adapters.ChapterAdapter;
import com.example.mangaapp.MangaApiService;
import com.example.mangaapp.R;

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
        Call<com.example.mangaapp.API_MangaDex.ChapterFeedResponse> call = apiService.getMangaChapters(mangaId, 100, 0, "asc", langs);

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
}