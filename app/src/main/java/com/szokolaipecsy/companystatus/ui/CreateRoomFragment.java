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
import com.szokolaipecsy.companystatus.util.Hashing;

import java.security.SecureRandom;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateRoomFragment extends Fragment {

    private EditText etName, etSubtitle, etDescription, etPassword;
    private Button btnCreate;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_room, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);

        etName        = v.findViewById(R.id.et_name);
        etSubtitle    = v.findViewById(R.id.et_subtitle);
        etDescription = v.findViewById(R.id.et_description);
        etPassword    = v.findViewById(R.id.et_password);
        btnCreate     = v.findViewById(R.id.btn_create);

        btnCreate.setOnClickListener(click -> doCreate());
    }

    private void doCreate() {
        hideKeyboard();

        String name = val(etName);
        String sub  = val(etSubtitle);
        String desc = val(etDescription);
        String pass = val(etPassword);

        if (name.isEmpty()) {
            etName.setError("Name is required");
            etName.requestFocus();
            toast("Name required");
            return;
        }

        boolean isProtected = !pass.isEmpty();
        String passwordSalt = "";
        String passwordHash = "";

        if (isProtected) {
            passwordHash = pass;
        }

        RoomCreateRequest body = new RoomCreateRequest(
                name,
                sub,
                desc,
                isProtected,
                passwordSalt,
                passwordHash
        );

        btnCreate.setEnabled(false);
        CharSequence oldText = btnCreate.getText();
        btnCreate.setText("Creating...");

        ApiClient.rooms().createRoom(body).enqueue(new Callback<RoomDto>() {
            @Override
            public void onResponse(Call<RoomDto> call, Response<RoomDto> resp) {
                if (!isAdded()) return;

                btnCreate.setEnabled(true);
                btnCreate.setText(oldText);

                if (!resp.isSuccessful() || resp.body() == null) {
                    snack("Create failed (" + resp.code() + ")");
                    return;
                }

                if (NavHostFragment.findNavController(CreateRoomFragment.this)
                        .getPreviousBackStackEntry() != null) {
                    NavHostFragment.findNavController(CreateRoomFragment.this)
                            .getPreviousBackStackEntry()
                            .getSavedStateHandle()
                            .set("refresh_home", true);
                }

                snack("Room created");
                NavHostFragment.findNavController(CreateRoomFragment.this).navigateUp();
            }

            @Override
            public void onFailure(Call<RoomDto> call, Throwable t) {
                if (!isAdded()) return;
                btnCreate.setEnabled(true);
                btnCreate.setText(oldText);
                snack("Network error: " + (t.getMessage() == null ? "unknown" : t.getMessage()));
            }
        });
    }

    private String val(EditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    private void toast(String s) {
        Toast.makeText(requireContext(), s, Toast.LENGTH_SHORT).show();
    }

    private void snack(String s) {
        if (getView() != null) Snackbar.make(getView(), s, Snackbar.LENGTH_LONG).show();
    }

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
            return Long.toHexString(System.currentTimeMillis());
        }
    }
}
