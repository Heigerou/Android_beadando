package com.szokolaipecsy.companystatus.ui.auth;

import android.os.Handler;
import android.os.Looper;

import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.szokolaipecsy.companystatus.R;
import com.szokolaipecsy.companystatus.auth.AuthManager;

// Ez a Fragment egy "kapu" az app elején
// Itt döntjük el, hogy loginra vagy főoldalra megyünk
public class AuthGateFragment extends Fragment {

    // Ehhez a fragmenthez tartozó layout
    public AuthGateFragment() {
        super(R.layout.fragment_auth_gate);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Lekérjük az AuthManager-t
        // Innen tudjuk, hogy be van-e jelentkezve a user
        AuthManager am = AuthManager.getInstance(requireContext());

        // Main (UI) szálra rakjuk a navigációt
        // Így biztos nem lesz lifecycle vagy navigációs hiba
        new Handler(Looper.getMainLooper()).post(() -> {

            // Ha a user be van jelentkezve ÉS be van pipálva az automatikus belépés
            if (am.shouldAutoLogin() && am.isLoggedIn()) {

                // Akkor megyünk a főoldalra (Home)
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_authGate_to_home);

            } else {

                // Ha nincs belépve, vagy nincs automatikus belépés
                // Akkor a login képernyőre megyünk
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_authGate_to_login);
            }
        });
    }
}
