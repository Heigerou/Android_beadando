package com.szokolaipecsy.companystatus.ui.auth;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.snackbar.Snackbar;
import com.szokolaipecsy.companystatus.R;
import com.szokolaipecsy.companystatus.auth.AuthManager;
import com.szokolaipecsy.companystatus.network.ApiClient;
import com.szokolaipecsy.companystatus.network.UserDto;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginFragment extends Fragment {
    public LoginFragment() { super(R.layout.fragment_login); }

    private EditText etEmail, etPass;
    private CheckBox cbStay;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        etEmail = view.findViewById(R.id.etEmail);
        etPass = view.findViewById(R.id.etPassword);
        cbStay = view.findViewById(R.id.cbStay);

        Button btnLogin = view.findViewById(R.id.btnLogin);
        Button btnGoRegister = view.findViewById(R.id.btnGoRegister);

        btnLogin.setOnClickListener(v -> doLogin());
        btnGoRegister.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_login_to_register));

    }

    private void doLogin() {
        String email = etEmail.getText().toString().trim();
        String pass  = etPass.getText().toString().trim();

        ApiClient.users().findUser(email, pass).enqueue(new Callback<List<UserDto>>() {
            @Override public void onResponse(Call<List<UserDto>> call, Response<List<UserDto>> resp) {
                if (!isAdded()) return;
                if (!resp.isSuccessful() || resp.body() == null || resp.body().isEmpty()) {
                    Snackbar.make(requireView(), "Wrong email or password", Snackbar.LENGTH_LONG).show();
                    return;
                }
                UserDto u = resp.body().get(0);

                AuthManager.getInstance(requireContext())
                        .save(u.id, u.name, u.email, cbStay.isChecked());

                NavHostFragment.findNavController(LoginFragment.this)
                        .navigate(R.id.action_login_to_home);
            }
            @Override public void onFailure(Call<List<UserDto>> call, Throwable t) {
                if (!isAdded()) return;
                Snackbar.make(requireView(), "Network fail: " + t.getMessage(), Snackbar.LENGTH_LONG).show();
            }
        });
    }
}
