package com.zugobite.skaflik.data;

import androidx.annotation.NonNull;

/**
 * Delivers the result of an asynchronous repository call back to the UI.
 *
 * <p>Every Firestore operation is asynchronous, so repository methods cannot
 * simply return a value. This interface keeps that fact contained: the UI
 * supplies what to do on success and on failure, and never has to touch a
 * Firebase {@code Task} directly.</p>
 *
 * @param <T> the type produced on success
 */
public interface RepositoryCallback<T> {

    /**
     * Turns a Firestore failure into the right message for the user.
     *
     * <p>Every backend failure used to be reported as "check your connection",
     * which is wrong and unhelpful when the real cause is that the security
     * rules rejected the request – no amount of reconnecting fixes that.</p>
     *
     * @param error       the failure from Firestore
     * @param networkText what to say when the cause really is connectivity
     * @param deniedText  what to say when the rules refused the request
     * @return the message to show
     */
    static String messageFor(@NonNull Exception error,
                             @NonNull String networkText,
                             @NonNull String deniedText) {
        if (error instanceof com.google.firebase.firestore.FirebaseFirestoreException) {
            com.google.firebase.firestore.FirebaseFirestoreException.Code code =
                    ((com.google.firebase.firestore.FirebaseFirestoreException) error).getCode();
            if (code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                return deniedText;
            }
        }
        return networkText;
    }

    /** Called when the operation completed and returned {@code result}. */
    void onSuccess(T result);

    /**
     * Called when the operation failed.
     *
     * <p>Implementations should show the user something useful rather than
     * failing silently - a network drop reaches here.</p>
     */
    void onError(@NonNull Exception error);
}
