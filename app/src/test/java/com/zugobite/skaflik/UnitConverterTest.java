package com.zugobite.skaflik;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.zugobite.skaflik.logic.UnitConverter;

import org.junit.Test;

/** Tests for unit conversion and the rules on which units may be compared. */
public class UnitConverterTest {

    private static final double DELTA = 0.0001;

    @Test
    public void toBaseUnit_convertsMassToGrams() {
        assertEquals(1000.0, UnitConverter.toBaseUnit(1, "kg"), DELTA);
        assertEquals(200.0, UnitConverter.toBaseUnit(200, "g"), DELTA);
        assertEquals(0.5, UnitConverter.toBaseUnit(500, "mg"), DELTA);
    }

    @Test
    public void toBaseUnit_convertsVolumeToMillilitres() {
        assertEquals(1000.0, UnitConverter.toBaseUnit(1, "l"), DELTA);
        assertEquals(5.0, UnitConverter.toBaseUnit(1, "tsp"), DELTA);
        assertEquals(15.0, UnitConverter.toBaseUnit(1, "tbsp"), DELTA);
        assertEquals(250.0, UnitConverter.toBaseUnit(1, "cup"), DELTA);
    }

    @Test
    public void toBaseUnit_treatsCountUnitsAsOneEach() {
        assertEquals(3.0, UnitConverter.toBaseUnit(3, "piece"), DELTA);
        assertEquals(3.0, UnitConverter.toBaseUnit(3, "pcs"), DELTA);
    }

    @Test
    public void toBaseUnit_isCaseAndPunctuationTolerant() {
        assertEquals(1000.0, UnitConverter.toBaseUnit(1, " KG "), DELTA);
        assertEquals(1000.0, UnitConverter.toBaseUnit(1, "Kg."), DELTA);
    }

    @Test
    public void toBaseUnit_returnsNegativeOneForUnknownUnits() {
        assertEquals(-1, UnitConverter.toBaseUnit(1, "handful"), DELTA);
        assertEquals(-1, UnitConverter.toBaseUnit(1, null), DELTA);
    }

    @Test
    public void familyOf_groupsUnitsCorrectly() {
        assertEquals(UnitConverter.Family.MASS, UnitConverter.familyOf("kg"));
        assertEquals(UnitConverter.Family.VOLUME, UnitConverter.familyOf("tbsp"));
        assertEquals(UnitConverter.Family.COUNT, UnitConverter.familyOf("piece"));
        assertEquals(UnitConverter.Family.UNKNOWN, UnitConverter.familyOf("pinch"));
    }

    @Test
    public void areComparable_allowsSameFamily() {
        assertTrue(UnitConverter.areComparable("g", "kg"));
        assertTrue(UnitConverter.areComparable("ml", "cup"));
    }

    @Test
    public void areComparable_refusesCrossFamily() {
        // Converting mass to volume needs a per-ingredient density we do not hold.
        assertFalse(UnitConverter.areComparable("g", "ml"));
        assertFalse(UnitConverter.areComparable("piece", "g"));
    }

    @Test
    public void areComparable_refusesUnknownUnits() {
        assertFalse(UnitConverter.areComparable("handful", "handful"));
        assertFalse(UnitConverter.areComparable(null, "g"));
    }

    @Test
    public void allowedUnits_coverEveryFamily() {
        for (String unit : UnitConverter.ALLOWED_UNITS) {
            assertTrue("Spinner unit not convertible: " + unit,
                    UnitConverter.isKnownUnit(unit));
            assertFalse("Spinner unit has no family: " + unit,
                    UnitConverter.familyOf(unit) == UnitConverter.Family.UNKNOWN);
        }
    }
}
