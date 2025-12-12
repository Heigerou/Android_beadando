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

// Ez a RoomDetailFragment
// Itt látjuk egy szoba részleteit + a tagokat (members)
// Innen lehet belépni a szobába, státuszt frissíteni, és tagokra kattintva részleteket nézni
public class RoomDetailFragment extends Fragment {

    // Szoba név, leírás, üres nézet, dátum-idő kijelzés
    private TextView tvName, tvDesc, emptyView, tvDateTime;

    // Join gomb + státusz frissítés gomb
    private MaterialButton btnJoin, btnUpdateStatus;

    // betöltés jelző
    private ProgressBar progress;

    // Tagok listája
    private RecyclerView rvMembers;

    // lehúzós frissítés
    private SwipeRefreshLayout swipe;

    // Tag adapter
    private MemberAdapter memberAdapter;

    // ViewModel (szoba + members + join + státusz logika innen jön)
    private RoomDetailViewModel vm;

    // Profil adatok (Room DB) - tagokra kattintva innen tudunk adatot megjeleníteni
    private UserProfileDao userDao;

    // Automatikus frissítés (10 mp-enként)
    private final Handler autoRefreshHandler = new Handler(Looper.getMainLooper());

    // Ez fut le mindig 10 mp-enként
    private final Runnable autoRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            // Ha van vm, akkor újratöltjük a szobát és a tagokat
            if (vm != null) {
                vm.refreshRoom();
                vm.refreshMembers();
            }
            // újra beütemezzük 10 másodpercre
            autoRefreshHandler.postDelayed(this, 10_000);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Layout betöltése
        return inflater.inflate(R.layout.fragment_room_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);

        // UI elemek összekötése
        tvDateTime      = v.findViewById(R.id.tvDateTime);
        tvName          = v.findViewById(R.id.name);
        tvDesc          = v.findViewById(R.id.desc);
        btnJoin         = v.findViewById(R.id.btnJoin);
        btnUpdateStatus = v.findViewById(R.id.btnUpdateStatus);
        progress        = v.findViewById(R.id.progress);
        emptyView       = v.findViewById(R.id.emptyView);
        rvMembers       = v.findViewById(R.id.recyclerMembers);
        swipe           = v.findViewById(R.id.swipeRefresh);

        // dátum-idő kiírás (Budapest időzóna)
        updateDateTime();

        // RecyclerView beállítás tagokhoz
        rvMembers.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Adapter: ha tagra kattintunk, megnyitjuk a bottom sheetet
        memberAdapter = new MemberAdapter(userId -> showMemberBottomSheet(userId));
        rvMembers.setAdapter(memberAdapter);

        // ViewModel és DAO
        vm = new ViewModelProvider(this).get(RoomDetailViewModel.class);
        userDao = DbProvider.get(requireContext()).userProfileDao();

