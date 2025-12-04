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
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private boolean isAuthScreen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean dark = prefs.getBoolean("pref_dark_theme", false);
        AppCompatDelegate.setDefaultNightMode(
                dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        NavHostFragment navHost = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);

        NavController nav = navHost.getNavController();
        BottomNavigationView bottom = findViewById(R.id.bottom_nav);

        AppBarConfiguration appBarConfig = new AppBarConfiguration.Builder(
                R.id.homeFragment,
                R.id.profileFragment,
                R.id.settingsFragment
        ).build();

        NavigationUI.setupActionBarWithNavController(this, nav, appBarConfig);
        NavigationUI.setupWithNavController(bottom, nav);

        bottom.setOnItemReselectedListener(item -> { /* no-op */ });

        AuthManager auth = AuthManager.getInstance(this);

        nav.addOnDestinationChangedListener((controller, destination, args) -> {
            int destId = destination.getId();
            isAuthScreen =
                    destId == R.id.authGateFragment ||
                            destId == R.id.loginFragment   ||
                            destId == R.id.registerFragment;

            toolbar.setVisibility(isAuthScreen ? View.GONE : View.VISIBLE);
            bottom.setVisibility(isAuthScreen ? View.GONE : View.VISIBLE);

            if (!isAuthScreen && !auth.isLoggedIn()) {
                bottom.post(() -> {
                    try {
                        controller.navigate(R.id.authGateFragment);
                    } catch (Exception ignored) {}
                });
            }

            invalidateOptionsMenu();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_top_appbar, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
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

        if (id == R.id.action_about) {
            Toast.makeText(this, "Company Status v1.0", Toast.LENGTH_SHORT).show();
            return true;

        } else if (id == R.id.action_logout) {

            AuthManager.getInstance(this).logout();

            AppDatabase.EXECUTOR.execute(() ->
                    AppDatabase.getInstance(this).clearAllTables()
            );

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
        NavHostFragment navHost = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        NavController nav = navHost != null ? navHost.getNavController() : null;
        return (nav != null && nav.navigateUp()) || super.onSupportNavigateUp();
    }
}
