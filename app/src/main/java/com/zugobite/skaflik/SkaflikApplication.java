package com.zugobite.skaflik;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.zugobite.skaflik.data.UserPreferences;

/**
 * Applies the saved theme before any screen is drawn.
 *
 * <p>The night mode has to be set once, at process start, rather than in each
 * activity: an activity that applied it in {@code onCreate} would already have
 * inflated its views under the wrong theme and would have to recreate itself,
 * which the user sees as a flash of the theme they did not pick.</p>
 */
public class SkaflikApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(UserPreferences.getNightMode(this));
    }
}
