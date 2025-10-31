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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mangaapp.API_MangaDex.MangaResponse;
import com.example.mangaapp.api.ApiClient;
import com.example.mangaapp.Manga;
import com.example.mangaapp.adapters.MangaAdapter;
import com.example.mangaapp.api.MangaApiService;
import com.example.mangaapp.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.util.Log;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.concurrent.atomic.AtomicInteger;
import android.graphics.Rect;

public class HomeFragment extends Fragment {
    private RecyclerView mangaGrid;
    private ProgressBar progressBar;
    private MangaAdapter adapter;
    private androidx.appcompat.widget.SearchView searchView;
    private String selectedLanguage = "en";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = requireContext().getSharedPreferences("manga_prefs", Context.MODE_PRIVATE);
        selectedLanguage = prefs.getString("selected_language", "en");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        SharedPreferences prefs = requireContext().getSharedPreferences("manga_prefs", Context.MODE_PRIVATE);
        selectedLanguage = prefs.getString("selected_language", "en");
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        mangaGrid = root.findViewById(R.id.manga_grid);
        progressBar = root.findViewById(R.id.progress_bar);
        searchView = root.findViewById(R.id.search_view);

        // Налаштування GridLayoutManager для центрування
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 3);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return 1;
            }
        });
        mangaGrid.setLayoutManager(layoutManager);
        
        // Додавання відступів для центрування
        int spacing = getResources().getDimensionPixelSize(R.dimen.grid_spacing);
        mangaGrid.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, 
                                     @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                outRect.left = spacing;
                outRect.right = spacing;
                outRect.top = spacing;
                outRect.bottom = spacing;
            }
        });

        int id = searchView.getContext().getResources().getIdentifier("android:id/search_src_text", null, null);
        android.widget.TextView textView = searchView.findViewById(id);
        if (textView != null) {
            textView.setTextColor(android.graphics.Color.WHITE);
            textView.setHintTextColor(android.graphics.Color.WHITE);
        }

        loadManga("");

        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                loadManga(query);
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    loadManga("");
                }
                return false;
            }
        });

        return root;
    }

    private void loadManga(String query) {
        progressBar.setVisibility(View.VISIBLE);
        MangaApiService apiService = ApiClient.getClient().create(MangaApiService.class);
        List<String> includes = new ArrayList<>();
        includes.add("cover_art");
        Call<MangaResponse> call = apiService.searchManga(query, 20, 0, includes);

        final List<Manga> mangaList = new ArrayList<>();

        call.enqueue(new Callback<MangaResponse>() {
            @Override
            public void onResponse(Call<MangaResponse> call, Response<MangaResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<MangaResponse.Data> dataList = response.body().getData();
                    if (dataList.isEmpty()) {
                        progressBar.setVisibility(View.GONE);
                        adapter = new MangaAdapter(mangaList, manga -> {});
                        mangaGrid.setAdapter(adapter);
                        return;
                    }
                    AtomicInteger counter = new AtomicInteger(0);
                    for (MangaResponse.Data data : dataList) {
                        // Створюємо фінальні копії змінних для використання у внутрішньому класі
                        final String mangaId = data.getId();
                        final Map<String, String> titles = data.getAttributes().getTitle();
                        String mangaTitle = null;
                        if (titles != null) {
                            // Try to get title in selected language first
                            mangaTitle = titles.get(selectedLanguage);
                            // If not available in selected language, try English
                            if (mangaTitle == null || mangaTitle.isEmpty()) {
                                mangaTitle = titles.get("en");
                            }
                            // If still not available, try any available language
                            if (mangaTitle == null || mangaTitle.isEmpty()) {
                                for (String title : titles.values()) {
                                    if (title != null && !title.isEmpty()) {
                                        mangaTitle = title;
                                        break;
                                    }
                                }
                            }
                        }
                        if (mangaTitle == null || mangaTitle.isEmpty()) {
                            mangaTitle = "No Title";
                        }
                        final String finalMangaTitle = mangaTitle;
                        final String mangaType = data.getAttributes().getTags().isEmpty() ?
                                "Manga" : data.getAttributes().getTags().get(0).getName();
                        // Дістаємо правильний fileName обкладинки з relationships
                        String coverFileName = null;
                        if (data.getRelationships() != null) {
                            for (com.example.mangaapp.API_MangaDex.MangaResponse.Relationship rel : data.getRelationships()) {
                                if ("cover_art".equals(rel.getType()) && rel.getAttributes() != null) {
                                    coverFileName = rel.getAttributes().getFileName();
                                    break;
                                }
                            }
                        }
                        final String mangaCoverUrl;
                        if (coverFileName != null) {
                            mangaCoverUrl = "https://uploads.mangadex.org/covers/" + mangaId + "/" + coverFileName;
                        } else {
                            mangaCoverUrl = null;
                        }
                        Log.e("MangaApp", "coverFileName: " + coverFileName + ", mangaCoverUrl: " + mangaCoverUrl);

                        if (finalMangaTitle != null && !finalMangaTitle.isEmpty()) {
                            final List<String> langs = new ArrayList<>();
                            langs.add(selectedLanguage);

                            apiService.getMangaChapters(mangaId, 1, 0, "asc", langs).enqueue(new Callback<com.example.mangaapp.API_MangaDex.ChapterFeedResponse>() {
                                @Override
                                public void onResponse(Call<com.example.mangaapp.API_MangaDex.ChapterFeedResponse> call, Response<com.example.mangaapp.API_MangaDex.ChapterFeedResponse> response) {
                                    if (response.isSuccessful() && response.body() != null && response.body().getData() != null && !response.body().getData().isEmpty()) {
                                        mangaList.add(new Manga(mangaId, finalMangaTitle, mangaType, mangaCoverUrl));
                                    }
                                    if (counter.incrementAndGet() == dataList.size()) {
                                        progressBar.setVisibility(View.GONE);
                                        adapter = new MangaAdapter(mangaList, manga -> {
                                            Bundle bundle = new Bundle();
                                            bundle.putString("manga_id", manga.getId());
                                            try {
                                                Navigation.findNavController(requireView()).navigate(R.id.action_homeFragment_to_mangaDetailFragment, bundle);
                                            } catch (Exception e) {
                                                Log.e("MangaApp", "Navigation error", e);
                                            }
                                        });
                                        mangaGrid.setAdapter(adapter);
                                    }
                                }
                                @Override
                                public void onFailure(Call<com.example.mangaapp.API_MangaDex.ChapterFeedResponse> call, Throwable t) {
                                    if (counter.incrementAndGet() == dataList.size()) {
                                        progressBar.setVisibility(View.GONE);
                                        adapter = new MangaAdapter(mangaList, manga -> {});
                                        mangaGrid.setAdapter(adapter);
                                    }
                                }
                            });
                        } else {
                            if (counter.incrementAndGet() == dataList.size()) {
                                progressBar.setVisibility(View.GONE);
                                adapter = new MangaAdapter(mangaList, manga -> {});
                                mangaGrid.setAdapter(adapter);
                            }
                        }
                    }
                } else {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Помилка завантаження даних", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<MangaResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Помилка: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}