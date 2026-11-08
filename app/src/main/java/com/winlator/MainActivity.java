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
import com.winlator.core.AppUtils;
import com.winlator.core.Callback;
import com.winlator.core.LocaleHelper;
import com.winlator.core.PreloaderDialog;
import com.winlator.xenvironment.RootFSInstaller;

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
        try {
            AppUtils.setActivityTheme(this);
        } catch (Exception ignored) {
        }
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main_gamehub);

        try {
            preloaderDialog = new PreloaderDialog(this);
        } catch (Exception ignored) {
            preloaderDialog = null;
        }

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        Intent intent = getIntent();
        editInputControls = intent != null && intent.getBooleanExtra("edit_input_controls", false);

        drawerLayout = findViewById(R.id.DrawerLayout);
        NavigationView navigationView = findViewById(R.id.NavigationView);
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
        }

        wireBottomNav();

        if (editInputControls) {
            selectedProfileId = intent.getIntExtra("selected_profile_id", 0);
            safeShowFragment(new InputControlsFragment(selectedProfileId));
        } else {
            boolean showShortcutsFirst = preferences.getBoolean("show_shortcuts_first", true);
            if (showShortcutsFirst) {
                safeShowFragment(new ShortcutsFragment());
            } else {
                safeShowFragment(new ContainersFragment());
            }

            // RootFS / native may be missing in this CI build — never crash the UI
            try {
                if (!requestAppPermissions()) {
                    tryInstallRootFs();
                }
            } catch (Exception ignored) {
            }

            if (intent != null) {
                int containerId = intent.getIntExtra("container_id", 0);
                String startPath = intent.getStringExtra("start_path");
                if (containerId > 0 && startPath != null) {
                    safeShowFragment(new ContainerFileManagerFragment(containerId, startPath));
                }
            }
        }
    }

    private void wireBottomNav() {
        View navHome = findViewById(R.id.nav_home);
        View navLibrary = findViewById(R.id.nav_library);
        View navAdd = findViewById(R.id.nav_add);
        View navProfile = findViewById(R.id.nav_profile);
        View btnSettings = findViewById(R.id.gh_btn_settings);

        if (navHome != null) {
            navHome.setOnClickListener(v -> safeShowFragment(new ShortcutsFragment()));
        }
        if (navLibrary != null) {
            navLibrary.setOnClickListener(v -> safeShowFragment(new ContainersFragment()));
        }
        if (navAdd != null) {
            navAdd.setOnClickListener(v -> safeShowFragment(new ContainersFragment()));
        }
        View.OnClickListener openSettings = v -> safeShowFragment(new SettingsFragment());
        if (navProfile != null) navProfile.setOnClickListener(openSettings);
        if (btnSettings != null) btnSettings.setOnClickListener(openSettings);
    }

    private void tryInstallRootFs() {
        try {
            RootFSInstaller.installIfNeeded(this);
        } catch (Throwable t) {
            // Native libs may be absent — UI must still open
        }
    }

    private void safeShowFragment(Fragment fragment) {
        try {
            showFragment(fragment);
        } catch (Throwable t) {
            // keep empty shell visible
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        try {
            super.attachBaseContext(LocaleHelper.setSystemLocale(newBase));
        } catch (Exception e) {
            super.attachBaseContext(newBase);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_WRITE_EXTERNAL_STORAGE_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                tryInstallRootFs();
            }
            // do not finish() — allow UI without storage
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MainActivity.OPEN_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (openFileCallback != null && data != null) {
                openFileCallback.call(data.getData());
                openFileCallback = null;
            }
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        try {
            if ((newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE ||
                newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) && currentFragment instanceof BaseFileManagerFragment) {
                ((BaseFileManagerFragment) currentFragment).onOrientationChanged();
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onBackPressed() {
        try {
            if (currentFragment != null && currentFragment.isVisible()) {
                if (currentFragment instanceof BaseFileManagerFragment) {
                    if (((BaseFileManagerFragment) currentFragment).onBackPressed()) return;
                } else if (currentFragment instanceof ContainersFragment) {
                    finish();
                    return;
                }
            }
            safeShowFragment(new ShortcutsFragment());
        } catch (Exception e) {
            finish();
        }
    }

    public void setOpenFileCallback(Callback<Uri> openFileCallback) {
        this.openFileCallback = openFileCallback;
    }

    private boolean requestAppPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        try {
            String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
        } catch (Exception ignored) {
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == R.id.menu_item_add ||
            itemId == R.id.menu_item_home ||
            itemId == R.id.menu_item_view_style ||
            itemId == R.id.menu_item_new_folder) {
            return super.onOptionsItemSelected(menuItem);
        } else {
            if (editInputControls) {
                setResult(RESULT_OK);
                finish();
            } else if (currentFragment instanceof BaseFileManagerFragment) {
                try {
                    if (((BaseFileManagerFragment) currentFragment).onOptionsMenuClicked()) return true;
                } catch (Exception ignored) {
                }
            } else if (drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
            return true;
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        try {
            FragmentManager fragmentManager = getSupportFragmentManager();
            if (fragmentManager.getBackStackEntryCount() > 0) {
                fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }

            switch (item.getItemId()) {
                case R.id.menu_item_winebox_home:
                    safeShowFragment(new HomeWineBoxFragment());
                    break;
                case R.id.menu_item_shortcuts:
                    preferences.edit().putBoolean("show_shortcuts_first", true).apply();
                    safeShowFragment(new ShortcutsFragment());
                    break;
                case R.id.menu_item_containers:
                    preferences.edit().putBoolean("show_shortcuts_first", false).apply();
                    safeShowFragment(new ContainersFragment());
                    break;
                case R.id.menu_item_input_controls:
                    safeShowFragment(new InputControlsFragment(selectedProfileId));
                    break;
                case R.id.menu_item_settings:
                    safeShowFragment(new SettingsFragment());
                    break;
                case R.id.menu_item_about:
                    (new AboutDialog(this)).show();
                    break;
            }
        } catch (Exception ignored) {
        }
        return true;
    }

    public void showFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
            .replace(R.id.FLFragmentContainer, fragment)
            .commitAllowingStateLoss();

        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        currentFragment = fragment;
    }
}
