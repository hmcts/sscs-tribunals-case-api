package uk.gov.hmcts.reform.sscs.controller;

import java.util.Collections;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LivenessController {

    @GetMapping("/health/liveness")
    public Map<String,String> testLiveness() {
        return Collections.singletonMap("status", "OK");
    }
}
