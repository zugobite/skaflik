package com.zugobite.skaflik;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.zugobite.skaflik.logic.IngredientMatcher;
import com.zugobite.skaflik.logic.RecipeSuggestionEngine;
import com.zugobite.skaflik.model.PantryItem;
import com.zugobite.skaflik.model.Recipe;
import com.zugobite.skaflik.model.RecipeIngredient;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Tests for the strict-matching rule.
 *
 * <p>Each case in the assignment's acceptance checklist has a test here, since
 * this is the logic the marker exercises directly.</p>
 */
public class RecipeSuggestionEngineTest {

    // --- Builders, so each test reads as the scenario it describes ---

    private PantryItem pantryItem(String name, double quantity, String unit) {
        return new PantryItem(name, IngredientMatcher.normalize(name), quantity, unit, null);
    }

    private RecipeIngredient ingredient(String name, double quantity, String unit) {
        return new RecipeIngredient(name, IngredientMatcher.normalize(name), quantity, unit);
    }

    private Recipe recipe(String name, RecipeIngredient... ingredients) {
        return new Recipe(name, 2, 20,
                Collections.singletonList("Cook it."),
                Arrays.asList(ingredients));
    }

    // --- The core rule ---

    @Test
    public void suggests_recipeWhenEveryIngredientIsPresent() {
        List<PantryItem> pantry = Arrays.asList(
                pantryItem("Eggs", 6, "piece"),
                pantryItem("Milk", 500, "ml"),
                pantryItem("Butter", 250, "g"));

        Recipe scrambledEggs = recipe("Scrambled Eggs",
                ingredient("Eggs", 4, "piece"),
                ingredient("Milk", 30, "ml"),
                ingredient("Butter", 15, "g"));

        RecipeSuggestionEngine.SuggestionResult result =
                RecipeSuggestionEngine.suggestRecipes(pantry, Collections.singletonList(scrambledEggs));

        assertEquals(1, result.getSuggested().size());
        assertEquals("Scrambled Eggs", result.getSuggested().get(0).getName());
    }

    /** The checklist's headline case: 5 required, 4 present must not appear. */
    @Test
    public void excludes_recipeMissingOneOfFiveIngredients() {
        List<PantryItem> pantry = Arrays.asList(
                pantryItem("Pasta", 500, "g"),
                pantryItem("Cheese", 200, "g"),
                pantryItem("Milk", 1, "l"),
                pantryItem("Flour", 500, "g"));
        // No butter.

        Recipe macaroniCheese = recipe("Macaroni Cheese",
                ingredient("Pasta", 250, "g"),
                ingredient("Cheese", 150, "g"),
                ingredient("Milk", 300, "ml"),
                ingredient("Flour", 25, "g"),
                ingredient("Butter", 25, "g"));

        RecipeSuggestionEngine.SuggestionResult result =
                RecipeSuggestionEngine.suggestRecipes(pantry, Collections.singletonList(macaroniCheese));

        assertTrue("A recipe missing one ingredient must not be suggested",
                result.getSuggested().isEmpty());
    }

    @Test
    public void excludes_recipeWhenQuantityIsInsufficient() {
        // Needs 500 g, only 200 g held.
        List<PantryItem> pantry = Collections.singletonList(pantryItem("Flour", 200, "g"));
        Recipe flatbread = recipe("Flatbread", ingredient("Flour", 500, "g"));

        RecipeSuggestionEngine.SuggestionResult result =
                RecipeSuggestionEngine.suggestRecipes(pantry, Collections.singletonList(flatbread));

        assertTrue(result.getSuggested().isEmpty());
    }

    @Test
    public void suggests_whenQuantityExactlyMeetsRequirement() {
        List<PantryItem> pantry = Collections.singletonList(pantryItem("Flour", 500, "g"));
        Recipe flatbread = recipe("Flatbread", ingredient("Flour", 500, "g"));

        RecipeSuggestionEngine.SuggestionResult result =
                RecipeSuggestionEngine.suggestRecipes(pantry, Collections.singletonList(flatbread));

        assertEquals(1, result.getSuggested().size());
    }

