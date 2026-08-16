package com.zugobite.skaflik.logic;

import androidx.annotation.NonNull;

import com.zugobite.skaflik.model.PantryItem;
import com.zugobite.skaflik.model.Recipe;
import com.zugobite.skaflik.model.RecipeIngredient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Decides which recipes the user can cook right now.
 *
 * <p><b>The rule:</b> a recipe is suggested if and only if <em>every</em>
 * ingredient it requires is in the pantry in at least the required quantity.
 * Four out of five ingredients is not a match. There is no partial credit and
 * no "close enough".</p>
 *
 * <p>Two ingredients are the same thing when their normalised names are equal
 * (see {@link IngredientMatcher}), and the pantry has enough when both amounts
 * convert to the same base unit and the pantry's is greater than or equal to
 * the recipe's (see {@link UnitConverter}).</p>
 *
 * <p>Recipes missing exactly one ingredient are collected separately as
 * "almost there". That list is never merged into the suggestions – it exists
 * so the UI can show it under its own heading.</p>
 */
public final class RecipeSuggestionEngine {

    /** Static helper class; never instantiated. */
    private RecipeSuggestionEngine() {
    }

    /**
     * What the engine produces: the strict matches, and the near misses kept
     * firmly apart from them.
     */
    public static class SuggestionResult {

        private final List<Recipe> suggested;
        private final List<Recipe> almostThere;

        SuggestionResult(List<Recipe> suggested, List<Recipe> almostThere) {
            this.suggested = Collections.unmodifiableList(suggested);
            this.almostThere = Collections.unmodifiableList(almostThere);
        }

        /** Recipes the pantry fully satisfies. This is the main list. */
        @NonNull
        public List<Recipe> getSuggested() {
            return suggested;
        }

        /** Recipes missing exactly one ingredient. Shown separately, or not at all. */
        @NonNull
        public List<Recipe> getAlmostThere() {
            return almostThere;
        }

        /** True when nothing at all matched, so the UI can show its empty state. */
        public boolean isEmpty() {
            return suggested.isEmpty();
        }
    }

    /**
     * Sorts every recipe into "can cook", "one short", or neither.
     *
     * @param pantryItems what the user has
     * @param recipes     every known recipe
     * @return the strict matches and the near misses
     */
    @NonNull
    public static SuggestionResult suggestRecipes(@NonNull List<PantryItem> pantryItems,
                                                  @NonNull List<Recipe> recipes) {
        // Index the pantry once rather than scanning it per ingredient.
        Map<String, List<PantryItem>> pantryByName = indexPantryByNormalisedName(pantryItems);

        List<Recipe> suggested = new ArrayList<>();
        List<Recipe> almostThere = new ArrayList<>();

        for (Recipe recipe : recipes) {
            int missingCount = countMissingIngredients(recipe, pantryByName);

            if (missingCount == 0) {
                suggested.add(recipe);
            } else if (missingCount == 1) {
                almostThere.add(recipe);
            }
            // Two or more missing: the recipe does not appear anywhere.
        }

        return new SuggestionResult(suggested, almostThere);
    }

