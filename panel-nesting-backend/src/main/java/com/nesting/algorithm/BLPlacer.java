package com.nesting.algorithm;

import com.nesting.model.*;

import java.util.ArrayList;
import java.util.List;

public class BLPlacer {

    public record PlacementResult(List<Placement> placements, int sheetsUsed) {}

    /**
     * BL（Bottom-Left）放置算法。
     * 按给定顺序逐个将零件放置到最低最左的可用位置。
     */
    public static PlacementResult place(Sheet sheet, List<Part> parts, List<Placement> existing) {
        List<Placement> placements = new ArrayList<>(existing);
        int sheetsUsed = existing.isEmpty() ? 0 : existing.stream().mapToInt(Placement::sheetIndex).max().orElse(0) + 1;

        for (Part part : parts) {
            boolean placed = false;
            for (int sheetIdx = 0; sheetIdx <= sheetsUsed && !placed; sheetIdx++) {
                if (tryPlaceOnSheet(sheet, part, sheetIdx, placements)) {
                    placed = true;
                }
            }
            if (!placed) {
                sheetsUsed++;
                tryPlaceOnSheet(sheet, part, sheetsUsed - 1, placements);
            }
            sheetsUsed = Math.max(sheetsUsed, placements.stream().mapToInt(Placement::sheetIndex).max().orElse(0) + 1);
        }

        return new PlacementResult(placements, sheetsUsed);
    }

    private static boolean tryPlaceOnSheet(Sheet sheet, Part part, int sheetIdx, List<Placement> placements) {
        double margin = sheet.margin();
        double gap = sheet.gap();
        double innerW = sheet.width() - 2 * margin;
        double innerH = sheet.height() - 2 * margin;

        // Try original orientation first, then rotated if allowed
        boolean[] rotations = part.rotatable() ? new boolean[]{false, true} : new boolean[]{false};

        for (boolean rotated : rotations) {
            double pw = rotated ? part.height() : part.width();
            double ph = rotated ? part.width() : part.height();

            if (pw > innerW || ph > innerH) continue;

            double bestX = Double.MAX_VALUE;
            double bestY = Double.MAX_VALUE;
            boolean found = false;

            // Scan from bottom-left, row by row
            for (double y = margin; y + ph <= sheet.height() - margin; y += 1) {
                for (double x = margin; x + pw <= sheet.width() - margin; x += 1) {
                    Placement candidate = new Placement(part.id(), part.name(), sheetIdx, x, y, rotated, pw, ph);
                    if (!collides(candidate, placements, gap) && !collidesAvoidZone(candidate, sheet.avoidZones(), gap)) {
                        if (y < bestY || (y == bestY && x < bestX)) {
                            bestX = x;
                            bestY = y;
                            found = true;
                        }
                        break; // Found leftmost in this row, no need to scan further right
                    }
                }
                if (found) break; // Found the lowest row, stop scanning up
            }

            if (found) {
                placements.add(new Placement(part.id(), part.name(), sheetIdx, bestX, bestY, rotated, pw, ph));
                return true;
            }
        }
        return false;
    }

    static boolean collides(Placement candidate, List<Placement> placed, double gap) {
        for (Placement p : placed) {
            if (p.sheetIndex() != candidate.sheetIndex()) continue;
            if (rectsOverlap(candidate.x(), candidate.y(), candidate.width(), candidate.height(),
                             p.x(), p.y(), p.width(), p.height(), gap)) {
                return true;
            }
        }
        return false;
    }

    static boolean collidesAvoidZone(Placement candidate, List<AvoidZone> zones, double gap) {
        for (AvoidZone z : zones) {
            if (rectCircleOverlap(candidate.x(), candidate.y(), candidate.width(), candidate.height(),
                                  z.cx(), z.cy(), z.radius(), gap)) {
                return true;
            }
        }
        return false;
    }

    static boolean rectsOverlap(double x1, double y1, double w1, double h1,
                                double x2, double y2, double w2, double h2, double gap) {
        return !(x1 + w1 + gap <= x2 || x2 + w2 + gap <= x1 ||
                 y1 + h1 + gap <= y2 || y2 + h2 + gap <= y1);
    }

    static boolean rectCircleOverlap(double rx, double ry, double rw, double rh,
                                     double cx, double cy, double cr, double gap) {
        // Expand rect by gap, then check circle overlap
        double ex = rx - gap;
        double ey = ry - gap;
        double ew = rw + 2 * gap;
        double eh = rh + 2 * gap;
        double nearestX = Math.max(ex, Math.min(cx, ex + ew));
        double nearestY = Math.max(ey, Math.min(cy, ey + eh));
        double dx = cx - nearestX;
        double dy = cy - nearestY;
        return (dx * dx + dy * dy) <= (cr * cr);
    }
}
