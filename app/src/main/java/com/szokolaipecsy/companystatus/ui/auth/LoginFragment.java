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

// Ez a LoginFragment
// Itt tud a felhasználó bejelentkezni email + jelszóval
public class LoginFragment extends Fragment {

    // Ehhez a fragmenthez tartozó layout
    public LoginFragment() { super(R.layout.fragment_login); }

    // Beviteli mezők és checkbox
    private EditText etEmail, etPass;
    private CheckBox cbStay;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        // Input mezők összekötése az XML-ben lévőkkel
        etEmail = view.findViewById(R.id.etEmail);
        etPass = view.findViewById(R.id.etPassword);
        cbStay = view.findViewById(R.id.cbStay);

        // Gombok
        Button btnLogin = view.findViewById(R.id.btnLogin);
        Button btnGoRegister = view.findViewById(R.id.btnGoRegister);

        // Login gomb -> bejelentkezés indítása
        btnLogin.setOnClickListener(v -> doLogin());

        // Regisztráció gomb -> átnavigál a RegisterFragmentre
        btnGoRegister.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_login_to_register));
    }

    // Bejelentkezési logika
    private void doLogin() {

        // Email és jelszó kiolvasása
        String email = etEmail.getText().toString().trim();
        String pass  = etPass.getText().toString().trim();

        // API hívás: felhasználó keresése email + jelszó alapján
        ApiClient.users().findUser(email, pass).enqueue(new Callback<List<UserDto>>() {

            @Override
            public void onResponse(Call<List<UserDto>> call, Response<List<UserDto>> resp) {

                // Ha a fragment már nincs a képernyőn, nem csinálunk semmit
                if (!isAdded()) return;

                // Ha nincs találat vagy hiba van -> rossz adatok
                if (!resp.isSuccessful() || resp.body() == null || resp.body().isEmpty()) {
                    Snackbar.make(requireView(),
                            "Wrong email or password",
                            Snackbar.LENGTH_LONG).show();
                    return;
                }

                // Az első (és elvileg egyetlen) felhasználó
                UserDto u = resp.body().get(0);

                // Bejelentkezési adatok mentése AuthManagerrel
                // id, név, email + maradjak belépve checkbox
                AuthManager.getInstance(requireContext())
                        .save(u.id, u.name, u.email, cbStay.isChecked());

                // Sikeres login után főoldalra navigálunk
                NavHostFragment.findNavController(LoginFragment.this)
                        .navigate(R.id.action_login_to_home);
            }

            @Override
            public void onFailure(Call<List<UserDto>> call, Throwable t) {

                // Ha már nem aktív a fragment, nem csinálunk semmit
                if (!isAdded()) return;

                // Hálózati hiba esetén hibaüzenet
                Snackbar.make(requireView(),
                        "Network fail: " + t.getMessage(),
                        Snackbar.LENGTH_LONG).show();
            }
        });
    }
}
