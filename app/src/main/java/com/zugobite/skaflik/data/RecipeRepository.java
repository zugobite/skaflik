package com.zugobite.skaflik.data;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import com.zugobite.skaflik.model.Recipe;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads the shared recipe collection.
 *
 * <p>Read-only by design: recipes are seeded once by {@link RecipeSeeder} and
 * the app never edits them, which is also what the Firestore security rules
 * enforce.</p>
 */
public class RecipeRepository {

    private final FirebaseFirestore firestore;

    public RecipeRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Fetches every recipe, alphabetically.
     *
     * <p>The suggestion engine needs all of them in memory to test each against
     * the pantry. With ~18 seeded recipes that is a single small read; a much
     * larger catalogue would need paging or a server-side query instead.</p>
     *
     * @param callback receives the recipes
     */
    public void getAllRecipes(@NonNull RepositoryCallback<List<Recipe>> callback) {
        firestore.collection(FirestoreContract.COLLECTION_RECIPES)
                .orderBy(FirestoreContract.FIELD_NAME, Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Recipe> recipes = new ArrayList<>();
                    for (QueryDocumentSnapshot document : snapshot) {
                        recipes.add(document.toObject(Recipe.class));
                    }
                    callback.onSuccess(recipes);
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Fetches one recipe by document ID.
     *
     * <p>This is how the Recipe Detail screen loads its content: the list screen
     * passes only the ID through the Intent, and the detail screen reads the
     * full recipe here.</p>
     *
     * @param recipeId the document to load
     * @param callback receives the recipe, or null when no such document exists
     */
    public void getRecipeById(@NonNull String recipeId,
                              @NonNull RepositoryCallback<Recipe> callback) {
        firestore.collection(FirestoreContract.COLLECTION_RECIPES)
                .document(recipeId)
                .get()
                .addOnSuccessListener(document ->
                        callback.onSuccess(document.exists()
                                ? document.toObject(Recipe.class)
                                : null))
                .addOnFailureListener(callback::onError);
    }
}
