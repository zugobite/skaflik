package com.zugobite.skaflik.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.Objects;

/**
 * A single ingredient the user currently has at home.
 *
 * <p>Stored as one document in {@code users/{uid}/pantryItems}. Firestore maps
 * this class to and from a document automatically, which requires a public
 * no-argument constructor plus a getter and setter for every persisted field.</p>
 *
 * <p>{@link #nameNorm} holds the normalised form of {@link #name} (see
 * {@code IngredientMatcher}). It is stored rather than computed on read so that
 * matching never has to re-normalise the whole pantry, and so Firestore can
 * query on it directly.</p>
 */
public class PantryItem {

    /** Firestore document ID. Populated on read; never written as a field. */
    @DocumentId
    private String id;

    /** Ingredient name exactly as the user typed it, e.g. "Cherry Tomatoes". */
    private String name;

    /** Normalised name used for matching, e.g. "tomato". */
    private String nameNorm;

    /** Amount held, in {@link #unit}. Always greater than zero. */
    private double quantity;

    /** Unit code from {@code UnitConverter}, e.g. "g", "ml", "piece". */
    private String unit;

    /** Optional expiry date as ISO {@code yyyy-MM-dd}. Null when not set. */
    @Nullable
    private String expiryDate;

    /** Set by the server on insert; used to order the pantry list. */
    @ServerTimestamp
    private Date createdAt;

    /** Required by Firestore's deserialiser. */
    public PantryItem() {
    }

    public PantryItem(String name, String nameNorm, double quantity, String unit,
                      @Nullable String expiryDate) {
        this.name = name;
        this.nameNorm = nameNorm;
        this.quantity = quantity;
        this.unit = unit;
        this.expiryDate = expiryDate;
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

    @Nullable
    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(@Nullable String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * True when this item carries an expiry date.
     *
     * <p>Annotated {@code @Exclude} so Firestore treats it as a convenience
     * method rather than a field to persist.</p>
     */
    @Exclude
    public boolean hasExpiryDate() {
        return expiryDate != null && !expiryDate.trim().isEmpty();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PantryItem)) {
            return false;
        }
        PantryItem that = (PantryItem) other;
        // Identity is the document ID; two items with the same ID are the same row.
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @NonNull
    @Override
    public String toString() {
        return name + " (" + quantity + " " + unit + ")";
    }
}
