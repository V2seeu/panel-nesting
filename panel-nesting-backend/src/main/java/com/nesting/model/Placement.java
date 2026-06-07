package com.nesting.model;

public record Placement(String partId, String partName, int sheetIndex, double x, double y, boolean rotated, double width, double height) {}
