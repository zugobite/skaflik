package com.zugobite.skaflik.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.zugobite.skaflik.logic.UnitConverter;

import java.util.List;

/**
 * Reads the user's settings.
 *
 * <p>Settings live in {@link SharedPreferences} rather than Firestore: they
 * describe how this device behaves, not what is in the pantry, and they must
 * be readable instantly at startup without a network round trip.</p>
 *
 * <p>Every key is declared here and nowhere else. The Settings screen writes
 * through {@code androidx.preference}, which uses these same keys – they are
 * defined in {@code res/xml/preferences.xml} and must stay in step.</p>
 */
public final class UserPreferences {

    /** Whether to flag items that are close to their expiry date. */
    public static final String KEY_EXPIRY_ALERTS = "pref_expiry_alerts";

    /** Which unit system the Add/Edit form should suggest first. */
    public static final String KEY_UNIT_SYSTEM = "pref_unit_system";

    public static final String UNIT_SYSTEM_METRIC = "metric";
    public static final String UNIT_SYSTEM_IMPERIAL = "imperial";

    /** Which theme the app draws in. */
    public static final String KEY_THEME = "pref_theme";

    public static final String THEME_SYSTEM = "system";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";

    /** Expiry alerts are on by default; cutting food waste is the app's point. */
    private static final boolean DEFAULT_EXPIRY_ALERTS = true;

    /** Static helper class; never instantiated. */
    private UserPreferences() {
    }

    private static SharedPreferences preferencesFor(@NonNull Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    /** True when the user wants to be warned about items expiring soon. */
    public static boolean isExpiryAlertsEnabled(@NonNull Context context) {
        return preferencesFor(context).getBoolean(KEY_EXPIRY_ALERTS, DEFAULT_EXPIRY_ALERTS);
    }

    /**
     * The chosen theme as an {@link AppCompatDelegate} night mode.
     *
     * <p>Defaults to following the system, which is what a user who has set
     * their phone to dark at sunset already expects of every app on it.</p>
     */
    public static int getNightMode(@NonNull Context context) {
        String theme = preferencesFor(context).getString(KEY_THEME, THEME_SYSTEM);
        if (THEME_LIGHT.equals(theme)) {
            return AppCompatDelegate.MODE_NIGHT_NO;
        }
        if (THEME_DARK.equals(theme)) {
            return AppCompatDelegate.MODE_NIGHT_YES;
        }
        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }

    /** The chosen unit system, defaulting to metric. */
    @NonNull
    public static String getUnitSystem(@NonNull Context context) {
        String system = preferencesFor(context)
                .getString(KEY_UNIT_SYSTEM, UNIT_SYSTEM_METRIC);
        return system == null ? UNIT_SYSTEM_METRIC : system;
    }

    /** The chosen unit system, as the converter expects it. */
    @NonNull
    public static UnitConverter.System getDisplaySystem(@NonNull Context context) {
        return UNIT_SYSTEM_IMPERIAL.equals(getUnitSystem(context))
                ? UnitConverter.System.IMPERIAL
                : UnitConverter.System.METRIC;
    }

    /**
     * The units the Add/Edit form should offer first.
     *
     * <p>Only the ordering of the spinner changes: every unit stays selectable
     * whatever the preference, so switching systems can never orphan an item
     * already saved in the other one.</p>
     */
    @NonNull
    public static List<String> getPreferredUnits(@NonNull Context context) {
        return UNIT_SYSTEM_IMPERIAL.equals(getUnitSystem(context))
                ? UnitConverter.IMPERIAL_UNITS
                : UnitConverter.METRIC_UNITS;
    }

    /** The unit the Add/Edit form starts on for a new item. */
    @NonNull
    public static String getDefaultUnit(@NonNull Context context) {
        return getPreferredUnits(context).get(0);
    }
}
