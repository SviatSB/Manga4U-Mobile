package com.example.mangaapp;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.Menu;
import android.content.SharedPreferences;
import android.view.MenuItem;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.example.mangaapp.fragments.main.ReaderFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.mangaapp.databinding.ActivityMainBinding;
import com.example.mangaapp.utils.AuthManager;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private String currentReadingMode = "horizontal";
    private AuthManager authManager;

    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Встановлення темної теми за замовчуванням
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        // Налаштування навігації
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);

        // Налаштування AppBarConfiguration для правильного відображення кнопки "назад"
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_account)
                .build();

        // Налаштування ActionBar з навігацією
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);

        // BottomNavigationView
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);

        // Автоматична навігація через NavigationUI
        NavigationUI.setupWithNavController(bottomNav, navController);

        // Анімація активного пункту (Material Design)
        bottomNav.setItemIconTintList(getResources().getColorStateList(R.color.selector_bottom_nav));
        bottomNav.setItemTextColor(getResources().getColorStateList(R.color.selector_bottom_nav));

        // Перехоплення пунктів-плейсхолдерів
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_favorites || id == R.id.nav_collections) {
                Toast.makeText(this, "Вікно зараз не доступне", Toast.LENGTH_SHORT).show();
                return false; // не перемикаємо вкладку
            }
            return NavigationUI.onNavDestinationSelected(item, navController);
        });

        // Load reading mode preference
        SharedPreferences prefs = getSharedPreferences("MangaAppPrefs", Context.MODE_PRIVATE);
        currentReadingMode = prefs.getString("reading_mode", "horizontal");
        Log.e("MangaApp", "[MainActivity] Loaded reading mode: " + currentReadingMode);
        
        // Ініціалізуємо AuthManager
        authManager = AuthManager.getInstance(this);
        
        // Перевіряємо стан авторизації при запуску
        checkAuthStatus();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // TODO: Реалізувати налаштування
            Toast.makeText(this, "Налаштування", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_language) {
            // TODO: Реалізувати вибір мови
            Toast.makeText(this, "Вибір мови", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_reading_mode) {
            toggleReadingMode();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void toggleReadingMode() {
        currentReadingMode = currentReadingMode.equals("horizontal") ? "vertical" : "horizontal";
        
        // Save preference
        SharedPreferences prefs = getSharedPreferences("MangaAppPrefs", Context.MODE_PRIVATE);
        prefs.edit().putString("reading_mode", currentReadingMode).apply();
        
        // Show toast message
        String message = currentReadingMode.equals("vertical") ? 
            "Режим читання змінено на вертикальний" : 
            "Режим читання змінено на горизонтальний";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        
        // Update current ReaderFragment if it exists
        ReaderFragment readerFragment = (ReaderFragment) getSupportFragmentManager()
                .findFragmentByTag("reader_fragment");
        if (readerFragment != null) {
            readerFragment.setReadingMode(currentReadingMode);
        }
        
        Log.e("MangaApp", "[MainActivity] Reading mode toggled to: " + currentReadingMode);
    }
    
    private void checkAuthStatus() {
        if (authManager.isLoggedIn()) {
            Log.d("MainActivity", "User is logged in: " + authManager.getCurrentUser().getLogin());
            // Оновлюємо дані користувача з серверу
            authManager.refreshUserData(this);
        } else {
            Log.d("MainActivity", "User is not logged in");
        }
    }
}