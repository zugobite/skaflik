package com.zugobite.skaflik.model;

import androidx.annotation.NonNull;

/**
 * One ingredient line required by a {@link Recipe}, e.g. "200 g pasta".
 *
 * <p>Unlike {@link PantryItem} this is not a document of its own. Ingredients
 * are stored as an array of nested maps inside the recipe document, because a
 * recipe's ingredients are only ever read together with the recipe itself -
 * embedding them means one read instead of a query per recipe.</p>
 */
public class RecipeIngredient {

    /** Ingredient name as displayed, e.g. "Tomatoes". */
    private String name;

    /** Normalised name used for matching against the pantry, e.g. "tomato". */
    private String nameNorm;

    /** Amount required, in {@link #unit}. */
    private double quantity;

    /** Unit code from {@code UnitConverter}, e.g. "g", "ml", "piece". */
    private String unit;

    /** Required by Firestore's deserialiser. */
    public RecipeIngredient() {
    }

    public RecipeIngredient(String name, String nameNorm, double quantity, String unit) {
        this.name = name;
        this.nameNorm = nameNorm;
        this.quantity = quantity;
        this.unit = unit;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNameNorm() {
        return nameNorm;
    }

    public void setNameNorm(String nameNorm) {
        this.nameNorm = nameNorm;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    @NonNull
    @Override
    public String toString() {
        return quantity + " " + unit + " " + name;
    }
}
