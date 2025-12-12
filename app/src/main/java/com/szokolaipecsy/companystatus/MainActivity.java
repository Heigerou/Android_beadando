package com.szokolaipecsy.companystatus;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.szokolaipecsy.companystatus.auth.AuthManager;
import com.szokolaipecsy.companystatus.data.AppDatabase;
import androidx.navigation.ui.AppBarConfiguration;

// MainActivity = az app "fő kerete"
// Itt indul az egész: téma beállítás, toolbar, bottom nav, navigáció, top menü (about/logout)
public class MainActivity extends AppCompatActivity {

    // Ez jelzi, hogy épp Auth képernyőn vagyunk-e (Login/Register/AuthGate)
    // Ha igen -> toolbar + bottom nav legyen elrejtve
    private boolean isAuthScreen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // 1) Téma beállítása induláskor (dark mode)
        // azért itt, mert így már a setContentView előtt érvényesül
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean dark = prefs.getBoolean("pref_dark_theme", false);
        AppCompatDelegate.setDefaultNightMode(
                dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);

        // 2) Fő layout betöltés (activity_main)
        setContentView(R.layout.activity_main);

        // 3) Toolbar beállítás (TopAppBar)
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        // 4) Navigation host kikeresése, innen kapjuk a NavController-t
        NavHostFragment navHost = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);

        NavController nav = navHost.getNavController();

        // 5) Bottom navigation (alsó menü)
        BottomNavigationView bottom = findViewById(R.id.bottom_nav);

        // 6) AppBarConfiguration: ezek a "top level" oldalak
        // Ilyenkor a back gomb helyett "hamburger/back" viselkedés rendesebb,
        // meg a toolbar cím kezelése is korrekt
        AppBarConfiguration appBarConfig = new AppBarConfiguration.Builder(
                R.id.homeFragment,
                R.id.profileFragment,
                R.id.settingsFragment
        ).build();

        // 7) Toolbar + NavController összekötése (cím, vissza nyíl)
        NavigationUI.setupActionBarWithNavController(this, nav, appBarConfig);

        // 8) Bottom nav összekötése a navigációval
        NavigationUI.setupWithNavController(bottom, nav);

        // 9) Ha ugyanarra a menüpontra kattintunk újra, ne csináljon semmit
        bottom.setOnItemReselectedListener(item -> { /* no-op */ });

        // AuthManager: belépett user ellenőrzés
        AuthManager auth = AuthManager.getInstance(this);

        // 10) Minden oldalletöltésnél figyeljük, melyik fragment aktív
        nav.addOnDestinationChangedListener((controller, destination, args) -> {
            int destId = destination.getId();

            // Auth screen-ek felismerése
            isAuthScreen =
                    destId == R.id.authGateFragment ||
                            destId == R.id.loginFragment   ||
                            destId == R.id.registerFragment;

            // Ha auth screen -> toolbar+bottom eltűnik
            // Ha nem auth -> látszódik
            toolbar.setVisibility(isAuthScreen ? View.GONE : View.VISIBLE);
            bottom.setVisibility(isAuthScreen ? View.GONE : View.VISIBLE);

            // Extra védelem:
            // ha nem auth képernyőn vagyunk, de közben nincs login,
            // akkor dobjuk vissza AuthGate-re
            if (!isAuthScreen && !auth.isLoggedIn()) {
                bottom.post(() -> {
                    try {
                        controller.navigate(R.id.authGateFragment);
                    } catch (Exception ignored) {}
                });
            }

            // Menüpontok (about/logout) frissítése
            invalidateOptionsMenu();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // TopAppBar menü betöltés (jobb felső sarok)
        getMenuInflater().inflate(R.menu.menu_top_appbar, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Itt döntjük el, mikor látszódjanak a menüpontok
        // Auth képernyőn ne legyen About/Logout
        if (menu != null) {
            MenuItem about = menu.findItem(R.id.action_about);
            MenuItem logout = menu.findItem(R.id.action_logout);
            if (about != null) about.setVisible(!isAuthScreen);
            if (logout != null) logout.setVisible(!isAuthScreen);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // About: csak egy gyors Toast
        if (id == R.id.action_about) {
            Toast.makeText(this, "Company Status v1.0", Toast.LENGTH_SHORT).show();
            return true;

            // Logout: kijelentkezés + helyi adat törlés + vissza AuthGate-re
        } else if (id == R.id.action_logout) {

            // Auth adatok törlése
            AuthManager.getInstance(this).logout();

            // Room DB ürítése (tagok, profil, kedvencek, stb.)
            AppDatabase.EXECUTOR.execute(() ->
                    AppDatabase.getInstance(this).clearAllTables()
            );

            // Navigáció AuthGate-re
            NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment);
            if (navHost != null) {
                navHost.getNavController().navigate(R.id.authGateFragment);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        // A toolbar back gomb működése (NavigationUI)
        NavHostFragment navHost = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        NavController nav = navHost != null ? navHost.getNavController() : null;
        return (nav != null && nav.navigateUp()) || super.onSupportNavigateUp();
    }
}
