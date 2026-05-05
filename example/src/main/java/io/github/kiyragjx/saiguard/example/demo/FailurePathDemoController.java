package io.github.kiyragjx.saiguard.example.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FailurePathDemoController {

    private final FailurePathDemoService demoService;

    public FailurePathDemoController(FailurePathDemoService demoService) {
        this.demoService = demoService;
    }

    @GetMapping("/demo/failure-paths")
    public FailurePathDemoResult failurePaths() {
        return demoService.runAll();
    }
}
