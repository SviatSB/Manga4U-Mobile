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
    private boolean isUpdatingMenuSelection = false; // Flag to prevent recursion loop

    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);

        // Initialize auth manager before setting up bottom nav listener
        authManager = AuthManager.getInstance(this);

        mAppBarConfiguration = new AppBarConfiguration.Builder(
            R.id.nav_home, R.id.nav_search, R.id.nav_account, R.id.collectionsFragment)
            .build();

        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);

        // Handle menu item selection
        bottomNav.setOnItemSelectedListener(item -> {
            // Skip if we're updating from destination change listener (avoid recursion)
            if (isUpdatingMenuSelection) {
                return true;
            }
            
            int id = item.getItemId();
            if (id == R.id.collectionsFragment) {
                 if (authManager.isLoggedIn()) {
                    // Use global action to navigate to collections
                    navController.navigate(R.id.action_global_to_collectionsFragment);
                    return true;
                 } else {
                     Toast.makeText(this, "Будь ласка, увійдіть в акаунт", Toast.LENGTH_SHORT).show();
                     return false; // Don't select the item
                 }
            }
           // if (id == R.id.nav_favorites) {
           //     Toast.makeText(this, "Вікно зараз не доступне", Toast.LENGTH_SHORT).show();
           //     return false;
//}
            // For all other menu items (nav_home, nav_search, nav_account), navigate directly
            // WITHOUT calling NavigationUI.onNavDestinationSelected to avoid double-setting selection
            if (id == R.id.nav_home) {
                navController.navigate(R.id.nav_home);
                return true;
            } else if (id == R.id.nav_search) {
                navController.navigate(R.id.nav_search);
                return true;
            } else if (id == R.id.nav_account) {
                navController.navigate(R.id.nav_account);
                return true;
            }
            return false;
        });
        bottomNav.setItemIconTintList(getResources().getColorStateList(R.color.selector_bottom_nav));
        bottomNav.setItemTextColor(getResources().getColorStateList(R.color.selector_bottom_nav));

        // Sync menu selection when destination changes (with flag to prevent recursion)
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int destId = destination.getId();
            if (destId == R.id.nav_home || destId == R.id.nav_search || destId == R.id.nav_account || destId == R.id.collectionsFragment) {
                isUpdatingMenuSelection = true;
                bottomNav.setSelectedItemId(destId);
                isUpdatingMenuSelection = false;
            }
        });

        SharedPreferences prefs = getSharedPreferences("MangaAppPrefs", Context.MODE_PRIVATE);
        currentReadingMode = prefs.getString("reading_mode", "horizontal");
        Log.e("MangaApp", "[MainActivity] Loaded reading mode: " + currentReadingMode);

        authManager = AuthManager.getInstance(this);
        checkAuthStatus();
        setupFragmentTransitions();
    }

    private void setupFragmentTransitions() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            Log.d("MainActivity", "Navigated to: " + destination.getLabel());
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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
            Toast.makeText(this, "Налаштування", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_language) {
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

        SharedPreferences prefs = getSharedPreferences("MangaAppPrefs", Context.MODE_PRIVATE);
        prefs.edit().putString("reading_mode", currentReadingMode).apply();

        String message = currentReadingMode.equals("vertical") ?
                "Режим читання змінено на вертикальний" :
                "Режим читання змінено на горизонтальний";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

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
            authManager.refreshUserData(this);
        } else {
            Log.d("MainActivity", "User is not logged in");
        }
    }
}
