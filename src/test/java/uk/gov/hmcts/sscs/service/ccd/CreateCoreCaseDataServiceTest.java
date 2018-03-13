package uk.gov.hmcts.sscs.service.ccd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.EventRequestData;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.sscs.model.ccd.CaseData;

@RunWith(MockitoJUnitRunner.class)
public class CreateCoreCaseDataServiceTest {

    @Mock
    private CoreCaseDataApi coreCaseDataApiMock;
    @Mock
    private CoreCaseDataService coreCaseDataServiceMock;

    private CreateCoreCaseDataService createCoreCaseDataService;

    @Before
    public void setUp() {
        createCoreCaseDataService = new CreateCoreCaseDataService(coreCaseDataServiceMock);
    }

    @Test
    public void givenACase_shouldSaveItIntoCcd() {
        //Given
        mockStartEventResponse();
        mockCaseDetails();
        when(coreCaseDataServiceMock.getEventRequestData(eq("appealCreated")))
                .thenReturn(EventRequestData.builder().build());
        when(coreCaseDataServiceMock.generateServiceAuthorization())
                .thenReturn("s2s token");
        when(coreCaseDataServiceMock.getCaseDataContent(
                any(CaseData.class),
                any(StartEventResponse.class),
                anyString(),
                anyString()
        )).thenReturn(CaseDataContent.builder().build());
        when(coreCaseDataServiceMock.getCcdUrl()).thenReturn("ccdUrl");
        when(coreCaseDataServiceMock.getCoreCaseDataApi()).thenReturn(coreCaseDataApiMock);

        //When
        CaseDetails caseDetails = createCoreCaseDataService.createCcdCase(
            CaseDataUtils.buildCaseData());

        //Then
        assertNotNull(caseDetails);
        String caseReference = (String) caseDetails.getData().get("caseReference");
        assertEquals("SC068/17/00013", caseReference);
    }

    private void mockCaseDetails() {
        when(coreCaseDataApiMock.submitForCaseworker(anyString(), anyString(), anyString(), anyString(), anyString(),
                anyBoolean(), any(CaseDataContent.class))).thenReturn(CaseDataUtils.buildCaseDetails());
    }

    private void mockStartEventResponse() {
        when(coreCaseDataApiMock.startForCaseworker(anyString(), anyString(), anyString(), anyString(),
            anyString(), anyString())).thenReturn(StartEventResponse.builder()
            .caseDetails(CaseDetails.builder().build())
            .build());
    }
}
