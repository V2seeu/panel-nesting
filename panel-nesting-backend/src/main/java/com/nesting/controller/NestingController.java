package com.nesting.controller;

import com.nesting.algorithm.NestingEngine;
import com.nesting.model.NestingRequest;
import com.nesting.model.NestingResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/nesting")
public class NestingController {

    @PostMapping("/compute")
    public NestingResult compute(@RequestBody NestingRequest request) {
        return NestingEngine.nest(request);
    }
}
