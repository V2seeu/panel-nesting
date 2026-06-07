package com.nesting.controller;

import com.nesting.algorithm.NestingEngine;
import com.nesting.model.NestingRequest;
import com.nesting.model.NestingResult;
import com.nesting.model.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/nesting")
public class NestingController {

    private static final Logger log = LoggerFactory.getLogger(NestingController.class);

    @PostMapping("/compute")
    public NestingResult compute(@RequestBody NestingRequest request) {
        log.info("收到排样请求: 板材 {}x{}, 零件种类 {}, 总数量 {}, 策略: {}",
                request.sheet().width(), request.sheet().height(),
                request.parts().size(),
                request.parts().stream().mapToInt(Part::quantity).sum(),
                request.strategy());

        long start = System.currentTimeMillis();
        NestingResult result = NestingEngine.nest(request);
        long elapsed = System.currentTimeMillis() - start;

        log.info("排样完成: 用板 {} 块, 利用率 {}%, 耗时 {}ms",
                result.totalSheets(),
                String.format("%.1f", result.utilization() * 100),
                elapsed);
        return result;
    }
}
