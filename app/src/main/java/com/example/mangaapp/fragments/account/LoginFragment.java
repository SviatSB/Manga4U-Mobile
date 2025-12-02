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
import com.example.mangaapp.api.AccountApiService.LoginRequest;
import com.example.mangaapp.api.AccountApiService.AuthResponse;
import com.example.mangaapp.api.AccountApiClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginFragment extends Fragment {

    private TextInputLayout loginInputLayout;
    private TextInputLayout passwordInputLayout;
    private TextInputEditText loginInput;
    private TextInputEditText passwordInput;
    private MaterialButton loginButton;
    private ProgressBar progressBar;
    private TextView errorText;

    private OnLoginSuccessListener loginSuccessListener;

    public interface OnLoginSuccessListener {
        void onLoginSuccess(String token, String userId);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getParentFragment() instanceof OnLoginSuccessListener) {
            loginSuccessListener = (OnLoginSuccessListener) getParentFragment();
            Log.d("LoginFragment", "LoginSuccessListener set from parent fragment");
        } else if (getActivity() instanceof OnLoginSuccessListener) {
            loginSuccessListener = (OnLoginSuccessListener) getActivity();
        } else {
            Log.d("LoginFragment", "Parent is not OnLoginSuccessListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        initViews(view);
        setupListeners();

        return view;
    }

    private void initViews(View view) {
        loginInputLayout = view.findViewById(R.id.login_input_layout);
        passwordInputLayout = view.findViewById(R.id.password_input_layout);
        loginInput = view.findViewById(R.id.login_input);
        passwordInput = view.findViewById(R.id.password_input);
        loginButton = view.findViewById(R.id.btn_login);
        progressBar = view.findViewById(R.id.login_progress);
        errorText = view.findViewById(R.id.login_error);
    }

    private void setupListeners() {
        loginButton.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String login = loginInput.getText() != null ? loginInput.getText().toString().trim() : "";
        String password = passwordInput.getText() != null ? passwordInput.getText().toString() : "";

        if (login.isEmpty()) {
            loginInputLayout.setError("Введіть логін або email");
            return;
        }

        if (password.isEmpty()) {
            passwordInputLayout.setError("Введіть пароль");
            return;
        }

        loginInputLayout.setError(null);
        passwordInputLayout.setError(null);
        errorText.setVisibility(View.GONE);

        showProgress(true);

        LoginRequest request = new LoginRequest(login, password);

        AccountApiService apiService = AccountApiClient.getClient().create(AccountApiService.class);
        Call<AuthResponse> call = apiService.login(request);

        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse> call, @NonNull Response<AuthResponse> response) {
                showProgress(false);

                if (response.isSuccessful() && response.body() != null && response.body().getToken() != null) {
                    AuthResponse authResponse = response.body();
                    Toast.makeText(requireContext(), "Успішний вхід!", Toast.LENGTH_SHORT).show();

                    if (loginSuccessListener != null) {
                        loginSuccessListener.onLoginSuccess(authResponse.getToken(), "user_id"); // Вам може знадобитися отримати ID користувача з відповіді
                    } else {
                        Log.e("LoginFragment", "loginSuccessListener is null!");
                    }
                    
                    // Навігація до AccountFragment після успішного входу
                    navigateToAccount();
                } else {
                    // Обробка помилок HTTP
                    if (response.code() == 400 || response.code() == 401) {
                        showError("Невірний логін або пароль");
                    } else {
                        showError("Помилка підключення до сервера. Код: " + response.code());
                    }
                    Log.e("LoginFragment", "Login failed. Code: " + response.code() + ", Message: " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
                showProgress(false);
                showError("Помилка мережі: " + t.getMessage());
                t.printStackTrace();
            }
        });
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!show);
    }

    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }

    public void setOnLoginSuccessListener(OnLoginSuccessListener listener) {
        this.loginSuccessListener = listener;
    }
    
    private void navigateToAccount() {
        try {
            androidx.navigation.NavController navController = androidx.navigation.Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
            
            // Перевіряємо, чи вже знаходимося на AccountFragment
            int currentDestination = navController.getCurrentDestination() != null ? 
                    navController.getCurrentDestination().getId() : -1;
            
            // Якщо не на AccountFragment, виконуємо навігацію
            if (currentDestination != R.id.nav_account) {
                navController.navigate(R.id.nav_account);
                Log.d("LoginFragment", "Navigating to AccountFragment after login");
            } else {
                Log.d("LoginFragment", "Already on AccountFragment, no navigation needed");
            }
        } catch (Exception e) {
            Log.e("LoginFragment", "Error navigating to AccountFragment", e);
        }
    }
}