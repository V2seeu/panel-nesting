package com.nesting.model;

import java.util.List;

public record NestingRequest(Sheet sheet, List<Part> parts) {}
