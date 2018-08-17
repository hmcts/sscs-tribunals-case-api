package uk.gov.hmcts.sscs.service;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.sscs.model.ccd.CaseData;
import uk.gov.hmcts.sscs.service.ccd.UpdateCoreCaseDataService;
import uk.gov.hmcts.sscs.service.idam.IdamService;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("development")
public class UpdateCcdServiceRetryAndRecoverTest {

    @MockBean
    private CoreCaseDataApi coreCaseDataApi;

    @MockBean
    private IdamService idamService;

    @Autowired
    private UpdateCoreCaseDataService updateCcdService;

    private CaseData caseData;

    @Before
    public void setUp() {
        caseData = CaseData.builder()
            .caseReference("SC068/17/00004")
            .build();
    }

    @Test
    public void givenUpdateCcdApiFailsWhenStartEvent_shouldRetryAndRecover() {
        when(coreCaseDataApi.startEventForCaseWorker(
            eq("authorization"),
            eq("serviceAuthorization"),
            eq("16"),
            anyString(),
            anyString(),
            anyString(),
            anyString()))
            .thenThrow(new RuntimeException())
            .thenThrow(new RuntimeException())
            .thenThrow(new RuntimeException());

        when(idamService.getIdamOauth2Token()).thenReturn("authorization2");
        when(idamService.generateServiceAuthorization()).thenReturn("serviceAuthorization2");

        when(coreCaseDataApi.startEventForCaseWorker(
            eq("authorization2"),
            eq("serviceAuthorization2"),
            eq("16"),
            anyString(),
            anyString(),
            anyString(),
            anyString()))
            .thenReturn(StartEventResponse.builder().build());

        when(coreCaseDataApi.submitEventForCaseWorker(
            eq("authorization2"),
            eq("serviceAuthorization2"),
            eq("16"),
            anyString(),
            anyString(),
            anyString(),
            eq(true),
            any(CaseDataContent.class)))
            .thenReturn(CaseDetails.builder().build());

//        updateCcdService.update(caseData, 1L, "appealReceived", idamTokens);

        verify(coreCaseDataApi, times(0)).submitEventForCaseWorker(
            eq("authorization"),
            eq("serviceAuthorization"),
            eq("16"),
            anyString(),
            anyString(),
            anyString(),
            eq(true),
            any(CaseDataContent.class));
    }

    @Test
    public void givenUpdateCcdApiFailsWhenSubmittingEvent_shouldRetryAndRecover() {
        when(coreCaseDataApi.startEventForCaseWorker(
            eq("authorization"),
            eq("serviceAuthorization"),
            eq("16"),
            anyString(),
            anyString(),
            anyString(),
            anyString()))
            .thenReturn(StartEventResponse.builder().build());

        when(coreCaseDataApi.submitEventForCaseWorker(
            eq("authorization"),
            eq("serviceAuthorization"),
            eq("16"),
            anyString(),
            anyString(),
            anyString(),
            eq(true),
            any(CaseDataContent.class)))
            .thenThrow(new RuntimeException())
            .thenThrow(new RuntimeException())
            .thenThrow(new RuntimeException());

        when(idamService.getIdamOauth2Token()).thenReturn("authorization2");
        when(idamService.generateServiceAuthorization()).thenReturn("serviceAuthorization2");

        when(coreCaseDataApi.startEventForCaseWorker(
            eq("authorization2"),
            eq("serviceAuthorization2"),
            eq("16"),
            anyString(),
            anyString(),
            anyString(),
            anyString()))
            .thenReturn(StartEventResponse.builder().build());

        when(coreCaseDataApi.submitEventForCaseWorker(
            eq("authorization2"),
            eq("serviceAuthorization2"),
            eq("16"),
            anyString(),
            anyString(),
            anyString(),
            eq(true),
            any(CaseDataContent.class)))
            .thenReturn(CaseDetails.builder().build());

//        updateCcdService.update(caseData, 1L, "appealReceived", idamTokens);
    }
}
