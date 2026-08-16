package com.zugobite.skaflik.ui;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;

import com.zugobite.skaflik.R;

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
 */
public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }
}