        // Saját profil figyelése:
        // ha van displayName -> azt írjuk ki
        // ha nincs -> name, ha az sincs -> userId
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
                // átadjuk az adapternek, hogy (You) és a név jó legyen
                memberAdapter.setMyProfile(vm.myUserId, display);
            } else {
                memberAdapter.setMyProfile(vm.myUserId, null);
            }
        });

        // A roomId a navigációból jön (SafeArgs)
        String roomId = RoomDetailFragmentArgs.fromBundle(requireArguments()).getRoomId();

        // Ha nincs roomId, akkor nem tudjuk mit megnyitni
        if (roomId == null) {
            Snackbar.make(v, "Missing roomId", Snackbar.LENGTH_LONG).show();
            NavHostFragment.findNavController(this).popBackStack();
            return;
        }

        // ViewModel indul + első betöltés
        vm.start(roomId);
        vm.refreshRoom();

        // Szoba adatok figyelése: név + leírás frissítése
        vm.room.observe(getViewLifecycleOwner(), dto -> {
            if (dto != null) {
                tvName.setText(dto.name != null ? dto.name : "—");
                tvDesc.setText(dto.description != null ? dto.description : "");
            }
        });

        // Tagok figyelése: adapter frissítés + üres nézet ki/be
        vm.members.observe(getViewLifecycleOwner(), list -> {
            memberAdapter.submit(list);
            emptyView.setVisibility((list == null || list.isEmpty()) ? View.VISIBLE : View.GONE);
        });

        // Loading figyelése: progress + swipe animáció
        vm.loading.observe(getViewLifecycleOwner(), loading -> {
            boolean show = Boolean.TRUE.equals(loading);
            progress.setVisibility(show ? View.VISIBLE : View.GONE);
            swipe.setRefreshing(show);
        });

        // Hibák megjelenítése Snackbarban
        vm.error.observe(getViewLifecycleOwner(), err -> {
            if (err != null && getView() != null) {
                Snackbar.make(getView(), err, Snackbar.LENGTH_LONG).show();
            }
        });

        // Ha beléptünk a szobába:
        // - Join gomb eltűnik
        // - UpdateStatus megjelenik
        vm.isJoined.observe(getViewLifecycleOwner(), joined -> {
            boolean j = Boolean.TRUE.equals(joined);
            btnJoin.setVisibility(j ? View.GONE : View.VISIBLE);
            btnUpdateStatus.setVisibility(j ? View.VISIBLE : View.GONE);
        });

        // Lehúzás -> tagok frissítése
        swipe.setOnRefreshListener(vm::refreshMembers);

        // Join gomb:
        // ha a szoba jelszavas -> dialog + jelszó ellenőrzés
        // ha nem -> simán join
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

        // Status gomb -> bottom sheet (available/busy/away + status text)
        btnUpdateStatus.setOnClickListener(click -> showStatusBottomSheet());
    }

    @Override
    public void onResume() {
        super.onResume();

        // dátum-idő frissítése
        updateDateTime();

        // ha van vm, akkor:
        // - beírjuk, hogy aktív voltam (lastActive)
        // - frissítjük a tagokat
        if (vm != null) {
            vm.updateLastActiveIfJoined();
            vm.refreshMembers();
        }

        // automatikus frissítés indul (10 mp múlva)
        autoRefreshHandler.postDelayed(autoRefreshRunnable, 10_000);
    }

    @Override
    public void onPause() {
        super.onPause();

        // ha elmegyünk innen, ne fusson tovább a 10 mp-es frissítés
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
    }

    // Ha jelszavas a szoba, itt kérünk jelszót és ellenőrizzük
    private void showPasswordDialogAndJoin(RoomDto r) {

        // jelszó mező
        final EditText et = new EditText(requireContext());
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.enter_password)
                .setView(et)
                .setPositiveButton(R.string.join, (d, w) -> {

                    // beírt jelszó + elvárt jelszó
                    String typed = et.getText() != null ? et.getText().toString().trim() : "";
                    String expected = r.passwordHash != null ? r.passwordHash.trim() : "";

                    // ha egyezik -> join
                    if (!expected.isEmpty() && typed.equals(expected)) {
                        vm.joinRoom();
                    } else if (getView() != null) {
                        // ha nem -> hiba
                        Snackbar.make(getView(), R.string.wrong_password, Snackbar.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, (d, w) -> {})
                .show();
    }

    // Státusz beállító bottom sheet
    private void showStatusBottomSheet() {
        if (vm == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheet = getLayoutInflater().inflate(R.layout.bottomsheet_status, null, false);
        dialog.setContentView(sheet);

        // availability radio gombok + status text mező
        RadioGroup rgAvail   = sheet.findViewById(R.id.rgAvailability);
        RadioButton rbBusy   = sheet.findViewById(R.id.rbBusy);
        RadioButton rbAway   = sheet.findViewById(R.id.rbAway);
        RadioButton rbAvail  = sheet.findViewById(R.id.rbAvailable);
        EditText etStatus    = sheet.findViewById(R.id.etStatusText);

        // alapból legyen Available
        if (rbAvail != null) rbAvail.setChecked(true);

        // cancel
        sheet.findViewById(R.id.btnCancelStatus).setOnClickListener(v -> dialog.dismiss());

        // save -> ViewModel update
        sheet.findViewById(R.id.btnSaveStatus).setOnClickListener(v -> {

            // availability kiválasztása
            String availability;
            int checkedId = rgAvail.getCheckedRadioButtonId();
            if (checkedId == R.id.rbBusy) {
                availability = "BUSY";
            } else if (checkedId == R.id.rbAway) {
                availability = "AWAY";
            } else {
                availability = "AVAILABLE";
            }

            // státusz szöveg
            String statusText = etStatus.getText() != null
                    ? etStatus.getText().toString().trim()
                    : "";

            // elküldjük a ViewModelnek, ő megoldja az API/DB részét
            vm.updateMyStatus(availability, statusText);

            dialog.dismiss();
        });

        dialog.show();
    }

    // Tagra kattintás -> bottom sheet megnyitása tag adatokkal
    private void showMemberBottomSheet(String userId) {

        // Kikeressük a kattintott tag MembershipEntity-jét a listából
        MembershipEntity member = null;
        if (vm != null && vm.members.getValue() != null) {
            for (MembershipEntity m : vm.members.getValue()) {
                if (userId != null && userId.equals(m.userId)) {
                    member = m;
                    break;
                }
            }
        }

        // bottomsheet layout
        View sheet = getLayoutInflater().inflate(R.layout.bottomsheet_member, null, false);

        // UI elemek a sheet-ben
        TextView tvUserName        = sheet.findViewById(R.id.tvUserName);
        TextView tvStatusText      = sheet.findViewById(R.id.tvStatusText);
        TextView tvLastActive      = sheet.findViewById(R.id.tvLastActive);
        TextView tvAvailabilityDot = sheet.findViewById(R.id.tvAvailabilityDot);
        TextView tvPersonalPhone   = sheet.findViewById(R.id.tvPersonalPhone);
        TextView tvCompanyPhone    = sheet.findViewById(R.id.tvCompanyPhone);
        TextView tvInternalEmail   = sheet.findViewById(R.id.tvInternalEmail);

        // kijelzendő név: userName vagy userId
        String displayName;
        if (member != null && member.userName != null && !member.userName.isEmpty()) {
            displayName = member.userName;
        } else if (userId != null && !userId.isEmpty()) {
            displayName = userId;
        } else {
            displayName = "—";
        }

        // saját id (hogy tudjuk: (You))
        String myId = com.szokolaipecsy.companystatus.auth.AuthManager
                .getInstance(requireContext())
                .getUserId();

        // név kiírás (You) jelzéssel
        if (member != null && myId != null && myId.equals(member.userId)) {
            tvUserName.setText(displayName + " (You)");
        } else {
            tvUserName.setText(displayName);
        }

        // státusz szöveg
        String status = (member != null && member.statusText != null && !member.statusText.isEmpty())
                ? member.statusText
                : "—";
        tvStatusText.setText(status);

        // lastActive formázás
        String last = (member != null && member.lastActiveAt != null && !member.lastActiveAt.isEmpty())
                ? member.lastActiveAt
                : null;
        tvLastActive.setText(formatLastActive(last));

        // availability szín pötty
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

        // Dialog létrehozása
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(sheet);

        // Profil adatok figyelése Room DB-ből (telefonok, belső email)
        if (userDao != null && userId != null) {
            userDao.observeByUserId(userId).observe(
                    getViewLifecycleOwner(),
                    profile -> fillProfileFields(profile, tvPersonalPhone, tvCompanyPhone, tvInternalEmail)
            );
        }

        // close gomb
        sheet.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());

        // ez most csak "preview", nem navigál tovább
        sheet.findViewById(R.id.btnViewProfile).setOnClickListener(v -> {
            Snackbar.make(sheet, "This is a read-only profile preview", Snackbar.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    // Profil mezők kitöltése (ha nincs adat, —)
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

    // Dátum-idő kiírás a fejlécbe
    private void updateDateTime() {
        if (tvDateTime == null) return;

        java.time.ZoneId zone = java.time.ZoneId.of("Europe/Budapest");
        java.time.LocalDateTime now = java.time.LocalDateTime.now(zone);
        java.time.format.DateTimeFormatter fmt =
                java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd • HH:mm");

        tvDateTime.setText(now.format(fmt));
    }

    // Last active formázás: ha régi -> konkrét dátum-idő
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
