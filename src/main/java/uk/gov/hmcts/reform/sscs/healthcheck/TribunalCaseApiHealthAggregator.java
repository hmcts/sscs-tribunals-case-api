package uk.gov.hmcts.reform.sscs.healthcheck;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.stereotype.Component;

@Component
public class TribunalCaseApiHealthAggregator implements HealthAggregator {

    @Override
    public Health aggregate(Map<String, Health> healths) {
        // default status is up
        Health overallHealth = Health.up().build();
        Map<String, Health> filteredMap = healths.entrySet().stream()
                .filter(k -> k.getKey().contains("serviceAuth") || k.getKey().contains("coreCaseData")).collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue()));

        long downStatusCount = filteredMap.keySet().stream()
                .filter(k -> filteredMap.get(k).getStatus().equals(Health.down().build().getStatus())).count();
        if (downStatusCount > 0) {
            overallHealth = Health.down().build();
        }
        return new Health.Builder(overallHealth.getStatus())
                .withDetail("All downstream systems", healths)
                .build();
    }
}
