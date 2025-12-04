package com.szokolaipecsy.companystatus.ui.auth;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.snackbar.Snackbar;
import com.szokolaipecsy.companystatus.R;
import com.szokolaipecsy.companystatus.network.ApiClient;
import com.szokolaipecsy.companystatus.network.UserDto;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterFragment extends Fragment {
    public RegisterFragment() { super(R.layout.fragment_register); }

    private EditText etName, etEmail, etPassword;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        etName = view.findViewById(R.id.etName);
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);

        Button btnRegister = view.findViewById(R.id.btnRegister);
        Button btnBack = view.findViewById(R.id.btnBack);

        btnRegister.setOnClickListener(v -> doRegister());
        btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());
    }

    private void doRegister() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();

        UserDto user = new UserDto(name, email, pass);
        ApiClient.users().createUser(user).enqueue(new Callback<UserDto>() {
            @Override public void onResponse(Call<UserDto> call, Response<UserDto> resp) {
                if (!isAdded()) return;
                if (!resp.isSuccessful() || resp.body() == null) {
                    Snackbar.make(requireView(), "Registration fail", Snackbar.LENGTH_LONG).show();
                    return;
                }
                Snackbar.make(requireView(), "Registration successful! Please sign in.", Snackbar.LENGTH_LONG).show();
                NavHostFragment.findNavController(RegisterFragment.this)
                        .navigate(R.id.action_register_to_login);
            }
            @Override public void onFailure(Call<UserDto> call, Throwable t) {
                if (!isAdded()) return;
                Snackbar.make(requireView(), "Network fail: " + t.getMessage(), Snackbar.LENGTH_LONG).show();
            }
        });
    }
}
