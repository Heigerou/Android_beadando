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

public class ProfileFragment extends Fragment {

    public ProfileFragment() { super(R.layout.fragment_profile); }

    private EditText etName, etEmail, etPersonal, etCompany, etInternal;
    private Button btnSave;

    private UserProfileDao dao;

    private String userId;
    private String nameFromAuth;
    private String emailFromAuth;
    private boolean readOnlyOtherUser = false;

    private String currentDisplayName = null;

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);

        etName     = v.findViewById(R.id.etName);
        etEmail    = v.findViewById(R.id.etEmail);
        etPersonal = v.findViewById(R.id.etPersonalPhone);
        etCompany  = v.findViewById(R.id.etCompanyPhone);
        etInternal = v.findViewById(R.id.etInternalEmail);
        btnSave    = v.findViewById(R.id.btnSaveProfile);

        AuthManager am = AuthManager.getInstance(requireContext());
        String selfId = am.getUserId();

        Bundle args = getArguments();
        String argUserId   = (args != null) ? args.getString("userId", null) : null;
        boolean argReadOnly = (args != null) && args.getBoolean("readOnly", false);

        if (argUserId != null && !argUserId.isEmpty() && !argUserId.equals(selfId)) {
            userId = argUserId;
            readOnlyOtherUser = argReadOnly;
            nameFromAuth = null;
            emailFromAuth = null;

            etName.setEnabled(false);
            etEmail.setEnabled(false);
            etPersonal.setEnabled(false);
            etCompany.setEnabled(false);
            etInternal.setEnabled(false);
            btnSave.setVisibility(View.GONE);

        } else {
            userId        = selfId;
            nameFromAuth  = am.getUserName();
            emailFromAuth = am.getEmail();

            etName.setEnabled(false);
            etEmail.setEnabled(false);
            etPersonal.setEnabled(true);
            etCompany.setEnabled(true);
            etInternal.setEnabled(true);
            btnSave.setVisibility(View.VISIBLE);
        }

        if (!TextUtils.isEmpty(nameFromAuth))  etName.setText(nameFromAuth);
        if (!TextUtils.isEmpty(emailFromAuth)) etEmail.setText(emailFromAuth);

        AppDatabase db = DbProvider.get(requireContext());
        dao = db.userProfileDao();

        if (userId != null) {
            dao.observeByUserId(userId).observe(getViewLifecycleOwner(), profile -> {
                if (profile != null) {

                    currentDisplayName = profile.name;

                    if (TextUtils.isEmpty(nameFromAuth) && profile.name != null) {
                        etName.setText(profile.name);
                    }
                    if (TextUtils.isEmpty(emailFromAuth) && profile.email != null) {
                        etEmail.setText(profile.email);
                    }

                    etPersonal.setText(profile.personalPhone == null ? "" : profile.personalPhone);
                    etCompany.setText(profile.companyPhone == null ? "" : profile.companyPhone);
                    etInternal.setText(profile.internalEmail == null ? "" : profile.internalEmail);
                }
            });
        }

        btnSave.setOnClickListener(click -> {
            if (readOnlyOtherUser) {
                Toast.makeText(requireContext(), "You cannot edit this profile", Toast.LENGTH_LONG).show();
                return;
            }

            if (userId == null) {
                Toast.makeText(requireContext(), "You are not logged in", Toast.LENGTH_LONG).show();
                return;
            }

            String personal = etPersonal.getText().toString().trim();
            String company  = etCompany.getText().toString().trim();
            String internal = etInternal.getText().toString().trim();

            String name  = TextUtils.isEmpty(nameFromAuth)  ? "" : nameFromAuth;   // FULL NAME
            String email = TextUtils.isEmpty(emailFromAuth) ? "" : emailFromAuth;

            UserProfile profile = new UserProfile(
                    userId,
                    name,
                    email,
                    personal,
                    company,
                    internal
            );

            Executors.newSingleThreadExecutor().execute(() -> {
                UserProfile latest = dao.getSync(userId);
                if (latest != null && latest.displayName != null) {
                    profile.displayName = latest.displayName;
                }

                dao.upsert(profile);
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Profile saved", Toast.LENGTH_SHORT).show()
                );
            });
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!AuthManager.getInstance(requireContext()).isLoggedIn()) {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.authGateFragment);
        }
    }
}
