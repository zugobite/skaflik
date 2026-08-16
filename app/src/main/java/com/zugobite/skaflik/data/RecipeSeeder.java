package com.zugobite.skaflik.data;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import com.zugobite.skaflik.logic.IngredientMatcher;
import com.zugobite.skaflik.model.Recipe;
import com.zugobite.skaflik.model.RecipeIngredient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fills the shared {@code recipes} collection on first run.
 *
 * <p>The recipes below are built from a small, deliberately overlapping set of
 * everyday ingredients - egg, milk, butter, cheese, bread, flour, rice, pasta,
 * tomato, onion, garlic, potato, oil. That overlap is the point: a pantry with
 * a handful of staples in it unlocks several recipes, and adding or removing a
 * single ingredient visibly changes the suggestions.</p>
 *
 * <p>Water and salt are left out of the ingredient lists. Nobody tracks them in
 * a pantry, and requiring them would block almost every recipe for no useful
 * reason.</p>
 *
 * <p>Kept separate from the repositories so the seed data does not bury the
 * logic that reads it.</p>
 */
public final class RecipeSeeder {

    private static final String TAG = "RecipeSeeder";

    /** Static helper class; never instantiated. */
    private RecipeSeeder() {
    }

    /**
     * Seeds the recipe collection unless it has already been seeded.
     *
     * <p>A marker document at {@code meta/seed} guards the write, so this is
     * safe to call on every launch: the second and later runs cost one small
     * read and write nothing. Without that guard, reinstalling the app would
     * pile up duplicate recipes for everyone.</p>
     *
     * @param callback receives the number of recipes written, or 0 when seeding
     *                 was already done
     */
    public static void seedIfNeeded(@NonNull RepositoryCallback<Integer> callback) {
        FirebaseFirestore firestore = FirestoreProvider.get();
        DocumentReference seedMarker = firestore
                .collection(FirestoreContract.COLLECTION_META)
                .document(FirestoreContract.DOC_SEED);

        seedMarker.get()
                .addOnSuccessListener(snapshot -> {
                    boolean alreadySeeded = snapshot.exists()
                            && Boolean.TRUE.equals(
                                    snapshot.getBoolean(FirestoreContract.FIELD_SEEDED));
                    if (alreadySeeded) {
                        Log.d(TAG, "Recipes already seeded; skipping.");
                        callback.onSuccess(0);
                        return;
                    }
                    writeRecipes(firestore, seedMarker, callback);
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Writes every recipe and the seed marker in one batch.
     *
     * <p>A batch is atomic: either all 18 recipes and the marker land, or none
     * of them do. That rules out the state where a dropped connection leaves
     * half a recipe list behind a marker saying seeding is finished.</p>
     */
    private static void writeRecipes(@NonNull FirebaseFirestore firestore,
                                     @NonNull DocumentReference seedMarker,
                                     @NonNull RepositoryCallback<Integer> callback) {
        List<Recipe> recipes = buildRecipes();
        WriteBatch batch = firestore.batch();

        for (Recipe recipe : recipes) {
            DocumentReference document = firestore
                    .collection(FirestoreContract.COLLECTION_RECIPES)
                    .document();
            batch.set(document, recipe);
        }

        Map<String, Object> marker = new HashMap<>();
        marker.put(FirestoreContract.FIELD_SEEDED, true);
        marker.put(FirestoreContract.FIELD_SEEDED_AT, FieldValue.serverTimestamp());
        marker.put(FirestoreContract.FIELD_RECIPE_COUNT, recipes.size());
        batch.set(seedMarker, marker);

        batch.commit()
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Seeded " + recipes.size() + " recipes.");
                    callback.onSuccess(recipes.size());
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Builds an ingredient line, normalising the name as it goes.
     *
     * <p>Normalising here rather than by hand keeps the seed data honest - a
     * recipe's stored key can never drift from what the matcher would produce.</p>
     */
    private static RecipeIngredient ingredient(String name, double quantity, String unit) {
        return new RecipeIngredient(name, IngredientMatcher.normalize(name), quantity, unit);
    }

    /** The 18 starter recipes. */
    private static List<Recipe> buildRecipes() {
        List<Recipe> recipes = new ArrayList<>();

        recipes.add(new Recipe("Scrambled Eggs", 2, 10,
                Arrays.asList(
                        "Crack the eggs into a bowl and beat them with the milk.",
                        "Melt the butter in a pan over a low heat.",
                        "Pour in the eggs and stir slowly until just set.",
                        "Take off the heat while still slightly runny and serve."),
                Arrays.asList(
                        ingredient("Eggs", 4, "piece"),
                        ingredient("Milk", 30, "ml"),
                        ingredient("Butter", 15, "g"))));

        recipes.add(new Recipe("Cheese Omelette", 1, 12,
                Arrays.asList(
                        "Beat the eggs until the yolks and whites are combined.",
                        "Melt the butter in a non-stick pan over a medium heat.",
                        "Pour in the eggs and let them set around the edges.",
                        "Scatter the cheese over one half, fold, and slide onto a plate."),
                Arrays.asList(
                        ingredient("Eggs", 3, "piece"),
                        ingredient("Cheese", 50, "g"),
                        ingredient("Butter", 10, "g"))));

        recipes.add(new Recipe("Tomato Pasta", 2, 25,
                Arrays.asList(
                        "Boil the pasta until just tender.",
                        "Warm the oil in a pan and soften the garlic without browning it.",
                        "Add the chopped tomatoes and simmer for 15 minutes.",
                        "Drain the pasta, fold it through the sauce and serve."),
                Arrays.asList(
                        ingredient("Pasta", 200, "g"),
                        ingredient("Tomatoes", 4, "piece"),
                        ingredient("Garlic", 2, "piece"),
                        ingredient("Oil", 15, "ml"))));

        recipes.add(new Recipe("Garlic Butter Rice", 2, 20,
                Arrays.asList(
                        "Rinse the rice until the water runs clear.",
                        "Melt the butter and cook the garlic gently for a minute.",
                        "Stir in the rice, add water, cover and simmer until absorbed.",
                        "Rest for five minutes, then fluff with a fork."),
                Arrays.asList(
                        ingredient("Rice", 200, "g"),
                        ingredient("Garlic", 3, "piece"),
                        ingredient("Butter", 25, "g"))));

        recipes.add(new Recipe("Egg Fried Rice", 2, 15,
                Arrays.asList(
                        "Heat the oil in a wide pan until shimmering.",
                        "Fry the diced onion until translucent.",
                        "Push the onion aside, scramble the eggs in the space.",
                        "Add the cooked rice, toss everything together and serve hot."),
                Arrays.asList(
                        ingredient("Rice", 250, "g"),
                        ingredient("Eggs", 2, "piece"),
                        ingredient("Onion", 1, "piece"),
                        ingredient("Oil", 15, "ml"))));

        recipes.add(new Recipe("Pancakes", 4, 20,
                Arrays.asList(
                        "Whisk the flour, eggs and milk into a smooth batter.",
                        "Rest the batter for ten minutes.",
                        "Heat a little oil in a pan and pour in a ladle of batter.",
                        "Cook until bubbles form, flip, and cook the other side."),
                Arrays.asList(
                        ingredient("Flour", 150, "g"),
                        ingredient("Milk", 250, "ml"),
                        ingredient("Eggs", 2, "piece"),
                        ingredient("Oil", 15, "ml"))));

        recipes.add(new Recipe("Cheese Toastie", 1, 8,
                Arrays.asList(
                        "Butter the outer side of both slices of bread.",
                        "Pile the cheese between them, buttered sides facing out.",
                        "Cook in a hot pan until golden, pressing down gently.",
                        "Flip, brown the other side, and cut in half."),
                Arrays.asList(
                        ingredient("Bread", 2, "piece"),
                        ingredient("Cheese", 60, "g"),
                        ingredient("Butter", 15, "g"))));

        recipes.add(new Recipe("Tomato Soup", 3, 35,
                Arrays.asList(
                        "Soften the onion and garlic in the oil over a low heat.",
                        "Add the roughly chopped tomatoes and cook until collapsed.",
                        "Simmer for 20 minutes, then blend until smooth.",
                        "Season to taste and serve hot."),
                Arrays.asList(
                        ingredient("Tomatoes", 6, "piece"),
                        ingredient("Onion", 1, "piece"),
                        ingredient("Garlic", 2, "piece"),
                        ingredient("Oil", 20, "ml"))));

        recipes.add(new Recipe("French Toast", 2, 15,
                Arrays.asList(
                        "Beat the eggs with the milk in a shallow dish.",
                        "Soak each slice of bread until saturated but not falling apart.",
                        "Fry in melted butter until golden on both sides.",
                        "Serve immediately."),
                Arrays.asList(
                        ingredient("Bread", 4, "piece"),
                        ingredient("Eggs", 2, "piece"),
                        ingredient("Milk", 100, "ml"),
                        ingredient("Butter", 20, "g"))));

        recipes.add(new Recipe("Macaroni Cheese", 4, 40,
                Arrays.asList(
                        "Boil the pasta until just short of tender, then drain.",
                        "Melt the butter, stir in the flour and cook for a minute.",
                        "Whisk in the milk gradually until the sauce thickens.",
                        "Add most of the cheese, fold in the pasta, top with the rest.",
                        "Bake until bubbling and browned on top."),
                Arrays.asList(
                        ingredient("Pasta", 250, "g"),
                        ingredient("Cheese", 150, "g"),
                        ingredient("Milk", 300, "ml"),
                        ingredient("Flour", 25, "g"),
                        ingredient("Butter", 25, "g"))));

        recipes.add(new Recipe("Spanish Omelette", 3, 35,
                Arrays.asList(
                        "Slice the potatoes thinly and the onion finely.",
                        "Cook both gently in plenty of oil until soft, not coloured.",
                        "Beat the eggs, then stir the drained potato and onion through.",
                        "Return to the pan and cook slowly until set, flipping once."),
                Arrays.asList(
                        ingredient("Potatoes", 3, "piece"),
                        ingredient("Eggs", 4, "piece"),
                        ingredient("Onion", 1, "piece"),
                        ingredient("Oil", 45, "ml"))));

        recipes.add(new Recipe("Cheesy Baked Potatoes", 2, 60,
                Arrays.asList(
                        "Prick the potatoes all over and rub them with oil.",
                        "Bake for about an hour until the skins are crisp.",
                        "Split them open and work the butter into the fluffy insides.",
                        "Top with grated cheese and return briefly to the oven."),
                Arrays.asList(
                        ingredient("Potatoes", 2, "piece"),
                        ingredient("Cheese", 80, "g"),
                        ingredient("Butter", 20, "g"),
                        ingredient("Oil", 10, "ml"))));

        recipes.add(new Recipe("Onion Soup", 4, 50,
                Arrays.asList(
                        "Slice the onions thinly and cook them slowly in the butter.",
                        "Keep going for 30 minutes until deeply golden and sweet.",
                        "Stir in the flour, then add water and simmer.",
                        "Serve with toasted bread floated on top."),
                Arrays.asList(
                        ingredient("Onions", 4, "piece"),
                        ingredient("Butter", 40, "g"),
                        ingredient("Flour", 15, "g"),
                        ingredient("Bread", 2, "piece"))));

        recipes.add(new Recipe("Garlic Bread", 4, 18,
                Arrays.asList(
                        "Crush the garlic and beat it into the softened butter.",
                        "Slice the bread most of the way through at intervals.",
                        "Push the garlic butter into each cut.",
                        "Wrap in foil and bake until the crust is crisp."),
                Arrays.asList(
                        ingredient("Bread", 4, "piece"),
                        ingredient("Butter", 60, "g"),
                        ingredient("Garlic", 4, "piece"))));

        recipes.add(new Recipe("Pasta with Garlic and Oil", 2, 20,
                Arrays.asList(
                        "Boil the pasta, keeping a cup of the cooking water.",
                        "Warm the oil gently and infuse it with the sliced garlic.",
                        "Toss the drained pasta through the oil.",
                        "Loosen with the reserved water and finish with cheese."),
                Arrays.asList(
                        ingredient("Pasta", 200, "g"),
                        ingredient("Garlic", 4, "piece"),
                        ingredient("Oil", 45, "ml"),
                        ingredient("Cheese", 40, "g"))));

        recipes.add(new Recipe("Rice Pudding", 4, 55,
                Arrays.asList(
                        "Put the rice, milk and sugar into a heavy pan.",
                        "Bring to a bare simmer, stirring to stop it catching.",
                        "Cook very gently for 45 minutes until thick and creamy.",
                        "Dot with butter and serve warm."),
                Arrays.asList(
                        ingredient("Rice", 100, "g"),
                        ingredient("Milk", 600, "ml"),
                        ingredient("Sugar", 60, "g"),
                        ingredient("Butter", 15, "g"))));

        recipes.add(new Recipe("Flatbread", 4, 30,
                Arrays.asList(
                        "Mix the flour and oil with enough water to form a soft dough.",
                        "Knead for five minutes, then rest for twenty.",
                        "Divide and roll each piece out thinly.",
                        "Cook in a dry hot pan until puffed and blistered."),
                Arrays.asList(
                        ingredient("Flour", 250, "g"),
                        ingredient("Oil", 30, "ml"))));

        recipes.add(new Recipe("Tomato and Cheese Bake", 3, 40,
                Arrays.asList(
                        "Slice the tomatoes and tear the bread into rough pieces.",
                        "Layer bread and tomato in an oiled dish.",
                        "Scatter the cheese over the top.",
                        "Bake for 30 minutes until crisp on top and soft beneath."),
                Arrays.asList(
                        ingredient("Tomatoes", 5, "piece"),
                        ingredient("Cheese", 100, "g"),
                        ingredient("Bread", 3, "piece"),
                        ingredient("Oil", 20, "ml"))));

        return recipes;
    }
}
