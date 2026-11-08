package com.winlator;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

import com.google.android.material.navigation.NavigationView;
import com.winlator.contentdialog.AboutDialog;
import com.winlator.core.Callback;
import com.winlator.core.PreloaderDialog;

/**
 * GameHub-style shell. Fragments are loaded only on user tap to avoid startup crashes
 * when native / RootFS pieces are missing.
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    public static final boolean DEBUG_MODE = false;
    public static final @IntRange(from = 1, to = 19) byte CONTAINER_PATTERN_COMPRESSION_LEVEL = 9;
    public static final byte PERMISSION_WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 1;
    public static final byte OPEN_FILE_REQUEST_CODE = 2;
    public static final byte EDIT_INPUT_CONTROLS_REQUEST_CODE = 3;
    public static final byte OPEN_DIRECTORY_REQUEST_CODE = 4;

    private DrawerLayout drawerLayout;
    public PreloaderDialog preloaderDialog;
    private boolean editInputControls = false;
    private int selectedProfileId;
    private Callback<Uri> openFileCallback;
    private SharedPreferences preferences;
    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_gamehub);

        try {
            preferences = PreferenceManager.getDefaultSharedPreferences(this);
        } catch (Exception e) {
            preferences = null;
        }

        try {
            preloaderDialog = new PreloaderDialog(this);
        } catch (Exception e) {
            preloaderDialog = null;
        }

        Intent intent = getIntent();
        if (intent != null) {
            editInputControls = intent.getBooleanExtra("edit_input_controls", false);
            selectedProfileId = intent.getIntExtra("selected_profile_id", 0);
        }

        drawerLayout = findViewById(R.id.DrawerLayout);

        // Placeholder text inside content area so user sees a stable home
        View container = findViewById(R.id.FLFragmentContainer);
        if (container instanceof android.widget.FrameLayout) {
            TextView hint = new TextView(this);
            hint.setText("GameLator\n\nTocca Library o + Add per continuare");
            hint.setTextColor(0xFFFFFFFF);
            hint.setTextSize(16f);
            hint.setPadding(48, 48, 48, 48);
            ((android.widget.FrameLayout) container).addView(hint);
        }

        wireBottomNav();

        // Do NOT auto-load ShortcutsFragment / RootFS on startup (common crash source)
        if (editInputControls) {
            tryLoad(() -> showFragment(new InputControlsFragment(selectedProfileId)));
        }
    }

    private void wireBottomNav() {
        View navHome = findViewById(R.id.nav_home);
        View navLibrary = findViewById(R.id.nav_library);
        View navAdd = findViewById(R.id.nav_add);
        View navProfile = findViewById(R.id.nav_profile);
        View btnSettings = findViewById(R.id.gh_btn_settings);

        if (navHome != null) {
            navHome.setOnClickListener(v -> tryLoad(() -> showFragment(new ShortcutsFragment())));
        }
        if (navLibrary != null) {
            navLibrary.setOnClickListener(v -> tryLoad(() -> showFragment(new ContainersFragment())));
        }
        if (navAdd != null) {
            navAdd.setOnClickListener(v -> tryLoad(() -> showFragment(new ContainersFragment())));
        }
        View.OnClickListener settings = v -> tryLoad(() -> showFragment(new SettingsFragment()));
        if (navProfile != null) navProfile.setOnClickListener(settings);
        if (btnSettings != null) btnSettings.setOnClickListener(settings);
    }

    private void tryLoad(Runnable action) {
        try {
            action.run();
        } catch (Throwable t) {
            Toast.makeText(this, "Funzione non disponibile in questa build", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OPEN_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK && openFileCallback != null && data != null) {
            openFileCallback.call(data.getData());
            openFileCallback = null;
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        if (currentFragment != null) {
            currentFragment = null;
            recreate();
            return;
        }
        super.onBackPressed();
    }

    public void setOpenFileCallback(Callback<Uri> openFileCallback) {
        this.openFileCallback = openFileCallback;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        return true;
    }

    public void showFragment(Fragment fragment) {
        View host = findViewById(R.id.FLFragmentContainer);
        if (host instanceof android.widget.FrameLayout) {
            ((android.widget.FrameLayout) host).removeAllViews();
        }
        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction()
            .replace(R.id.FLFragmentContainer, fragment)
            .commitAllowingStateLoss();
        currentFragment = fragment;
        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }
}
