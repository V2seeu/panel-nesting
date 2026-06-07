package com.nesting.model;

import java.util.List;

public record NestingRequest(Sheet sheet, List<Part> parts, OptimizationStrategy strategy) {
    public NestingRequest {
        if (strategy == null) strategy = OptimizationStrategy.UTILIZATION;
    }
}
