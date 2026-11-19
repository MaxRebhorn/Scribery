package de.scribery.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TerrainShiftTest {
    @Test
    void flatGroundDoesNotMoveAnything() {
        var s = new TerrainShift(-12, 12, -6, 6, 2, 4, 2, 60, (u, v) -> 60);
        for (int u = -12; u <= 12; u++) for (int v = -6; v <= 6; v++) assertEquals(0, s.shift(u, v));
    }

    @Test
    void groundFallingAwayAtTheBowMovesTheBowDown() {
        // Huegel: in der Mitte 60, zum Bug (u > 0) faellt der Boden bis auf 54 ab
        var s = new TerrainShift(-12, 12, -6, 6, 2, 4, 1, 60, (u, v) -> u > 0 ? 60 - u / 2 : 60);
        assertEquals(0, s.shift(0, 0));
        assertTrue(s.shift(12, 0) < 0, "Bug muss nach unten");
        assertEquals(0, s.shift(-12, 0));
    }

    @Test
    void shiftIsLimitedToMaxAndChangesGently() {
        var s = new TerrainShift(-12, 12, -6, 6, 2, 4, 2, 60, (u, v) -> 60 - 3 * Math.max(0, u));   // sehr steil
        for (int u = -12; u <= 12; u++) {
            assertTrue(s.shift(u, 0) >= -4 && s.shift(u, 0) <= 4, "ueber der Grenze bei u=" + u);
            if (u < 12) assertTrue(Math.abs(s.shift(u + 1, 0) - s.shift(u, 0)) <= 2, "Sprung bei u=" + u);
        }
    }

    @Test
    void sideSlopeTiltsTheCrossSection() {
        var s = new TerrainShift(-12, 12, -6, 6, 2, 4, 1, 60, (u, v) -> 60 + v / 2);        // Boden steigt nach rechts
        assertTrue(s.shift(0, 6) > s.shift(0, -6), "rechte Seite muss hoeher liegen");
    }
}
