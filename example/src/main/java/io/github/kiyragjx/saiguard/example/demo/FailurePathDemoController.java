package io.github.kiyragjx.saiguard.example.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP endpoint for local scripted failure-path demos.
 */
@RestController
public class FailurePathDemoController {

    private final FailurePathDemoService demoService;

    /**
     * Creates the failure-path demo controller.
     *
     * @param demoService service that runs scripted scenarios
     */
    public FailurePathDemoController(FailurePathDemoService demoService) {
        this.demoService = demoService;
    }

    /**
     * Runs all scripted failure-path scenarios.
     *
     * @return demo results for each scenario
     */
    @GetMapping("/demo/failure-paths")
    public FailurePathDemoResult failurePaths() {
        return demoService.runAll();
    }
}
