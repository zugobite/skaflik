package com.zugobite.skaflik.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Gives the app a user identity without asking anyone to create an account.
 *
 * <p>Firebase anonymous sign-in mints a UID on first launch and keeps it on the
 * device, which is exactly what a single-user pantry app needs: pantry data can
 * be scoped to that UID by the security rules, with no login screen, no
 * password to forget, and nothing extra for the user to do.</p>
 */
public final class AuthManager {

    private static final FirebaseAuth AUTH = FirebaseAuth.getInstance();

    /** Static helper class; never instantiated. */
    private AuthManager() {
    }

    /**
     * Returns the current user's ID, or null when nobody is signed in yet.
     *
     * <p>Call {@link #ensureSignedIn} before relying on this.</p>
     */
    @Nullable
    public static String getCurrentUserId() {
        FirebaseUser user = AUTH.getCurrentUser();
        return user == null ? null : user.getUid();
    }

    /** True when a UID is available and repositories can be used. */
    public static boolean isSignedIn() {
        return AUTH.getCurrentUser() != null;
    }

    /**
     * Signs in anonymously if needed, then hands back the UID.
     *
     * <p>Safe to call on every launch: if the device already has a session,
     * the existing UID is returned immediately without a network round trip.</p>
     *
     * @param callback receives the UID on success
     */
    public static void ensureSignedIn(@NonNull RepositoryCallback<String> callback) {
        String existingUserId = getCurrentUserId();
        if (existingUserId != null) {
            callback.onSuccess(existingUserId);
            return;
        }

        AUTH.signInAnonymously()
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) {
                        // Should not happen, but never hand the caller a null UID.
                        callback.onError(new IllegalStateException(
                                "Anonymous sign-in returned no user"));
                    } else {
                        callback.onSuccess(user.getUid());
                    }
                })
                .addOnFailureListener(callback::onError);
    }
}
