package com.example.mangaapp.fragments.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.content.Context;
import android.util.Log;
import android.widget.ScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import androidx.navigation.Navigation;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback;

import com.example.mangaapp.adapters.ImagePagerAdapter;
import com.example.mangaapp.databinding.FragmentReaderBinding;
import com.example.mangaapp.API_MangaDex.Chapter;
import com.example.mangaapp.API_MangaDex.AtHomeServerResponse;
import com.example.mangaapp.API_MangaDex.ChapterFeedResponse;
import com.example.mangaapp.api.ApiClient;
import com.example.mangaapp.MangaApiService;
import com.example.mangaapp.api.AccountApiService;
import com.example.mangaapp.api.AccountApiClient;
import com.example.mangaapp.utils.AuthManager;
import okhttp3.ResponseBody;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

public class ReaderFragment extends Fragment {
    private FragmentReaderBinding binding;
    private String mangaId;
    private String chapterId;
    private String chapterHash;
    private String baseUrl;
    private List<String> imageUrls = new ArrayList<>();
    private ProgressBar progressBar;
    private ViewPager2 viewPager;
    private ScrollView verticalScroll;
    private String readingMode = "horizontal"; // Default mode
    private LinearLayout verticalContainer;
    private int currentPage = 0;
    private PhotoView currentPageView;
    private float startX = 0;
    private float startY = 0;
    private static final float SWIPE_THRESHOLD = 100; // Мінімальна відстань для свайпу
    private static final float SWIPE_ANGLE_THRESHOLD = 30; // Кут для визначення напрямку свайпу
    private boolean isInitialized = false;
    private float initialX = 0;
    private float initialY = 0;
    
