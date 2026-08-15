package com.zugobite.skaflik.logic;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Turns messily-typed ingredient names into a stable key so that "Fresh
 * Tomatoes", "tomato" and "TOMATOES" all match each other.
 *
 * <p>This is deliberately a small set of string rules rather than any kind of
 * language model. It handles the realistic ways a person types an ingredient
 * into a form - stray capitals, plurals, and prefixes like "fresh" or
 * "chopped" - and nothing more.</p>
 *
 * <p>The normalised value is stored on every pantry item and recipe ingredient
 * as {@code nameNorm}, so matching is a plain string comparison at read time.</p>
 */
public final class IngredientMatcher {

    /**
     * Descriptive words that say how an ingredient was prepared or sized, none
     * of which change what the ingredient actually is.
     */
    private static final Set<String> QUALIFIERS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    "fresh", "frozen", "dried", "raw", "cooked",
                    "chopped", "diced", "sliced", "minced", "grated", "crushed", "ground",
                    "large", "small", "medium", "extra",
                    "organic", "free", "range", "whole", "plain")));

    /** Plurals that do not follow the usual rules. */
    private static final Map<String, String> IRREGULAR_PLURALS = new HashMap<>();

    /** Different names for the same ingredient, folded onto one spelling. */
    private static final Map<String, String> SYNONYMS = new HashMap<>();

    static {
        IRREGULAR_PLURALS.put("leaves", "leaf");
        IRREGULAR_PLURALS.put("loaves", "loaf");
        IRREGULAR_PLURALS.put("knives", "knife");
        IRREGULAR_PLURALS.put("halves", "half");
        IRREGULAR_PLURALS.put("potatoes", "potato");
        IRREGULAR_PLURALS.put("tomatoes", "tomato");
        IRREGULAR_PLURALS.put("chillies", "chilli");
        IRREGULAR_PLURALS.put("berries", "berry");

        // Regional and shop-label variants. Both sides normalise to one form.
        SYNONYMS.put("scallion", "spring onion");
        SYNONYMS.put("green onion", "spring onion");
        SYNONYMS.put("eggplant", "aubergine");
        SYNONYMS.put("zucchini", "courgette");
        SYNONYMS.put("cilantro", "coriander");
        SYNONYMS.put("garbanzo bean", "chickpea");
        SYNONYMS.put("bell pepper", "pepper");
        SYNONYMS.put("capsicum", "pepper");
        SYNONYMS.put("aubergine", "aubergine");
    }

    /** Static helper class; never instantiated. */
    private IngredientMatcher() {
    }

    /**
     * Reduces an ingredient name to its matching key.
     *
     * <p>The steps, in order:</p>
     * <ol>
     *     <li>trim, lowercase, and collapse repeated whitespace</li>
     *     <li>strip punctuation</li>
     *     <li>drop qualifier words such as "fresh" or "chopped"</li>
     *     <li>fold each remaining word from plural to singular</li>
     *     <li>apply the synonym map to the result</li>
     * </ol>
     *
     * @param rawName the name as typed, may be null
     * @return the normalised key, or an empty string for null/blank input
     */
    @NonNull
    public static String normalize(@Nullable String rawName) {
        if (rawName == null) {
            return "";
        }

        // 1 & 2: lowercase, strip anything that is not a letter, digit or space,
        // then collapse the whitespace that stripping may have left behind.
        String cleaned = rawName.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (cleaned.isEmpty()) {
            return "";
        }

        // 3 & 4: drop qualifiers and singularise what is left.
        StringBuilder result = new StringBuilder();
        for (String word : cleaned.split(" ")) {
            if (QUALIFIERS.contains(word)) {
                continue;
            }
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(singularise(word));
        }

        // Every word was a qualifier (e.g. "fresh chopped"). Fall back to the
        // cleaned input rather than returning nothing, which would match everything.
        String folded = result.toString().trim();
        if (folded.isEmpty()) {
            folded = cleaned;
        }

        // 5: collapse regional variants onto a single spelling.
        String synonym = SYNONYMS.get(folded);
        return synonym != null ? synonym : folded;
    }

    /**
     * True when two names refer to the same ingredient.
     *
     * @param nameA first name, in raw form
     * @param nameB second name, in raw form
     */
    public static boolean isSameIngredient(@Nullable String nameA, @Nullable String nameB) {
        String normA = normalize(nameA);
        return !normA.isEmpty() && normA.equals(normalize(nameB));
    }

    /**
     * Folds a single word from plural to singular.
     *
     * <p>Handles the irregular map first, then "-es" after a sibilant
     * ("tomatoes" → "tomato", "boxes" → "box"), then a plain trailing "s"
     * ("eggs" → "egg"). Short words and words ending in "ss" are left alone so
     * that "gas" and "grass" survive intact.</p>
     */
    private static String singularise(String word) {
        String irregular = IRREGULAR_PLURALS.get(word);
        if (irregular != null) {
            return irregular;
        }

        // Too short to safely strip a letter from, or not a plural at all.
        if (word.length() <= 3 || !word.endsWith("s") || word.endsWith("ss")) {
            return word;
        }

        if (word.endsWith("es")) {
            String stem = word.substring(0, word.length() - 2);
            // "-es" is only a plural marker after s, x, z, ch or sh.
            if (stem.endsWith("s") || stem.endsWith("x") || stem.endsWith("z")
                    || stem.endsWith("ch") || stem.endsWith("sh")) {
                return stem;
            }
        }

        return word.substring(0, word.length() - 1);
    }
}
