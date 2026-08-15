package com.zugobite.skaflik;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.zugobite.skaflik.logic.IngredientMatcher;

import org.junit.Test;

/**
 * Tests for the normalisation rules that make matching robust to the way
 * people actually type ingredient names.
 */
public class IngredientMatcherTest {

    @Test
    public void normalize_lowercasesAndTrims() {
        assertEquals("tomato", IngredientMatcher.normalize("  Tomato  "));
    }

    @Test
    public void normalize_collapsesRepeatedWhitespace() {
        assertEquals("spring onion", IngredientMatcher.normalize("spring    onion"));
    }

    @Test
    public void normalize_stripsPunctuation() {
        assertEquals("tomato", IngredientMatcher.normalize("tomato!!!"));
    }

    @Test
    public void normalize_dropsQualifierWords() {
        assertEquals("tomato", IngredientMatcher.normalize("fresh chopped tomatoes"));
        assertEquals("onion", IngredientMatcher.normalize("large diced onion"));
    }

    @Test
    public void normalize_foldsRegularPlurals() {
        assertEquals("egg", IngredientMatcher.normalize("eggs"));
        assertEquals("onion", IngredientMatcher.normalize("onions"));
    }

    @Test
    public void normalize_foldsEsPluralsAfterSibilants() {
        assertEquals("box", IngredientMatcher.normalize("boxes"));
        assertEquals("dish", IngredientMatcher.normalize("dishes"));
    }

    @Test
    public void normalize_handlesIrregularPlurals() {
        assertEquals("tomato", IngredientMatcher.normalize("tomatoes"));
        assertEquals("potato", IngredientMatcher.normalize("potatoes"));
        assertEquals("leaf", IngredientMatcher.normalize("leaves"));
        assertEquals("loaf", IngredientMatcher.normalize("loaves"));
    }

    @Test
    public void normalize_leavesShortWordsAndDoubleSAlone() {
        // Stripping the trailing "s" here would corrupt the name.
        assertEquals("grass", IngredientMatcher.normalize("grass"));
        assertEquals("gas", IngredientMatcher.normalize("gas"));
    }

    @Test
    public void normalize_appliesSynonyms() {
        assertEquals("spring onion", IngredientMatcher.normalize("scallion"));
        assertEquals("aubergine", IngredientMatcher.normalize("eggplant"));
        assertEquals("courgette", IngredientMatcher.normalize("zucchini"));
    }

    @Test
    public void normalize_handlesNullAndBlank() {
        assertEquals("", IngredientMatcher.normalize(null));
        assertEquals("", IngredientMatcher.normalize("   "));
    }

    @Test
    public void normalize_fallsBackWhenEveryWordIsAQualifier() {
        // Must not collapse to an empty key, which would match everything.
        assertFalse(IngredientMatcher.normalize("fresh chopped").isEmpty());
    }

    /** The acceptance checklist calls this case out explicitly. */
    @Test
    public void isSameIngredient_matchesTomatoAndTomatoes() {
        assertTrue(IngredientMatcher.isSameIngredient("tomatoes", "tomato"));
        assertTrue(IngredientMatcher.isSameIngredient("Tomato", "TOMATOES"));
        assertTrue(IngredientMatcher.isSameIngredient("fresh tomatoes", "tomato"));
    }

    @Test
    public void isSameIngredient_rejectsDifferentIngredients() {
        assertFalse(IngredientMatcher.isSameIngredient("tomato", "potato"));
        assertFalse(IngredientMatcher.isSameIngredient("egg", "eggplant"));
    }

    @Test
    public void isSameIngredient_rejectsBlanks() {
        // Two empty keys are not a match, or a blank name would match a blank name.
        assertFalse(IngredientMatcher.isSameIngredient("", ""));
        assertFalse(IngredientMatcher.isSameIngredient(null, null));
    }
}
