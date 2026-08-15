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
