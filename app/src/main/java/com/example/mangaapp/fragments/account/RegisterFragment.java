package com.example.mangaapp.fragments.account;

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

import com.example.mangaapp.R;
import com.example.mangaapp.api.AccountApiService;
import com.example.mangaapp.api.AccountApiService.RegisterRequest;
import com.example.mangaapp.api.AccountApiService.LoginRequest;
import com.example.mangaapp.api.AccountApiService.AuthResponse;
import com.example.mangaapp.api.AccountApiClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterFragment extends Fragment {

    private TextInputLayout nicknameInputLayout;
    private TextInputLayout loginInputLayout;
    private TextInputLayout passwordInputLayout;
    private TextInputLayout confirmPasswordInputLayout;
    private TextInputEditText nicknameInput;
    private TextInputEditText loginInput;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmPasswordInput;
    private MaterialButton registerButton;
    private ProgressBar progressBar;
    private TextView errorText;

    private OnRegisterSuccessListener registerSuccessListener;

    public interface OnRegisterSuccessListener {
        void onRegisterSuccess(String token, String userId);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getParentFragment() instanceof OnRegisterSuccessListener) {
            registerSuccessListener = (OnRegisterSuccessListener) getParentFragment();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        initViews(view);
        setupListeners();

        return view;
    }

    private void initViews(View view) {
        nicknameInputLayout = view.findViewById(R.id.nickname_input_layout);
        loginInputLayout = view.findViewById(R.id.register_login_input_layout);
        passwordInputLayout = view.findViewById(R.id.register_password_input_layout);
        confirmPasswordInputLayout = view.findViewById(R.id.confirm_password_input_layout);
        nicknameInput = view.findViewById(R.id.nickname_input);
        loginInput = view.findViewById(R.id.register_login_input);
        passwordInput = view.findViewById(R.id.register_password_input);
        confirmPasswordInput = view.findViewById(R.id.confirm_password_input);
        registerButton = view.findViewById(R.id.btn_register);
        progressBar = view.findViewById(R.id.register_progress);
        errorText = view.findViewById(R.id.register_error);
    }

    private void setupListeners() {
        registerButton.setOnClickListener(v -> attemptRegister());
    }

    private void attemptRegister() {
        String nickname = nicknameInput.getText() != null ? nicknameInput.getText().toString().trim() : "";
        String userName = loginInput.getText() != null ? loginInput.getText().toString().trim() : "";
        String password = passwordInput.getText() != null ? passwordInput.getText().toString() : "";
        String confirmPassword = confirmPasswordInput.getText() != null ? confirmPasswordInput.getText().toString() : "";

        // Валідація
        if (nickname.isEmpty()) {
            nicknameInputLayout.setError("Введіть нікнейм");
            return;
        }

        if (nickname.length() < 3) {
            nicknameInputLayout.setError("Нікнейм повинен містити мінімум 3 символи");
            return;
        }

        if (nickname.length() > 16) {
            nicknameInputLayout.setError("Нікнейм повинен містити максимум 16 символів");
            return;
        }

        if (userName.isEmpty()) {
            loginInputLayout.setError("Введіть логін");
            return;
        }

        if (userName.length() < 3) {
            loginInputLayout.setError("Логін повинен містити мінімум 3 символи");
            return;
        }

        if (password.isEmpty()) {
            passwordInputLayout.setError("Введіть пароль");
            return;
        }

        if (password.length() < 6) {
            passwordInputLayout.setError("Пароль повинен містити мінімум 6 символів");
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordInputLayout.setError("Паролі не співпадають");
            return;
        }

        // Очищення помилок
        nicknameInputLayout.setError(null);
        loginInputLayout.setError(null);
        passwordInputLayout.setError(null);
        confirmPasswordInputLayout.setError(null);
        errorText.setVisibility(View.GONE);

        // Показ прогресу
        showProgress(true);

        Log.d("RegisterFragment", "Starting registration with userName: " + userName + " and nickname: " + nickname);

        RegisterRequest request = new RegisterRequest(userName, password, nickname);
        Log.d("RegisterFragment", "RegisterRequest created: " + request.getLogin() + ", " + request.getNickname());

        try {
            AccountApiService apiService = AccountApiClient.getClient().create(AccountApiService.class);
            Call<ResponseBody> call = apiService.register(request);

            Log.d("RegisterFragment", "API call created, executing...");
            Log.d("RegisterFragment", "Base URL: " + AccountApiClient.getClient().baseUrl());

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                    showProgress(false);
                    Log.d("RegisterFragment", "Response received. Code: " + response.code() + ", Success: " + response.isSuccessful());

                    if (response.isSuccessful()) {
                        Log.d("RegisterFragment", "Registration successful!");
                        Toast.makeText(requireContext(), "Акаунт успішно створено!", Toast.LENGTH_SHORT).show();
                        performAutoLogin(userName, password);
                    } else {
                        Log.e("RegisterFragment", "Registration failed. Code: " + response.code() + ", Message: " + response.message());

                        // Спробуємо отримати детальну інформацію про помилку
                        String errorMessage = "Помилка реєстрації. Код: " + response.code();
                        try {
                            if (response.errorBody() != null) {
                                String errorBody = response.errorBody().string();
                                Log.e("RegisterFragment", "Error body: " + errorBody);
                                if (errorBody.contains("already exists") || errorBody.contains("вже існує")) {
                                    errorMessage = "Користувач з таким логіном або нікнеймом вже існує";
                                }
                            }
                        } catch (Exception e) {
                            Log.e("RegisterFragment", "Error reading error body", e);
                        }

                        showError(errorMessage);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                    showProgress(false);
                    Log.e("RegisterFragment", "Network error: " + t.getMessage(), t);
                    showError("Помилка мережі: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            showProgress(false);
            Log.e("RegisterFragment", "Exception during API call setup", e);
            showError("Помилка створення запиту: " + e.getMessage());
        }
    }

    private void performAutoLogin(String login, String password) {
        LoginRequest loginRequest = new LoginRequest(login, password);
        AccountApiService apiService = AccountApiClient.getClient().create(AccountApiService.class);
        Call<AuthResponse> call = apiService.login(loginRequest);

        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse> call, @NonNull Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    if (authResponse.getToken() != null) {
                        if (registerSuccessListener != null) {
                            registerSuccessListener.onRegisterSuccess(authResponse.getToken(), "new_user");
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Реєстрація успішна, але автологін не вдався. Спробуйте увійти вручну.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
                Toast.makeText(requireContext(), "Реєстрація успішна, але автологін не вдався: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        registerButton.setEnabled(!show);
    }

    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }

    public void setOnRegisterSuccessListener(OnRegisterSuccessListener listener) {
        this.registerSuccessListener = listener;
    }
}
