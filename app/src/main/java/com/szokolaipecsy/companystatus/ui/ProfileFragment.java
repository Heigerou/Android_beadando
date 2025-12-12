package com.szokolaipecsy.companystatus.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.szokolaipecsy.companystatus.R;
import com.szokolaipecsy.companystatus.auth.AuthManager;
import com.szokolaipecsy.companystatus.data.AppDatabase;
import com.szokolaipecsy.companystatus.data.DbProvider;
import com.szokolaipecsy.companystatus.data.UserProfile;
import com.szokolaipecsy.companystatus.data.UserProfileDao;

import java.util.concurrent.Executors;

// Ez a ProfileFragment
// Profil adatok megjelenítése + (saját profilon) mentés a Room adatbázisba
// Más user profilja esetén read-only módban is működik
public class ProfileFragment extends Fragment {

    // Layout hozzárendelése
    public ProfileFragment() { super(R.layout.fragment_profile); }

    // Beviteli mezők + mentés gomb
    private EditText etName, etEmail, etPersonal, etCompany, etInternal;
    private Button btnSave;

    // DAO a user_profile táblához
    private UserProfileDao dao;

    // Ki van megnyitva (melyik user profilja)
    private String userId;

    // Authból jövő alap adatok (név, email)
    private String nameFromAuth;
    private String emailFromAuth;

    // Ha másik user profilját nézzük, akkor lehet read-only
    private boolean readOnlyOtherUser = false;

    // Ezt eltároljuk, hogy ha van displayName később, ne vesszen el
    private String currentDisplayName = null;

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);

        // UI elemek összekötése
        etName     = v.findViewById(R.id.etName);
        etEmail    = v.findViewById(R.id.etEmail);
        etPersonal = v.findViewById(R.id.etPersonalPhone);
        etCompany  = v.findViewById(R.id.etCompanyPhone);
        etInternal = v.findViewById(R.id.etInternalEmail);
        btnSave    = v.findViewById(R.id.btnSaveProfile);

        // AuthManagerből lekérjük, ki a belépett user
        AuthManager am = AuthManager.getInstance(requireContext());
        String selfId = am.getUserId();

        // Argumentek:
        // - userId: ha másik user profilját akarjuk megnyitni
        // - readOnly: tiltjuk-e a szerkesztést
        Bundle args = getArguments();
        String argUserId   = (args != null) ? args.getString("userId", null) : null;
        boolean argReadOnly = (args != null) && args.getBoolean("readOnly", false);

        // Ha kaptunk userId-t és az nem én vagyok, akkor másik user profilját nézzük
        if (argUserId != null && !argUserId.isEmpty() && !argUserId.equals(selfId)) {

            userId = argUserId;
            readOnlyOtherUser = argReadOnly;

            // más user esetén nem Authból töltünk nevet/emailt
            nameFromAuth = null;
            emailFromAuth = null;

            // read-only: mindent letiltunk + mentés gomb elrejtése
            etName.setEnabled(false);
            etEmail.setEnabled(false);
            etPersonal.setEnabled(false);
            etCompany.setEnabled(false);
            etInternal.setEnabled(false);
            btnSave.setVisibility(View.GONE);

        } else {
            // Saját profil
            userId        = selfId;
            nameFromAuth  = am.getUserName();
            emailFromAuth = am.getEmail();

            // Név + email itt fix (Authból jön), ezért ezeket tiltjuk
            etName.setEnabled(false);
            etEmail.setEnabled(false);

            // Ezeket engedjük szerkeszteni
            etPersonal.setEnabled(true);
            etCompany.setEnabled(true);
            etInternal.setEnabled(true);

            // mentés gomb látszik
            btnSave.setVisibility(View.VISIBLE);
        }

        // Authból jövő értékek beírása a mezőkbe (ha van)
        if (!TextUtils.isEmpty(nameFromAuth))  etName.setText(nameFromAuth);
        if (!TextUtils.isEmpty(emailFromAuth)) etEmail.setText(emailFromAuth);

        // DB + DAO lekérése
        AppDatabase db = DbProvider.get(requireContext());
        dao = db.userProfileDao();

        // Ha van userId, akkor figyeljük a profilját LiveData-val
        // Így ha a DB-ben változik, frissül a képernyő is
        if (userId != null) {
            dao.observeByUserId(userId).observe(getViewLifecycleOwner(), profile -> {
                if (profile != null) {

                    // eltesszük a jelenlegi nevet (itt most a profile.name kerül bele)
                    currentDisplayName = profile.name;

                    // Ha Authból nincs név/email, akkor DB-ből töltjük be
                    if (TextUtils.isEmpty(nameFromAuth) && profile.name != null) {
                        etName.setText(profile.name);
                    }
                    if (TextUtils.isEmpty(emailFromAuth) && profile.email != null) {
                        etEmail.setText(profile.email);
                    }

                    // A többi mező DB-ből jön
                    etPersonal.setText(profile.personalPhone == null ? "" : profile.personalPhone);
                    etCompany.setText(profile.companyPhone == null ? "" : profile.companyPhone);
                    etInternal.setText(profile.internalEmail == null ? "" : profile.internalEmail);
                }
            });
        }

        // Mentés gomb
        btnSave.setOnClickListener(click -> {

            // Ha read-only profilt nézünk, akkor nem engedjük a mentést
            if (readOnlyOtherUser) {
                Toast.makeText(requireContext(), "You cannot edit this profile", Toast.LENGTH_LONG).show();
                return;
            }

            // Ha nincs userId, akkor nincs belépve
            if (userId == null) {
                Toast.makeText(requireContext(), "You are not logged in", Toast.LENGTH_LONG).show();
                return;
            }

            // Mezők kiolvasása
            String personal = etPersonal.getText().toString().trim();
            String company  = etCompany.getText().toString().trim();
            String internal = etInternal.getText().toString().trim();

            // Név + email itt Authból jön (ha nincs, akkor üres)
            String name  = TextUtils.isEmpty(nameFromAuth)  ? "" : nameFromAuth;   // FULL NAME
            String email = TextUtils.isEmpty(emailFromAuth) ? "" : emailFromAuth;

            // UserProfile objektum összerakása
            UserProfile profile = new UserProfile(
                    userId,
                    name,
                    email,
                    personal,
                    company,
                    internal
            );

            // DB műveletet háttérszálon csináljuk (ne fagyjon a UI)
            Executors.newSingleThreadExecutor().execute(() -> {

                // lekérjük a legfrissebb profilt, és ha volt displayName, ne veszítsük el
                UserProfile latest = dao.getSync(userId);
                if (latest != null && latest.displayName != null) {
                    profile.displayName = latest.displayName;
                }

                // mentés / felülírás
                dao.upsert(profile);

                // UI szálon visszajelzés
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Profile saved", Toast.LENGTH_SHORT).show()
                );
            });
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        // ha nincs belépve, visszadob AuthGate-re
        if (!AuthManager.getInstance(requireContext()).isLoggedIn()) {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.authGateFragment);
        }
    }
}
