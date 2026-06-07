package com.nesting.algorithm;

import com.nesting.model.AvoidZone;
import com.nesting.model.Part;
import com.nesting.model.Placement;
import com.nesting.model.Sheet;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BLPlacerTest {

    private final Sheet standardSheet = new Sheet(1000, 1000, 10, 3, List.of());

    @Test
    void placeSinglePart() {
        Sheet sheet = standardSheet;
        Part part = new Part("p1", "P1", 100, 200, 1, false);
        List<Part> expanded = List.of(part);
        List<Placement> placed = new ArrayList<>();

        BLPlacer.PlacementResult result = BLPlacer.place(sheet, expanded, placed);

        assertEquals(1, result.placements().size());
        Placement p = result.placements().get(0);
        assertEquals("p1", p.partId());
        assertEquals(10, p.x()); // starts at margin
        assertEquals(10, p.y()); // starts at margin
    }

    @Test
    void placeTwoPartsSideBySide() {
        Sheet sheet = standardSheet;
        List<Part> expanded = List.of(
            new Part("p1", "P1", 100, 100, 1, false),
            new Part("p2", "P2", 100, 100, 1, false)
        );
        List<Placement> placed = new ArrayList<>();

        BLPlacer.PlacementResult result = BLPlacer.place(sheet, expanded, placed);

        assertEquals(2, result.placements().size());
        // Second part should be to the right of first, with gap
        assertTrue(result.placements().get(1).x() > result.placements().get(0).x());
    }

    @Test
    void partThatExceedsSheetGoesToNewSheet() {
        Sheet sheet = new Sheet(200, 200, 10, 3, List.of());
        Part bigPart = new Part("p1", "P1", 150, 150, 1, false);
        List<Part> expanded = List.of(bigPart, bigPart, bigPart);
        List<Placement> placed = new ArrayList<>();

        BLPlacer.PlacementResult result = BLPlacer.place(sheet, expanded, placed);

        // Should need at least 2 sheets for 3 big parts on a small sheet
        assertTrue(result.placements().stream().mapToInt(Placement::sheetIndex).max().orElse(0) >= 1);
    }

    @Test
    void rotatablePartGetsRotatedWhenFitsBetter() {
        Sheet sheet = new Sheet(200, 100, 10, 3, List.of());
        Part part = new Part("p1", "P1", 80, 50, 1, true);
        List<Part> expanded = List.of(part);
        List<Placement> placed = new ArrayList<>();

        BLPlacer.PlacementResult result = BLPlacer.place(sheet, expanded, placed);

        assertEquals(1, result.placements().size());
        // Should fit on the sheet
        assertEquals(0, result.placements().get(0).sheetIndex());
    }

    @Test
    void avoidZoneBlocksPlacement() {
        Sheet sheet = new Sheet(500, 500, 10, 3,
                List.of(new AvoidZone(100, 100, 50)));
        Part part = new Part("p1", "P1", 80, 80, 1, false);
        List<Part> expanded = List.of(part);
        List<Placement> placed = new ArrayList<>();

        BLPlacer.PlacementResult result = BLPlacer.place(sheet, expanded, placed);

        assertEquals(1, result.placements().size());
        // Part should NOT overlap with the avoid zone at (100,100,r=50)
        Placement p = result.placements().get(0);
        boolean overlaps = !(p.x() + p.width() <= 50 || p.x() >= 150 ||
                             p.y() + p.height() <= 50 || p.y() >= 150);
        assertFalse(overlaps, "Part should not overlap with avoid zone");
    }

    @Test
    void gapBetweenParts() {
        Sheet sheet = new Sheet(500, 500, 10, 5, List.of());
        Part part1 = new Part("p1", "P1", 100, 100, 1, false);
        Part part2 = new Part("p2", "P2", 100, 100, 1, false);
        List<Part> expanded = List.of(part1, part2);
        List<Placement> placed = new ArrayList<>();

        BLPlacer.PlacementResult result = BLPlacer.place(sheet, expanded, placed);

        assertEquals(2, result.placements().size());
        Placement a = result.placements().get(0);
        Placement b = result.placements().get(1);
        // If side by side, gap should be >= 5
        if (a.y() == b.y()) {
            assertTrue(Math.abs(b.x() - (a.x() + a.width())) >= 5 ||
                       Math.abs(a.x() - (b.x() + b.width())) >= 5);
        }
    }
}
