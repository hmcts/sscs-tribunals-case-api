package uk.gov.hmcts.reform.sscs.healthcheck;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.*;
import org.springframework.stereotype.Component;

@Component
public class TribunalCaseApiHealthAggregator implements StatusAggregator {

    @Autowired
    protected HealthContributorRegistry healthContributorRegistry;

    @Override
    public Status getAggregateStatus(Set<Status> statuses) {
        Status overallStatus = Status.UP;
        Map<String, HealthContributor> hardCheckMap = healthContributorRegistry.stream().filter(i -> i.getName().equals("serviceAuth")
                || i.getName().equals("coreCaseData")).collect(Collectors.toMap(x -> x.getName(), x -> x.getContributor()));
        long downStatusCount = hardCheckMap.keySet().stream()
                .filter(k -> ((HealthIndicator) hardCheckMap.get(k)).health().getStatus().equals(Health.down().build().getStatus())).count();

        if (downStatusCount > 0) {
            overallStatus = Status.DOWN;
        }
        return overallStatus;
    }
}
