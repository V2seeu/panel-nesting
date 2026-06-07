package com.nesting.algorithm;

import com.nesting.model.*;

import java.util.ArrayList;
import java.util.List;

public class NestingEngine {

    private static final System.Logger log = System.getLogger(NestingEngine.class.getName());

    public static NestingResult nest(NestingRequest request) {
        Sheet sheet = request.sheet();
        List<Part> parts = request.parts();

        // Expand parts by quantity
        List<Part> expanded = new ArrayList<>();
        for (Part p : parts) {
            for (int i = 0; i < p.quantity(); i++) {
                expanded.add(new Part(p.id() + "_" + i, p.name(), p.width(), p.height(), 1, p.rotatable()));
            }
        }

        log.log(System.Logger.Level.INFO,
                "展开零件列表: {0} 种 -> {1} 个实际零件",
                parts.size(), expanded.size());

        if (expanded.isEmpty()) {
            return new NestingResult(0, 0, List.of());
        }

        // Use genetic algorithm to find good ordering
        GeneticOptimizer optimizer = new GeneticOptimizer(sheet, expanded);
        GeneticOptimizer.OptimizationResult optResult = optimizer.optimize();

        // Build NestingResult from the best placement
        List<Placement> allPlacements = optResult.placementResult().placements();
        int sheetsUsed = optResult.placementResult().sheetsUsed();

        // Group by sheet index
        List<SheetResult> sheetResults = new ArrayList<>();
        for (int i = 0; i < sheetsUsed; i++) {
            int idx = i;
            List<Placement> sheetPlacements = allPlacements.stream()
                    .filter(p -> p.sheetIndex() == idx)
                    .toList();
            sheetResults.add(new SheetResult(idx, sheetPlacements));
        }

        // Calculate utilization
        double totalPartArea = expanded.stream()
                .mapToDouble(p -> p.width() * p.height())
                .sum();
        double totalSheetArea = sheet.width() * sheet.height() * sheetsUsed;
        double utilization = totalSheetArea > 0 ? totalPartArea / totalSheetArea : 0;

        return new NestingResult(sheetsUsed, utilization, sheetResults);
    }
}
