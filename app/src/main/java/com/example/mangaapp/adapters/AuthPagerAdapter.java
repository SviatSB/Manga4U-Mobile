package com.example.mangaapp.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.mangaapp.fragments.account.LoginFragment;
import com.example.mangaapp.fragments.account.RegisterFragment;

public class AuthPagerAdapter extends FragmentStateAdapter {

    private LoginFragment.OnLoginSuccessListener loginListener;
    private RegisterFragment.OnRegisterSuccessListener registerListener;

    public AuthPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    public void setLoginListener(LoginFragment.OnLoginSuccessListener listener) {
        this.loginListener = listener;
    }

    public void setRegisterListener(RegisterFragment.OnRegisterSuccessListener listener) {
        this.registerListener = listener;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                LoginFragment loginFragment = new LoginFragment();
                if (loginListener != null) {
                    loginFragment.setOnLoginSuccessListener(loginListener);
                }
                return loginFragment;
            case 1:
                RegisterFragment registerFragment = new RegisterFragment();
                if (registerListener != null) {
                    registerFragment.setOnRegisterSuccessListener(registerListener);
                }
                return registerFragment;
            default:
                return new LoginFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2; // Вхід та реєстрація
    }
}