package com.nesting.model;

import java.util.List;

public record Sheet(double width, double height, double margin, double gap, List<AvoidZone> avoidZones) {
    public Sheet {
        if (avoidZones == null) avoidZones = List.of();
    }
}
