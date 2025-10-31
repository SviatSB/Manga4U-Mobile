package com.example.mangaapp.fragments.main;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.navigation.Navigation;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.mangaapp.API_MangaDex.MangaDetail;
import com.example.mangaapp.R;
import com.example.mangaapp.api.ApiClient;
import com.example.mangaapp.api.MangaApiService;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.example.mangaapp.databinding.FragmentMangaDetailBinding;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;
import java.util.Map;

public class MangaDetailFragment extends Fragment {
    private static final String ARG_MANGA_ID = "manga_id";
    private String mangaId;
    private ProgressBar progressBar;
    private String selectedLanguage = "en";
    private FragmentMangaDetailBinding binding;

    public static MangaDetailFragment newInstance(String mangaId) {
        MangaDetailFragment fragment = new MangaDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MANGA_ID, mangaId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mangaId = getArguments().getString(ARG_MANGA_ID);
        }
        // Завантажуємо вибрану мову
        SharedPreferences prefs = requireContext().getSharedPreferences("manga_prefs", Context.MODE_PRIVATE);
        selectedLanguage = prefs.getString("selected_language", "en");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMangaDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        progressBar = binding.progressBar;
        loadMangaDetails();

        // Додаємо Chip-и для мов
        String[] allLanguages = {"en", "uk", "ru", "pl"};
        for (String lang : allLanguages) {
            Chip chip = new Chip(requireContext());
            chip.setText(lang);
            chip.setCheckable(true);
            chip.setChecked(lang.equals(selectedLanguage)); // за замовчуванням вибрана мова
            binding.languageChipGroup.addView(chip);
        }

        binding.readButton.setOnClickListener(v -> {
            Log.e("MangaApp", "[MangaDetailFragment] Read button clicked for mangaId: " + mangaId);
            // Збираємо вибрані мови
            java.util.ArrayList<String> selectedLangs = new java.util.ArrayList<>();
            ChipGroup languageChipGroup = binding.languageChipGroup;
            for (int i = 0; i < languageChipGroup.getChildCount(); i++) {
                Chip chip = (Chip) languageChipGroup.getChildAt(i);
                if (chip.isChecked()) {
                    selectedLangs.add(chip.getText().toString());
                }
            }
            if (selectedLangs.isEmpty()) selectedLangs.add("en"); // fallback
            // Перехід до фрагменту з главами
            Bundle bundle = new Bundle();
            bundle.putString("manga_id", mangaId);
            bundle.putStringArrayList("selected_languages", selectedLangs);
            try {
                Navigation.findNavController(v).navigate(R.id.action_mangaDetailFragment_to_chapterListFragment, bundle);
            } catch (Exception e) {
                Log.e("MangaApp", "[MangaDetailFragment] Navigation error: ", e);
            }
        });
    }


    private void loadMangaDetails() {
        progressBar.setVisibility(View.VISIBLE);
        MangaApiService apiService = ApiClient.getClient().create(MangaApiService.class);
        List<String> includes = new java.util.ArrayList<>();
        includes.add("cover_art");
        Call<MangaDetail> call = apiService.getMangaDetails(mangaId, includes);
        call.enqueue(new Callback<MangaDetail>() {
            @Override
            public void onResponse(Call<MangaDetail> call, Response<MangaDetail> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    MangaDetail mangaDetail = response.body();
                    MangaDetail.Data data = mangaDetail.getData();
                    MangaDetail.Attributes attributes = data.getAttributes();

                    // Встановлення заголовку
                    String title = null;
                    Map<String, String> titleMap = attributes.getTitle();
                    if (titleMap != null) {
                        title = titleMap.get("en");
                        if (title == null) {
                            for (String t : titleMap.values()) {
                                if (t != null && !t.isEmpty()) {
                                    title = t;
                                    break;
                                }
                            }
                        }
                    }
                    if (title == null) title = "No Title";
                    binding.mangaTitle.setText(title);

                    // Встановлення авторів
                    StringBuilder authors = new StringBuilder();
                    if (attributes.getAuthors() != null) {
                        for (MangaDetail.Author author : attributes.getAuthors()) {
                            if (authors.length() > 0) {
                                authors.append(", ");
                            }
                            authors.append(author.getName());
                        }
                    }
                    binding.mangaAuthors.setText(authors.toString());

                    // Встановлення опису
                    String description = null;
                    Map<String, String> descMap = attributes.getDescription();
                    if (descMap != null) {
                        description = descMap.get("en");
                        if (description == null) {
                            for (String d : descMap.values()) {
                                if (d != null && !d.isEmpty()) {
                                    description = d;
                                    break;
                                }
                            }
                        }
                    }
                    if (description == null) description = "";
                    binding.mangaDescription.setText(description);

                    // Завантаження обкладинки через relationships (cover_art)
                    String coverFileName = null;
                    if (data.getRelationships() != null) {
                        for (com.example.mangaapp.API_MangaDex.MangaResponse.Relationship rel : data.getRelationships()) {
                            if ("cover_art".equals(rel.getType()) && rel.getAttributes() != null) {
                                coverFileName = rel.getAttributes().getFileName();
                                break;
                            }
                        }
                    }
                    String coverUrl = null;
                    if (coverFileName != null) {
                        coverUrl = "https://uploads.mangadex.org/covers/" + mangaId + "/" + coverFileName;
                    }
                    Log.e("MangaApp", "DETAIL coverFileName: " + coverFileName + ", coverUrl: " + coverUrl);
                    if (coverUrl != null) {
                        RequestOptions requestOptions = new RequestOptions()
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .placeholder(R.drawable.placeholder_manga)
                                .error(R.drawable.placeholder_manga);
                        Glide.with(requireContext())
                                .load(coverUrl)
                                .apply(requestOptions)
                                .into(binding.mangaCover);
                    } else {
                        binding.mangaCover.setImageResource(R.drawable.placeholder_manga);
                    }
                }
            }

            @Override
            public void onFailure(Call<MangaDetail> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Помилка завантаження: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}