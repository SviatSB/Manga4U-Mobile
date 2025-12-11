package com.example.mangaapp.fragments.main;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mangaapp.API_MangaDex.MangaResponse;
import com.example.mangaapp.Manga;
import com.example.mangaapp.adapters.MangaAdapter;
import com.example.mangaapp.api.AccountApiClient;
import com.example.mangaapp.api.AccountApiService;
import com.example.mangaapp.api.ApiClient;
import com.example.mangaapp.MangaApiService;
import com.example.mangaapp.R;
import com.example.mangaapp.models.Collection;
import com.example.mangaapp.utils.AuthManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CollectionDetailFragment extends Fragment {

    private String collectionId;
    private String collectionName; // Optional, can be fetched
    private TextView titleView;
    private ImageButton renameButton, deleteButton;
    private SwitchCompat visibilitySwitch;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private AuthManager authManager;
    private Collection currentCollection;
    private MangaAdapter adapter;
    private List<Manga> mangaList = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            collectionId = getArguments().getString("collection_id");
            collectionName = getArguments().getString("collection_name");
        }
        authManager = AuthManager.getInstance(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_collection_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        titleView = view.findViewById(R.id.collection_title);
        renameButton = view.findViewById(R.id.rename_button);
        visibilitySwitch = view.findViewById(R.id.visibility_switch);
        deleteButton = view.findViewById(R.id.delete_button);
        recyclerView = view.findViewById(R.id.manga_recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);

        if (collectionName != null) {
            titleView.setText(collectionName);
        }

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        adapter = new MangaAdapter(mangaList, this::onMangaClick);
        recyclerView.setAdapter(adapter);

        renameButton.setOnClickListener(v -> showRenameDialog());
        visibilitySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> toggleVisibility(isChecked));
        deleteButton.setOnClickListener(v -> showDeleteDialog());

        loadCollectionDetails();
    }

    private void loadCollectionDetails() {
        if (!authManager.isLoggedIn()) return;

        progressBar.setVisibility(View.VISIBLE);
        AccountApiService apiService = AccountApiClient.getClient().create(AccountApiService.class);
        String token = "Bearer " + authManager.getAuthToken();

        apiService.getCollectionDetails(token, collectionId).enqueue(new Callback<Collection>() {
            @Override
            public void onResponse(Call<Collection> call, Response<Collection> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentCollection = response.body();
                    titleView.setText(currentCollection.getName());
                    
                    if (currentCollection.isSystem()) {
                        renameButton.setVisibility(View.GONE);
                        deleteButton.setVisibility(View.GONE);
                        visibilitySwitch.setVisibility(View.GONE);
                    } else {
                        renameButton.setVisibility(View.VISIBLE);
                        deleteButton.setVisibility(View.VISIBLE);
                        visibilitySwitch.setVisibility(View.VISIBLE);
                        visibilitySwitch.setChecked(currentCollection.isPublic());
                    }

                    List<String> ids = currentCollection.getMangaExternalIds();
                    if (ids != null && !ids.isEmpty()) {
                        loadMangaList(ids);
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Колекція порожня", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Помилка завантаження колекції", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Collection> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Помилка мережі", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMangaList(List<String> ids) {
        MangaApiService apiService = ApiClient.getClient().create(MangaApiService.class);
        List<String> includes = new ArrayList<>();
        includes.add("cover_art");

        apiService.getMangaList("manga", ids, 100, includes).enqueue(new Callback<MangaResponse>() {
            @Override
            public void onResponse(Call<MangaResponse> call, Response<MangaResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    processMangaResponse(response.body().getData());
                } else {
                    Toast.makeText(getContext(), "Помилка завантаження манги", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<MangaResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Помилка мережі", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processMangaResponse(List<MangaResponse.Data> dataList) {
        mangaList.clear();
        if (dataList == null) return;

        for (MangaResponse.Data data : dataList) {
            String id = data.getId();
            String title = "No Title";
            if (data.getAttributes().getTitle() != null) {
                // Simplistic title extraction, ideally use logic from HomeFragment
                Map<String, String> titles = data.getAttributes().getTitle();
                if (titles.containsKey("en")) title = titles.get("en");
                else if (!titles.isEmpty()) title = titles.values().iterator().next();
            }
            
            String coverFileName = null;
            if (data.getRelationships() != null) {
                for (MangaResponse.Relationship rel : data.getRelationships()) {
                    if ("cover_art".equals(rel.getType()) && rel.getAttributes() != null) {
                        coverFileName = rel.getAttributes().getFileName();
                        break;
                    }
                }
            }
            String coverUrl = coverFileName != null ? 
                "https://uploads.mangadex.org/covers/" + id + "/" + coverFileName : null;

            mangaList.add(new Manga(id, title, "manga", coverUrl));
        }
        adapter.notifyDataSetChanged();
    }

    private void onMangaClick(Manga manga) {
        // Show dialog: Open or Remove
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(manga.getTitle());
        String[] options = {"Відкрити", "Видалити з колекції"};
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                Bundle bundle = new Bundle();
                bundle.putString("manga_id", manga.getId());
                Navigation.findNavController(getView()).navigate(R.id.action_collectionDetailFragment_to_mangaDetailFragment, bundle);
            } else {
                removeMangaFromCollection(manga);
            }
        });
        builder.show();
    }

    private void removeMangaFromCollection(Manga manga) {
        progressBar.setVisibility(View.VISIBLE);
        AccountApiService apiService = AccountApiClient.getClient().create(AccountApiService.class);
        String token = "Bearer " + authManager.getAuthToken();

        apiService.removeMangaFromCollection(token, collectionId, manga.getId()).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Мангу видалено", Toast.LENGTH_SHORT).show();
                    loadCollectionDetails(); // Reload
                } else {
                    Toast.makeText(getContext(), "Помилка видалення", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Помилка мережі", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showRenameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Перейменувати колекцію");
        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(titleView.getText());
        builder.setView(input);

        builder.setPositiveButton("Зберегти", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                renameCollection(newName);
            }
        });
        builder.setNegativeButton("Скасувати", null);
        builder.show();
    }

    private void renameCollection(String newName) {
        progressBar.setVisibility(View.VISIBLE);
        AccountApiService apiService = AccountApiClient.getClient().create(AccountApiService.class);
        String token = "Bearer " + authManager.getAuthToken();

        Log.d("CollectionDetailFragment", "Renaming collection: " + collectionId + " to: " + newName);

        apiService.renameCollection(token, collectionId, newName).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                progressBar.setVisibility(View.GONE);
                Log.d("CollectionDetailFragment", "Rename response code: " + response.code() + ", successful: " + response.isSuccessful());
                
                if (response.isSuccessful()) {
                    titleView.setText(newName);
                    Toast.makeText(getContext(), "Перейменовано", Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "No error body";
                        Log.e("CollectionDetailFragment", "Rename failed - Code: " + response.code() + ", Error: " + errorBody);
                    } catch (Exception e) {
                        Log.e("CollectionDetailFragment", "Error reading response body", e);
                    }
                    Toast.makeText(getContext(), "Помилка перейменування (код: " + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Log.e("CollectionDetailFragment", "Rename network error", t);
                Toast.makeText(getContext(), "Помилка мережі: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDeleteDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("Видалити колекцію?")
                .setMessage("Це дію не можна скасувати.")
                .setPositiveButton("Видалити", (dialog, which) -> deleteCollection())
                .setNegativeButton("Скасувати", null)
                .show();
    }

    private void deleteCollection() {
        progressBar.setVisibility(View.VISIBLE);
        AccountApiService apiService = AccountApiClient.getClient().create(AccountApiService.class);
        String token = "Bearer " + authManager.getAuthToken();

        apiService.deleteCollection(token, collectionId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Колекцію видалено", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(getView()).navigateUp();
                } else {
                    Toast.makeText(getContext(), "Помилка видалення", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Помилка мережі", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleVisibility(boolean isChecked) {
        if (currentCollection == null || currentCollection.isSystem()) {
            Toast.makeText(getContext(), "Неможна змінити видимість системної колекції", Toast.LENGTH_SHORT).show();
            visibilitySwitch.setChecked(!isChecked);
            return;
        }

        setCollectionVisibility(isChecked);
    }

    private void setCollectionVisibility(boolean isPublic) {
        progressBar.setVisibility(View.VISIBLE);
        AccountApiService apiService = AccountApiClient.getClient().create(AccountApiService.class);
        String token = "Bearer " + authManager.getAuthToken();

        apiService.setCollectionVisibility(token, collectionId, isPublic).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    currentCollection.setPublic(isPublic);
                    String status = isPublic ? "публічною" : "приватною";
                    Toast.makeText(getContext(), "Колекція стала " + status, Toast.LENGTH_SHORT).show();
                } else {
                    visibilitySwitch.setChecked(!isPublic);
                    Toast.makeText(getContext(), "Помилка змінення видимості", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                visibilitySwitch.setChecked(!isPublic);
                Toast.makeText(getContext(), "Помилка мережі: " + (t != null ? t.getMessage() : "невідома"), Toast.LENGTH_SHORT).show();
                Log.e("CollectionDetailFragment", "setCollectionVisibility error", t);
            }
        });
    }
}

