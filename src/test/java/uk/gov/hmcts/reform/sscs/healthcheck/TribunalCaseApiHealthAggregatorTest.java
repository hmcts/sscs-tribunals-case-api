package uk.gov.hmcts.reform.sscs.healthcheck;

import static org.mockito.MockitoAnnotations.openMocks;

import java.util.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.boot.actuate.health.*;

public class TribunalCaseApiHealthAggregatorTest {

    private TribunalCaseApiHealthAggregator tribunalCaseApiHealthAggregator;

    @Mock
    private HealthContributorRegistry healthContributorRegistry;

    @Before
    public void setUp() {
        openMocks(this);
        tribunalCaseApiHealthAggregator = new TribunalCaseApiHealthAggregator();
        tribunalCaseApiHealthAggregator.healthContributorRegistry = healthContributorRegistry;
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

        when(healthContributorRegistry.getContributor("coreCaseData")).thenReturn(mockCoreCaseData);
        when(healthContributorRegistry.getContributor("documentManagement")).thenReturn(mockDocumentManagement);
        when(healthContributorRegistry.stream()).thenReturn(namedContributors.stream());
        // when
        Status actual = tribunalCaseApiHealthAggregator.getAggregateStatus();

        // then
        Assert.assertEquals(Health.up().build().getStatus(), actual);
    }

    @Test
    public void shouldReturnOverallHealthDownWhenHardCheckHealthIsDown() {
        //Given
        HealthIndicator mockCoreCaseData = mock(HealthIndicator.class);
        HealthIndicator mockDocumentManagement = mock(HealthIndicator.class);

        when(mockCoreCaseData.health()).thenReturn(Health.down().build());
        when(mockDocumentManagement.health()).thenReturn(Health.up().build());

        List<NamedContributor<HealthContributor>> namedContributors = new ArrayList<>();
        namedContributors.add(NamedContributor.of("coreCaseData", mockCoreCaseData));
        namedContributors.add(NamedContributor.of("documentManagement", mockDocumentManagement));

        when(healthContributorRegistry.getContributor("coreCaseData")).thenReturn(mockCoreCaseData);
        when(healthContributorRegistry.getContributor("documentManagement")).thenReturn(mockDocumentManagement);
        when(healthContributorRegistry.stream()).thenReturn(namedContributors.stream());
        // when
        Status actual = tribunalCaseApiHealthAggregator.getAggregateStatus();

        // then
        Assert.assertEquals(Health.down().build().getStatus(), actual);
    }

}
