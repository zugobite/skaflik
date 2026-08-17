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

    /**
     * Which revision of the recipes below this build carries.
     *
     * <p>Recorded on the seed marker so it is possible to tell which text a
     * collection holds. It is not a migration: the security rules close the
     * recipe write path for good once the marker exists, so changed seed
     * content only reaches a database that has already been seeded if the
     * {@code recipes} collection and {@code meta/seed} are cleared first.
     * Version 2 rewrote every set of preparation steps into fuller
     * instructions.</p>
     */
    private static final int SEED_VERSION = 2;

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

    /** The seed marker's contents after writing the given number of recipes. */
    @NonNull
    private static Map<String, Object> markerFor(int recipeCount) {
        Map<String, Object> marker = new HashMap<>();
        marker.put(FirestoreContract.FIELD_SEEDED, true);
        marker.put(FirestoreContract.FIELD_SEEDED_AT, FieldValue.serverTimestamp());
        marker.put(FirestoreContract.FIELD_RECIPE_COUNT, recipeCount);
        marker.put(FirestoreContract.FIELD_SEED_VERSION, SEED_VERSION);
        return marker;
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

        batch.set(seedMarker, markerFor(recipes.size()));

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
                        "Crack the 4 eggs into a bowl, add the milk, and beat with a fork for about 20 seconds, until no streaks of white are left.",
                        "Melt the butter in a non-stick pan over a low heat, swirling it so it coats the base without browning.",
                        "Pour in the eggs and leave them for 20 seconds, until a thin layer begins to set on the bottom.",
                        "Stir slowly with a spatula, pushing the set egg into the middle and letting the liquid run underneath.",
                        "Keep going for 2 to 3 minutes, until the eggs hold together in soft curds but still look glossy.",
                        "Take the pan off the heat while they are slightly runnier than you want; they carry on cooking on the plate. Serve at once."),
                Arrays.asList(
                        ingredient("Eggs", 4, "piece"),
                        ingredient("Milk", 30, "ml"),
                        ingredient("Butter", 15, "g"))));

        recipes.add(new Recipe("Cheese Omelette", 1, 12,
                Arrays.asList(
                        "Grate the cheese and set it beside the hob, so it is ready the moment the eggs are.",
                        "Beat the 3 eggs until the yolks and whites are fully combined and the surface is a little frothy.",
                        "Melt the butter in a 20cm non-stick pan over a medium heat until it foams but does not colour.",
                        "Pour in the eggs. As the edges set, draw them into the centre with a spatula and tilt the pan so the raw egg fills the gap.",
                        "When the top is just barely wet, after about 2 minutes, scatter the cheese over one half.",
                        "Fold the bare half over the cheese, count to ten while it melts, then slide the omelette onto a plate."),
                Arrays.asList(
                        ingredient("Eggs", 3, "piece"),
                        ingredient("Cheese", 50, "g"),
                        ingredient("Butter", 10, "g"))));

        recipes.add(new Recipe("Tomato Pasta", 2, 25,
                Arrays.asList(
                        "Bring a large pan of salted water to a rolling boil for the pasta.",
                        "Meanwhile chop the tomatoes roughly and slice the garlic thinly.",
                        "Warm the oil in a wide pan over a low heat, add the garlic, and cook for 1 to 2 minutes until fragrant. Do not let it brown or it will turn bitter.",
                        "Add the tomatoes, raise the heat to medium, and simmer for 15 minutes, stirring now and then, until they collapse into a thick sauce.",
                        "Cook the pasta in the boiling water until just tender with a little bite, then scoop out a cup of the cooking water before draining.",
                        "Fold the pasta through the sauce over a low heat for a minute, loosening it with splashes of the reserved water until it coats every strand. Season and serve."),
                Arrays.asList(
                        ingredient("Pasta", 200, "g"),
                        ingredient("Tomatoes", 4, "piece"),
                        ingredient("Garlic", 2, "piece"),
                        ingredient("Oil", 15, "ml"))));

        recipes.add(new Recipe("Garlic Butter Rice", 2, 20,
                Arrays.asList(
                        "Rinse the rice in a sieve under cold water, stirring with your hand, until the water runs clear. This stops it turning sticky.",
                        "Crush or finely chop the garlic.",
                        "Melt the butter in a pan with a tight-fitting lid over a medium-low heat, add the garlic, and cook for 1 minute until it smells sweet.",
                        "Stir in the drained rice and turn it in the butter for another minute, until the grains look glossy.",
                        "Pour in 400ml water, season with salt, and bring to the boil. Cover, drop the heat to its lowest setting, and leave for 12 minutes without lifting the lid.",
                        "Take the pan off the heat and rest it, still covered, for 5 minutes, then fluff the grains apart with a fork."),
                Arrays.asList(
                        ingredient("Rice", 200, "g"),
                        ingredient("Garlic", 3, "piece"),
                        ingredient("Butter", 25, "g"))));

        recipes.add(new Recipe("Egg Fried Rice", 2, 15,
                Arrays.asList(
                        "Use cold cooked rice; freshly cooked rice steams instead of frying and turns to mush.",
                        "Dice the onion finely and beat the eggs in a small bowl.",
                        "Heat the oil in a wide pan or wok over a high heat until it shimmers.",
                        "Fry the onion for 2 to 3 minutes, stirring often, until translucent and just starting to catch at the edges.",
                        "Push the onion to one side, pour the eggs into the clear space, and scramble them for 30 seconds until barely set.",
                        "Add the rice, breaking up any clumps with the back of the spoon, and toss everything together over a high heat for 3 minutes until piping hot and beginning to crisp. Serve straight away."),
                Arrays.asList(
                        ingredient("Rice", 250, "g"),
                        ingredient("Eggs", 2, "piece"),
                        ingredient("Onion", 1, "piece"),
                        ingredient("Oil", 15, "ml"))));

        recipes.add(new Recipe("Pancakes", 4, 20,
                Arrays.asList(
                        "Put the flour in a bowl and make a well in the middle.",
                        "Crack in the eggs and add a splash of the milk, then whisk from the centre outwards, drawing in the flour a little at a time.",
                        "Pour in the rest of the milk in two or three additions, whisking until the batter is smooth and has the consistency of single cream.",
                        "Rest the batter for 10 minutes; this lets the flour absorb the liquid and gives a more tender pancake.",
                        "Heat a little oil in a non-stick pan over a medium heat, wiping out any excess with kitchen paper, then pour in a ladle of batter and swirl to coat the base.",
                        "Cook for 1 to 2 minutes, until bubbles form across the surface and the underside is golden, then flip and give it another minute.",
                        "Repeat with the remaining batter, stacking the pancakes on a warm plate as you go."),
                Arrays.asList(
                        ingredient("Flour", 150, "g"),
                        ingredient("Milk", 250, "ml"),
                        ingredient("Eggs", 2, "piece"),
                        ingredient("Oil", 15, "ml"))));

        recipes.add(new Recipe("Cheese Toastie", 1, 8,
                Arrays.asList(
                        "Grate the cheese; grated melts far more evenly than sliced.",
                        "Butter one side of each slice of bread, right to the edges.",
                        "Lay one slice butter side down, pile the cheese on evenly, and top with the second slice, butter side up.",
                        "Put the sandwich into a cold non-stick pan and set it over a medium-low heat. Starting cold gives the cheese time to melt before the bread burns.",
                        "Cook for 3 to 4 minutes, pressing down gently with a spatula, until the underside is golden brown.",
                        "Flip, brown the other side for 2 to 3 minutes, then rest for a minute before cutting in half so the cheese does not run out."),
                Arrays.asList(
                        ingredient("Bread", 2, "piece"),
                        ingredient("Cheese", 60, "g"),
                        ingredient("Butter", 15, "g"))));

        recipes.add(new Recipe("Tomato Soup", 3, 35,
                Arrays.asList(
                        "Dice the onion, slice the garlic, and roughly chop the tomatoes.",
                        "Warm the oil in a large pan over a low heat, add the onion with a pinch of salt, and cook gently for 8 to 10 minutes until soft and translucent but not coloured.",
                        "Add the garlic and cook for 1 minute more.",
                        "Tip in the tomatoes, raise the heat, and cook for 5 minutes until they start to collapse.",
                        "Pour in 500ml water, bring to a simmer, and cook uncovered for 20 minutes.",
                        "Blend until smooth, with a stick blender in the pan or in a jug blender filled no more than halfway with the hot soup.",
                        "Season with salt and pepper, reheat if needed, and serve hot."),
                Arrays.asList(
                        ingredient("Tomatoes", 6, "piece"),
                        ingredient("Onion", 1, "piece"),
                        ingredient("Garlic", 2, "piece"),
                        ingredient("Oil", 20, "ml"))));

        recipes.add(new Recipe("French Toast", 2, 15,
                Arrays.asList(
                        "Beat the eggs and milk together in a shallow dish wide enough to hold a slice of bread flat.",
                        "Lay a slice in the mixture and leave it for 20 to 30 seconds a side, until soaked through but still holding together. Slightly stale bread works best.",
                        "Melt a third of the butter in a non-stick pan over a medium heat until it foams.",
                        "Fry two slices at a time for 2 to 3 minutes, until deep golden underneath.",
                        "Flip and cook the other side for another 2 minutes, adding more butter to the pan for each batch.",
                        "Serve immediately, while the outside is still crisp."),
                Arrays.asList(
                        ingredient("Bread", 4, "piece"),
                        ingredient("Eggs", 2, "piece"),
                        ingredient("Milk", 100, "ml"),
                        ingredient("Butter", 20, "g"))));

        recipes.add(new Recipe("Macaroni Cheese", 4, 40,
                Arrays.asList(
                        "Heat the oven to 200C (180C fan) and grate the cheese.",
                        "Boil the pasta in salted water for 2 minutes less than the packet says, so it stays firm; it finishes cooking in the oven. Drain and set aside.",
                        "Melt the butter in a saucepan over a medium heat, stir in the flour, and cook the paste for 1 minute, stirring constantly.",
                        "Add the milk a splash at a time, whisking smooth after each addition before adding more. Rushing this is what makes the sauce lumpy.",
                        "Simmer gently for 3 to 4 minutes, stirring, until the sauce thickly coats the back of a spoon.",
                        "Take off the heat, stir in three quarters of the cheese until melted, and season well.",
                        "Fold the pasta through the sauce, tip into an ovenproof dish, and scatter the remaining cheese over the top.",
                        "Bake for 20 minutes, until bubbling at the edges and browned on top. Rest for 5 minutes before serving."),
                Arrays.asList(
                        ingredient("Pasta", 250, "g"),
                        ingredient("Cheese", 150, "g"),
                        ingredient("Milk", 300, "ml"),
                        ingredient("Flour", 25, "g"),
                        ingredient("Butter", 25, "g"))));

        recipes.add(new Recipe("Spanish Omelette", 3, 35,
                Arrays.asList(
                        "Peel the potatoes and slice them into thin rounds, about 3mm thick. Slice the onion finely.",
                        "Warm the oil in a 20cm non-stick pan over a low heat, add the potato and onion with a good pinch of salt, and cook gently for 15 to 20 minutes.",
                        "Turn them over every few minutes with a spatula. They should end up soft enough to break with the edge of the spatula and barely coloured.",
                        "Drain them in a sieve set over a bowl, keeping the oil.",
                        "Beat the eggs in a large bowl, season, then stir the warm potato and onion through and leave to stand for 5 minutes.",
                        "Return a tablespoon of the reserved oil to the pan over a medium heat, pour in the mixture, and cook for 4 to 5 minutes until the edges set and only the middle wobbles.",
                        "Cover the pan with a plate, turn the whole thing over, and slide the omelette back in to cook the other side for 3 minutes. Serve warm or at room temperature."),
                Arrays.asList(
                        ingredient("Potatoes", 3, "piece"),
                        ingredient("Eggs", 4, "piece"),
                        ingredient("Onion", 1, "piece"),
                        ingredient("Oil", 45, "ml"))));

        recipes.add(new Recipe("Cheesy Baked Potatoes", 2, 60,
                Arrays.asList(
                        "Heat the oven to 220C (200C fan).",
                        "Scrub the potatoes, dry them well, and prick each one several times with a fork so steam can escape.",
                        "Rub them all over with the oil and a little salt, which is what gives a crisp skin.",
                        "Bake directly on the oven shelf for 50 to 60 minutes, until the skins are crisp and a knife slides into the centre with no resistance.",
                        "Meanwhile grate the cheese.",
                        "Split each potato open along the top, squeeze the ends to open it up, and fork the butter through the fluffy inside.",
                        "Pile the cheese in and return to the oven for 3 to 4 minutes, until melted."),
                Arrays.asList(
                        ingredient("Potatoes", 2, "piece"),
                        ingredient("Cheese", 80, "g"),
                        ingredient("Butter", 20, "g"),
                        ingredient("Oil", 10, "ml"))));

        recipes.add(new Recipe("Onion Soup", 4, 50,
                Arrays.asList(
                        "Halve and thinly slice all 4 onions.",
                        "Melt the butter in a heavy pan over a medium-low heat, add the onions and a good pinch of salt, and stir to coat.",
                        "Cook for 30 minutes, stirring every few minutes and scraping the brown film off the base as it forms. The onions should collapse and turn deep golden and sweet; rushing this on a high heat burns them instead.",
                        "Stir in the flour and cook for 2 minutes, until no dry flour is visible.",
                        "Pour in 800ml water a little at a time, stirring to keep it smooth, then simmer gently for 15 minutes.",
                        "Toast the bread until crisp on both sides.",
                        "Season the soup to taste, ladle into bowls, and float a piece of toast on each."),
                Arrays.asList(
                        ingredient("Onions", 4, "piece"),
                        ingredient("Butter", 40, "g"),
                        ingredient("Flour", 15, "g"),
                        ingredient("Bread", 2, "piece"))));

        recipes.add(new Recipe("Garlic Bread", 4, 18,
                Arrays.asList(
                        "Heat the oven to 200C (180C fan) and leave the butter somewhere warm until soft enough to stir.",
                        "Crush the garlic to a paste with the flat of a knife and a pinch of salt.",
                        "Beat the garlic into the softened butter until evenly mixed.",
                        "Slice the bread at 2cm intervals, cutting down but not all the way through, so the loaf stays in one piece.",
                        "Push the garlic butter into each cut, spreading it on both faces.",
                        "Wrap the loaf in foil and bake for 10 minutes, then open the foil out and give it 5 minutes more so the crust crisps up. Serve hot."),
                Arrays.asList(
                        ingredient("Bread", 4, "piece"),
                        ingredient("Butter", 60, "g"),
                        ingredient("Garlic", 4, "piece"))));

        recipes.add(new Recipe("Pasta with Garlic and Oil", 2, 20,
                Arrays.asList(
                        "Bring a large pan of well-salted water to the boil and slice the garlic as thinly as you can.",
                        "Cook the pasta until just tender with a little bite.",
                        "While it cooks, warm the oil and garlic together in a cold wide pan over a low heat.",
                        "Let the garlic sizzle very gently for 3 to 4 minutes until pale gold. Take the pan off the heat the moment it starts to colour further, as burnt garlic is bitter.",
                        "Scoop out a cupful of the pasta water before draining.",
                        "Add the drained pasta to the oil with a good splash of the cooking water and toss hard over a medium heat for a minute, until the water and oil come together into a light sauce that clings.",
                        "Grate the cheese over, toss once more, and serve."),
                Arrays.asList(
                        ingredient("Pasta", 200, "g"),
                        ingredient("Garlic", 4, "piece"),
                        ingredient("Oil", 45, "ml"),
                        ingredient("Cheese", 40, "g"))));

        recipes.add(new Recipe("Rice Pudding", 4, 55,
                Arrays.asList(
                        "Put the rice, milk and sugar into a heavy-based pan; a thin pan will catch and scorch.",
                        "Bring slowly to a bare simmer over a medium-low heat, stirring so the sugar dissolves and the rice does not sink and stick.",
                        "Turn the heat to its lowest setting and cook uncovered for 45 minutes.",
                        "Stir every 5 minutes, reaching into the corners of the pan, until the rice is soft and the milk has thickened to a loose, creamy consistency. It firms up as it cools, so stop while it still looks slightly slack.",
                        "Take the pan off the heat and stir the butter through until it melts.",
                        "Serve warm."),
                Arrays.asList(
                        ingredient("Rice", 100, "g"),
                        ingredient("Milk", 600, "ml"),
                        ingredient("Sugar", 60, "g"),
                        ingredient("Butter", 15, "g"))));

        recipes.add(new Recipe("Flatbread", 4, 30,
                Arrays.asList(
                        "Put the flour and a teaspoon of salt in a bowl, add the oil, and rub it through with your fingertips.",
                        "Add about 150ml warm water a little at a time, mixing until it comes together into a soft dough that is slightly tacky but not sticky.",
                        "Knead on a lightly floured surface for 5 minutes, until smooth and springy.",
                        "Cover with a cloth and rest for 20 minutes; this relaxes the dough so it rolls out without pinging back.",
                        "Divide into 8 pieces and roll each one out to about 2mm thick.",
                        "Heat a dry frying pan over a high heat until a drop of water skips across it.",
                        "Cook each flatbread for 1 to 2 minutes a side, until it puffs up and blisters brown in patches. Stack them under a cloth to stay soft."),
                Arrays.asList(
                        ingredient("Flour", 250, "g"),
                        ingredient("Oil", 30, "ml"))));

        recipes.add(new Recipe("Tomato and Cheese Bake", 3, 40,
                Arrays.asList(
                        "Heat the oven to 190C (170C fan) and brush an ovenproof dish with a little of the oil.",
                        "Slice the tomatoes about 1cm thick and tear the bread into rough chunks.",
                        "Grate the cheese.",
                        "Layer half the bread in the dish, then half the tomatoes, seasoning with salt and pepper as you go, and repeat.",
                        "Drizzle the remaining oil over the top so the exposed bread crisps rather than dries out.",
                        "Scatter the cheese evenly over the surface.",
                        "Bake for 30 minutes, until the top is crisp and golden and the tomatoes below have collapsed into the bread. Rest for 5 minutes before serving."),
                Arrays.asList(
                        ingredient("Tomatoes", 5, "piece"),
                        ingredient("Cheese", 100, "g"),
                        ingredient("Bread", 3, "piece"),
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
