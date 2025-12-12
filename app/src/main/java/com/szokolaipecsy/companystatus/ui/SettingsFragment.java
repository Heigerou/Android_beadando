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

// SettingsFragment
// Itt vannak a beállítások:
// - Dark mode (mentés SharedPreferences-be)
// - Display name módosítás (Room DB + memberships frissítés)
// - Default status beállítás (amit Join-nál használunk)
// - Logout (Auth törlés + helyi DB tisztítás)
public class SettingsFragment extends Fragment {

    // app_prefs -> minden beállítás itt van
    private SharedPreferences prefs;

    // profil tábla DAO
    private UserProfileDao profileDao;

    // belépett user id
    private String myUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // layout betöltés
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        // SharedPreferences + DAO + userId beállítás
        prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        profileDao = DbProvider.get(requireContext()).userProfileDao();
        myUserId = AuthManager.getInstance(requireContext()).getUserId();

        // Dark theme kapcsoló
        SwitchMaterial swDark = v.findViewById(R.id.switch_dark_theme);

        // Elmentett érték betöltése
        boolean dark = prefs.getBoolean("pref_dark_theme", false);
        swDark.setChecked(dark);

        // Ha átkapcsoljuk:
        // - prefs-be mentjük
        // - AppCompatDelegate meg átállítja az egész app témáját
        swDark.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("pref_dark_theme", isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });

        // Display name módosítás gomb
        MaterialButton btnChangeName = v.findViewById(R.id.btnChangeName);
        btnChangeName.setOnClickListener(click -> showChangeNameDialog());

        // Default status módosítás gomb
        // Ez Join-nál számít, mert belépéskor ezt állítja be alapból (Online/Busy/Away)
        MaterialButton btnChangeStatus = v.findViewById(R.id.btnChangeDefaultStatus);
        btnChangeStatus.setOnClickListener(click -> showChangeDefaultStatus());

        // Logout
        v.findViewById(R.id.btnLogout).setOnClickListener(click -> {

            // Auth adat törlése (SharedPreferences auth_prefs)
            AuthManager.getInstance(requireContext()).logout();

            // Helyi Room DB táblák ürítése (pl. members, favorite, profile)
            AppDatabase.EXECUTOR.execute(() ->
                    AppDatabase.getInstance(requireContext()).clearAllTables()
            );

            // visszajelzés + visszanavigálás AuthGate-re
            Snackbar.make(v, R.string.logged_out, Snackbar.LENGTH_SHORT).show();
            NavHostFragment.findNavController(this).navigate(R.id.authGateFragment);
        });
    }

    // Display name módosítás dialógus
    private void showChangeNameDialog() {

        // DB műveletek háttérszálon
        AppDatabase.EXECUTOR.execute(() -> {

            AuthManager auth = AuthManager.getInstance(requireContext());

            // profil lekérése DB-ből
            UserProfile p = profileDao.getSync(myUserId);

            // "full name" (ez a regisztrált név)
            // ha DB-ben van -> azt használjuk, különben Authból
            String currentFullName =
                    (p != null && p.name != null && !p.name.isEmpty())
                            ? p.name
                            : auth.getUserName();

            // "display name" (ez a becenév / megjelenítendő név)
            // ha van, akkor ezt mutatjuk, különben full name
            String currentDisplay =
                    (p != null && p.displayName != null && !p.displayName.isEmpty())
                            ? p.displayName
                            : currentFullName;

            // UI művelet csak UI szálon!
            requireActivity().runOnUiThread(() -> {

                // EditText a dialogban
                final EditText et = new EditText(requireContext());
                et.setText(currentDisplay);

                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Change display name")
                        .setView(et)

                        // Save gomb
                        .setPositiveButton("Save", (d, w) -> {

                            String newDisplay = et.getText().toString().trim();

                            // üres név tiltás
                            if (newDisplay.isEmpty()) {
                                Snackbar.make(requireView(), "Name cannot be empty", Snackbar.LENGTH_LONG).show();
                                return;
                            }

                            // mentés háttérszálon
                            AppDatabase.EXECUTOR.execute(() -> {

                                // legfrissebb profil kiolvasás (hogy ne veszítsük el a többi mezőt)
                                UserProfile latest = profileDao.getSync(myUserId);

                                AuthManager a = AuthManager.getInstance(requireContext());

                                // fullName + email: DB-ből ha van, különben Authból
                                String fullName =
                                        (latest != null && latest.name != null && !latest.name.isEmpty())
                                                ? latest.name
                                                : a.getUserName();

                                String email =
                                        (latest != null && latest.email != null && !latest.email.isEmpty())
                                                ? latest.email
                                                : a.getEmail();

                                // új UserProfile objektum összerakása
                                // telefonok + belső email átörökítése (ha van)
                                UserProfile updated = new UserProfile(
                                        myUserId,
                                        fullName,
                                        email,
                                        latest != null ? latest.personalPhone : null,
                                        latest != null ? latest.companyPhone : null,
                                        latest != null ? latest.internalEmail : null
                                );

                                // displayName beállítása
                                updated.displayName = newDisplay;

                                // profil mentése a Room DB-be
                                profileDao.upsert(updated);

                                // Helyben a memberships táblában is frissítjük a nevet,
                                // hogy a taglistában rögtön átíródjon
                                DbProvider.get(requireContext())
                                        .membershipDao()
                                        .updateNameForUser(myUserId, newDisplay);

                                // API oldalon is frissítjük minden membership rekordban
                                // (ha több szobában is benne vagyok)
                                MembershipsRepository repo =
                                        new MembershipsRepository(requireContext());
                                repo.updateUserNameEverywhere(myUserId, newDisplay);

                                // UI visszajelzés
                                requireActivity().runOnUiThread(() ->
                                        Snackbar.make(requireView(),
                                                "Display name updated",
                                                Snackbar.LENGTH_LONG
                                        ).show()
                                );
                            });
                        })

                        // Cancel gomb
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        });
    }

    // Default status beállítás
    private void showChangeDefaultStatus() {

        // amit a user lát
        final String[] opts = {"Available", "Busy", "Away"};

        // belső kulcsok (joinRoom-ban is ezeket használjuk)
        final String[] keys = {"AVAILABLE", "BUSY", "AWAY"};

        // jelenlegi kiválasztott index prefs-ből
        int selected = prefs.getInt("pref_default_status", 0);

        // Egyszerű single choice dialog
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Default status")
                .setSingleChoiceItems(opts, selected, (dialog, which) -> {

                    // eltároljuk az indexet (0/1/2)
                    prefs.edit().putInt("pref_default_status", which).apply();

                    Snackbar.make(requireView(), "Default status updated", Snackbar.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();

        // védett oldal: ha nincs login, vissza AuthGate-re
        if (!AuthManager.getInstance(requireContext()).isLoggedIn()) {
            NavHostFragment.findNavController(this).navigate(R.id.authGateFragment);
        }
    }
}
