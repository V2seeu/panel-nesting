package com.nesting.algorithm;

import com.nesting.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NestingEngineTest {

    @Test
    void nestMultiplePartsOnStandardSheet() {
        Sheet sheet = new Sheet(1220, 2440, 10, 3, List.of());
        List<Part> parts = List.of(
            new Part("p1", "P1", 100, 200, 10, true),
            new Part("p2", "P2", 150, 150, 5, false)
        );
        NestingRequest request = new NestingRequest(sheet, parts);

        NestingResult result = NestingEngine.nest(request);

        assertNotNull(result);
        assertTrue(result.totalSheets() >= 1);
        assertTrue(result.utilization() > 0);
        assertEquals(15, result.sheets().stream().mapToInt(s -> s.placements().size()).sum());
    }

    @Test
    void nestWithAvoidZones() {
        Sheet sheet = new Sheet(500, 500, 10, 3,
                List.of(new AvoidZone(250, 250, 20)));
        List<Part> parts = List.of(
            new Part("p1", "P1", 100, 100, 4, false)
        );
        NestingRequest request = new NestingRequest(sheet, parts);

        NestingResult result = NestingEngine.nest(request);

        for (SheetResult sr : result.sheets()) {
            for (Placement p : sr.placements()) {
                boolean overlaps = !(p.x() + p.width() <= 230 || p.x() >= 270 ||
                                     p.y() + p.height() <= 230 || p.y() >= 270);
                assertFalse(overlaps, "Placement should not overlap avoid zone");
            }
        }
    }

    @Test
    void utilizationReasonable() {
        // 5 parts of 200x200 on a 1220x2440 sheet should fit on 1 sheet with decent utilization
        Sheet sheet = new Sheet(1220, 2440, 10, 3, List.of());
        List<Part> parts = List.of(
            new Part("p1", "P1", 200, 200, 5, false)
        );
        NestingRequest request = new NestingRequest(sheet, parts);

        NestingResult result = NestingEngine.nest(request);

        assertEquals(1, result.totalSheets());
        assertTrue(result.utilization() > 0);
        assertEquals(5, result.sheets().get(0).placements().size());
    }
}
