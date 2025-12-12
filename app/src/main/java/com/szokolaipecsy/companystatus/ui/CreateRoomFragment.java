package com.szokolaipecsy.companystatus.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.Snackbar;
import com.szokolaipecsy.companystatus.R;
import com.szokolaipecsy.companystatus.network.ApiClient;
import com.szokolaipecsy.companystatus.network.RoomCreateRequest;
import com.szokolaipecsy.companystatus.network.RoomDto;

import java.security.SecureRandom;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Ez a CreateRoomFragment
// Itt tudunk új szobát létrehozni (név, alcím, leírás, opcionális jelszó)
public class CreateRoomFragment extends Fragment {

    // Input mezők + Create gomb
    private EditText etName, etSubtitle, etDescription, etPassword;
    private Button btnCreate;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Betölti a fragment_create_room layoutot
        return inflater.inflate(R.layout.fragment_create_room, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);

        // Összekötjük a változókat az XML-ben lévő mezőkkel
        etName        = v.findViewById(R.id.et_name);
        etSubtitle    = v.findViewById(R.id.et_subtitle);
        etDescription = v.findViewById(R.id.et_description);
        etPassword    = v.findViewById(R.id.et_password);
        btnCreate     = v.findViewById(R.id.btn_create);

        // Create gomb -> szoba létrehozás indítása
        btnCreate.setOnClickListener(click -> doCreate());
    }

    // Szoba létrehozás logika
    private void doCreate() {

        // billentyűzet elrejtése, hogy ne lógjon a képernyőn
        hideKeyboard();

        // értékek kiolvasása a mezőkből
        String name = val(etName);
        String sub  = val(etSubtitle);
        String desc = val(etDescription);
        String pass = val(etPassword);

        // név kötelező
        if (name.isEmpty()) {
            etName.setError("Name is required");
            etName.requestFocus();
            toast("Name required");
            return;
        }

        // ha van jelszó, akkor a szoba védett
        boolean isProtected = !pass.isEmpty();

        // ezek most üresen vannak (később lehetne igazi salt+hash)
        String passwordSalt = "";
        String passwordHash = "";

        // ha védett, akkor itt most a passwordHash mezőbe a jelszó kerül
        // (egyszerűsített megoldás)
        if (isProtected) {
            passwordHash = pass;
        }

        // request objektum összerakása a szoba létrehozáshoz
        RoomCreateRequest body = new RoomCreateRequest(
                name,
                sub,
                desc,
                isProtected,
                passwordSalt,
                passwordHash
        );

        // gomb letiltása, hogy ne nyomják meg kétszer
        btnCreate.setEnabled(false);
        CharSequence oldText = btnCreate.getText();
        btnCreate.setText("Creating...");

        // API hívás: szoba létrehozása
        ApiClient.rooms().createRoom(body).enqueue(new Callback<RoomDto>() {

            @Override
            public void onResponse(Call<RoomDto> call, Response<RoomDto> resp) {

                // ha a fragment már nincs a képernyőn, nem csinálunk semmit
                if (!isAdded()) return;

                // gomb visszaállítása
                btnCreate.setEnabled(true);
                btnCreate.setText(oldText);

                // ha nem sikerült a kérés vagy nincs body -> hiba
                if (!resp.isSuccessful() || resp.body() == null) {
                    snack("Create failed (" + resp.code() + ")");
                    return;
                }

                // jelzés a Home felé, hogy frissítsen (pl. új szoba megjelenjen listában)
                if (NavHostFragment.findNavController(CreateRoomFragment.this)
                        .getPreviousBackStackEntry() != null) {

                    NavHostFragment.findNavController(CreateRoomFragment.this)
                            .getPreviousBackStackEntry()
                            .getSavedStateHandle()
                            .set("refresh_home", true);
                }

                // siker üzenet + visszalépés
                snack("Room created");
                NavHostFragment.findNavController(CreateRoomFragment.this).navigateUp();
            }

            @Override
            public void onFailure(Call<RoomDto> call, Throwable t) {

                // ha a fragment már nincs meg, ne mutassunk semmit
                if (!isAdded()) return;

                // gomb visszaállítása
                btnCreate.setEnabled(true);
                btnCreate.setText(oldText);

                // hálózati hiba üzenet
                snack("Network error: " + (t.getMessage() == null ? "unknown" : t.getMessage()));
            }
        });
    }

    // Segédfüggvény: EditTextből kiolvasás (null védelem + trim)
    private String val(EditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    // Rövid üzenet (Toast)
    private void toast(String s) {
        Toast.makeText(requireContext(), s, Toast.LENGTH_SHORT).show();
    }

    // Snackbar üzenet (alul felugró)
    private void snack(String s) {
        if (getView() != null) Snackbar.make(getView(), s, Snackbar.LENGTH_LONG).show();
    }

    // Billentyűzet elrejtése
    private void hideKeyboard() {
        try {
            View v = getView();
            if (v != null) {
                InputMethodManager imm = (InputMethodManager)
                        requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        } catch (Exception ignored) {}
    }

    // Só generálás (hex formában)
    // (itt most nincs ténylegesen használva a create-nél, de később jól jöhet)
    private String generateSaltHex(int byteCount) {
        try {
            byte[] bytes = new byte[byteCount];
            new SecureRandom().nextBytes(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            // ha valami gond van, akkor legalább valami legyen
            return Long.toHexString(System.currentTimeMillis());
        }
    }
}
