package com.zugobite.skaflik.data;

/**
 * Single source of truth for Firestore collection and field names.
 *
 * <p>Firestore has no schema of its own - a document's shape is whatever the
 * code that wrote it decided. Keeping every collection path and field name in
 * one place is what stops that flexibility turning into typo-driven bugs, and
 * it doubles as the written record of the data model.</p>
 *
 * <pre>
 * users/{uid}/pantryItems/{itemId}
 *     name        string    // as typed by the user
 *     nameNorm    string    // normalised key used for matching
 *     quantity    number
 *     unit        string
 *     expiryDate  string?   // ISO yyyy-MM-dd, nullable
 *     createdAt   timestamp // set by the server
 *
 * recipes/{recipeId}
 *     name        string
 *     steps       array&lt;string&gt;
 *     servings    number
 *     prepMinutes number
 *     ingredients array&lt;{ name, nameNorm, quantity, unit }&gt;
 *
 * meta/seed
 *     seeded         boolean   // guards against re-seeding the recipe list
 *     seededAt       timestamp
 *     recipeCount    number
 * </pre>
 *
 * <p>Pantry items are nested under the signed-in user so that one device's
 * pantry is invisible to another. Recipes sit at the top level because they are
 * seeded once and shared by everyone.</p>
 */
public final class FirestoreContract {

    /** Static constants only; never instantiated. */
    private FirestoreContract() {
    }

    // --- Collections ---

    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_PANTRY_ITEMS = "pantryItems";
    public static final String COLLECTION_RECIPES = "recipes";
    public static final String COLLECTION_META = "meta";

    /** Document under {@link #COLLECTION_META} recording that seeding has run. */
    public static final String DOC_SEED = "seed";

    // --- Pantry item fields ---

    public static final String FIELD_NAME = "name";
    public static final String FIELD_NAME_NORM = "nameNorm";
    public static final String FIELD_QUANTITY = "quantity";
    public static final String FIELD_UNIT = "unit";
    public static final String FIELD_EXPIRY_DATE = "expiryDate";
    public static final String FIELD_CREATED_AT = "createdAt";

    // --- Recipe fields ---

    public static final String FIELD_STEPS = "steps";
    public static final String FIELD_SERVINGS = "servings";
    public static final String FIELD_PREP_MINUTES = "prepMinutes";
    public static final String FIELD_INGREDIENTS = "ingredients";

    // --- Seed marker fields ---

    public static final String FIELD_SEEDED = "seeded";
    public static final String FIELD_SEEDED_AT = "seededAt";
    public static final String FIELD_RECIPE_COUNT = "recipeCount";
}
