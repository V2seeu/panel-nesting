package com.nesting.algorithm;

import com.nesting.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class BLPlacer {

    public record PlacementResult(List<Placement> placements, int sheetsUsed) {}

    /**
     * BL（Bottom-Left）放置算法。
     * 按给定顺序逐个将零件放置到最低最左的可用位置。
     * 使用候选关键点法：只检查已放置零件的边界角点，而非逐像素扫描。
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
        double sheetW = sheet.width();
        double sheetH = sheet.height();

        boolean[] rotations = part.rotatable() ? new boolean[]{false, true} : new boolean[]{false};

        // Build candidate positions from existing placements on this sheet
        TreeSet<Double> candidateXs = new TreeSet<>();
        TreeSet<Double> candidateYs = new TreeSet<>();
        candidateXs.add(margin);
        candidateYs.add(margin);

        for (Placement p : placements) {
            if (p.sheetIndex() != sheetIdx) continue;
            candidateXs.add(p.x());
            candidateXs.add(p.x() + p.width() + gap);
            candidateYs.add(p.y());
            candidateYs.add(p.y() + p.height() + gap);
        }

        // Add avoid zone boundaries as candidates
        for (AvoidZone z : sheet.avoidZones()) {
            candidateXs.add(z.cx() - z.radius() - gap);
            candidateXs.add(z.cx() + z.radius() + gap);
            candidateYs.add(z.cy() - z.radius() - gap);
            candidateYs.add(z.cy() + z.radius() + gap);
        }

        // Find best BL position across all orientations
        double bestX = Double.MAX_VALUE;
        double bestY = Double.MAX_VALUE;
        boolean bestRotated = false;
        double bestW = 0, bestH = 0;
        boolean found = false;

        for (boolean rotated : rotations) {
            double pw = rotated ? part.height() : part.width();
            double ph = rotated ? part.width() : part.height();

            if (pw > sheetW - 2 * margin || ph > sheetH - 2 * margin) continue;

            for (double y : candidateYs) {
                if (y < margin || y + ph > sheetH - margin) continue;
                for (double x : candidateXs) {
                    if (x < margin || x + pw > sheetW - margin) continue;

                    if (!collidesRects(x, y, pw, ph, sheetIdx, placements, gap)
                            && !collidesAvoidZones(x, y, pw, ph, sheet.avoidZones(), gap)) {
                        if (y < bestY || (y == bestY && x < bestX)) {
                            bestX = x;
                            bestY = y;
                            bestRotated = rotated;
                            bestW = pw;
                            bestH = ph;
                            found = true;
                        }
                        break; // Found leftmost in this row
                    }
                }
                if (found) break; // Found the lowest row
            }
        }

        if (found) {
            placements.add(new Placement(part.id(), part.name(), sheetIdx, bestX, bestY, bestRotated, bestW, bestH));
            return true;
        }
        return false;
    }

    static boolean collidesRects(double x, double y, double w, double h,
                                 int sheetIdx, List<Placement> placed, double gap) {
        for (Placement p : placed) {
            if (p.sheetIndex() != sheetIdx) continue;
            if (rectsOverlap(x, y, w, h, p.x(), p.y(), p.width(), p.height(), gap)) {
                return true;
            }
        }
        return false;
    }

    static boolean collidesAvoidZones(double x, double y, double w, double h,
                                      List<AvoidZone> zones, double gap) {
        for (AvoidZone z : zones) {
            if (rectCircleOverlap(x, y, w, h, z.cx(), z.cy(), z.radius(), gap)) {
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
