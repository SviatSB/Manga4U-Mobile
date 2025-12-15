package com.example.mangaapp.fragments.account;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mangaapp.R;
import com.example.mangaapp.adapters.HistoryAdapter;
import com.example.mangaapp.api.AccountApiService;
import com.example.mangaapp.api.AccountApiClient;
import com.example.mangaapp.fragments.main.MangaDetailFragment;
import com.example.mangaapp.models.HistoryItem;
import com.example.mangaapp.utils.AuthManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistoryFragment extends Fragment {

    private RecyclerView historyRecyclerView;
    private ProgressBar progressBar;
    private TextView emptyHistoryText;
    private HistoryAdapter historyAdapter;
    private List<HistoryItem> historyList;
    private String authToken;
    private AuthManager authManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        historyList = new ArrayList<>();
        authManager = AuthManager.getInstance(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        historyRecyclerView = view.findViewById(R.id.history_recycler_view);
        progressBar = view.findViewById(R.id.history_progress_bar);
        emptyHistoryText = view.findViewById(R.id.empty_history_text);

        setupRecyclerView();
        loadHistory();

        return view;
    }

    private void setupRecyclerView() {
        historyAdapter = new HistoryAdapter(historyList, historyItem -> {
            // ПРОСТИЙ СПОСІБ: Використовуємо FragmentManager напряму
            try {
                // 1. Закриваємо HistoryFragment
                FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();

                // 2. Створюємо MangaDetailFragment
                MangaDetailFragment mangaDetailFragment = new MangaDetailFragment();
                Bundle args = new Bundle();
                args.putString("manga_id", historyItem.getMangaExternalId());
                args.putString("mangaTitle", historyItem.getMangaName());
                mangaDetailFragment.setArguments(args);

                // 3. Замінюємо ВСЮ навігацію (NavHost) нашим фрагментом
                // Знаходимо NavHostFragment
                FragmentTransaction transaction = fragmentManager.beginTransaction();

                // Шукаємо контейнер для головної навігації
                transaction.replace(R.id.nav_host_fragment_content_main, mangaDetailFragment);
                transaction.addToBackStack("manga_detail");
                transaction.commit();

                Log.d("HistoryFragment", "Opened manga: " + historyItem.getMangaName());

            } catch (Exception e) {
                Log.e("HistoryFragment", "Error opening manga", e);
                Toast.makeText(requireContext(), "Помилка відкриття манги", Toast.LENGTH_SHORT).show();
            }
        });

        historyRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        historyRecyclerView.setAdapter(historyAdapter);
    }

    private void loadHistory() {
        if (!authManager.isLoggedIn()) {
            emptyHistoryText.setVisibility(View.VISIBLE);
            emptyHistoryText.setText("Будь ласка, увійдіть в акаунт");
            return;
        }

        authToken = authManager.getAuthToken();
        progressBar.setVisibility(View.VISIBLE);
        historyRecyclerView.setVisibility(View.GONE);
        emptyHistoryText.setVisibility(View.GONE);

        AccountApiService apiService = AccountApiClient.getClient().create(AccountApiService.class);
        Call<List<HistoryItem>> call = apiService.getHistory("Bearer " + authToken);

        call.enqueue(new Callback<List<HistoryItem>>() {
            @Override
            public void onResponse(@NonNull Call<List<HistoryItem>> call, @NonNull Response<List<HistoryItem>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    historyList.clear();
                    historyList.addAll(response.body());
                    historyAdapter.notifyDataSetChanged();

                    if (historyList.isEmpty()) {
                        emptyHistoryText.setVisibility(View.VISIBLE);
                        historyRecyclerView.setVisibility(View.GONE);
                    } else {
                        emptyHistoryText.setVisibility(View.GONE);
                        historyRecyclerView.setVisibility(View.VISIBLE);
                    }
                } else {
                    Log.e("HistoryFragment", "Error loading history: " + response.code());
                    Toast.makeText(requireContext(), "Помилка завантаження історії", Toast.LENGTH_SHORT).show();
                    emptyHistoryText.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<HistoryItem>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Log.e("HistoryFragment", "Error loading history", t);
                Toast.makeText(requireContext(), "Помилка: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                emptyHistoryText.setVisibility(View.VISIBLE);
            }
        });
    }
}