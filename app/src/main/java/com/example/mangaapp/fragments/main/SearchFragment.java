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
import com.example.mangaapp.API_MangaDex.TagResponse;
import com.example.mangaapp.api.ApiClient;
import com.example.mangaapp.Manga;
import com.example.mangaapp.adapters.MangaAdapter;
import com.example.mangaapp.adapters.GenreAdapter;
import com.example.mangaapp.MangaApiService;
import com.example.mangaapp.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.button.MaterialButton;

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

public class SearchFragment extends Fragment {
    private RecyclerView mangaGrid;
    private ProgressBar progressBar;
    private MangaAdapter adapter;
    private androidx.appcompat.widget.SearchView searchView;
    private MaterialButton genreButton;
    private String selectedLanguage = "en";
    private List<String> selectedGenreIds = new ArrayList<>();

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
        View root = inflater.inflate(R.layout.fragment_search, container, false);

        mangaGrid = root.findViewById(R.id.manga_grid);
        progressBar = root.findViewById(R.id.progress_bar);
        searchView = root.findViewById(R.id.search_view);
        genreButton = root.findViewById(R.id.genre_button);

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

        // Обробник натискання на кнопку "Жанр"
        genreButton.setOnClickListener(v -> {
            showGenreSelectionDialog();
        });

        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty() && selectedGenreIds.isEmpty()) {
                    // Якщо поле порожнє і жанри не вибрані, очищаємо результат
                    adapter = new MangaAdapter(new ArrayList<>(), manga -> {});
                    mangaGrid.setAdapter(adapter);
                } else {
                    performSearch(newText);
                }
                return false;
            }
        });

        return root;
    }

    private void performSearch(String query) {
        String searchQuery = (query != null) ? query : "";
        boolean hasQuery = !searchQuery.isEmpty();
        boolean hasGenres = !selectedGenreIds.isEmpty();

        if (!hasQuery && !hasGenres) {
            // Якщо нічого не введено і жанри не вибрані, очищаємо результат
            adapter = new MangaAdapter(new ArrayList<>(), manga -> {});
            mangaGrid.setAdapter(adapter);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        MangaApiService apiService = ApiClient.getClient().create(MangaApiService.class);
        List<String> includes = new ArrayList<>();
        includes.add("cover_art");

        Call<MangaResponse> call;
        if (hasGenres) {
            // Пошук за назвою та жанрами (або тільки за жанрами)
            Log.d("SearchFragment", "Searching with genres: " + selectedGenreIds.size() + " genres");
            Log.d("SearchFragment", "Genre IDs: " + selectedGenreIds.toString());
            if (hasQuery) {
                // Пошук за назвою та жанрами
                Log.d("SearchFragment", "Title: " + searchQuery);
                call = apiService.searchMangaWithTags("manga", searchQuery, selectedGenreIds, 20, 0, includes);
            } else {
                // Пошук тільки за жанрами
                Log.d("SearchFragment", "Searching by tags only");
                call = apiService.searchMangaByTagsOnly("manga", selectedGenreIds, 20, 0, includes);
            }
        } else {
            // Звичайний пошук за назвою
            Log.d("SearchFragment", "Searching by title: " + searchQuery);
            call = apiService.searchManga("manga", searchQuery, 20, 0, includes);
        }

        processMangaResponse(call, apiService);
    }

    private void processMangaResponse(Call<MangaResponse> call, MangaApiService apiService) {
        final List<Manga> mangaList = new ArrayList<>();

        call.enqueue(new Callback<MangaResponse>() {
            @Override
            public void onResponse(Call<MangaResponse> call, Response<MangaResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<MangaResponse.Data> dataList = response.body().getData();
                    if (dataList == null || dataList.isEmpty()) {
                        progressBar.setVisibility(View.GONE);
                        adapter = new MangaAdapter(mangaList, manga -> {});
                        mangaGrid.setAdapter(adapter);
                        return;
                    }
                    AtomicInteger counter = new AtomicInteger(0);
                    for (MangaResponse.Data data : dataList) {
                        // Перевірка на null
                        if (data == null || data.getId() == null || data.getAttributes() == null) {
                            if (counter.incrementAndGet() == dataList.size()) {
                                progressBar.setVisibility(View.GONE);
                                adapter = new MangaAdapter(mangaList, manga -> {});
                                mangaGrid.setAdapter(adapter);
                            }
                            continue;
                        }
                        
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
                        final String mangaType;
                        if (data.getAttributes().getTags() != null && !data.getAttributes().getTags().isEmpty()) {
                            mangaType = data.getAttributes().getTags().get(0).getName();
                        } else {
                            mangaType = "Manga";
                        }
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

                            apiService.getMangaChapters("manga/" + mangaId + "/feed", 1, 0, "asc", langs).enqueue(new Callback<com.example.mangaapp.API_MangaDex.ChapterFeedResponse>() {
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
                                                Navigation.findNavController(requireView()).navigate(R.id.action_searchFragment_to_mangaDetailFragment, bundle);
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
                    String errorMessage = "Помилка завантаження даних";
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e("SearchFragment", "Error response: " + errorBody);
                            if (errorBody.contains("400") || errorBody.contains("Bad Request")) {
                                errorMessage = "Невірні параметри пошуку. Спробуйте інші жанри.";
                            }
                        }
                    } catch (Exception e) {
                        Log.e("SearchFragment", "Error reading error body", e);
                    }
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                    // Показуємо порожній список замість краша
                    adapter = new MangaAdapter(new ArrayList<>(), manga -> {});
                    mangaGrid.setAdapter(adapter);
                }
            }
            @Override
            public void onFailure(Call<MangaResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Помилка: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showGenreSelectionDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_genre_search, null);
        RecyclerView genreRecyclerView = dialogView.findViewById(R.id.genre_recycler_view);
        android.widget.ProgressBar genreProgressBar = dialogView.findViewById(R.id.genre_progress_bar);

        // Налаштування RecyclerView для жанрів
        androidx.recyclerview.widget.GridLayoutManager genreLayoutManager = 
            new androidx.recyclerview.widget.GridLayoutManager(getContext(), 3);
        genreRecyclerView.setLayoutManager(genreLayoutManager);

        GenreAdapter genreAdapter = new GenreAdapter();
        // Зберігаємо копію вибраних жанрів для відновлення
        final List<String> savedSelectedGenres = new ArrayList<>(selectedGenreIds);
        genreRecyclerView.setAdapter(genreAdapter);

        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Пошук", (dialog, which) -> {
                    selectedGenreIds = genreAdapter.getSelectedGenreIds();
                    String query = searchView.getQuery().toString();
                    performSearch(query);
                })
                .setNegativeButton("Скасувати", (dialog, which) -> dialog.dismiss())
                .setNeutralButton("Очистити", (dialog, which) -> {
                    genreAdapter.clearSelection();
                    selectedGenreIds.clear();
                });

        android.app.Dialog dialog = dialogBuilder.create();
        dialog.show();

        // Завантажуємо список жанрів
        loadGenres(genreAdapter, genreProgressBar, genreRecyclerView, savedSelectedGenres);
    }

    private void loadGenres(GenreAdapter adapter, android.widget.ProgressBar progressBar, RecyclerView recyclerView, List<String> savedSelectedGenres) {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        MangaApiService apiService = ApiClient.getClient().create(MangaApiService.class);
        Call<TagResponse> call = apiService.getTags("manga/tag");

        call.enqueue(new Callback<TagResponse>() {
            @Override
            public void onResponse(Call<TagResponse> call, Response<TagResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<TagResponse.Data> tagDataList = response.body().getData();
                    List<GenreAdapter.GenreItem> genres = new ArrayList<>();

                    SharedPreferences prefs = requireContext().getSharedPreferences("manga_prefs", Context.MODE_PRIVATE);
                    String lang = prefs.getString("selected_language", "en");

                    for (TagResponse.Data tagData : tagDataList) {
                        if (tagData.getAttributes() != null && tagData.getAttributes().getName() != null) {
                            Map<String, String> nameMap = tagData.getAttributes().getName();
                            String name = nameMap.get(lang);
                            if (name == null || name.isEmpty()) {
                                name = nameMap.get("en");
                            }
                            if (name == null || name.isEmpty()) {
                                // Якщо немає назви, пропускаємо
                                continue;
                            }
                            String group = tagData.getAttributes().getGroup();
                            // Показуємо тільки жанри (genre), теми (theme) та формати (format)
                            if ("genre".equals(group) || "theme".equals(group) || "format".equals(group)) {
                                genres.add(new GenreAdapter.GenreItem(tagData.getId(), name, group));
                            }
                        }
                    }

                    adapter.setGenres(genres);
                    // Відновлюємо вибрані жанри після завантаження
                    if (!savedSelectedGenres.isEmpty()) {
                        adapter.setSelectedGenreIds(savedSelectedGenres);
                    }
                    recyclerView.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(getContext(), "Помилка завантаження жанрів", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<TagResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Помилка: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("SearchFragment", "Error loading genres", t);
            }
        });
    }
}