    // --- Normalisation and units ---

    /** "tomatoes" in the pantry must satisfy a recipe needing "tomato". */
    @Test
    public void matches_acrossPluralAndQualifierDifferences() {
        List<PantryItem> pantry = Collections.singletonList(
                pantryItem("Fresh Tomatoes", 6, "piece"));
        Recipe soup = recipe("Tomato Soup", ingredient("tomato", 5, "piece"));

        RecipeSuggestionEngine.SuggestionResult result =
                RecipeSuggestionEngine.suggestRecipes(pantry, Collections.singletonList(soup));

        assertEquals(1, result.getSuggested().size());
    }

    @Test
    public void matches_acrossDifferentUnitsInTheSameFamily() {
        // 1 kg held against 500 g required.
        List<PantryItem> pantry = Collections.singletonList(pantryItem("Flour", 1, "kg"));
        Recipe flatbread = recipe("Flatbread", ingredient("Flour", 500, "g"));

        RecipeSuggestionEngine.SuggestionResult result =
                RecipeSuggestionEngine.suggestRecipes(pantry, Collections.singletonList(flatbread));

        assertEquals(1, result.getSuggested().size());
    }

    @Test
    public void excludes_whenUnitsAreInDifferentFamilies() {
        // 500 g of milk cannot be shown to cover 300 ml without a density.
        List<PantryItem> pantry = Collections.singletonList(pantryItem("Milk", 500, "g"));
        Recipe pancakes = recipe("Pancakes", ingredient("Milk", 300, "ml"));

        RecipeSuggestionEngine.SuggestionResult result =
                RecipeSuggestionEngine.suggestRecipes(pantry, Collections.singletonList(pancakes));

        assertTrue(result.getSuggested().isEmpty());
    }

    @Test
    public void sums_multiplePantryRowsOfTheSameIngredient() {
        // Two part-used bags together cover the requirement.
        List<PantryItem> pantry = Arrays.asList(
                pantryItem("Flour", 300, "g"),
                pantryItem("Flour", 0.3, "kg"));
        Recipe flatbread = recipe("Flatbread", ingredient("Flour", 500, "g"));

        RecipeSuggestionEngine.SuggestionResult result =
                RecipeSuggestionEngine.suggestRecipes(pantry, Collections.singletonList(flatbread));

        assertEquals(1, result.getSuggested().size());
    }

    // --- The separate "almost there" list ---

    @Test
    public void almostThere_holdsRecipesMissingExactlyOneIngredient() {
        List<PantryItem> pantry = Arrays.asList(
                pantryItem("Eggs", 6, "piece"),
                pantryItem("Milk", 500, "ml"));
        // No butter.

        Recipe scrambledEggs = recipe("Scrambled Eggs",
                ingredient("Eggs", 4, "piece"),
                ingredient("Milk", 30, "ml"),
                ingredient("Butter", 15, "g"));

        RecipeSuggestionEngine.SuggestionResult result =
                RecipeSuggestionEngine.suggestRecipes(pantry, Collections.singletonList(scrambledEggs));

        assertTrue(result.getSuggested().isEmpty());
        assertEquals(1, result.getAlmostThere().size());
    }

    @Test
    public void almostThere_excludesRecipesMissingTwoOrMore() {
        List<PantryItem> pantry = Collections.singletonList(pantryItem("Eggs", 6, "piece"));

        Recipe scrambledEggs = recipe("Scrambled Eggs",
                ingredient("Eggs", 4, "piece"),
                ingredient("Milk", 30, "ml"),
                ingredient("Butter", 15, "g"));

        RecipeSuggestionEngine.SuggestionResult result =
                RecipeSuggestionEngine.suggestRecipes(pantry, Collections.singletonList(scrambledEggs));

        assertTrue(result.getSuggested().isEmpty());
        assertTrue(result.getAlmostThere().isEmpty());
    }

