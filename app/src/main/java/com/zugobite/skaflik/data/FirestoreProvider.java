package com.zugobite.skaflik.data;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Hands out the Firestore instance this app uses.
 *
 * <p>A Firebase project can hold several databases. This app talks to the
 * {@code (default)} one, which is what {@link FirebaseFirestore#getInstance()}
 * returns. Routing every caller through here keeps that choice in one place: if
 * the app ever has to move to a named database, only this method changes rather
 * than each repository.</p>
 */
public final class FirestoreProvider {

    /** Static helper class; never instantiated. */
    private FirestoreProvider() {
    }

    /** The Firestore instance backing the whole app. */
    @NonNull
    public static FirebaseFirestore get() {
        return FirebaseFirestore.getInstance();
    }
}
