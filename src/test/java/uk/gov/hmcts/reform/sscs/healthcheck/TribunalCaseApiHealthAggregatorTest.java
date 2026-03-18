package uk.gov.hmcts.reform.sscs.healthcheck;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

class TribunalCaseApiHealthAggregatorTest {

    private TribunalCaseApiHealthAggregator aggregator;

    @BeforeEach
    void setUp() {
        aggregator = new TribunalCaseApiHealthAggregator();
    }

    @Test
    void shouldAlwaysReturnUpEvenWhenDependenciesAreDown() {
        Status result = aggregator.getAggregateStatus(Set.of(Status.UP, Status.DOWN));

        assertEquals(Status.UP, result);
    }

    @Test
    void shouldReturnUpWhenAllDependenciesAreUp() {
        Status result = aggregator.getAggregateStatus(Set.of(Status.UP));

        assertEquals(Status.UP, result);
    }

    @Test
    void shouldReturnUpWhenOnlyDependencyIsDown() {
        Status result = aggregator.getAggregateStatus(Set.of(Status.DOWN));

        assertEquals(Status.UP, result);
    }
}
