package com.zugobite.skaflik;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Host activity for Skaflik.
 *
 * <p>Owns the {@link BottomNavigationView} and swaps the three top-level
 * destinations (Pantry, Suggestions, Settings) into a single fragment
 * container. The Add/Edit Ingredient and Recipe Detail screens are separate
 * activities launched from these fragments via explicit Intents.</p>
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(this::onNavigationItemSelected);

        // Only set the starting destination on a cold start; on rotation the
        // FragmentManager restores whichever fragment was already showing.
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_pantry);
        }
    }

    /**
     * Routes a bottom-navigation tap to its fragment.
     *
     * @param item the tapped menu item
     * @return true once the destination has been shown
     */
    private boolean onNavigationItemSelected(@NonNull android.view.MenuItem item) {
        // TODO (Phase 5/11/14): replace these placeholders with
        // PantryListFragment, SuggestedRecipesFragment and SettingsFragment.
        Fragment destination = new Fragment();
        showFragment(destination);
        return true;
    }

    /** Replaces whatever is currently in the fragment container. */
    private void showFragment(@NonNull Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
