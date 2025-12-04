package com.szokolaipecsy.companystatus.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.szokolaipecsy.companystatus.R;
import com.szokolaipecsy.companystatus.data.DbProvider;
import com.szokolaipecsy.companystatus.data.MembershipEntity;
import com.szokolaipecsy.companystatus.data.UserProfile;
import com.szokolaipecsy.companystatus.data.UserProfileDao;
import com.szokolaipecsy.companystatus.network.RoomDto;
import com.szokolaipecsy.companystatus.util.Hashing;

public class RoomDetailFragment extends Fragment {

    private TextView tvName, tvDesc, emptyView, tvDateTime;
    private MaterialButton btnJoin, btnUpdateStatus;
    private ProgressBar progress;
    private RecyclerView rvMembers;
    private SwipeRefreshLayout swipe;
    private MemberAdapter memberAdapter;
    private RoomDetailViewModel vm;
    private UserProfileDao userDao;
    private final Handler autoRefreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (vm != null) {
                vm.refreshRoom();
                vm.refreshMembers();
            }
            autoRefreshHandler.postDelayed(this, 10_000);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_room_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);

        tvDateTime      = v.findViewById(R.id.tvDateTime);
        tvName          = v.findViewById(R.id.name);
        tvDesc          = v.findViewById(R.id.desc);
        btnJoin         = v.findViewById(R.id.btnJoin);
        btnUpdateStatus = v.findViewById(R.id.btnUpdateStatus);
        progress        = v.findViewById(R.id.progress);
        emptyView       = v.findViewById(R.id.emptyView);
        rvMembers       = v.findViewById(R.id.recyclerMembers);
        swipe           = v.findViewById(R.id.swipeRefresh);

        updateDateTime();

        rvMembers.setLayoutManager(new LinearLayoutManager(requireContext()));
        memberAdapter = new MemberAdapter(userId -> showMemberBottomSheet(userId));
        rvMembers.setAdapter(memberAdapter);

        vm = new ViewModelProvider(this).get(RoomDetailViewModel.class);
        userDao = DbProvider.get(requireContext()).userProfileDao();

        vm.myProfile.observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                String display;
                if (profile.displayName != null && !profile.displayName.isEmpty()) {
                    display = profile.displayName;
                } else if (profile.name != null && !profile.name.isEmpty()) {
                    display = profile.name;
                } else {
                    display = vm.myUserId;
                }
                memberAdapter.setMyProfile(vm.myUserId, display);
            } else {
                memberAdapter.setMyProfile(vm.myUserId, null);
            }
        });

        String roomId = RoomDetailFragmentArgs.fromBundle(requireArguments()).getRoomId();
        if (roomId == null) {
            Snackbar.make(v, "Missing roomId", Snackbar.LENGTH_LONG).show();
            NavHostFragment.findNavController(this).popBackStack();
            return;
        }
        vm.start(roomId);
        vm.refreshRoom();

        vm.room.observe(getViewLifecycleOwner(), dto -> {
            if (dto != null) {
                tvName.setText(dto.name != null ? dto.name : "—");
                tvDesc.setText(dto.description != null ? dto.description : "");
            }
        });

        vm.members.observe(getViewLifecycleOwner(), list -> {
            memberAdapter.submit(list);
            emptyView.setVisibility((list == null || list.isEmpty()) ? View.VISIBLE : View.GONE);
        });

        vm.loading.observe(getViewLifecycleOwner(), loading -> {
            boolean show = Boolean.TRUE.equals(loading);
            progress.setVisibility(show ? View.VISIBLE : View.GONE);
            swipe.setRefreshing(show);
        });

        vm.error.observe(getViewLifecycleOwner(), err -> {
            if (err != null && getView() != null) {
                Snackbar.make(getView(), err, Snackbar.LENGTH_LONG).show();
            }
        });

        vm.isJoined.observe(getViewLifecycleOwner(), joined -> {
            boolean j = Boolean.TRUE.equals(joined);
            btnJoin.setVisibility(j ? View.GONE : View.VISIBLE);
            btnUpdateStatus.setVisibility(j ? View.VISIBLE : View.GONE);
        });

        swipe.setOnRefreshListener(vm::refreshMembers);

        btnJoin.setOnClickListener(click -> {
            RoomDto r = vm.room.getValue();
            if (r == null) return;

            boolean hasPassword = r.passwordHash != null && !r.passwordHash.isEmpty();
            if (hasPassword) {
                showPasswordDialogAndJoin(r);
            } else {
                vm.joinRoom();
            }
        });

        btnUpdateStatus.setOnClickListener(click -> showStatusBottomSheet());
    }

    @Override
    public void onResume() {
        super.onResume();
        updateDateTime();
        if (vm != null) {
            vm.updateLastActiveIfJoined();
            vm.refreshMembers();
        }
        autoRefreshHandler.postDelayed(autoRefreshRunnable, 10_000);
    }

    @Override
    public void onPause() {
        super.onPause();
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
    }

    private void showPasswordDialogAndJoin(RoomDto r) {
        final EditText et = new EditText(requireContext());
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.enter_password)
                .setView(et)
                .setPositiveButton(R.string.join, (d, w) -> {
                    String typed = et.getText() != null ? et.getText().toString().trim() : "";
                    String expected = r.passwordHash != null ? r.passwordHash.trim() : "";

                    if (!expected.isEmpty() && typed.equals(expected)) {
                        vm.joinRoom();
                    } else if (getView() != null) {
                        Snackbar.make(getView(), R.string.wrong_password, Snackbar.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, (d, w) -> {})
                .show();
    }

    private void showStatusBottomSheet() {
        if (vm == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheet = getLayoutInflater().inflate(R.layout.bottomsheet_status, null, false);
        dialog.setContentView(sheet);

        RadioGroup rgAvail   = sheet.findViewById(R.id.rgAvailability);
        RadioButton rbBusy   = sheet.findViewById(R.id.rbBusy);
        RadioButton rbAway   = sheet.findViewById(R.id.rbAway);
        RadioButton rbAvail  = sheet.findViewById(R.id.rbAvailable);
        EditText etStatus    = sheet.findViewById(R.id.etStatusText);

        if (rbAvail != null) rbAvail.setChecked(true);

        sheet.findViewById(R.id.btnCancelStatus).setOnClickListener(v -> dialog.dismiss());

        sheet.findViewById(R.id.btnSaveStatus).setOnClickListener(v -> {
            String availability;
            int checkedId = rgAvail.getCheckedRadioButtonId();
            if (checkedId == R.id.rbBusy) {
                availability = "BUSY";
            } else if (checkedId == R.id.rbAway) {
                availability = "AWAY";
            } else {
                availability = "AVAILABLE";
            }

            String statusText = etStatus.getText() != null
                    ? etStatus.getText().toString().trim()
                    : "";

            vm.updateMyStatus(availability, statusText);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showMemberBottomSheet(String userId) {

        MembershipEntity member = null;
        if (vm != null && vm.members.getValue() != null) {
            for (MembershipEntity m : vm.members.getValue()) {
                if (userId != null && userId.equals(m.userId)) {
                    member = m;
                    break;
                }
            }
        }

        View sheet = getLayoutInflater().inflate(R.layout.bottomsheet_member, null, false);

        TextView tvUserName        = sheet.findViewById(R.id.tvUserName);
        TextView tvStatusText      = sheet.findViewById(R.id.tvStatusText);
        TextView tvLastActive      = sheet.findViewById(R.id.tvLastActive);
        TextView tvAvailabilityDot = sheet.findViewById(R.id.tvAvailabilityDot);
        TextView tvPersonalPhone   = sheet.findViewById(R.id.tvPersonalPhone);
        TextView tvCompanyPhone    = sheet.findViewById(R.id.tvCompanyPhone);
        TextView tvInternalEmail   = sheet.findViewById(R.id.tvInternalEmail);

        String displayName;
        if (member != null && member.userName != null && !member.userName.isEmpty()) {
            displayName = member.userName;
        } else if (userId != null && !userId.isEmpty()) {
            displayName = userId;
        } else {
            displayName = "—";
        }

        String myId = com.szokolaipecsy.companystatus.auth.AuthManager
                .getInstance(requireContext())
                .getUserId();

        if (member != null && myId != null && myId.equals(member.userId)) {
            tvUserName.setText(displayName + " (You)");
        } else {
            tvUserName.setText(displayName);
        }

        String status = (member != null && member.statusText != null && !member.statusText.isEmpty())
                ? member.statusText
                : "—";
        tvStatusText.setText(status);

        String last = (member != null && member.lastActiveAt != null && !member.lastActiveAt.isEmpty())
                ? member.lastActiveAt
                : null;
        tvLastActive.setText(formatLastActive(last));

        String availability = (member != null && member.availability != null)
                ? member.availability.toUpperCase()
                : "";
        int color;
        switch (availability) {
            case "AVAILABLE":
                color = Color.parseColor("#4CAF50");
                break;
            case "BUSY":
                color = Color.parseColor("#F44336");
                break;
            case "AWAY":
                color = Color.parseColor("#FFC107");
                break;
            default:
                color = Color.parseColor("#9E9E9E");
                break;
        }
        tvAvailabilityDot.setText("●");
        tvAvailabilityDot.setTextColor(color);

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(sheet);

        if (userDao != null && userId != null) {
            userDao.observeByUserId(userId).observe(
                    getViewLifecycleOwner(),
                    profile -> fillProfileFields(profile, tvPersonalPhone, tvCompanyPhone, tvInternalEmail)
            );
        }

        sheet.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());

        sheet.findViewById(R.id.btnViewProfile).setOnClickListener(v -> {
            Snackbar.make(sheet, "This is a read-only profile preview", Snackbar.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void fillProfileFields(@Nullable UserProfile profile,
                                   @NonNull TextView tvPersonalPhone,
                                   @NonNull TextView tvCompanyPhone,
                                   @NonNull TextView tvInternalEmail) {

        if (profile == null) {
            tvPersonalPhone.setText("Personal phone: —");
            tvCompanyPhone.setText("Company phone: —");
            tvInternalEmail.setText("Internal email: —");
            return;
        }

        String personal = (profile.personalPhone != null && !profile.personalPhone.isEmpty())
                ? profile.personalPhone : "—";
        String company = (profile.companyPhone != null && !profile.companyPhone.isEmpty())
                ? profile.companyPhone : "—";
        String internal = (profile.internalEmail != null && !profile.internalEmail.isEmpty())
                ? profile.internalEmail : "—";

        tvPersonalPhone.setText("Personal phone: " + personal);
        tvCompanyPhone.setText("Company phone: " + company);
        tvInternalEmail.setText("Internal email: " + internal);
    }

    private void updateDateTime() {
        if (tvDateTime == null) return;

        java.time.ZoneId zone = java.time.ZoneId.of("Europe/Budapest");
        java.time.LocalDateTime now = java.time.LocalDateTime.now(zone);
        java.time.format.DateTimeFormatter fmt =
                java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd • HH:mm");

        tvDateTime.setText(now.format(fmt));
    }
    private String formatLastActive(@Nullable String isoString) {
        if (isoString == null || isoString.isEmpty()) {
            return "Last active: —";
        }
        try {
            java.time.Instant instant = java.time.Instant.parse(isoString);
            java.time.Instant now = java.time.Instant.now();
            java.time.Duration diff = java.time.Duration.between(instant, now);

            long seconds = diff.getSeconds();
            if (seconds < 60) {
                return "Last active: just now";
            } else if (seconds < 3600) {
                long mins = seconds / 60;
                return "Last active: " + mins + " min ago";
            } else {
                java.time.ZoneId zone = java.time.ZoneId.of("Europe/Budapest");
                java.time.LocalDateTime dt = java.time.LocalDateTime.ofInstant(instant, zone);
                java.time.format.DateTimeFormatter fmt =
                        java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd • HH:mm");
                return "Last active: " + dt.format(fmt);
            }
        } catch (Exception e) {
            return "Last active: —";
        }
    }

}
