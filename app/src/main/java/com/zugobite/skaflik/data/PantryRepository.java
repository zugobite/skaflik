package com.zugobite.skaflik.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import com.zugobite.skaflik.logic.IngredientMatcher;
import com.zugobite.skaflik.model.PantryItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Every read and write of the user's pantry lives here.
 *
 * <p>This is the full CRUD surface: {@link #insertItem}, {@link #getAllItems},
 * {@link #getItemById}, {@link #updateItem} and {@link #deleteItem}. Keeping it
 * in one class means no activity or fragment ever talks to Firestore directly,
 * and the normalised name is guaranteed to be written consistently.</p>
 */
public class PantryRepository {

    private final FirebaseFirestore firestore;

    public PantryRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Returns the current user's pantry collection:
     * {@code users/{uid}/pantryItems}.
     *
     * @throws IllegalStateException if nobody is signed in, which means
     *                               {@link AuthManager#ensureSignedIn} was skipped
     */
    @NonNull
    private CollectionReference pantryCollection() {
        String userId = AuthManager.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException(
                    "No signed-in user. Call AuthManager.ensureSignedIn() before using the pantry.");
        }
        return firestore.collection(FirestoreContract.COLLECTION_USERS)
                .document(userId)
                .collection(FirestoreContract.COLLECTION_PANTRY_ITEMS);
    }

    // --- Create ---

    /**
     * Adds an item to the pantry.
     *
     * <p>The normalised name is derived here rather than trusted from the
     * caller, so a stored item can never disagree with the matching rules.</p>
     *
     * @param item     the item to store; its ID is ignored and assigned by Firestore
     * @param callback receives the new document ID
     */
    public void insertItem(@NonNull PantryItem item,
                           @NonNull RepositoryCallback<String> callback) {
        item.setNameNorm(IngredientMatcher.normalize(item.getName()));

        pantryCollection().add(item)
                .addOnSuccessListener(document -> callback.onSuccess(document.getId()))
                .addOnFailureListener(callback::onError);
    }

    // --- Read ---

    /**
     * Fetches the whole pantry once, newest first.
     *
     * @param callback receives the items, empty list when the pantry is bare
     */
    public void getAllItems(@NonNull RepositoryCallback<List<PantryItem>> callback) {
        pantryCollection()
                .orderBy(FirestoreContract.FIELD_CREATED_AT, Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<PantryItem> items = new ArrayList<>();
                    for (QueryDocumentSnapshot document : snapshot) {
                        items.add(document.toObject(PantryItem.class));
                    }
                    callback.onSuccess(items);
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Watches the pantry and reports it again on every change.
     *
     * <p>This is what keeps the list on screen correct without manual
     * refreshing: an insert, edit or delete re-fires the callback, and the
     * adapter simply redraws.</p>
     *
     * @param callback receives the current items on every change
     * @return the registration; the caller must call
     *         {@link ListenerRegistration#remove()} when its view goes away,
     *         or the listener leaks
     */
    @NonNull
    public ListenerRegistration observeItems(
            @NonNull RepositoryCallback<List<PantryItem>> callback) {
        return pantryCollection()
                .orderBy(FirestoreContract.FIELD_CREATED_AT, Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        callback.onError(error);
                        return;
                    }
                    List<PantryItem> items = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot document : snapshot.getDocuments()) {
                            PantryItem item = document.toObject(PantryItem.class);
                            if (item != null) {
                                items.add(item);
                            }
                        }
                    }
                    callback.onSuccess(items);
                });
    }

    /**
     * Fetches one item by document ID, for the edit screen.
     *
     * @param callback receives the item, or null when no such document exists
     */
    public void getItemById(@NonNull String itemId,
                            @NonNull RepositoryCallback<PantryItem> callback) {
        pantryCollection().document(itemId)
                .get()
                .addOnSuccessListener(document ->
                        callback.onSuccess(document.exists()
                                ? document.toObject(PantryItem.class)
                                : null))
                .addOnFailureListener(callback::onError);
    }

    /**
     * Finds an existing item whose normalised name matches the one given.
     *
     * <p>Used by the Add form to spot a duplicate - "Tomatoes" when "tomato" is
     * already held - so the user can be offered a merge instead of ending up
     * with two rows for the same thing.</p>
     *
     * @param rawName  the name as typed
     * @param callback receives the matching item, or null when there is none
     */
    public void findByNormalisedName(@NonNull String rawName,
                                     @NonNull RepositoryCallback<PantryItem> callback) {
        String nameNorm = IngredientMatcher.normalize(rawName);

        pantryCollection()
                .whereEqualTo(FirestoreContract.FIELD_NAME_NORM, nameNorm)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        callback.onSuccess(null);
                    } else {
                        callback.onSuccess(snapshot.getDocuments().get(0).toObject(PantryItem.class));
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    // --- Update ---

    /**
     * Overwrites an existing item's editable fields.
     *
     * <p>Only the fields the user can change are written, so the server-set
     * {@code createdAt} survives the edit.</p>
     *
     * @param itemId   the document to update
     * @param item     the new values
     * @param callback receives null on success
     */
    public void updateItem(@NonNull String itemId, @NonNull PantryItem item,
                           @NonNull RepositoryCallback<Void> callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(FirestoreContract.FIELD_NAME, item.getName());
        updates.put(FirestoreContract.FIELD_NAME_NORM,
                IngredientMatcher.normalize(item.getName()));
        updates.put(FirestoreContract.FIELD_QUANTITY, item.getQuantity());
        updates.put(FirestoreContract.FIELD_UNIT, item.getUnit());
        updates.put(FirestoreContract.FIELD_EXPIRY_DATE, item.getExpiryDate());

        pantryCollection().document(itemId)
                .update(updates)
                .addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(callback::onError);
    }

    /**
     * Sets an item's quantity, used when merging a duplicate into an existing row.
     *
     * @param itemId      the document to update
     * @param newQuantity the combined amount
     * @param unit        the unit the combined amount is expressed in
     * @param callback    receives null on success
     */
    public void updateQuantity(@NonNull String itemId, double newQuantity,
                               @NonNull String unit,
                               @NonNull RepositoryCallback<Void> callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(FirestoreContract.FIELD_QUANTITY, newQuantity);
        updates.put(FirestoreContract.FIELD_UNIT, unit);

        pantryCollection().document(itemId)
                .update(updates)
                .addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(callback::onError);
    }

    // --- Delete ---

    /**
     * Removes an item from the pantry.
     *
     * <p>The confirmation dialog is the UI's job; by the time this is called
     * the user has already agreed.</p>
     *
     * @param itemId   the document to delete
     * @param callback receives null on success
     */
    public void deleteItem(@Nullable String itemId,
                           @NonNull RepositoryCallback<Void> callback) {
        if (itemId == null) {
            callback.onError(new IllegalArgumentException("Cannot delete an item with no ID"));
            return;
        }

        pantryCollection().document(itemId)
                .delete()
                .addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(callback::onError);
    }
}
