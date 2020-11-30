package uk.gov.hmcts.reform.sscs.healthcheck;

import java.util.Set;
import org.springframework.boot.actuate.health.*;
import org.springframework.stereotype.Component;

@Component
public class TribunalCaseApiHealthAggregator implements StatusAggregator {

    @Override
    public Status getAggregateStatus(Set<Status> statuses) {
        return Status.UP;
    }
}
