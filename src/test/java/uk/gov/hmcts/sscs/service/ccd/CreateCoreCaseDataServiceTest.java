package uk.gov.hmcts.sscs.service.ccd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
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
import uk.gov.hmcts.sscs.service.idam.IdamService;

@RunWith(MockitoJUnitRunner.class)
public class CreateCoreCaseDataServiceTest {

    public static final String S2S_TOKEN = "s2s token";
    public static final String SSCS_APPEAL_CREATED_EVENT = "SSCS - appeal created event";
    public static final String CREATED_SSCS = "Created SSCS";
    @Mock
    private CoreCaseDataApi coreCaseDataApiMock;
    @Mock
    private IdamService idamServiceMock;
    @Mock
    private CoreCaseDataService coreCaseDataServiceMock;

    private CreateCoreCaseDataService createCoreCaseDataService;

    @Before
    public void setUp() {
        createCoreCaseDataService = new CreateCoreCaseDataService(
            coreCaseDataServiceMock, idamServiceMock
        );
    }

    @Test
    public void givenACase_shouldSaveItIntoCcd() {
        //Given

        CaseData caseData = CaseDataUtils.buildCaseData();
        EventRequestData eventRequestData = EventRequestData.builder().build();

        StartEventResponse startEventResponse = StartEventResponse.builder().caseDetails(CaseDetails.builder().build()).build();
        CaseDataContent caseDataContent = CaseDataContent.builder().build();

        when(coreCaseDataApiMock.startForCaseworker(
            eventRequestData.getUserToken(),
            S2S_TOKEN,
            eventRequestData.getUserId(),
            eventRequestData.getJurisdictionId(),
            eventRequestData.getCaseTypeId(),
            eventRequestData.getEventId()
        )).thenReturn(startEventResponse);

        when(coreCaseDataApiMock.submitForCaseworker(
            eventRequestData.getUserToken(),
            S2S_TOKEN,
            eventRequestData.getUserId(),
            eventRequestData.getJurisdictionId(),
            eventRequestData.getCaseTypeId(),
            eventRequestData.isIgnoreWarning(),
            caseDataContent
        )).thenReturn(CaseDataUtils.buildCaseDetails());

        when(coreCaseDataServiceMock.getEventRequestData(eq("appealCreated"))).thenReturn(eventRequestData);
        when(coreCaseDataServiceMock.getCaseDataContent(
            caseData,
            startEventResponse,
            SSCS_APPEAL_CREATED_EVENT,
            CREATED_SSCS
        )).thenReturn(caseDataContent);

        when(coreCaseDataServiceMock.getCcdUrl()).thenReturn("ccdUrl");
        when(coreCaseDataServiceMock.getCoreCaseDataApi()).thenReturn(coreCaseDataApiMock);

        when(idamServiceMock.generateServiceAuthorization()).thenReturn(S2S_TOKEN);

        //When
        CaseDetails caseDetails = createCoreCaseDataService.createCcdCase(caseData);

        //Then
        assertNotNull(caseDetails);
        String caseReference = (String) caseDetails.getData().get("caseReference");
        assertEquals("SC068/17/00013", caseReference);
    }

}
