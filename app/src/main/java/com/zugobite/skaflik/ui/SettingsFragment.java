package com.zugobite.skaflik.ui;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.zugobite.skaflik.R;
import com.zugobite.skaflik.data.UserPreferences;

/**
 * The Settings screen.
 *
 * <p>Built on {@code PreferenceFragmentCompat}, which reads
 * {@code res/xml/preferences.xml} and writes every change straight to
 * {@link android.content.SharedPreferences}. There is no save button and no
 * persistence code here – that is the whole reason for using the preference
 * framework rather than hand-rolling the screen.</p>
 *
 * <p>The rest of the app reads these values through
 * {@link com.zugobite.skaflik.data.UserPreferences}.</p>
 *
 * <p>Two things do need code. The theme has to be handed to
 * {@link AppCompatDelegate} the moment it changes, and the About entries are
 * filled from the installed package rather than from fixed strings.</p>
 */
public class SettingsFragment extends PreferenceFragmentCompat {

    private static final String TAG = "SettingsFragment";

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        applyThemeOnChange();
        showAppDetails();
    }

    /**
     * Repaints the app as soon as a different theme is picked.
     *
     * <p>{@code setDefaultNightMode} recreates the running activities itself,
     * so the change is visible on this screen rather than at the next launch.
     * The value is saved by the preference framework either way; this only
     * decides when it takes effect.</p>
     */
    private void applyThemeOnChange() {
        Preference themePreference = findPreference(UserPreferences.KEY_THEME);
        if (themePreference == null) {
            return;
        }
        themePreference.setOnPreferenceChangeListener((preference, newValue) -> {
            AppCompatDelegate.setDefaultNightMode(nightModeFor(String.valueOf(newValue)));
            return true;
        });
    }

    /**
     * Maps a stored theme value to a night mode.
     *
     * <p>Read from the new value rather than from
     * {@link UserPreferences#getNightMode}, because the listener runs before
     * the preference framework has written the choice to disk.</p>
     */
    private int nightModeFor(@NonNull String theme) {
        if (UserPreferences.THEME_LIGHT.equals(theme)) {
            return AppCompatDelegate.MODE_NIGHT_NO;
        }
        if (UserPreferences.THEME_DARK.equals(theme)) {
            return AppCompatDelegate.MODE_NIGHT_YES;
        }
        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }

    /** Fills the About entries from the package actually installed. */
    private void showAppDetails() {
        setSummary("pref_about_package", requireContext().getPackageName());
        setSummary("pref_about_version", readVersion());
    }

    /**
     * Reads the installed version as "1.0 (1)".
     *
     * <p>Taken from the {@link PackageManager} rather than {@code BuildConfig}
     * so it reports the build the user is actually running.</p>
     */
    @NonNull
    @SuppressWarnings("deprecation") // versionCode, only on API < 28.
    private String readVersion() {
        try {
            PackageInfo info = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0);

            long versionCode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                    ? info.getLongVersionCode()
                    : info.versionCode;

            return getString(R.string.settings_about_version_format,
                    info.versionName, versionCode);
        } catch (PackageManager.NameNotFoundException cannotHappen) {
            // The app asking about its own package; logged rather than crashed.
            Log.e(TAG, "Could not read this app's own package info", cannotHappen);
            return getString(R.string.settings_about_version_unknown);
        }
    }

    private void setSummary(@NonNull String key, @NonNull String summary) {
        Preference preference = findPreference(key);
        if (preference != null) {
            preference.setSummary(summary);
        }
    }
}
