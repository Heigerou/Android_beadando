package com.szokolaipecsy.companystatus.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.button.MaterialButton;
import com.szokolaipecsy.companystatus.R;
import com.szokolaipecsy.companystatus.auth.AuthManager;
import com.szokolaipecsy.companystatus.data.AppDatabase;
import com.szokolaipecsy.companystatus.data.DbProvider;
import com.szokolaipecsy.companystatus.data.UserProfile;
import com.szokolaipecsy.companystatus.data.UserProfileDao;
import com.szokolaipecsy.companystatus.data.MembershipsRepository;

public class SettingsFragment extends Fragment {

    private SharedPreferences prefs;
    private UserProfileDao profileDao;
    private String myUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        profileDao = DbProvider.get(requireContext()).userProfileDao();
        myUserId = AuthManager.getInstance(requireContext()).getUserId();

        SwitchMaterial swDark = v.findViewById(R.id.switch_dark_theme);
        boolean dark = prefs.getBoolean("pref_dark_theme", false);
        swDark.setChecked(dark);

        swDark.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("pref_dark_theme", isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });

        MaterialButton btnChangeName = v.findViewById(R.id.btnChangeName);
        btnChangeName.setOnClickListener(click -> showChangeNameDialog());

        MaterialButton btnChangeStatus = v.findViewById(R.id.btnChangeDefaultStatus);
        btnChangeStatus.setOnClickListener(click -> showChangeDefaultStatus());

        v.findViewById(R.id.btnLogout).setOnClickListener(click -> {

            AuthManager.getInstance(requireContext()).logout();

            AppDatabase.EXECUTOR.execute(() ->
                    AppDatabase.getInstance(requireContext()).clearAllTables()
            );

            Snackbar.make(v, R.string.logged_out, Snackbar.LENGTH_SHORT).show();
            NavHostFragment.findNavController(this).navigate(R.id.authGateFragment);
        });
    }

    private void showChangeNameDialog() {
        AppDatabase.EXECUTOR.execute(() -> {
            AuthManager auth = AuthManager.getInstance(requireContext());
            UserProfile p = profileDao.getSync(myUserId);

            String currentFullName =
                    (p != null && p.name != null && !p.name.isEmpty())
                            ? p.name
                            : auth.getUserName();

            String currentDisplay =
                    (p != null && p.displayName != null && !p.displayName.isEmpty())
                            ? p.displayName
                            : currentFullName;

            requireActivity().runOnUiThread(() -> {
                final EditText et = new EditText(requireContext());
                et.setText(currentDisplay);

                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Change display name")
                        .setView(et)
                        .setPositiveButton("Save", (d, w) -> {
                            String newDisplay = et.getText().toString().trim();
                            if (newDisplay.isEmpty()) {
                                Snackbar.make(requireView(), "Name cannot be empty", Snackbar.LENGTH_LONG).show();
                                return;
                            }

                            AppDatabase.EXECUTOR.execute(() -> {
                                UserProfile latest = profileDao.getSync(myUserId);
                                AuthManager a = AuthManager.getInstance(requireContext());

                                String fullName =
                                        (latest != null && latest.name != null && !latest.name.isEmpty())
                                                ? latest.name
                                                : a.getUserName();

                                String email =
                                        (latest != null && latest.email != null && !latest.email.isEmpty())
                                                ? latest.email
                                                : a.getEmail();

                                UserProfile updated = new UserProfile(
                                        myUserId,
                                        fullName,
                                        email,
                                        latest != null ? latest.personalPhone : null,
                                        latest != null ? latest.companyPhone : null,
                                        latest != null ? latest.internalEmail : null
                                );
                                updated.displayName = newDisplay;

                                profileDao.upsert(updated);

                                DbProvider.get(requireContext())
                                        .membershipDao()
                                        .updateNameForUser(myUserId, newDisplay);

                                MembershipsRepository repo =
                                        new MembershipsRepository(requireContext());
                                repo.updateUserNameEverywhere(myUserId, newDisplay);

                                requireActivity().runOnUiThread(() ->
                                        Snackbar.make(requireView(),
                                                "Display name updated",
                                                Snackbar.LENGTH_LONG
                                        ).show()
                                );
                            });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        });
    }

    private void showChangeDefaultStatus() {
        final String[] opts = {"Available", "Busy", "Away"};
        final String[] keys = {"AVAILABLE", "BUSY", "AWAY"};

        int selected = prefs.getInt("pref_default_status", 0);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Default status")
                .setSingleChoiceItems(opts, selected, (dialog, which) -> {
                    prefs.edit().putInt("pref_default_status", which).apply();
                    Snackbar.make(requireView(), "Default status updated", Snackbar.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!AuthManager.getInstance(requireContext()).isLoggedIn()) {
            NavHostFragment.findNavController(this).navigate(R.id.authGateFragment);
        }
    }
}
