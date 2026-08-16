package com.zugobite.skaflik;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import com.zugobite.skaflik.data.AuthManager;
import com.zugobite.skaflik.data.RecipeSeeder;
import com.zugobite.skaflik.data.RepositoryCallback;
import com.zugobite.skaflik.ui.PantryListFragment;
import com.zugobite.skaflik.ui.SettingsFragment;
import com.zugobite.skaflik.ui.SuggestedRecipesFragment;

/**
 * Host activity for Skaflik.
 *
 * <p>Owns the {@link BottomNavigationView} and swaps the three top-level
 * destinations (Pantry, Suggestions, Settings) into a single fragment
 * container. The Add/Edit Ingredient and Recipe Detail screens are separate
 * activities launched from these fragments via explicit Intents.</p>
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

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

        prepareBackend();
    }

    /**
     * Signs in anonymously and seeds the recipe list if this is the first run.
     *
     * <p>Nothing can read or write Firestore until a UID exists, so sign-in has
     * to complete before seeding starts - hence the nesting rather than two
     * independent calls.</p>
     */
    private void prepareBackend() {
        AuthManager.ensureSignedIn(new RepositoryCallback<String>() {
            @Override
            public void onSuccess(String userId) {
                Log.d(TAG, "Signed in as " + userId);
                seedRecipes();
            }

            @Override
            public void onError(@NonNull Exception error) {
                Log.e(TAG, "Anonymous sign-in failed", error);
                showBackendError(R.string.error_sign_in);
            }
        });
    }

    /** Seeds the shared recipe collection; a no-op after the first run. */
    private void seedRecipes() {
        RecipeSeeder.seedIfNeeded(new RepositoryCallback<Integer>() {
            @Override
            public void onSuccess(Integer seededCount) {
                if (seededCount > 0) {
                    Log.d(TAG, "Seeded " + seededCount + " recipes.");
                }
            }

            @Override
            public void onError(@NonNull Exception error) {
                Log.e(TAG, "Recipe seeding failed", error);
                showBackendError(error);
            }
        });
    }

    /**
     * Tells the user when the backend is unreachable.
     *
     * <p>Firestore serves cached data offline, so this is a warning rather than
     * a fatal error - the pantry still works.</p>
     */
    private void showBackendError(@StringRes int messageResId) {
        Snackbar.make(findViewById(R.id.fragment_container),
                messageResId, Snackbar.LENGTH_LONG).show();
    }

    /** Reports a backend failure, naming the actual cause. */
    private void showBackendError(@NonNull Exception error) {
        Snackbar.make(findViewById(R.id.fragment_container),
                RepositoryCallback.messageFor(error,
                        getString(R.string.error_seed),
                        getString(R.string.error_permission_denied)),
                Snackbar.LENGTH_LONG).show();
    }

    /**
     * Routes a bottom-navigation tap to its fragment.
     *
     * @param item the tapped menu item
     * @return true once the destination has been shown
     */
    private boolean onNavigationItemSelected(@NonNull android.view.MenuItem item) {
        Fragment destination;
        int itemId = item.getItemId();

        if (itemId == R.id.nav_pantry) {
            destination = new PantryListFragment();
        } else if (itemId == R.id.nav_suggestions) {
            destination = new SuggestedRecipesFragment();
        } else {
            destination = new SettingsFragment();
        }

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
