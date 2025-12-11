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
import com.example.mangaapp.MangaApiService;
import com.example.mangaapp.R;
import com.example.mangaapp.utils.AuthManager;
import com.example.mangaapp.api.AccountApiClient;
import com.example.mangaapp.api.AccountApiService;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import okhttp3.ResponseBody;
import android.util.Log;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.concurrent.atomic.AtomicInteger;
import android.graphics.Rect;
import org.json.JSONArray;
import org.json.JSONObject;

public class HomeFragment extends Fragment {
    private RecyclerView mangaGrid;
    private ProgressBar progressBar;
    private MangaAdapter adapter;
    private TabLayout tabLayout;
    private String selectedLanguage = "en";
    private AuthManager authManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = requireContext().getSharedPreferences("manga_prefs", Context.MODE_PRIVATE);
        selectedLanguage = prefs.getString("selected_language", "en");
        authManager = AuthManager.getInstance(requireContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        Log.d("HomeFragment", "=== onCreateView called ===");
        SharedPreferences prefs = requireContext().getSharedPreferences("manga_prefs", Context.MODE_PRIVATE);
        selectedLanguage = prefs.getString("selected_language", "en");
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        Log.d("HomeFragment", "Layout inflated");

        mangaGrid = root.findViewById(R.id.manga_grid);
        progressBar = root.findViewById(R.id.progress_bar);
        tabLayout = root.findViewById(R.id.tab_layout);
        Log.d("HomeFragment", "Views found");

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

        // Налаштування TabLayout
        tabLayout.addTab(tabLayout.newTab().setText("Новинки"));
        tabLayout.addTab(tabLayout.newTab().setText("Популярні"));
        tabLayout.addTab(tabLayout.newTab().setText("Рекомендації"));
        
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                if (position == 0) {
                    // Новинки
                    loadLatestManga();
                } else if (position == 1) {
                    // Популярні
                    loadPopularManga();
                } else if (position == 2) {
                    // Рекомендації
                    loadRecommendations();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Не потрібно нічого робити
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Не потрібно нічого робити
            }
        });

        // За замовчуванням завантажуємо новинки
        Log.d("HomeFragment", "About to load latest manga");
        loadLatestManga();
        Log.d("HomeFragment", "loadLatestManga() called");

        return root;
    }


    private void loadLatestManga() {
        Log.d("HomeFragment", "loadLatestManga() started");
        progressBar.setVisibility(View.VISIBLE);
        MangaApiService apiService = ApiClient.getClient().create(MangaApiService.class);
        List<String> includes = new ArrayList<>();
        includes.add("cover_art");
        Log.d("HomeFragment", "Making API call for latest manga");
        Call<MangaResponse> call = apiService.getLatestManga("manga", 20, 0, "desc", includes);
        processMangaResponse(call, apiService);
    }

    private void loadPopularManga() {
        progressBar.setVisibility(View.VISIBLE);
        MangaApiService apiService = ApiClient.getClient().create(MangaApiService.class);
        List<String> includes = new ArrayList<>();
        includes.add("cover_art");
        Call<MangaResponse> call = apiService.getPopularManga("manga", 20, 0, "desc", includes);
        processMangaResponse(call, apiService);
    }

    private void loadRecommendations() {
        if (!authManager.isLoggedIn()) {
            Toast.makeText(getContext(), "Увійдіть, щоб отримати рекомендації", Toast.LENGTH_SHORT).show();
            tabLayout.getTabAt(0).select(); // Повернутися до новинок
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        
        AccountApiService accountApiService = AccountApiClient.getClient().create(AccountApiService.class);
        String token = "Bearer " + authManager.getAuthToken();
        
        // Використовуємо ResponseBody для ручного парсингу
        accountApiService.getRecommendationVector(token, 20).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsonString = response.body().string();
                        Log.d("HomeFragment", "Recommendation JSON: " + jsonString);
                        
                        Map<String, Integer> vector = new HashMap<>();

                        // Попробуем распарсить ответ в нескольких ожидаемых форматах:
                        // 1) JSON-массив объектов { "genre":..., "genreId":..., "number":... }
                        // 2) JSON-массив объектов { "key":..., "value":... } (устаревший)
                        // 3) JSON-объект { "genreId": count, ... } (map)
                        boolean parsed = false;
                        try {
                            JSONArray jsonArray = new JSONArray(jsonString);
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject obj = jsonArray.getJSONObject(i);

                                // Формат с полями genreId/number
                                if (obj.has("genreId") || obj.has("GenreId") || obj.has("genre_id") ) {
                                    String id = null;
                                    int num = 0;
                                    if (obj.has("genreId")) id = obj.getString("genreId");
                                    else if (obj.has("GenreId")) id = obj.getString("GenreId");
                                    else if (obj.has("genre_id")) id = obj.getString("genre_id");

                                    if (obj.has("number")) num = obj.getInt("number");
                                    else if (obj.has("Number")) num = obj.getInt("Number");
                                    else if (obj.has("value")) num = obj.getInt("value");

                                    if (id != null && !id.isEmpty()) {
                                        vector.put(id, num);
                                    }
                                    parsed = true;
                                    continue;
                                }

                                // Старый формат key/value
                                if (obj.has("key") || obj.has("Key")) {
                                    String key = obj.has("key") ? obj.getString("key") : obj.optString("Key", null);
                                    int value = 0;
                                    if (obj.has("value")) value = obj.getInt("value");
                                    else if (obj.has("Value")) value = obj.getInt("Value");
                                    if (key != null) vector.put(key, value);
                                    parsed = true;
                                }
                            }
                        } catch (Exception e) {
                            // не массив — попробуем как объект map
                            try {
                                JSONObject jsonObject = new JSONObject(jsonString);
                                java.util.Iterator<String> keys = jsonObject.keys();
                                while (keys.hasNext()) {
                                    String key = keys.next();
                                    try {
                                        int value = jsonObject.getInt(key);
                                        vector.put(key, value);
                                        parsed = true;
                                    } catch (Exception ignore) {
                                        // если значение не int — пропускаем
                                    }
                                }
                            } catch (Exception e2) {
                                Log.e("HomeFragment", "Error parsing recommendation JSON", e2);
                            }
                        }

                        if (!vector.isEmpty()) {
                            processRecommendationVector(vector);
                        } else {
                            progressBar.setVisibility(View.GONE);
                            if (!parsed) {
                                Log.d("HomeFragment", "Recommendation response parsed but empty: " + jsonString);
                            }
                            Toast.makeText(getContext(), "Недостатньо історії для рекомендацій. Почитайте більше манг!", Toast.LENGTH_LONG).show();
                            loadPopularManga();
                        }
                    } catch (Exception e) {
                        Log.e("HomeFragment", "Error reading response body", e);
                        progressBar.setVisibility(View.GONE);
                        loadPopularManga();
                    }
                } else {
                    progressBar.setVisibility(View.GONE);
                    Log.e("HomeFragment", "Error fetching recommendations: " + response.code());
                    Toast.makeText(getContext(), "Помилка сервера рекомендацій: " + response.code(), Toast.LENGTH_SHORT).show();
                    loadPopularManga();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Log.e("HomeFragment", "Error fetching recommendations", t);
                Toast.makeText(getContext(), "Помилка підключення: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                loadPopularManga();
            }
        });
    }
    
    private void processRecommendationVector(Map<String, Integer> vector) {
        // Сортуємо вектор за спаданням кількості
        List<Map.Entry<String, Integer>> list = new LinkedList<>(vector.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        });

        // Беремо топ жанрів (наприклад, топ 5)
        int maxGenres = 5;
        List<Map.Entry<String, Integer>> topGenres = list.subList(0, Math.min(list.size(), maxGenres));
        
        // Рахуємо загальну вагу для топ жанрів
        int totalWeight = 0;
        for (Map.Entry<String, Integer> entry : topGenres) {
            totalWeight += entry.getValue();
        }
        
        if (totalWeight == 0) {
            progressBar.setVisibility(View.GONE);
            return;
        }

        final List<Manga> combinedMangaList = Collections.synchronizedList(new ArrayList<>());
        final AtomicInteger completedRequests = new AtomicInteger(0);
        final int totalRequests = topGenres.size();
        final int targetTotalItems = 20;
        
        MangaApiService apiService = ApiClient.getClient().create(MangaApiService.class);

        for (Map.Entry<String, Integer> entry : topGenres) {
            String tagId = entry.getKey();
            int count = entry.getValue();
            
            // Розраховуємо ліміт пропорційно
            int limit = Math.max(1, Math.round(((float) count / totalWeight) * targetTotalItems));
            
            List<String> includedTags = new ArrayList<>();
            includedTags.add(tagId);
            
            List<String> includes = new ArrayList<>();
            includes.add("cover_art");

            Call<MangaResponse> call = apiService.searchMangaByTagsOnly("manga", includedTags, limit, 0, includes);
            
            call.enqueue(new Callback<MangaResponse>() {
                @Override
                public void onResponse(Call<MangaResponse> call, Response<MangaResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        processPartialMangaResponse(response.body().getData(), combinedMangaList, completedRequests, totalRequests, apiService);
                    } else {
                        checkAllRequestsCompleted(completedRequests, totalRequests, combinedMangaList);
                    }
                }

                @Override
                public void onFailure(Call<MangaResponse> call, Throwable t) {
                    checkAllRequestsCompleted(completedRequests, totalRequests, combinedMangaList);
                }
            });
        }
    }

    // Допоміжний метод для обробки часткової відповіді і завантаження обкладинок/інформації
    private void processPartialMangaResponse(List<MangaResponse.Data> dataList, List<Manga> combinedList, AtomicInteger completedRequests, int totalRequests, MangaApiService apiService) {
        if (dataList == null || dataList.isEmpty()) {
            checkAllRequestsCompleted(completedRequests, totalRequests, combinedList);
            return;
        }

        final AtomicInteger itemsInBatchProcessed = new AtomicInteger(0);
        final int itemsInBatch = dataList.size();
        
        for (MangaResponse.Data data : dataList) {
             if (data == null || data.getId() == null || data.getAttributes() == null) {
                 if (itemsInBatchProcessed.incrementAndGet() == itemsInBatch) {
                     checkAllRequestsCompleted(completedRequests, totalRequests, combinedList);
                 }
                 continue;
             }

            final String mangaId = data.getId();
            final Map<String, String> titles = data.getAttributes().getTitle();
            String mangaTitle = null;
            if (titles != null) {
                mangaTitle = titles.get(selectedLanguage);
                if (mangaTitle == null || mangaTitle.isEmpty()) {
                    mangaTitle = titles.get("en");
                }
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

            String coverFileName = null;
            if (data.getRelationships() != null) {
                for (com.example.mangaapp.API_MangaDex.MangaResponse.Relationship rel : data.getRelationships()) {
                    if ("cover_art".equals(rel.getType()) && rel.getAttributes() != null) {
                        coverFileName = rel.getAttributes().getFileName();
                        break;
                    }
                }
            }
            final String mangaCoverUrl = (coverFileName != null) ? 
                "https://uploads.mangadex.org/covers/" + mangaId + "/" + coverFileName : null;

            // Додаємо в загальний список
            synchronized (combinedList) {
                boolean exists = false;
                for (Manga m : combinedList) {
                    if (m.getId().equals(mangaId)) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    combinedList.add(new Manga(mangaId, finalMangaTitle, mangaType, mangaCoverUrl));
                }
            }

            if (itemsInBatchProcessed.incrementAndGet() == itemsInBatch) {
                checkAllRequestsCompleted(completedRequests, totalRequests, combinedList);
            }
        }
    }

    private void checkAllRequestsCompleted(AtomicInteger completedRequests, int totalRequests, List<Manga> combinedList) {
        if (completedRequests.incrementAndGet() == totalRequests) {
            // Всі запити завершені
            if (getActivity() == null) return;
            
            getActivity().runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                // Можна перемішати рекомендації, щоб не йшли блоками по жанрах
                Collections.shuffle(combinedList);
                
                adapter = new MangaAdapter(combinedList, manga -> {
                    Bundle bundle = new Bundle();
                    bundle.putString("manga_id", manga.getId());
                    try {
                        Navigation.findNavController(requireView()).navigate(R.id.action_homeFragment_to_mangaDetailFragment, bundle);
                    } catch (Exception e) {
                        Log.e("MangaApp", "Navigation error", e);
                    }
                });
                mangaGrid.setAdapter(adapter);
            });
        }
    }

    private void processMangaResponse(Call<MangaResponse> call, MangaApiService apiService) {
        Log.d("HomeFragment", "processMangaResponse() called");
        final List<Manga> mangaList = new ArrayList<>();

        call.enqueue(new Callback<MangaResponse>() {
            @Override
            public void onResponse(Call<MangaResponse> call, Response<MangaResponse> response) {
                Log.d("HomeFragment", "API onResponse called, successful: " + response.isSuccessful());
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
                    String errorMessage = "Помилка завантаження даних";
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e("HomeFragment", "Error response: " + errorBody);
                            if (errorBody.contains("400") || errorBody.contains("Bad Request")) {
                                errorMessage = "Невірні параметри пошуку. Спробуйте інші жанри.";
                            }
                        }
                    } catch (Exception e) {
                        Log.e("HomeFragment", "Error reading error body", e);
                    }
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                    // Показуємо порожній список замість краша
                    adapter = new MangaAdapter(new ArrayList<>(), manga -> {});
                    mangaGrid.setAdapter(adapter);
                }
            }
            @Override
            public void onFailure(Call<MangaResponse> call, Throwable t) {
                Log.e("HomeFragment", "API onFailure: " + (t == null ? "null" : t.getMessage()), t);
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Помилка: " + (t == null ? "unknown" : t.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

}