    @Test
    public void almostThere_neverLeaksIntoTheSuggestedList() {
        List<PantryItem> pantry = Arrays.asList(
                pantryItem("Eggs", 6, "piece"),
                pantryItem("Milk", 500, "ml"),
                pantryItem("Butter", 250, "g"));

        Recipe cookable = recipe("Scrambled Eggs",
                ingredient("Eggs", 4, "piece"),
                ingredient("Milk", 30, "ml"),
                ingredient("Butter", 15, "g"));
        Recipe oneShort = recipe("Cheese Omelette",
                ingredient("Eggs", 3, "piece"),
                ingredient("Butter", 10, "g"),
                ingredient("Cheese", 50, "g"));

        RecipeSuggestionEngine.SuggestionResult result =
                RecipeSuggestionEngine.suggestRecipes(pantry, Arrays.asList(cookable, oneShort));

        assertEquals(1, result.getSuggested().size());
        assertEquals("Scrambled Eggs", result.getSuggested().get(0).getName());
        assertEquals(1, result.getAlmostThere().size());
        assertEquals("Cheese Omelette", result.getAlmostThere().get(0).getName());
    }

    // --- Behaviour the UI depends on ---

    @Test
    public void emptyPantry_suggestsNothing() {
        Recipe scrambledEggs = recipe("Scrambled Eggs", ingredient("Eggs", 4, "piece"));

        RecipeSuggestionEngine.SuggestionResult result = RecipeSuggestionEngine.suggestRecipes(
                new ArrayList<>(), Collections.singletonList(scrambledEggs));

        assertTrue(result.isEmpty());
        assertTrue(result.getSuggested().isEmpty());
    }

    @Test
    public void removingAnIngredientMakesTheRecipeDisappear() {
        Recipe scrambledEggs = recipe("Scrambled Eggs",
                ingredient("Eggs", 4, "piece"),
                ingredient("Butter", 15, "g"));

        List<PantryItem> fullPantry = Arrays.asList(
                pantryItem("Eggs", 6, "piece"),
                pantryItem("Butter", 250, "g"));
        List<PantryItem> withoutButter = Collections.singletonList(pantryItem("Eggs", 6, "piece"));

        assertEquals(1, RecipeSuggestionEngine
                .suggestRecipes(fullPantry, Collections.singletonList(scrambledEggs))
                .getSuggested().size());
        assertTrue(RecipeSuggestionEngine
                .suggestRecipes(withoutButter, Collections.singletonList(scrambledEggs))
                .getSuggested().isEmpty());
    }

    @Test
    public void findMissingIngredients_namesWhatIsShort() {
        List<PantryItem> pantry = Collections.singletonList(pantryItem("Eggs", 6, "piece"));
        Recipe scrambledEggs = recipe("Scrambled Eggs",
                ingredient("Eggs", 4, "piece"),
                ingredient("Butter", 15, "g"));

        List<RecipeIngredient> missing =
                RecipeSuggestionEngine.findMissingIngredients(scrambledEggs, pantry);

        assertEquals(1, missing.size());
        assertEquals("Butter", missing.get(0).getName());
    }

    @Test
    public void canCook_agreesWithTheSuggestionList() {
        List<PantryItem> pantry = Collections.singletonList(pantryItem("Flour", 1, "kg"));
        Recipe flatbread = recipe("Flatbread", ingredient("Flour", 500, "g"));
        Recipe pancakes = recipe("Pancakes",
                ingredient("Flour", 150, "g"),
                ingredient("Milk", 250, "ml"));

        assertTrue(RecipeSuggestionEngine.canCook(flatbread, pantry));
        assertFalse(RecipeSuggestionEngine.canCook(pancakes, pantry));
    }

    @Test
    public void recipeWithNoIngredients_isNeverSuggested() {
        // Bad seed data must not surface as something cookable from nothing.
        Recipe empty = new Recipe("Broken Recipe", 2, 5,
                Collections.singletonList("Do nothing."), new ArrayList<>());

        RecipeSuggestionEngine.SuggestionResult result = RecipeSuggestionEngine.suggestRecipes(
                Collections.singletonList(pantryItem("Eggs", 6, "piece")),
                Collections.singletonList(empty));

        assertTrue(result.getSuggested().isEmpty());
    }
}
