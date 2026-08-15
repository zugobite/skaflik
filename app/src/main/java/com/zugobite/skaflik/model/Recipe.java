package com.zugobite.skaflik.model;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A recipe, with everything needed to cook it and to decide whether the user
 * can cook it right now.
 *
 * <p>Stored as one document in the top-level {@code recipes} collection.
 * Recipes are seeded once and shared by every user, so they are not nested
 * under a user document.</p>
 */
public class Recipe {

    /** Firestore document ID. Passed between screens as an Intent extra. */
    @DocumentId
    private String id;

    /** Display name, e.g. "Tomato Pasta". */
    private String name;

    /** Preparation steps in order; rendered as a numbered list. */
    private List<String> steps = new ArrayList<>();

    /** How many people the quantities below serve. */
    private int servings;

    /** Rough preparation time in minutes, shown on the detail screen. */
    private int prepMinutes;

    /** Everything the recipe requires. All of it must be in the pantry to match. */
    private List<RecipeIngredient> ingredients = new ArrayList<>();

    /** Required by Firestore's deserialiser. */
    public Recipe() {
    }

    public Recipe(String name, int servings, int prepMinutes,
                  List<String> steps, List<RecipeIngredient> ingredients) {
        this.name = name;
        this.servings = servings;
        this.prepMinutes = prepMinutes;
        this.steps = steps;
        this.ingredients = ingredients;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getSteps() {
        return steps;
    }

    public void setSteps(List<String> steps) {
        this.steps = steps;
    }

    public int getServings() {
        return servings;
    }

    public void setServings(int servings) {
        this.servings = servings;
    }

    public int getPrepMinutes() {
        return prepMinutes;
    }

    public void setPrepMinutes(int prepMinutes) {
        this.prepMinutes = prepMinutes;
    }

    public List<RecipeIngredient> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<RecipeIngredient> ingredients) {
        this.ingredients = ingredients;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Recipe)) {
            return false;
        }
        return Objects.equals(id, ((Recipe) other).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @NonNull
    @Override
    public String toString() {
        return name;
    }
}
