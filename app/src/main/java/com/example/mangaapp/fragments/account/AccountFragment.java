package com.example.mangaapp.fragments.account;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.mangaapp.R;
import com.example.mangaapp.adapters.AuthPagerAdapter;
import com.example.mangaapp.adapters.RecentMangaAdapter;
import com.example.mangaapp.api.AccountApiService;
import com.example.mangaapp.api.UserDto;
import com.example.mangaapp.api.AccountApiClient;
import com.example.mangaapp.models.RecentManga;
import com.example.mangaapp.models.User;
import com.example.mangaapp.models.HistoryItem;
import com.example.mangaapp.utils.AuthManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountFragment extends Fragment implements
        LoginFragment.OnLoginSuccessListener,
        RegisterFragment.OnRegisterSuccessListener {

    // UI елементи
    private LinearLayout notLoggedInContainer;
    private LinearLayout loggedInContainer;
    private TabLayout authTabLayout;
    private ViewPager2 authViewPager;
    private ShapeableImageView userAvatar;
    private TextView userLogin;
    private TextView userNickname;
    private MaterialButton editProfileButton;
    private MaterialButton logoutButton;
    private MaterialButton viewAllHistoryButton;
    private MaterialButton collectionsButton;
    private RecyclerView recentMangaRecycler;

    // Адаптери
    private AuthPagerAdapter authPagerAdapter;
    private RecentMangaAdapter recentMangaAdapter;

    // Дані
    private String authToken;
    private User currentUser;
    private List<RecentManga> recentMangaList;
    private List<HistoryItem> historyList;

    // SharedPreferences
    private SharedPreferences prefs;
    private AuthManager authManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = requireContext().getSharedPreferences("MangaAppPrefs", 0);
        authManager = AuthManager.getInstance(requireContext());
        recentMangaList = new ArrayList<>();
        historyList = new ArrayList<>();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Оновлюємо дані при поверненні на фрагмент
        if (authManager.isLoggedIn()) {
            authToken = authManager.getAuthToken();
            currentUser = authManager.getCurrentUser();
            // Якщо дані є, оновлюємо відображення
            if (currentUser != null) {
                updateUserInfo();
            } else {
                // Якщо даних немає, завантажуємо з серверу
                loadUserProfile();
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        initViews(view);
        setupViewPager();
        setupRecyclerView();
        checkAuthStatus();

        return view;
    }

    private void initViews(View view) {
        notLoggedInContainer = view.findViewById(R.id.not_logged_in_container);
        loggedInContainer = view.findViewById(R.id.logged_in_container);
        authTabLayout = view.findViewById(R.id.auth_tab_layout);
        authViewPager = view.findViewById(R.id.auth_view_pager);
        userAvatar = view.findViewById(R.id.user_avatar);
        userLogin = view.findViewById(R.id.user_login);
        userNickname = view.findViewById(R.id.user_nickname);
        editProfileButton = view.findViewById(R.id.btn_edit_profile);
        logoutButton = view.findViewById(R.id.btn_logout);
        viewAllHistoryButton = view.findViewById(R.id.btn_view_all_history);
        collectionsButton = view.findViewById(R.id.btn_collections);
        recentMangaRecycler = view.findViewById(R.id.recent_manga_recycler);

        editProfileButton.setOnClickListener(v -> openProfileEdit());
        logoutButton.setOnClickListener(v -> logout());
        if (viewAllHistoryButton != null) {
            viewAllHistoryButton.setOnClickListener(v -> openHistoryFragment());
        }
        if (collectionsButton != null) {
            collectionsButton.setOnClickListener(v -> openCollectionsFragment());
        }
    }

    private void setupViewPager() {
        authPagerAdapter = new AuthPagerAdapter(requireActivity());

        // Встановлюємо callback для адаптера
        authPagerAdapter.setLoginListener(this);
        authPagerAdapter.setRegisterListener(this);

        authViewPager.setAdapter(authPagerAdapter);

        // Зв'язуємо TabLayout з ViewPager2
        new TabLayoutMediator(authTabLayout, authViewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Вхід");
                    break;
                case 1:
                    tab.setText("Реєстрація");
                    break;
            }
        }).attach();
    }

    private void setupRecyclerView() {
        recentMangaAdapter = new RecentMangaAdapter(recentMangaList, manga -> {
            // Навігація до останньої прочитаної глави
            navigateToLastChapter(manga);
        });

        recentMangaRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recentMangaRecycler.setAdapter(recentMangaAdapter);
    }

    private void checkAuthStatus() {
        if (authManager.isLoggedIn()) {
            // Користувач авторизований
            authToken = authManager.getAuthToken();
            currentUser = authManager.getCurrentUser();

            // Оновлюємо дані користувача з серверу
            loadUserProfile();
            loadHistory();
            showLoggedInState();
        } else {
            // Користувач не авторизований
            showNotLoggedInState();
        }
    }

    private void loadUserProfile() {
        AccountApiService apiService = AccountApiClient.getClient().create(AccountApiService.class);

        // Спочатку спробуємо отримати дані через новий API endpoint
        Call<UserDto> call = apiService.getMe("Bearer " + authToken);

        call.enqueue(new Callback<UserDto>() {
            @Override
            public void onResponse(@NonNull Call<UserDto> call, @NonNull Response<UserDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserDto userDto = response.body();
                    // Конвертуємо UserDto в User для сумісності
                    currentUser = convertUserDtoToUser(userDto);
                    // Зберігаємо дані в AuthManager
                    authManager.saveAuthData(authToken, userDto);
                    // Оновлюємо поточного користувача в AuthManager
                    authManager.saveAuthData(authToken, currentUser);
                    updateUserInfo();
                } else {
                    // Якщо новий API не працює, спробуємо старий
                    loadUserProfileLegacy();
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserDto> call, @NonNull Throwable t) {
                // Помилка мережі - спробуємо старий API
                loadUserProfileLegacy();
            }
        });
    }

    private void loadUserProfileLegacy() {
        AccountApiService apiService = AccountApiClient.getClient().create(AccountApiService.class);
        Call<User> call = apiService.getUserProfile(authToken);

        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentUser = response.body();
                    // Зберігаємо дані в AuthManager
                    authManager.saveAuthData(authToken, currentUser);
                    updateUserInfo();
                } else {
                    // Токен недійсний
                    logout();
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                // Помилка мережі - показуємо кешовані дані якщо є
                if (currentUser != null) {
                    updateUserInfo();
                }
            }
        });
    }

    private User convertUserDtoToUser(UserDto userDto) {
        User user = new User();
        user.setId(String.valueOf(userDto.getId()));
        user.setLogin(userDto.getLogin());
        user.setNickname(userDto.getNickname());
        user.setAvatarUrl(userDto.getAvatarUrl());
        user.setAboutMyself(userDto.getAboutMyself() != null ? userDto.getAboutMyself() : "");
        user.setLanguage(userDto.getLanguage() != null ? userDto.getLanguage() : "ua");
        return user;
    }

    private void loadHistory() {
        if (authToken == null) {
            Log.e("AccountFragment", "Auth token is null, cannot load history");
            return;
        }
        
        AccountApiService apiService = AccountApiClient.getClient().create(AccountApiService.class);
        Call<List<HistoryItem>> call = apiService.getHistory("Bearer " + authToken);
        Log.d("AccountFragment", "Requesting history with token: " + (authToken != null ? "present" : "null"));

        call.enqueue(new Callback<List<HistoryItem>>() {
            @Override
            public void onResponse(@NonNull Call<List<HistoryItem>> call, @NonNull Response<List<HistoryItem>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    historyList.clear();
                    historyList.addAll(response.body());
                    Log.d("AccountFragment", "Loaded " + historyList.size() + " history items");
                    
                    // Конвертуємо історію в RecentManga для відображення (останні 4)
                    recentMangaList.clear();
                    int count = Math.min(4, historyList.size());
                    Log.d("AccountFragment", "Converting " + count + " items to RecentManga");
                    
                    for (int i = 0; i < count; i++) {
                        HistoryItem item = historyList.get(i);
                        Log.d("AccountFragment", "Item " + i + ": " + item.getMangaName() + ", Chapter: " + item.getLastChapterNumber());
                        
                        // Конвертуємо дату
                        Date lastReadAt = null;
                        if (item.getUpdatedAt() != null && !item.getUpdatedAt().isEmpty()) {
                            try {
                                // Формат: "2025-11-18T09:32:46.4602245" або "2025-11-18T09:32:46"
                                String dateStr = item.getUpdatedAt();
                                if (dateStr.contains(".")) {
                                    dateStr = dateStr.substring(0, dateStr.indexOf("."));
                                }
                                if (dateStr.contains("+")) {
                                    dateStr = dateStr.substring(0, dateStr.indexOf("+"));
                                }
                                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault());
                                lastReadAt = sdf.parse(dateStr);
                            } catch (Exception e) {
                                Log.e("AccountFragment", "Error parsing date: " + item.getUpdatedAt(), e);
                            }
                        }
                        
                        // Створюємо RecentManga з даних історії
                        RecentManga recentManga = new RecentManga(
                            item.getMangaExternalId(),
                            item.getMangaName(),
                            null, // coverUrl - потрібно завантажити окремо
                            item.getLastChapterTitle() != null ? item.getLastChapterTitle() : "",
                            String.valueOf(item.getLastChapterNumber()),
                            0, // currentPage - не зберігається в історії
                            0, // totalPages - не зберігається в історії
                            lastReadAt,
                            item.getMangaExternalId(),
                            item.getLastChapterId()
                        );
                        recentMangaList.add(recentManga);
                    }
                    
                    Log.d("AccountFragment", "Converted " + recentMangaList.size() + " items, notifying adapter");
                    
                    // Перевіряємо, чи RecyclerView ініціалізований
                    if (recentMangaRecycler != null && recentMangaAdapter != null) {
                        recentMangaAdapter.notifyDataSetChanged();
                        recentMangaRecycler.setVisibility(recentMangaList.isEmpty() ? View.GONE : View.VISIBLE);
                        Log.d("AccountFragment", "RecyclerView visibility: " + (recentMangaList.isEmpty() ? "GONE" : "VISIBLE"));
                    } else {
                        Log.e("AccountFragment", "RecyclerView or adapter is null!");
                    }

                    // Завантажуємо обкладинки для recent items (обмежимося кількістю елементів)
                    try {
                        com.example.mangaapp.MangaApiService mangaApi = com.example.mangaapp.api.ApiClient.getClient().create(com.example.mangaapp.MangaApiService.class);
                        java.util.List<String> includes = new java.util.ArrayList<>();
                        includes.add("cover_art");

                        for (int i = 0; i < recentMangaList.size(); i++) {
                            final int idx = i;
                            RecentManga rm = recentMangaList.get(i);
                            if (rm.getCoverUrl() != null && !rm.getCoverUrl().isEmpty()) continue; // вже є

                            String path = "manga/" + rm.getId();
                            Call<com.example.mangaapp.API_MangaDex.MangaDetail> detailsCall = mangaApi.getMangaDetails(path, includes);
                            detailsCall.enqueue(new Callback<com.example.mangaapp.API_MangaDex.MangaDetail>() {
                                @Override
                                public void onResponse(Call<com.example.mangaapp.API_MangaDex.MangaDetail> call, Response<com.example.mangaapp.API_MangaDex.MangaDetail> response) {
                                    if (response.isSuccessful() && response.body() != null) {
                                        com.example.mangaapp.API_MangaDex.MangaDetail body = response.body();
                                        if (body.getData() != null && body.getData().getRelationships() != null) {
                                            String coverFileName = null;
                                            String mangaId = body.getData().getId();
                                            for (com.example.mangaapp.API_MangaDex.MangaResponse.Relationship rel : body.getData().getRelationships()) {
                                                if ("cover_art".equals(rel.getType()) && rel.getAttributes() != null) {
                                                    coverFileName = rel.getAttributes().getFileName();
                                                    break;
                                                }
                                            }
                                            if (coverFileName != null) {
                                                String coverUrl = "https://uploads.mangadex.org/covers/" + mangaId + "/" + coverFileName;
                                                // Оновлюємо об'єкт RecentManga у списку
                                                try {
                                                    RecentManga target = recentMangaList.get(idx);
                                                    target.setCoverUrl(coverUrl);
                                                } catch (Exception ex) {
                                                    Log.e("AccountFragment", "Error setting cover url", ex);
                                                }
                                                // Оновлюємо адаптер (тільки один елемент)
                                                if (recentMangaRecycler != null && recentMangaAdapter != null) {
                                                    requireActivity().runOnUiThread(() -> recentMangaAdapter.notifyItemChanged(idx));
                                                }
                                            }
                                        }
                                    }
                                }

                                @Override
                                public void onFailure(Call<com.example.mangaapp.API_MangaDex.MangaDetail> call, Throwable t) {
                                    Log.e("AccountFragment", "Failed to load manga details for cover", t);
                                }
                            });
                        }
                    } catch (Exception e) {
                        Log.e("AccountFragment", "Error loading covers for recent items", e);
                    }
                    
                    // Показуємо/ховаємо кнопку "Переглянути всі"
                    if (viewAllHistoryButton != null) {
                        viewAllHistoryButton.setVisibility(historyList.size() > 4 ? View.VISIBLE : View.GONE);
                    }
                } else {
                    Log.e("AccountFragment", "Error loading history: " + response.code());
                    if (response.errorBody() != null) {
                        try {
                            Log.e("AccountFragment", "Error body: " + response.errorBody().string());
                        } catch (Exception e) {
                            Log.e("AccountFragment", "Error reading error body", e);
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<HistoryItem>> call, @NonNull Throwable t) {
                Log.e("AccountFragment", "Error loading history", t);
                // Помилка мережі - показуємо кешовані дані
                if (!recentMangaList.isEmpty()) {
                    recentMangaAdapter.notifyDataSetChanged();
                }
            }
        });
    }
    
    private void openHistoryFragment() {
        try {
            Navigation.findNavController(requireView()).navigate(R.id.action_account_to_history);
        } catch (Exception e) {
            Log.e("AccountFragment", "Error navigating to HistoryFragment", e);
            Toast.makeText(requireContext(), "Помилка відкриття історії", Toast.LENGTH_SHORT).show();
        }
    }

    private void openCollectionsFragment() {
        try {
            Navigation.findNavController(requireView()).navigate(R.id.action_global_to_collectionsFragment);
        } catch (Exception e) {
            Log.e("AccountFragment", "Error navigating to CollectionsFragment", e);
            Toast.makeText(requireContext(), "Помилка відкриття колекцій", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void navigateToLastChapter(RecentManga manga) {
        if (manga.getChapterId() == null || manga.getMangaId() == null) {
            Toast.makeText(requireContext(), "Помилка: не вдалося знайти главу", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
            Bundle bundle = new Bundle();
            bundle.putString("chapterId", manga.getChapterId());
            bundle.putString("mangaId", manga.getMangaId());
            bundle.putString("mangaTitle", manga.getTitle());
            navController.navigate(R.id.readerFragment, bundle);
        } catch (Exception e) {
            Log.e("AccountFragment", "Error navigating to ReaderFragment", e);
            Toast.makeText(requireContext(), "Помилка відкриття глави", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUserInfo() {
        if (currentUser != null && getView() != null) {
            // Основні поля
            userLogin.setText("@" + currentUser.getLogin()); // Додаємо @ перед логіном

            // Відображаємо нікнейм
            String nickname = currentUser.getNickname() != null ? currentUser.getNickname() : "Нікнейм не вказано";
            userNickname.setText(nickname);

            // Оновлюємо тільки публічні поля (нікнейм і про себе)
            try {
                TextView userNicknameDisplay = getView().findViewById(R.id.user_nickname_display);
                if (userNicknameDisplay != null) {
                    userNicknameDisplay.setText(currentUser.getNickname() != null ? currentUser.getNickname() : "Не вказано");
                }
            } catch (Exception e) {
                Log.e("AccountFragment", "Error updating user_nickname_display", e);
            }

            try {
                TextView userAboutDisplay = getView().findViewById(R.id.user_about_display);
                if (userAboutDisplay != null) {
                    userAboutDisplay.setText(currentUser.getAboutMyself() != null && !currentUser.getAboutMyself().isEmpty() ?
                            currentUser.getAboutMyself() : "Не вказано");
                }
            } catch (Exception e) {
                Log.e("AccountFragment", "Error updating user_about_display", e);
            }

            // Завантаження аватара користувача
            String avatarUrl = currentUser.getAvatarUrl();
            Log.d("AccountFragment", "Avatar URL: " + avatarUrl);
            
            // Спочатку встановлюємо placeholder
            userAvatar.setImageResource(R.drawable.ic_person);
            
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                Log.d("AccountFragment", "Loading avatar from URL: " + avatarUrl);
                Glide.with(this)
                        .load(avatarUrl)
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .fallback(R.drawable.ic_person)
                        .centerCrop()
                        .circleCrop()
                        .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                                if (e != null) {
                                    Log.e("AccountFragment", "Failed to load avatar: " + e.getMessage());
                                    for (Throwable cause : e.getRootCauses()) {
                                        Log.e("AccountFragment", "Cause: " + cause.getMessage(), cause);
                                    }
                                } else {
                                    Log.e("AccountFragment", "Failed to load avatar: unknown error");
                                }
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                                Log.d("AccountFragment", "Avatar loaded successfully from: " + model);
                                return false;
                            }
                        })
                        .into(userAvatar);
            } else {
                Log.d("AccountFragment", "Avatar URL is empty, using placeholder");
            }
        }
    }

    private void showLoggedInState() {
        Log.d("AccountFragment", "Showing logged in state");
        notLoggedInContainer.setVisibility(View.GONE);
        loggedInContainer.setVisibility(View.VISIBLE);
    }

    private void showNotLoggedInState() {
        Log.d("AccountFragment", "Showing not logged in state");
        notLoggedInContainer.setVisibility(View.VISIBLE);
        loggedInContainer.setVisibility(View.GONE);
    }

    private void logout() {
        // Очищаємо токен та дані користувача через AuthManager
        authManager.logout();
        authToken = null;
        currentUser = null;
        recentMangaList.clear();
        recentMangaAdapter.notifyDataSetChanged();

        showNotLoggedInState();
        Toast.makeText(requireContext(), "Ви вийшли з акаунта", Toast.LENGTH_SHORT).show();
    }

    // Реалізація інтерфейсу LoginFragment.OnLoginSuccessListener
    @Override
    public void onLoginSuccess(String token, String userId) {
        Log.d("AccountFragment", "onLoginSuccess called with token: " + token + ", userId: " + userId);

        authToken = token;
        // Зберігаємо токен в AuthManager
        authManager.saveAuthData(token, new User(userId, "temp", "temp"));

        // Завантажуємо дані користувача
        loadUserProfile();
        loadHistory();
        showLoggedInState();

        // Навігація до AccountFragment після успішного входу
        navigateToAccount();

        Log.d("AccountFragment", "Login success - showing logged in state");
    }

    // Реалізація інтерфейсу RegisterFragment.OnRegisterSuccessListener
    @Override
    public void onRegisterSuccess(String token, String userId) {
        Log.d("AccountFragment", "onRegisterSuccess called with token: " + token + ", userId: " + userId);

        authToken = token;
        // Зберігаємо токен в AuthManager
        authManager.saveAuthData(token, new User(userId, "temp", "temp"));

        // Завантажуємо дані користувача
        loadUserProfile();
        loadHistory();
        showLoggedInState();

        // Навігація до AccountFragment після успішної реєстрації
        navigateToAccount();

        Log.d("AccountFragment", "Register success - showing logged in state");
    }

    // Метод для оновлення прогресу читання (викликається з ReaderFragment)
    public void updateReadingProgress(String mangaId, String chapterId, String mangaTitle,
                                      String chapterTitle, String chapterNumber, String coverUrl,
                                      int currentPage, int totalPages) {
        if (authToken != null) {
            AccountApiService apiService = AccountApiClient.getClient().create(AccountApiService.class);
            AccountApiService.ReadingProgressRequest request =
                    new AccountApiService.ReadingProgressRequest(mangaId, chapterId, mangaTitle,
                            chapterTitle, chapterNumber, coverUrl,
                            currentPage, totalPages);

            Call<Void> call = apiService.updateReadingProgress(authToken, request);
            call.enqueue(new Callback<Void>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    // Прогрес оновлено
                    if (response.isSuccessful()) {
                        // Оновлюємо список історії
                        loadHistory();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    // Помилка оновлення прогресу
                    Log.e("AccountFragment", "Error updating reading progress", t);
                }
            });
        }
    }

    private void navigateToAccount() {
        try {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
            
            // Перевіряємо, чи вже знаходимося на AccountFragment
            int currentDestination = navController.getCurrentDestination() != null ? 
                    navController.getCurrentDestination().getId() : -1;
            
            // Якщо не на AccountFragment, виконуємо навігацію
            if (currentDestination != R.id.nav_account) {
                navController.navigate(R.id.nav_account);
                Log.d("AccountFragment", "Navigating to AccountFragment after login/register");
            } else {
                Log.d("AccountFragment", "Already on AccountFragment, no navigation needed");
            }
        } catch (Exception e) {
            Log.e("AccountFragment", "Error navigating to AccountFragment", e);
            // Не показуємо Toast, бо це не критична помилка
        }
    }

    private void openProfileEdit() {
        try {
            Navigation.findNavController(requireView()).navigate(R.id.action_account_to_profile_edit);

            Log.d("AccountFragment", "Navigating to profile edit with animation");
        } catch (Exception e) {
            Log.e("AccountFragment", "Error navigating to profile edit", e);
            Toast.makeText(requireContext(), "Помилка відкриття редагування", Toast.LENGTH_SHORT).show();
        }
    }
}
