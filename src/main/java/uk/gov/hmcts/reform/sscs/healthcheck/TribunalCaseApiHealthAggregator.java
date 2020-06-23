package uk.gov.hmcts.reform.sscs.healthcheck;

import java.util.Map;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.stereotype.Component;

@Component
public class TribunalCaseApiHealthAggregator implements HealthAggregator {

    @Override
    public Health aggregate(Map<String, Health> healths) {
        // default status is up
        Health overallHealth = Health.up().build();
        long downStatusCount = healths.keySet().stream()
                .filter(k -> healths.get(k).getStatus().equals(Health.down().build().getStatus())).count();
        return new Health.Builder(overallHealth.getStatus())
                .withDetail("All downstream systems", (downStatusCount > 0) ? healths : Health.up().build())
                .build();
    }
}