    /**
     * Lists the ingredients a recipe needs that the pantry cannot cover.
     *
     * <p>The Recipe Detail screen uses this to mark each line as had or needed.</p>
     *
     * @param recipe      the recipe to check
     * @param pantryItems what the user has
     * @return the unsatisfied ingredients, empty when the recipe is cookable
     */
    @NonNull
    public static List<RecipeIngredient> findMissingIngredients(@NonNull Recipe recipe,
                                                                @NonNull List<PantryItem> pantryItems) {
        Map<String, List<PantryItem>> pantryByName = indexPantryByNormalisedName(pantryItems);
        List<RecipeIngredient> missing = new ArrayList<>();

        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            if (!isSatisfied(ingredient, pantryByName)) {
                missing.add(ingredient);
            }
        }
        return missing;
    }

    /** True when the pantry covers every ingredient this recipe needs. */
    public static boolean canCook(@NonNull Recipe recipe, @NonNull List<PantryItem> pantryItems) {
        return findMissingIngredients(recipe, pantryItems).isEmpty();
    }

    /**
     * How much of an ingredient the pantry holds, expressed in that
     * ingredient's own unit.
     *
     * <p>The Recipe Detail screen uses this to show "you have 2 piece" against
     * a line needing 4. Only pantry rows in a comparable unit family count, for
     * the same reason they are ignored when deciding whether a recipe matches.</p>
     *
     * @param ingredient  the ingredient to measure against
     * @param pantryItems what the user has
     * @return the amount held in the ingredient's unit, or 0 when none applies
     */
    public static double availableQuantityFor(@NonNull RecipeIngredient ingredient,
                                              @NonNull List<PantryItem> pantryItems) {
        Map<String, List<PantryItem>> pantryByName = indexPantryByNormalisedName(pantryItems);
        String requiredName = normalisedNameOf(ingredient);

        List<PantryItem> candidates = pantryByName.get(requiredName);
        if (candidates == null || candidates.isEmpty()) {
            return 0;
        }

        double perIngredientUnit = UnitConverter.toBaseUnit(1, ingredient.getUnit());
        if (perIngredientUnit <= 0) {
            // Unknown unit, so there is no meaningful figure to report.
            return 0;
        }

        double availableInBase = 0;
        for (PantryItem item : candidates) {
            if (!UnitConverter.areComparable(item.getUnit(), ingredient.getUnit())) {
                continue;
            }
            double itemInBase = UnitConverter.toBaseUnit(item.getQuantity(), item.getUnit());
            if (itemInBase > 0) {
                availableInBase += itemInBase;
            }
        }

        // Convert back out of base units into what the recipe line is written in.
        return availableInBase / perIngredientUnit;
    }

    // --- Internals ---

    /**
     * Groups the pantry by normalised name.
     *
     * <p>A name can map to more than one item: the Add screen only offers to
     * merge duplicates whose units are comparable, so "500 g rice" and
     * "2 cup rice" can legitimately sit side by side.</p>
     */
    @NonNull
    private static Map<String, List<PantryItem>> indexPantryByNormalisedName(
            @NonNull List<PantryItem> pantryItems) {
        Map<String, List<PantryItem>> index = new HashMap<>();

        for (PantryItem item : pantryItems) {
            // Older rows may predate the stored key, so fall back to computing it.
            String key = item.getNameNorm() == null || item.getNameNorm().isEmpty()
                    ? IngredientMatcher.normalize(item.getName())
                    : item.getNameNorm();

            if (key.isEmpty()) {
                continue;
            }
            List<PantryItem> matches = index.get(key);
            if (matches == null) {
                matches = new ArrayList<>();
                index.put(key, matches);
            }
            matches.add(item);
        }
        return index;
    }

    /**
     * The matching key for a recipe ingredient.
     *
     * <p>Prefers the key stored with the data, falling back to computing it so
     * that rows written before the field existed still match.</p>
     */
    @NonNull
    private static String normalisedNameOf(@NonNull RecipeIngredient ingredient) {
        String stored = ingredient.getNameNorm();
        return stored == null || stored.isEmpty()
                ? IngredientMatcher.normalize(ingredient.getName())
                : stored;
    }

    /** Counts how many of a recipe's ingredients the pantry cannot cover. */
    private static int countMissingIngredients(@NonNull Recipe recipe,
                                               @NonNull Map<String, List<PantryItem>> pantryByName) {
        List<RecipeIngredient> ingredients = recipe.getIngredients();

        // A recipe with no ingredients is bad data, not something anyone can
        // cook. Treating it as missing keeps it out of the suggestions.
        if (ingredients == null || ingredients.isEmpty()) {
            return 1;
        }

        int missing = 0;
        for (RecipeIngredient ingredient : ingredients) {
            if (!isSatisfied(ingredient, pantryByName)) {
                missing++;
            }
        }
        return missing;
    }

    /**
     * Decides whether the pantry covers one ingredient.
     *
     * <p>Amounts are summed across every pantry row with the same name whose
     * unit is comparable to the one the recipe asks for, so two half-full bags
     * of flour together satisfy a recipe needing a full one. Rows in an
     * incompatible family (grams against millilitres) contribute nothing –
     * converting between mass and volume needs a per-ingredient density this
     * app does not hold, and claiming a match on a guess would be worse than
     * hiding a cookable recipe.</p>
     */
    private static boolean isSatisfied(@NonNull RecipeIngredient ingredient,
                                       @NonNull Map<String, List<PantryItem>> pantryByName) {
        List<PantryItem> candidates = pantryByName.get(normalisedNameOf(ingredient));
        if (candidates == null || candidates.isEmpty()) {
            // Not in the pantry at all.
            return false;
        }

        double requiredInBase = UnitConverter.toBaseUnit(
                ingredient.getQuantity(), ingredient.getUnit());
        if (requiredInBase < 0) {
            // The recipe asks in a unit we cannot convert, so we cannot claim
            // the pantry satisfies it.
            return false;
        }

        double availableInBase = 0;
        for (PantryItem item : candidates) {
            if (!UnitConverter.areComparable(item.getUnit(), ingredient.getUnit())) {
                continue;
            }
            double itemInBase = UnitConverter.toBaseUnit(item.getQuantity(), item.getUnit());
            if (itemInBase > 0) {
                availableInBase += itemInBase;
            }
        }

        return availableInBase >= requiredInBase;
    }
}
