package uk.gov.hmcts.reform.sscs.healthcheck;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.*;

public class TribunalCaseApiHealthAggregatorTest {

    private TribunalCaseApiHealthAggregator tribunalCaseApiHealthAggregator;

    @BeforeEach
    public void setUp() {
        openMocks(this);
        tribunalCaseApiHealthAggregator = new TribunalCaseApiHealthAggregator();
    }

    @Test
    public void shouldReturnOverallHealthUpWithWhenSoftCheckHealthIsDown() {
        //Given
        HealthIndicator mockCoreCaseData = mock(HealthIndicator.class);
        HealthIndicator mockDocumentManagement = mock(HealthIndicator.class);
        when(mockCoreCaseData.health()).thenReturn(Health.up().build());
        when(mockDocumentManagement.health()).thenReturn(Health.down().build());

        List<NamedContributor<HealthContributor>> namedContributors = new ArrayList<>();
        namedContributors.add(NamedContributor.of("coreCaseData", mockCoreCaseData));
        namedContributors.add(NamedContributor.of("documentManagement", mockDocumentManagement));

        // when
        Status actual = tribunalCaseApiHealthAggregator.getAggregateStatus();

        // then
        Assertions.assertEquals(Health.up().build().getStatus(), actual);
    }

    @Test
    public void shouldReturnOverallHealthUpWhenHardCheckHealthIsDown() {
        //Given
        HealthIndicator mockCoreCaseData = mock(HealthIndicator.class);
        HealthIndicator mockDocumentManagement = mock(HealthIndicator.class);

        when(mockCoreCaseData.health()).thenReturn(Health.down().build());
        when(mockDocumentManagement.health()).thenReturn(Health.up().build());

        List<NamedContributor<HealthContributor>> namedContributors = new ArrayList<>();
        namedContributors.add(NamedContributor.of("coreCaseData", mockCoreCaseData));
        namedContributors.add(NamedContributor.of("documentManagement", mockDocumentManagement));

        // when
        Status actual = tribunalCaseApiHealthAggregator.getAggregateStatus();

        // then
        Assertions.assertEquals(Health.up().build().getStatus(), actual);
    }

}
