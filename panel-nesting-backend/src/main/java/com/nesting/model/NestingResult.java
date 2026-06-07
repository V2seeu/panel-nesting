package com.nesting.model;

import java.util.List;

public record NestingResult(int totalSheets, double utilization, List<SheetResult> sheets) {}
