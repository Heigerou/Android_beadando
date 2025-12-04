package com.szokolaipecsy.companystatus.ui.auth;

import android.os.Handler;
import android.os.Looper;

import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.szokolaipecsy.companystatus.R;
import com.szokolaipecsy.companystatus.auth.AuthManager;

public class AuthGateFragment extends Fragment {

    public AuthGateFragment() {
        super(R.layout.fragment_auth_gate);
    }

    @Override
    public void onResume() {
        super.onResume();

        AuthManager am = AuthManager.getInstance(requireContext());

        new Handler(Looper.getMainLooper()).post(() -> {
            if (am.shouldAutoLogin() && am.isLoggedIn()) {
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_authGate_to_home);
            } else {
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_authGate_to_login);
            }
        });
    }
}