    // Нові поля для навігації
    private TextView chapterInfo;
    private ImageView btnPrevious, btnNext, btnComments;
    private View navigationMenu;
    private boolean isNavigationMenuVisible = true;
    private List<ChapterFeedResponse.Result> allChapters = new ArrayList<>();
    private int currentChapterIndex = 0;
    private String currentChapterTitle = "";
    private String currentChapterNumber = "";
    private String mangaTitle = "";
    private AuthManager authManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mangaId = getArguments().getString("mangaId");
            chapterId = getArguments().getString("chapterId");
            mangaTitle = getArguments().getString("mangaTitle", "");
            Log.e("MangaApp", "[ReaderFragment] Created with mangaId: " + mangaId + ", chapterId: " + chapterId);
        }
        
        authManager = AuthManager.getInstance(requireContext());

        SharedPreferences prefs = requireContext().getSharedPreferences("MangaAppPrefs", Context.MODE_PRIVATE);
        readingMode = prefs.getString("reading_mode", "horizontal");
        currentPage = prefs.getInt("last_page_" + chapterId, 0);
        Log.e("MangaApp", "[ReaderFragment] Loaded reading mode: " + readingMode + ", current page: " + currentPage);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentReaderBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        progressBar = binding.progressBar;
        viewPager = binding.pager;
        verticalScroll = binding.verticalScroll;
        verticalContainer = binding.verticalContainer;
        
        // Ініціалізація елементів навігації
        chapterInfo = binding.chapterInfo;
        btnPrevious = binding.btnPrevious;
        btnNext = binding.btnNext;
        btnComments = binding.btnComments;
        navigationMenu = binding.navigationMenu;

        setupReadingMode();
        setupNavigationButtons();
        setupTouchHandling();
        
        if (!isInitialized) {
            loadChapter();
            loadAllChapters();
            isInitialized = true;
        } else {
            // Відновлюємо стан при перезаході
            if ("vertical".equals(readingMode)) {
                loadCurrentPage();
            }
        }
        
        return root;
    }

    private void setupReadingMode() {
        if ("vertical".equals(readingMode)) {
            viewPager.setVisibility(View.GONE);
            verticalScroll.setVisibility(View.VISIBLE);
            
            // Додаємо обробник дотиків для навігації
            verticalScroll.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getX();
                        startY = event.getY();
                        return false;
                        
                    case MotionEvent.ACTION_UP:
                        float endX = event.getX();
                        float endY = event.getY();
                        float deltaX = endX - startX;
                        float deltaY = endY - startY;
                        
                        // Обчислюємо кут свайпу
                        double angle = Math.abs(Math.toDegrees(Math.atan2(deltaY, deltaX)));
                        
                        // Перевіряємо, чи це був свайп і в якому напрямку
                        if (Math.abs(deltaX) > SWIPE_THRESHOLD || Math.abs(deltaY) > SWIPE_THRESHOLD) {
                            // Якщо кут менше порогу - це горизонтальний свайп
                            if (angle < SWIPE_ANGLE_THRESHOLD || angle > (180 - SWIPE_ANGLE_THRESHOLD)) {
                                if (deltaX > 0) {
                                    // Свайп вправо -> попередня сторінка
                                    navigateToPrevious();
                                } else {
                                    // Свайп вліво -> наступна сторінка
                                    navigateToNext();
                                }
                                return true;
                            }
                            // Якщо кут більше порогу - це вертикальний свайп, дозволяємо звичайний скролінг
                            return false;
                        }
                        return false;
                }
                return false;
            });
        } else {
            viewPager.setVisibility(View.VISIBLE);
            verticalScroll.setVisibility(View.GONE);
            viewPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
            
            // Додаємо обробник дотиків для навігації в горизонтальному режимі
            viewPager.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getX();
                        startY = event.getY();
                        return false;
                        
                    case MotionEvent.ACTION_UP:
                        float endX = event.getX();
                        float endY = event.getY();
                        float deltaX = endX - startX;
                        float deltaY = endY - startY;
                        
                        // Перевіряємо, чи це був свайп
                        if (Math.abs(deltaX) > SWIPE_THRESHOLD) {
                            if (deltaX > 0) {
                                // Свайп вправо -> попередня сторінка
                                navigateToPrevious();
                            } else {
                                // Свайп вліво -> наступна сторінка
                                navigateToNext();
                            }
                            return true;
                        } else if (Math.abs(deltaX) < 50 && Math.abs(deltaY) < 50) {
                            // Маленький рух - показ/скриття меню
                            float screenWidth = getResources().getDisplayMetrics().widthPixels;
                            float screenHeight = getResources().getDisplayMetrics().heightPixels;
                            if (Math.abs(startX - screenWidth / 2) < screenWidth * 0.3f && Math.abs(startY - screenHeight / 2) < screenHeight * 0.3f) {
                                toggleNavigationMenu();
                            }
                            return true;
                        }
                        return false;
                }
                return false;
            });
        }
    }

    private OnPageChangeCallback pageChangeCallback;
    
    private void setupNavigationButtons() {
        btnPrevious.setOnClickListener(v -> {
            Log.e("MangaApp", "[ReaderFragment] Previous button clicked");
            navigateToPrevious();
        });
        btnNext.setOnClickListener(v -> {
            Log.e("MangaApp", "[ReaderFragment] Next button clicked");
            navigateToNext();
        });

        // Додаємо обробку кліку по кнопці коментарів
        if (btnComments != null) {
            btnComments.setOnClickListener(v -> showComments());
        }
        
        // Обробка дотиків по центру екрану для показу/скриття меню
        View.OnClickListener centerClickListener = v -> {
            Log.e("MangaApp", "[ReaderFragment] Center clicked, toggling navigation menu");
            toggleNavigationMenu();
        };
        
        // Додаємо обробники дотиків до основних контейнерів
        viewPager.setOnClickListener(centerClickListener);
        verticalScroll.setOnClickListener(centerClickListener);
        
        // Додаємо обробник дотиків до PhotoView у горизонтальному режимі
        pageChangeCallback = new OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                // Оновлюємо currentPage тільки якщо це не було викликано через навігацію
                if (currentPage != position) {
                    currentPage = position;
                    updateChapterInfo();
                    saveCurrentPage();
                    Log.e("MangaApp", "[ReaderFragment] Page changed to: " + position);
                }
            }
        };
        viewPager.registerOnPageChangeCallback(pageChangeCallback);
    }

    private void showComments() {
        if (chapterId != null) {
            CommentsBottomSheetDialogFragment commentsFragment = CommentsBottomSheetDialogFragment.newInstance(chapterId);
            commentsFragment.show(getParentFragmentManager(), "CommentsSheet");
        } else {
            Toast.makeText(getContext(), "Помилка завантаження глави", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleNavigationMenu() {
        Log.e("MangaApp", "[ReaderFragment] Toggling navigation menu - current state: " + isNavigationMenuVisible);
        if (isNavigationMenuVisible) {
            // Анімація зникнення
            Animation fadeOut = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_out);
            fadeOut.setDuration(300);
            navigationMenu.startAnimation(fadeOut);
            navigationMenu.setVisibility(View.GONE);
            isNavigationMenuVisible = false;
            Log.e("MangaApp", "[ReaderFragment] Navigation menu hidden");
        } else {
            // Анімація появи
            navigationMenu.setVisibility(View.VISIBLE);
            Animation fadeIn = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in);
            fadeIn.setDuration(300);
            navigationMenu.startAnimation(fadeIn);
            isNavigationMenuVisible = true;
            Log.e("MangaApp", "[ReaderFragment] Navigation menu shown");
        }
    }

    private void navigateToPrevious() {
        Log.e("MangaApp", "[ReaderFragment] Navigating to previous - currentPage: " + currentPage + ", currentChapterIndex: " + currentChapterIndex);
        if (currentPage > 0) {
            // Перехід на попередню сторінку в поточній главі
            currentPage--;
            
            if ("horizontal".equals(readingMode)) {
                // У горизонтальному режимі синхронізуємо з ViewPager2
                Log.e("MangaApp", "[ReaderFragment] Horizontal mode: setting ViewPager2 to page " + currentPage);
                viewPager.setCurrentItem(currentPage, true);
            } else {
                // У вертикальному режимі завантажуємо сторінку
                Log.e("MangaApp", "[ReaderFragment] Vertical mode: loading page " + currentPage);
                loadCurrentPage();
            }
            
            saveCurrentPage();
            updateChapterInfo();
            Log.e("MangaApp", "[ReaderFragment] Moved to previous page: " + currentPage);
        } else if (currentChapterIndex > 0) {
            // Перехід на останню сторінку попередньої глави
            Log.e("MangaApp", "[ReaderFragment] Moving to previous chapter: " + (currentChapterIndex - 1));
            navigateToChapter(currentChapterIndex - 1, -1); // -1 означає останню сторінку
        } else {
            // Повернення до деталей манги
            Log.e("MangaApp", "[ReaderFragment] No previous chapter/page, navigating back");
            Navigation.findNavController(requireView()).navigateUp();
        }
    }

    private void navigateToNext() {
        Log.e("MangaApp", "[ReaderFragment] Navigating to next - currentPage: " + currentPage + ", totalPages: " + imageUrls.size() + ", currentChapterIndex: " + currentChapterIndex + ", totalChapters: " + allChapters.size());
        if (currentPage < imageUrls.size() - 1) {
            // Перехід на наступну сторінку в поточній главі
            currentPage++;
            
            if ("horizontal".equals(readingMode)) {
                // У горизонтальному режимі синхронізуємо з ViewPager2
                Log.e("MangaApp", "[ReaderFragment] Horizontal mode: setting ViewPager2 to page " + currentPage);
                viewPager.setCurrentItem(currentPage, true);
            } else {
                // У вертикальному режимі завантажуємо сторінку
                Log.e("MangaApp", "[ReaderFragment] Vertical mode: loading page " + currentPage);
                loadCurrentPage();
            }
            
            saveCurrentPage();
            updateChapterInfo();
            Log.e("MangaApp", "[ReaderFragment] Moved to next page: " + currentPage);
        } else if (currentChapterIndex < allChapters.size() - 1) {
            // Перехід на першу сторінку наступної глави
            Log.e("MangaApp", "[ReaderFragment] Moving to next chapter: " + (currentChapterIndex + 1));
            navigateToChapter(currentChapterIndex + 1, 0);
        } else {
            // Повернення до деталей манги
            Log.e("MangaApp", "[ReaderFragment] No next chapter/page, navigating back");
            Navigation.findNavController(requireView()).navigateUp();
        }
    }

    private void navigateToChapter(int chapterIndex, int pageIndex) {
        if (chapterIndex >= 0 && chapterIndex < allChapters.size()) {
            ChapterFeedResponse.Result chapter = allChapters.get(chapterIndex);
            currentChapterIndex = chapterIndex;
            chapterId = chapter.getId();
            
            // Оновлюємо інформацію про главу
            if (chapter.getAttributes() != null) {
                currentChapterTitle = chapter.getAttributes().getTitle();
                currentChapterNumber = chapter.getAttributes().getChapter();
            }
            
            // Завантажуємо нову главу
            loadChapter();
            
            // Встановлюємо потрібну сторінку
            if (pageIndex == -1) {
                // Остання сторінка
                currentPage = imageUrls.size() - 1;
            } else if (pageIndex == 0) {
                // Перша сторінка
                currentPage = 0;
            }
            
            // Синхронізуємо з ViewPager2 у горизонтальному режимі
            if ("horizontal".equals(readingMode) && viewPager != null && !imageUrls.isEmpty()) {
                int safePage = Math.min(currentPage, imageUrls.size() - 1);
                safePage = Math.max(safePage, 0);
                viewPager.setCurrentItem(safePage, false);
                currentPage = safePage;
                Log.e("MangaApp", "[ReaderFragment] Synced ViewPager2 to page " + currentPage + " after chapter change");
            }
            
            updateChapterInfo();
            // Оновлюємо історію при переході на нову главу
            updateHistory();
        }
    }

    private void updateChapterInfo() {
        if (chapterInfo != null && !imageUrls.isEmpty()) {
            String info = String.format("Глава %s, сторінка %d/%d", 
                currentChapterNumber, currentPage + 1, imageUrls.size());
            chapterInfo.setText(info);
            Log.e("MangaApp", "[ReaderFragment] Updated chapter info: " + info);
        } else {
            Log.e("MangaApp", "[ReaderFragment] Cannot update chapter info - chapterInfo: " + (chapterInfo != null) + ", imageUrls size: " + imageUrls.size());
        }
    }

    private void loadAllChapters() {
        Log.e("MangaApp", "[ReaderFragment] Loading all chapters for manga: " + mangaId);
        MangaApiService apiService = ApiClient.getClient().create(MangaApiService.class);
        List<String> langs = new ArrayList<>();
        langs.add("en");
        langs.add("uk");
        langs.add("ru");
        langs.add("ja");
        
        Call<ChapterFeedResponse> call = apiService.getMangaChapters("manga/" + mangaId + "/feed", 100, 0, "asc", langs);
        call.enqueue(new Callback<ChapterFeedResponse>() {
            @Override
            public void onResponse(Call<ChapterFeedResponse> call, Response<ChapterFeedResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allChapters = response.body().getData();
                    Log.e("MangaApp", "[ReaderFragment] Loaded " + allChapters.size() + " chapters");
                    
                    // Знаходимо поточну главу
                    for (int i = 0; i < allChapters.size(); i++) {
                        if (allChapters.get(i).getId().equals(chapterId)) {
                            currentChapterIndex = i;
                            ChapterFeedResponse.Result chapter = allChapters.get(i);
                            if (chapter.getAttributes() != null) {
                                currentChapterTitle = chapter.getAttributes().getTitle();
                                currentChapterNumber = chapter.getAttributes().getChapter();
                            }
                            Log.e("MangaApp", "[ReaderFragment] Found current chapter at index: " + i + ", title: " + currentChapterTitle + ", number: " + currentChapterNumber);
                            break;
                        }
                    }
                    
                    updateChapterInfo();
                } else {
                    Log.e("MangaApp", "[ReaderFragment] Failed to load chapters - response not successful");
                }
            }

            @Override
            public void onFailure(Call<ChapterFeedResponse> call, Throwable t) {
                Log.e("MangaApp", "[ReaderFragment] Failed to load chapters", t);
            }
        });
    }

    private void loadNextPage() {
        navigateToNext();
    }

    private void loadPreviousPage() {
        navigateToPrevious();
    }

    private void loadCurrentPage() {
        if (imageUrls.isEmpty() || currentPage < 0 || currentPage >= imageUrls.size()) {
            return;
        }
        
        verticalContainer.removeAllViews();
        
        currentPageView = new PhotoView(requireContext());
        currentPageView.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        
        // Додаємо обробку дотиків до PhotoView
        currentPageView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = event.getX();
                    initialY = event.getY();
                    return false;

                case MotionEvent.ACTION_UP:
                    float deltaX = event.getX() - initialX;
                    float deltaY = event.getY() - initialY;
                    
                    // Перевіряємо, чи дотик був біля краю екрану (в межах 20% ширини екрану)
                    float screenWidth = getResources().getDisplayMetrics().widthPixels;
                    float edgeThreshold = screenWidth * 0.2f;
                    
                    if (Math.abs(deltaX) < 50 && Math.abs(deltaY) < 50) { // Маленький рух, щоб уникнути спрацьовування при скролінгу
                        if (initialX < edgeThreshold) {
                            // Дотик по лівому краю - перехід на попередню сторінку
                            navigateToPrevious();
                            return true;
                        } else if (initialX > screenWidth - edgeThreshold) {
                            // Дотик по правому краю - перехід на наступну сторінку
                            navigateToNext();
                            return true;
                        } else if (Math.abs(initialX - screenWidth / 2) < screenWidth * 0.3f && Math.abs(initialY - getResources().getDisplayMetrics().heightPixels / 2) < getResources().getDisplayMetrics().heightPixels * 0.3f) {
                            // Дотик по центру екрану - показ/скриття меню
                            toggleNavigationMenu();
                            return true;
                        }
                    }
                    return false;
            }
            return false;
        });
        
        // Додаємо обробник дотиків по центру для показу/скриття меню
        currentPageView.setOnClickListener(v -> {
            // Додатковий обробник для дотиків по центру
            float screenWidth = getResources().getDisplayMetrics().widthPixels;
            float screenHeight = getResources().getDisplayMetrics().heightPixels;
            if (Math.abs(initialX - screenWidth / 2) < screenWidth * 0.3f && Math.abs(initialY - screenHeight / 2) < screenHeight * 0.3f) {
                toggleNavigationMenu();
            }
        });
        
        verticalContainer.addView(currentPageView);
        
        Glide.with(requireContext())
            .load(imageUrls.get(currentPage))
            .override(Target.SIZE_ORIGINAL)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(currentPageView);

        // Попереднє завантаження наступної сторінки
        if (currentPage + 1 < imageUrls.size()) {
            Glide.with(requireContext())
                .load(imageUrls.get(currentPage + 1))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .preload();
        }
        
        // Оновлюємо інформацію про главу
        updateChapterInfo();
    }

    private void loadChapter() {
        progressBar.setVisibility(View.VISIBLE);
        Log.e("MangaApp", "[ReaderFragment] Loading chapter: " + chapterId);

        MangaApiService service = ApiClient.getClient().create(MangaApiService.class);
        service.getChapterDetails("chapter/" + chapterId).enqueue(new Callback<Chapter>() {
            @Override
            public void onResponse(@NonNull Call<Chapter> call, @NonNull Response<Chapter> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Chapter chapter = response.body();
                    if (chapter.getData() != null && chapter.getData().getAttributes() != null) {
                        service.getAtHomeServer("at-home/server/" + chapterId).enqueue(new Callback<AtHomeServerResponse>() {
                            @Override
                            public void onResponse(@NonNull Call<AtHomeServerResponse> call, @NonNull Response<AtHomeServerResponse> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    baseUrl = response.body().getBaseUrl();
                                    chapterHash = response.body().getChapter().getHash();
                                    Log.e("MangaApp", "[ReaderFragment] Got at-home server URL: " + baseUrl);
                                    Log.e("MangaApp", "[ReaderFragment] Got chapter hash: " + chapterHash);

                                    imageUrls.clear();
                                    List<String> pages = response.body().getChapter().getData();
                                    for (String page : pages) {
                                        String imageUrl = baseUrl + "/data/" + chapterHash + "/" + page;
                                        imageUrls.add(imageUrl);
                                        Log.e("MangaApp", "[ReaderFragment] Added image URL: " + imageUrl);
                                    }

                                    if ("vertical".equals(readingMode)) {
                                        loadCurrentPage();
                                    } else {
                                        ImagePagerAdapter adapter = new ImagePagerAdapter(imageUrls, readingMode);
                                        viewPager.setAdapter(adapter);
                                        
                                        // Встановлюємо початкову позицію у ViewPager2
                                        int initialPage = Math.min(currentPage, imageUrls.size() - 1);
                                        initialPage = Math.max(initialPage, 0);
                                        viewPager.setCurrentItem(initialPage, false);
                                        
                                        // Оновлюємо історію після завантаження глави
                                        updateHistory();
                                        currentPage = initialPage;
                                        Log.e("MangaApp", "[ReaderFragment] Set initial page in ViewPager2: " + currentPage);
                                    }
                                    progressBar.setVisibility(View.GONE);
                                    Log.e("MangaApp", "[ReaderFragment] Set up display with " + imageUrls.size() + " images");
                                    
                                    // Оновлюємо інформацію про главу після завантаження
                                    updateChapterInfo();
                                } else {
                                    Log.e("MangaApp", "[ReaderFragment] Failed to get at-home server URL: " + response.code());
                                    showError("Failed to get server URL");
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<AtHomeServerResponse> call, @NonNull Throwable t) {
                                Log.e("MangaApp", "[ReaderFragment] Error getting at-home server URL", t);
                                showError("Error connecting to server");
                            }
                        });
                    } else {
                        Log.e("MangaApp", "[ReaderFragment] Invalid chapter data");
                        showError("Invalid chapter data");
                    }
                } else {
                    Log.e("MangaApp", "[ReaderFragment] Failed to load chapter: " + response.code());
                    showError("Failed to load chapter");
                }
            }

            @Override
            public void onFailure(@NonNull Call<Chapter> call, @NonNull Throwable t) {
                Log.e("MangaApp", "[ReaderFragment] Error loading chapter", t);
                showError("Error connecting to server");
            }
        });
    }

    public void setReadingMode(String mode) {
        if (!mode.equals(readingMode)) {
            readingMode = mode;
            SharedPreferences prefs = requireContext().getSharedPreferences("MangaAppPrefs", Context.MODE_PRIVATE);
            prefs.edit().putString("reading_mode", mode).apply();
            
            setupReadingMode();
            
            if ("vertical".equals(mode) && !imageUrls.isEmpty()) {
                // У вертикальному режимі завантажуємо поточну сторінку
                loadCurrentPage();
            } else if (!"vertical".equals(mode)) {
                // У горизонтальному режимі встановлюємо ViewPager2
                int currentPosition = Math.min(currentPage, imageUrls.size() - 1);
                currentPosition = Math.max(currentPosition, 0);
                ImagePagerAdapter newAdapter = new ImagePagerAdapter(imageUrls, mode);
                viewPager.setAdapter(newAdapter);
                viewPager.setCurrentItem(currentPosition, false);
                currentPage = currentPosition; // Синхронізуємо currentPage
            }
            
            // Оновлюємо інформацію про главу
            updateChapterInfo();
            
            Log.e("MangaApp", "[ReaderFragment] Reading mode changed to: " + mode + ", currentPage: " + currentPage);
        }
    }

    private void saveCurrentPage() {
        SharedPreferences prefs = requireContext().getSharedPreferences("MangaAppPrefs", Context.MODE_PRIVATE);
        prefs.edit().putInt("last_page_" + chapterId, currentPage).apply();
    }
    
    private void updateHistory() {
        if (!authManager.isLoggedIn() || mangaId == null || chapterId == null) {
            return;
        }
        
        String token = authManager.getAuthToken();
        if (token == null) {
            return;
        }
        
        // Отримуємо мову з SharedPreferences
        SharedPreferences prefs = requireContext().getSharedPreferences("MangaAppPrefs", Context.MODE_PRIVATE);
        String language = prefs.getString("selected_language", "en");
        
        // Конвертуємо номер глави в int
        int chapterNumber = 1; // Значення за замовчуванням (валідатор вимагає > 0)
        try {
            if (currentChapterNumber != null && !currentChapterNumber.isEmpty()) {
                // Можливо формат "1.5" або "1", потрібно обробити
                String[] parts = currentChapterNumber.split("\\.");
                float num = Float.parseFloat(parts[0]);
                chapterNumber = (int) num;
                if (chapterNumber <= 0) {
                    chapterNumber = 1; // Якщо отримали 0 або менше, використовуємо 1
                }
            }
        } catch (NumberFormatException e) {
            Log.e("ReaderFragment", "Error parsing chapter number: " + currentChapterNumber, e);
            chapterNumber = 1; // Якщо не вдалося розпарсити, використовуємо 1
        }
        
        String chapterTitle = currentChapterTitle != null && !currentChapterTitle.isEmpty() ? currentChapterTitle : "Перегляд манги";
        
        // Перевірка на null/порожні значення
        if (mangaId == null || mangaId.isEmpty()) {
            Log.e("ReaderFragment", "MangaId is null or empty, cannot update history");
            return;
        }
        if (chapterId == null || chapterId.isEmpty()) {
            Log.e("ReaderFragment", "ChapterId is null or empty, cannot update history");
            return;
        }
        if (language == null || language.isEmpty()) {
            language = "en"; // Значення за замовчуванням
        }
        
        Log.d("ReaderFragment", "Updating history with:");
        Log.d("ReaderFragment", "  MangaId: " + mangaId);
        Log.d("ReaderFragment", "  ChapterId: " + chapterId);
        Log.d("ReaderFragment", "  Language: " + language);
        Log.d("ReaderFragment", "  ChapterTitle: " + chapterTitle);
        Log.d("ReaderFragment", "  ChapterNumber: " + chapterNumber);
        
        AccountApiService apiService = AccountApiClient.getClient().create(AccountApiService.class);
        AccountApiService.UpdateHistoryRequest request = new AccountApiService.UpdateHistoryRequest(
            mangaId,
            chapterId,
            language,
            chapterTitle,
            chapterNumber
        );
        
        Call<ResponseBody> call = apiService.updateHistory("Bearer " + token, request);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d("ReaderFragment", "History updated successfully - Status: " + response.code());
                } else {
                    Log.e("ReaderFragment", "Error updating history: " + response.code());
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            Log.e("ReaderFragment", "Error body: " + errorBody);
                        } catch (Exception e) {
                            Log.e("ReaderFragment", "Error reading error body", e);
                        }
                    }
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.e("ReaderFragment", "Error updating history", t);
            }
        });
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        saveCurrentPage();
        
        // Очищаємо callback для ViewPager2
        if (viewPager != null && pageChangeCallback != null) {
            viewPager.unregisterOnPageChangeCallback(pageChangeCallback);
        }
        
        binding = null;
    }

    private void setupTouchHandling() {
        binding.verticalScroll.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // Зберігаємо початкову позицію дотику
                    initialX = event.getX();
                    initialY = event.getY();
                    return false; // Дозволяємо ScrollView обробляти дотик

                case MotionEvent.ACTION_UP:
                    float deltaX = event.getX() - initialX;
                    float deltaY = event.getY() - initialY;
                    
                    // Перевіряємо, чи дотик був біля краю екрану (в межах 20% ширини екрану)
                    float screenWidth = getResources().getDisplayMetrics().widthPixels;
                    float edgeThreshold = screenWidth * 0.2f;
                    
                    if (Math.abs(deltaX) < 50 && Math.abs(deltaY) < 50) { // Маленький рух, щоб уникнути спрацьовування при скролінгу
                        if (initialX < edgeThreshold) {
                            // Дотик по лівому краю - перехід на попередню сторінку
                            navigateToPrevious();
                            return true;
                        } else if (initialX > screenWidth - edgeThreshold) {
                            // Дотик по правому краю - перехід на наступну сторінку
                            navigateToNext();
                            return true;
                        }
                    }
                    return false;
            }
            return false;
        });
    }
}