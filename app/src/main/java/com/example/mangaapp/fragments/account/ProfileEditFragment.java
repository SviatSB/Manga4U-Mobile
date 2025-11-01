package com.example.mangaapp.fragments.account;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.mangaapp.R;
import com.example.mangaapp.api.AccountApiService;
import com.example.mangaapp.api.UserDto;
import com.example.mangaapp.api.AccountApiClient;
import com.example.mangaapp.utils.AuthManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileEditFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_REQUEST = 2;

    // UI елементи
    private ShapeableImageView avatarImageView;
    private MaterialButton uploadAvatarButton;
    private MaterialButton removeAvatarButton;
    private TextInputLayout nicknameLayout;
    private TextInputLayout aboutLayout;
    private TextInputLayout currentPasswordLayout;
    private TextInputLayout newPasswordLayout;

    private TextInputEditText nicknameInput;
    private TextInputEditText aboutInput;
    private TextInputEditText currentPasswordInput;
    private TextInputEditText newPasswordInput;

    private MaterialButton saveButton;
    private MaterialButton cancelButton;
    private ProgressBar progressBar;
    private TextView errorText;

    // Дані
    private UserDto currentUser;
    private AuthManager authManager;
    private String authToken;
    private Uri selectedImageUri;
    private Bitmap selectedImageBitmap;

    // Анімація фону
    private AnimationDrawable backgroundAnimation;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authManager = AuthManager.getInstance(requireContext());
        authToken = authManager.getAuthToken();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile_edit, container, false);

        initViews(view);
        setupListeners();
        loadUserData();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Запускаємо анімацію фону після створення View
        startBackgroundAnimation();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Перезапускаємо анімацію фону при поверненні на фрагмент
        startBackgroundAnimation();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Зупиняємо анімацію фону
        stopBackgroundAnimation();
    }

    private void initViews(View view) {
        // Аватар
        avatarImageView = view.findViewById(R.id.avatar_image);
        uploadAvatarButton = view.findViewById(R.id.btn_upload_avatar);
        removeAvatarButton = view.findViewById(R.id.btn_remove_avatar);

        // Основні поля
        nicknameLayout = view.findViewById(R.id.nickname_layout);
        aboutLayout = view.findViewById(R.id.about_layout);

        nicknameInput = view.findViewById(R.id.nickname_input);
        aboutInput = view.findViewById(R.id.about_input);

        // Поля пароля
        currentPasswordLayout = view.findViewById(R.id.current_password_layout);
        newPasswordLayout = view.findViewById(R.id.new_password_layout);

        currentPasswordInput = view.findViewById(R.id.current_password_input);
        newPasswordInput = view.findViewById(R.id.new_password_input);

        // Кнопки
        saveButton = view.findViewById(R.id.btn_save);
        cancelButton = view.findViewById(R.id.btn_cancel);
        progressBar = view.findViewById(R.id.progress_bar);
        errorText = view.findViewById(R.id.error_text);

        // Ініціалізація анімації фону
        initBackgroundAnimation(view);
    }

    private void initBackgroundAnimation(View view) {
        try {
            // Отримуємо фон з кореневого View (CoordinatorLayout)
            View rootView = view.getRootView();
            if (rootView != null) {
                android.graphics.drawable.Drawable background = rootView.getBackground();
                if (background instanceof AnimationDrawable) {
                    backgroundAnimation = (AnimationDrawable) background;
                    // Додаємо плавність анімації
                    backgroundAnimation.setEnterFadeDuration(300);
                    backgroundAnimation.setExitFadeDuration(300);
                    Log.d("ProfileEditFragment", "Background animation initialized");
                } else {
                    Log.d("ProfileEditFragment", "Background is not AnimationDrawable: " +
                            (background != null ? background.getClass().getSimpleName() : "null"));
                }
            }
        } catch (Exception e) {
            Log.e("ProfileEditFragment", "Error initializing background animation", e);
        }
    }

    private void startBackgroundAnimation() {
        if (backgroundAnimation != null && !backgroundAnimation.isRunning()) {
            backgroundAnimation.start();
            Log.d("ProfileEditFragment", "Background animation started");
        } else {
            // Якщо анімація не ініціалізована, спробуємо знайти її знову
            View rootView = getView();
            if (rootView != null) {
                initBackgroundAnimation(rootView);
                if (backgroundAnimation != null && !backgroundAnimation.isRunning()) {
                    backgroundAnimation.start();
                    Log.d("ProfileEditFragment", "Background animation started after re-initialization");
                }
            }
        }
    }

    private void stopBackgroundAnimation() {
        if (backgroundAnimation != null && backgroundAnimation.isRunning()) {
            backgroundAnimation.stop();
            Log.d("ProfileEditFragment", "Background animation stopped");
        }
    }

    private void setupListeners() {
        uploadAvatarButton.setOnClickListener(v -> showImagePicker());
        removeAvatarButton.setOnClickListener(v -> removeAvatar());
        saveButton.setOnClickListener(v -> saveProfile());

        // Оновлений обробник для кнопки "Скасувати"
        cancelButton.setOnClickListener(v -> {
            try {
                // Перевіряємо, чи є зміни
                if (hasUnsavedChanges()) {
                    showUnsavedChangesDialog();
                } else {
                    navigateBack();
                }
            } catch (Exception e) {
                Log.e("ProfileEditFragment", "Error handling cancel", e);
                navigateBack(); // Fallback
            }
        });
    }

    private boolean hasUnsavedChanges() {
        String currentNickname = currentUser != null ? currentUser.getNickname() : "";
        String currentAbout = currentUser != null ? currentUser.getAboutMyself() : "";

        String newNickname = nicknameInput.getText() != null ? nicknameInput.getText().toString().trim() : "";
        String newAbout = aboutInput.getText() != null ? aboutInput.getText().toString().trim() : "";

        return !currentNickname.equals(newNickname) || !currentAbout.equals(newAbout) ||
                selectedImageBitmap != null ||
                (newPasswordInput.getText() != null && !newPasswordInput.getText().toString().isEmpty());
    }

    private void showUnsavedChangesDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Незбережені зміни")
                .setMessage("У вас є незбережені зміни. Ви дійсно хочете вийти?")
                .setPositiveButton("Так", (dialog, which) -> navigateBack())
                .setNegativeButton("Ні", null)
                .show();
    }

    private void navigateBack() {
        try {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
            navController.navigateUp(); // Це використає popExitAnim з nav_graph
        } catch (Exception e) {
            Log.e("ProfileEditFragment", "Error navigating back", e);
            requireActivity().onBackPressed(); // Fallback
        }
    }

    private void loadUserData() {
        if (authManager.getCurrentUser() != null) {
            // Завантажуємо дані з AuthManager
            currentUser = convertUserToUserDto(authManager.getCurrentUser());
            populateFields();
        } else {
            // Завантажуємо дані з серверу
            loadUserFromServer();
        }
    }

    private void loadUserFromServer() {
        AccountApiService apiService = AccountApiClient.getClient().create(AccountApiService.class);
        Call<UserDto> call = apiService.getMe("Bearer " + authToken);

        call.enqueue(new Callback<UserDto>() {
            @Override
            public void onResponse(@NonNull Call<UserDto> call, @NonNull Response<UserDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentUser = response.body();
                    populateFields();
                } else {
                    showError("Помилка завантаження профілю");
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserDto> call, @NonNull Throwable t) {
                showError("Помилка мережі: " + t.getMessage());
            }
        });
    }

    private UserDto convertUserToUserDto(com.example.mangaapp.models.User user) {
        UserDto userDto = new UserDto();
        userDto.setId(Long.parseLong(user.getId()));
        userDto.setLogin(user.getLogin());
        userDto.setNickname(user.getNickname());
        userDto.setAvatarUrl(user.getAvatarUrl());
        userDto.setAboutMyself(user.getAboutMyself() != null ? user.getAboutMyself() : "");
        userDto.setLanguage(user.getLanguage() != null ? user.getLanguage() : "ua");
        return userDto;
    }

    private void populateFields() {
        if (currentUser != null) {
            nicknameInput.setText(currentUser.getNickname());
            aboutInput.setText(currentUser.getAboutMyself());

            // Завантаження аватара
            if (currentUser.getAvatarUrl() != null && !currentUser.getAvatarUrl().isEmpty()) {
                Glide.with(this)
                        .load(currentUser.getAvatarUrl())
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .into(avatarImageView);
            }
        }
    }

    private void showImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && data != null) {
                selectedImageUri = data.getData();
                try {
                    selectedImageBitmap = MediaStore.Images.Media.getBitmap(
                            requireActivity().getContentResolver(), selectedImageUri);
                    avatarImageView.setImageBitmap(selectedImageBitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                    showError("Помилка завантаження зображення");
                }
            }
        }
    }

    private void removeAvatar() {
        selectedImageUri = null;
        selectedImageBitmap = null;
        avatarImageView.setImageResource(R.drawable.ic_person);
    }

    private void saveProfile() {
        if (!validateInputs()) {
            return;
        }

        showProgress(true);

        // Оновлюємо дані по черзі
        updateNickname();
    }

    private boolean validateInputs() {
        boolean isValid = true;

        // Валідація nickname
        String nickname = nicknameInput.getText() != null ? nicknameInput.getText().toString().trim() : "";
        if (nickname.isEmpty()) {
            nicknameLayout.setError("Введіть нікнейм");
            isValid = false;
        } else if (nickname.length() < 3) {
            nicknameLayout.setError("Нікнейм повинен містити мінімум 3 символи");
            isValid = false;
        } else {
            nicknameLayout.setError(null);
        }

        // Валідація пароля (якщо введено)
        String newPassword = newPasswordInput.getText() != null ? newPasswordInput.getText().toString() : "";
        String currentPassword = currentPasswordInput.getText() != null ? currentPasswordInput.getText().toString() : "";

        if (!newPassword.isEmpty()) {
            if (newPassword.length() < 8) {
                newPasswordLayout.setError("Пароль повинен містити мінімум 8 символів");
                isValid = false;
            } else if (currentPassword.isEmpty()) {
                currentPasswordLayout.setError("Введіть поточний пароль для зміни");
                isValid = false;
            } else {
                newPasswordLayout.setError(null);
                currentPasswordLayout.setError(null);
            }
        }

        return isValid;
    }

    private void updateNickname() {
        String newNickname = nicknameInput.getText() != null ? nicknameInput.getText().toString().trim() : "";

        AccountApiService apiService = AccountApiClient.getClient().create(AccountApiService.class);
        Call<ResponseBody> call = apiService.changeNickname("Bearer " + authToken, newNickname);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    updateAboutMyself();
                } else {
                    showProgress(false);
                    showError("Помилка оновлення нікнейму");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                showProgress(false);
                showError("Помилка мережі: " + t.getMessage());
            }
        });
    }

    private void updateAboutMyself() {
        String about = aboutInput.getText() != null ? aboutInput.getText().toString().trim() : "";

        AccountApiService apiService = AccountApiClient.getClient().create(AccountApiService.class);
        Call<ResponseBody> call = apiService.setAboutMyself("Bearer " + authToken, about);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    updatePassword();
                } else {
                    showProgress(false);
                    showError("Помилка оновлення опису");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                showProgress(false);
                showError("Помилка мережі: " + t.getMessage());
            }
        });
    }

    private void updatePassword() {
        String newPassword = newPasswordInput.getText() != null ? newPasswordInput.getText().toString() : "";

        if (newPassword.isEmpty()) {
            updateAvatar();
            return;
        }

        String currentPassword = currentPasswordInput.getText() != null ? currentPasswordInput.getText().toString() : "";
        AccountApiService.ChangePasswordRequest request = new AccountApiService.ChangePasswordRequest(currentPassword, newPassword);

        AccountApiService apiService = AccountApiClient.getClient().create(AccountApiService.class);
        Call<ResponseBody> call = apiService.changePassword("Bearer " + authToken, request);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    updateAvatar();
                } else {
                    showProgress(false);
                    showError("Помилка зміни пароля");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                showProgress(false);
                showError("Помилка мережі: " + t.getMessage());
            }
        });
    }

    private void updateAvatar() {
        if (selectedImageBitmap != null) {
            uploadAvatar();
        } else {
            finishUpdate();
        }
    }

    private void uploadAvatar() {
        try {
            // Конвертуємо Bitmap в файл
            File imageFile = createImageFile();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            selectedImageBitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos);
            byte[] bitmapData = bos.toByteArray();

            FileOutputStream fos = new FileOutputStream(imageFile);
            fos.write(bitmapData);
            fos.flush();
            fos.close();

            // Створюємо MultipartBody.Part
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), imageFile);
            MultipartBody.Part avatarPart = MultipartBody.Part.createFormData("file", imageFile.getName(), requestFile);

            AccountApiService apiService = AccountApiClient.getClient().create(AccountApiService.class);
            Call<ResponseBody> call = apiService.changeAvatar("Bearer " + authToken, avatarPart);

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        finishUpdate();
                    } else {
                        showProgress(false);
                        showError("Помилка завантаження аватара");
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                    showProgress(false);
                    showError("Помилка мережі: " + t.getMessage());
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
            showProgress(false);
            showError("Помилка обробки зображення");
        }
    }

    private File createImageFile() throws IOException {
        String imageFileName = "avatar_" + System.currentTimeMillis();
        File storageDir = requireContext().getCacheDir();
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void finishUpdate() {
        showProgress(false);
        Toast.makeText(requireContext(), "Профіль успішно оновлено!", Toast.LENGTH_SHORT).show();

        // Оновлюємо дані в AuthManager
        authManager.refreshUserData(requireContext());

        // Оновлюємо поточні дані користувача
        updateCurrentUserData();

        // Повертаємося назад через NavController
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
        navController.navigateUp();
    }

    private void updateCurrentUserData() {
        if (currentUser != null) {
            // Оновлюємо дані з полів форми
            currentUser.setNickname(nicknameInput.getText() != null ? nicknameInput.getText().toString().trim() : "");
            currentUser.setAboutMyself(aboutInput.getText() != null ? aboutInput.getText().toString().trim() : "");

            // Зберігаємо оновлені дані в AuthManager
            authManager.saveAuthData(authToken, currentUser);
        }
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        saveButton.setEnabled(!show);
        cancelButton.setEnabled(!show);
    }

    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }
}