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

// Ez a RegisterFragment
// Itt tud új felhasználó regisztrálni
public class RegisterFragment extends Fragment {

    // Ehhez a fragmenthez tartozó layout
    public RegisterFragment() { super(R.layout.fragment_register); }

    // Beviteli mezők
    private EditText etName, etEmail, etPassword;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        // Input mezők összekötése az XML-ben lévőkkel
        etName = view.findViewById(R.id.etName);
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);

        // Gombok
        Button btnRegister = view.findViewById(R.id.btnRegister);
        Button btnBack = view.findViewById(R.id.btnBack);

        // Regisztráció indítása
        btnRegister.setOnClickListener(v -> doRegister());

        // Vissza a login képernyőre
        btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());
    }

    // Regisztrációs logika
    private void doRegister() {

        // Adatok kiolvasása a mezőkből
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();

        // UserDto létrehozása a megadott adatokkal
        UserDto user = new UserDto(name, email, pass);

        // API hívás: új felhasználó létrehozása
        ApiClient.users().createUser(user).enqueue(new Callback<UserDto>() {

            @Override
            public void onResponse(Call<UserDto> call, Response<UserDto> resp) {

                // Ha a fragment már nincs aktív állapotban, kilépünk
                if (!isAdded()) return;

                // Ha hiba van a válasszal
                if (!resp.isSuccessful() || resp.body() == null) {
                    Snackbar.make(requireView(),
                            "Registration fail",
                            Snackbar.LENGTH_LONG).show();
                    return;
                }

                // Sikeres regisztráció esetén üzenet
                Snackbar.make(requireView(),
                        "Registration successful! Please sign in.",
                        Snackbar.LENGTH_LONG).show();

                // Visszanavigálunk a login képernyőre
                NavHostFragment.findNavController(RegisterFragment.this)
                        .navigate(R.id.action_register_to_login);
            }

            @Override
            public void onFailure(Call<UserDto> call, Throwable t) {

                // Ha már nincs a fragment, nem csinálunk semmit
                if (!isAdded()) return;

                // Hálózati hiba üzenet
                Snackbar.make(requireView(),
                        "Network fail: " + t.getMessage(),
                        Snackbar.LENGTH_LONG).show();
            }
        });
    }
}
