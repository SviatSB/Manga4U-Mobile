package com.example.mangaapp.fragments.main;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mangaapp.R;
import com.example.mangaapp.adapters.CollectionAdapter;
import com.example.mangaapp.api.AccountApiClient;
import com.example.mangaapp.api.AccountApiService;
import com.example.mangaapp.models.Collection;
import com.example.mangaapp.utils.AuthManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import androidx.appcompat.widget.SearchView;
import java.util.ArrayList;
import java.util.List;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CollectionsFragment extends Fragment {

    private RecyclerView recyclerView;
    private CollectionAdapter adapter;
    private ProgressBar progressBar;
    private FloatingActionButton fabAdd;
    private TabLayout tabLayout;
    private SearchView searchView;
    private AuthManager authManager;
    private List<Collection> systemCollections = new ArrayList<>();
    private List<Collection> userCollections = new ArrayList<>();
    private List<Collection> publicCollections = new ArrayList<>();
    private int currentTab = 0; // 0 = System, 1 = User, 2 = Search

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authManager = AuthManager.getInstance(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_collections, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        fabAdd = view.findViewById(R.id.fab_add_collection);
        tabLayout = view.findViewById(R.id.tab_layout);
        searchView = view.findViewById(R.id.search_view);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CollectionAdapter(new ArrayList<>(), this::onCollectionClick, this::onDeleteClick);
        recyclerView.setAdapter(adapter);

        tabLayout.addTab(tabLayout.newTab().setText("Системні"));
        tabLayout.addTab(tabLayout.newTab().setText("Мої колекції"));
        tabLayout.addTab(tabLayout.newTab().setText("Пошук"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                updateSearchViewVisibility();
                updateList();
                fabAdd.setVisibility(currentTab == 1 ? View.VISIBLE : View.GONE);
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (currentTab == 2) {
                    searchPublicCollections(query);
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (currentTab == 2 && newText.isEmpty()) {
                    loadPublicCollections();
                }
                return true;
            }
        });

        fabAdd.setOnClickListener(v -> showCreateCollectionDialog());

        // Default to System collections
        fabAdd.setVisibility(View.GONE);
        loadCollections();
    }

    private void updateSearchViewVisibility() {
        if (currentTab == 2) {
            searchView.setVisibility(View.VISIBLE);
        } else {
            searchView.setVisibility(View.GONE);
            searchView.setQuery("", false);
        }
    }

    private void loadCollections() {
        if (!authManager.isLoggedIn()) {
            Toast.makeText(getContext(), "Будь ласка, увійдіть в акаунт", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        AccountApiService apiService = AccountApiClient.getClient().create(AccountApiService.class);
        String token = "Bearer " + authManager.getAuthToken();

        // Load System Collections
        apiService.getSystemCollections(token).enqueue(new Callback<List<Collection>>() {
            @Override
            public void onResponse(Call<List<Collection>> call, Response<List<Collection>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    systemCollections = response.body();
                    // System collections are marked isSystem=true implicitly by endpoint, but let's make sure
                    for(Collection c : systemCollections) c.setSystem(true);
                    
                    if (currentTab == 0) updateList();
                }
                
                // Load User Collections
                apiService.getUserCollections(token).enqueue(new Callback<List<Collection>>() {
                    @Override
                    public void onResponse(Call<List<Collection>> call, Response<List<Collection>> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            userCollections = response.body();
                            // User collections are not system
                            for(Collection c : userCollections) c.setSystem(false);
                            
                            if (currentTab == 1) updateList();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Collection>> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Помилка завантаження колекцій", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(Call<List<Collection>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Помилка завантаження колекцій", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateList() {
        if (currentTab == 0) {
            adapter.updateList(systemCollections);
        } else if (currentTab == 1) {
            adapter.updateList(userCollections);
        } else {
            // Tab 2 - Search public collections
            loadPublicCollections();
        }
    }

    private void showCreateCollectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Нова колекція");

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Створити", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                createCollection(name);
            }
        });
        builder.setNegativeButton("Скасувати", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void createCollection(String name) {
        progressBar.setVisibility(View.VISIBLE);
        AccountApiService apiService = AccountApiClient.getClient().create(AccountApiService.class);
        String token = "Bearer " + authManager.getAuthToken();

        // Backend expects raw string in body ([FromBody] string name)
        // Use application/json content type for ASP.NET Core compatibility
        okhttp3.RequestBody body = okhttp3.RequestBody.create("\"" + name + "\"", okhttp3.MediaType.parse("application/json; charset=utf-8"));

        apiService.createCollection(token, body).enqueue(new Callback<Collection>() {
            @Override
            public void onResponse(Call<Collection> call, Response<Collection> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Колекцію створено", Toast.LENGTH_SHORT).show();
                    loadCollections(); // Reload
                } else {
                    String errorMsg = "Помилка: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        android.util.Log.e("CollectionsFragment", "Error reading error body", e);
                    }
                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
                    android.util.Log.e("CollectionsFragment", "createCollection error: " + errorMsg);
                }
            }

            @Override
            public void onFailure(Call<Collection> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                String errorMsg = t == null ? "Невідома помилка" : t.getMessage();
                Toast.makeText(getContext(), "Помилка мережі: " + errorMsg, Toast.LENGTH_LONG).show();
                android.util.Log.e("CollectionsFragment", "createCollection onFailure", t);
            }
        });
    }

    private void onCollectionClick(Collection collection) {
        // For search tab (public collections), offer clone option
        if (currentTab == 2) {
            new AlertDialog.Builder(getContext())
                    .setTitle(collection.getName())
                    .setItems(new String[]{"Переглянути", "Додати копію"}, (dialog, which) -> {
                        if (which == 0) {
                            // View details
                            Bundle bundle = new Bundle();
                            bundle.putString("collection_id", String.valueOf(collection.getId()));
                            bundle.putString("collection_name", collection.getName());
                            Navigation.findNavController(requireView()).navigate(R.id.action_collectionsFragment_to_collectionDetailFragment, bundle);
                        } else {
                            // Clone collection
                            cloneCollection(collection);
                        }
                    })
                    .show();
        } else {
            // For system/user collections, just view details
            Bundle bundle = new Bundle();
            bundle.putString("collection_id", String.valueOf(collection.getId()));
            bundle.putString("collection_name", collection.getName());
            Navigation.findNavController(requireView()).navigate(R.id.action_collectionsFragment_to_collectionDetailFragment, bundle);
        }
    }

    private void onDeleteClick(Collection collection) {
        new AlertDialog.Builder(getContext())
                .setTitle("Видалити колекцію?")
                .setMessage("Ви впевнені, що хочете видалити '" + collection.getName() + "'?")
                .setPositiveButton("Так", (dialog, which) -> deleteCollection(collection))
                .setNegativeButton("Ні", null)
                .show();
    }

    private void deleteCollection(Collection collection) {
        progressBar.setVisibility(View.VISIBLE);
        AccountApiService apiService = AccountApiClient.getClient().create(AccountApiService.class);
        String token = "Bearer " + authManager.getAuthToken();

        apiService.deleteCollection(token, collection.getId()).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Колекцію видалено", Toast.LENGTH_SHORT).show();
                    loadCollections();
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

    private void loadPublicCollections() {
        progressBar.setVisibility(View.VISIBLE);
        AccountApiService apiService = AccountApiClient.getClient().create(AccountApiService.class);

        apiService.searchCollections("").enqueue(new Callback<List<Collection>>() {
            @Override
            public void onResponse(Call<List<Collection>> call, Response<List<Collection>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    publicCollections = response.body();
                    adapter.updateList(publicCollections);
                } else {
                    adapter.updateList(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<List<Collection>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Помилка завантаження публічних колекцій", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void searchPublicCollections(String query) {
        progressBar.setVisibility(View.VISIBLE);
        AccountApiService apiService = AccountApiClient.getClient().create(AccountApiService.class);

        apiService.searchCollections(query).enqueue(new Callback<List<Collection>>() {
            @Override
            public void onResponse(Call<List<Collection>> call, Response<List<Collection>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    publicCollections = response.body();
                    adapter.updateList(publicCollections);
                } else {
                    adapter.updateList(new ArrayList<>());
                    Toast.makeText(getContext(), "Колекцій не знайдено", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Collection>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Помилка пошуку", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cloneCollection(Collection collection) {
        if (!authManager.isLoggedIn()) {
            Toast.makeText(getContext(), "Будь ласка, увійдіть в акаунт", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        AccountApiService apiService = AccountApiClient.getClient().create(AccountApiService.class);
        String token = "Bearer " + authManager.getAuthToken();

        apiService.cloneCollection(token, collection.getId()).enqueue(new Callback<Collection>() {
            @Override
            public void onResponse(Call<Collection> call, Response<Collection> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Колекція скопійована!", Toast.LENGTH_SHORT).show();
                    loadCollections();
                } else {
                    Toast.makeText(getContext(), "Помилка копіювання колекції", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Collection> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Помилка мережі", Toast.LENGTH_SHORT).show();
            }
        });
    }
}


