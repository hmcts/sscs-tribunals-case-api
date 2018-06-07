package uk.gov.hmcts.sscs.service.ccd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
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
public class UpdateCoreCaseDataServiceTest {

    public static final String S_2_S_TOKEN = "s2s token";
    public static final String APPEAL_UPDATED = "appeal Updated";
    public static final String SSCS_APPEAL_UPDATED_EVENT = "SSCS - appeal updated event";
    public static final String UPDATED_SSCS = "Updated SSCS";
    public static final Long caseid = 1L;

    @Mock
    private CoreCaseDataApi coreCaseDataApiMock;
    @Mock
    private CoreCaseDataService coreCaseDataServiceMock;
    @Mock
    private IdamService idamService;

    private UpdateCoreCaseDataService updateCoreCaseDataService;

    @Before
    public void setUp() {
        updateCoreCaseDataService = new UpdateCoreCaseDataService(coreCaseDataServiceMock, idamService);
        when(idamService.generateServiceAuthorization()).thenReturn(S_2_S_TOKEN);
    }

    @Test
    public void givenACase_shouldUpdateCaseItIntoCcd() {
        //Given
        CaseData caseData = CaseDataUtils.buildCaseData();
        EventRequestData eventRequestData = EventRequestData.builder()
                .userId("user-id").eventId(APPEAL_UPDATED).caseTypeId("case-type-id").userToken("user-token")
                .jurisdictionId("jurisdiction-id").ignoreWarning(true).build();

        StartEventResponse startEventResponse = StartEventResponse.builder().caseDetails(CaseDetails.builder().build()).build();
        CaseDataContent caseDataContent = CaseDataContent.builder().build();

        when(coreCaseDataApiMock.startEventForCaseWorker(eventRequestData.getUserToken(),
                S_2_S_TOKEN,
                eventRequestData.getUserId(),
                eventRequestData.getJurisdictionId(),
                eventRequestData.getCaseTypeId(),
                caseid.toString(),
                eventRequestData.getEventId())).thenReturn(startEventResponse);

        when(coreCaseDataApiMock.submitEventForCaseWorker(eventRequestData.getUserToken(),
                S_2_S_TOKEN,
                eventRequestData.getUserId(),
                eventRequestData.getJurisdictionId(),
                eventRequestData.getCaseTypeId(),
                caseid.toString(),
                eventRequestData.isIgnoreWarning(),
                caseDataContent))
                .thenReturn(CaseDataUtils.buildCaseDetails());

        when(coreCaseDataServiceMock.getEventRequestData(eventRequestData.getEventId()))
                .thenReturn(eventRequestData);
        when(coreCaseDataServiceMock.getCaseDataContent(caseData,
                startEventResponse,
                SSCS_APPEAL_UPDATED_EVENT, UPDATED_SSCS)).thenReturn(caseDataContent);
        when(coreCaseDataServiceMock.getCcdUrl()).thenReturn("ccdUrl");
        when(coreCaseDataServiceMock.getCoreCaseDataApi()).thenReturn(coreCaseDataApiMock);

        //When
        CaseDetails caseDetails = updateCoreCaseDataService.updateCcdCase(
            caseData, caseid, APPEAL_UPDATED);

        //Then
        assertNotNull(caseDetails);
        verify(coreCaseDataApiMock).submitEventForCaseWorker(anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyBoolean(), any(CaseDataContent.class));
        String caseReference = (String) caseDetails.getData().get("caseReference");
        assertEquals("SC068/17/00013", caseReference);
    }

}
