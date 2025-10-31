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
import com.example.mangaapp.utils.AuthManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
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
    // Старі поля видалені - тепер використовуємо нові поля для детального відображення
    private MaterialButton editProfileButton;
    private MaterialButton logoutButton;
    private RecyclerView recentMangaRecycler;

    // Адаптери
    private AuthPagerAdapter authPagerAdapter;
    private RecentMangaAdapter recentMangaAdapter;

    // Дані
    private String authToken;
    private User currentUser;
    private List<RecentManga> recentMangaList;

    // SharedPreferences
    private SharedPreferences prefs;
    private AuthManager authManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = requireContext().getSharedPreferences("MangaAppPrefs", 0);
        authManager = AuthManager.getInstance(requireContext());
        recentMangaList = new ArrayList<>();
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
        // Старі поля видалені - тепер використовуємо нові поля для детального відображення
        editProfileButton = view.findViewById(R.id.btn_edit_profile);
        logoutButton = view.findViewById(R.id.btn_logout);
        recentMangaRecycler = view.findViewById(R.id.recent_manga_recycler);

        editProfileButton.setOnClickListener(v -> openProfileEdit());
        logoutButton.setOnClickListener(v -> logout());
        

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
            // Обробка кліку по манзі - перехід до читання
            // TODO: Реалізувати навігацію до ReaderFragment
            Toast.makeText(requireContext(), "Перехід до: " + manga.getTitle(), Toast.LENGTH_SHORT).show();
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
            loadRecentManga();
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

    private void loadRecentManga() {
        AccountApiService apiService = AccountApiClient.getClient().create(AccountApiService.class);
        Call<List<RecentManga>> call = apiService.getRecentManga(authToken, 10);

        call.enqueue(new Callback<List<RecentManga>>() {
            @Override
            public void onResponse(@NonNull Call<List<RecentManga>> call, @NonNull Response<List<RecentManga>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    recentMangaList.clear();
                    recentMangaList.addAll(response.body());
                    recentMangaAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<RecentManga>> call, @NonNull Throwable t) {
                // Помилка мережі - показуємо кешовані дані
                if (!recentMangaList.isEmpty()) {
                    recentMangaAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    private void updateUserInfo() {
        if (currentUser != null) {
            // Основні поля
            userLogin.setText(currentUser.getLogin());
            
            // Відображаємо нікнейм
            String nickname = currentUser.getNickname() != null ? currentUser.getNickname() : "Нікнейм не вказано";
            userNickname.setText(nickname);

            // Оновлюємо нові поля для детального відображення
            TextView userLoginDisplay = getView().findViewById(R.id.user_login_display);
            TextView userNicknameDisplay = getView().findViewById(R.id.user_nickname_display);
            TextView userAboutDisplay = getView().findViewById(R.id.user_about_display);
            TextView userLanguageDisplay = getView().findViewById(R.id.user_language_display);

            if (userLoginDisplay != null) {
                userLoginDisplay.setText(currentUser.getLogin());
            }
            if (userNicknameDisplay != null) {
                userNicknameDisplay.setText(currentUser.getNickname() != null ? currentUser.getNickname() : "Не вказано");
            }
            if (userAboutDisplay != null) {
                userAboutDisplay.setText(currentUser.getAboutMyself() != null && !currentUser.getAboutMyself().isEmpty() ?
                        currentUser.getAboutMyself() : "Не вказано");
            }
            if (userLanguageDisplay != null) {
                userLanguageDisplay.setText(currentUser.getLanguage() != null ? currentUser.getLanguage() : "ua");
            }

            // Завантаження аватара користувача
            if (currentUser.getAvatarUrl() != null && !currentUser.getAvatarUrl().isEmpty()) {
                Glide.with(this)
                        .load(currentUser.getAvatarUrl())
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .into(userAvatar);
            } else {
                // Показуємо початкові літери логіну
                userAvatar.setImageResource(R.drawable.ic_person);
                userAvatar.setBackgroundResource(R.color.primary_light);
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
        loadRecentManga();
        showLoggedInState();
        
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
        loadRecentManga();
        showLoggedInState();
        
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
                        // Оновлюємо список останніх манг
                        loadRecentManga();
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

    private void openProfileEdit() {
        // Створюємо новий ProfileEditFragment
        ProfileEditFragment profileEditFragment = new ProfileEditFragment();
        
        // Замінюємо поточний фрагмент
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment_content_main, profileEditFragment)
                .addToBackStack(null)
                .commit();
    }


}