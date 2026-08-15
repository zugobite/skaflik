package com.zugobite.skaflik.logic;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Converts quantities to a common base unit so amounts written in different
 * units can be compared.
 *
 * <p>Three unit families are supported, each with its own base:</p>
 * <ul>
 *     <li><b>Mass</b> - mg, g, kg → base <b>g</b></li>
 *     <li><b>Volume</b> - ml, l, tsp, tbsp, cup → base <b>ml</b></li>
 *     <li><b>Count</b> - piece, pcs, unit, whole → base <b>count</b></li>
 * </ul>
 *
 * <p>Spoon and cup measures use the common metric approximations: tsp = 5 ml,
 * tbsp = 15 ml, cup = 250 ml.</p>
 */
public final class UnitConverter {

    /** The family a unit belongs to. Quantities only compare within a family. */
    public enum Family {
        MASS,
        VOLUME,
        COUNT,
        /** Returned for anything unrecognised. */
        UNKNOWN
    }

    /** Canonical unit codes offered in the Add/Edit form's spinner. */
    public static final List<String> ALLOWED_UNITS = Collections.unmodifiableList(
            Arrays.asList("g", "kg", "ml", "l", "tsp", "tbsp", "cup", "piece"));

    /** Unit code → how many base units one of it is worth. */
    private static final Map<String, Double> TO_BASE = new HashMap<>();

    /** Unit code → the family it belongs to. */
    private static final Map<String, Family> FAMILIES = new HashMap<>();

    static {
        // Mass family, base gram.
        register("mg", 0.001, Family.MASS);
        register("g", 1.0, Family.MASS);
        register("gram", 1.0, Family.MASS);
        register("grams", 1.0, Family.MASS);
        register("kg", 1000.0, Family.MASS);

        // Volume family, base millilitre.
        register("ml", 1.0, Family.VOLUME);
        register("l", 1000.0, Family.VOLUME);
        register("litre", 1000.0, Family.VOLUME);
        register("liter", 1000.0, Family.VOLUME);
        register("tsp", 5.0, Family.VOLUME);
        register("tbsp", 15.0, Family.VOLUME);
        register("cup", 250.0, Family.VOLUME);
        register("cups", 250.0, Family.VOLUME);

        // Count family, base "one of the thing".
        register("piece", 1.0, Family.COUNT);
        register("pieces", 1.0, Family.COUNT);
        register("pcs", 1.0, Family.COUNT);
        register("unit", 1.0, Family.COUNT);
        register("units", 1.0, Family.COUNT);
        register("whole", 1.0, Family.COUNT);
    }

    private static void register(String unit, double factor, Family family) {
        TO_BASE.put(unit, factor);
        FAMILIES.put(unit, family);
    }

    /** Static helper class; never instantiated. */
    private UnitConverter() {
    }

    /**
     * Converts a quantity into its family's base unit.
     *
     * @param quantity the amount
     * @param unit     the unit it is expressed in
     * @return the amount in base units, or {@code -1} if the unit is unknown
     */
    public static double toBaseUnit(double quantity, @Nullable String unit) {
        Double factor = TO_BASE.get(canonicalise(unit));
        if (factor == null) {
            // Unknown unit: signal failure rather than guessing, so the caller
            // can treat the comparison as unsatisfied.
            return -1;
        }
        return quantity * factor;
    }

    /** Returns the family a unit belongs to, or {@link Family#UNKNOWN}. */
    @NonNull
    public static Family familyOf(@Nullable String unit) {
        Family family = FAMILIES.get(canonicalise(unit));
        return family == null ? Family.UNKNOWN : family;
    }

    /**
     * True when two units can be meaningfully compared.
     *
     * <p>Cross-family comparison is deliberately refused. 200 g of flour and
     * 200 ml of flour are not the same amount, and converting between them
     * needs a density per ingredient that this app does not hold. Treating such
     * a pair as "not satisfied" is the safe direction to fail: the worst case
     * is a recipe the user could have cooked staying hidden, rather than the
     * app claiming they can cook something they cannot.</p>
     */
    public static boolean areComparable(@Nullable String unitA, @Nullable String unitB) {
        Family familyA = familyOf(unitA);
        Family familyB = familyOf(unitB);
        return familyA != Family.UNKNOWN && familyA == familyB;
    }

    /** True when the unit is one this app knows how to convert. */
    public static boolean isKnownUnit(@Nullable String unit) {
        return TO_BASE.containsKey(canonicalise(unit));
    }

    /** Trims, lowercases and strips a trailing full stop, e.g. "Kg." → "kg". */
    private static String canonicalise(@Nullable String unit) {
        if (unit == null) {
            return "";
        }
        String cleaned = unit.trim().toLowerCase(Locale.ROOT);
        if (cleaned.endsWith(".")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        return cleaned;
    }
}
