package uk.gov.hmcts.sscs.service;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import gcardone.junidecode.App;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.sscs.model.ccd.Appeal;
import uk.gov.hmcts.sscs.model.ccd.BenefitType;
import uk.gov.hmcts.sscs.model.ccd.CaseData;
import uk.gov.hmcts.sscs.service.ccd.CreateCoreCaseDataService;
import uk.gov.hmcts.sscs.service.idam.IdamService;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("development")
public class CreateCcdServiceRetryAndRecoverTest {
    @MockBean
    private CoreCaseDataApi coreCaseDataApi;

    @MockBean
    private IdamService idamService;

    @Autowired
    private CreateCoreCaseDataService createCcdService;
    private CaseData caseData;

    @Before
    public void setUp() {
        caseData = CaseData.builder()
            .caseReference("SC068/17/00004")
            .appeal(Appeal.builder().build())
            .build();
    }

    @Test
    public void givenCreateCcdApiFailsWhenStartCaseWorker_shouldRetryAndRecover() {
        when(coreCaseDataApi.startForCaseworker(
            eq("authorization"),
            eq("serviceAuthorization"),
            any(),
            anyString(),
            anyString(),
            anyString()))
            .thenThrow(new RuntimeException());

        when(idamService.getIdamOauth2Token()).thenAnswer(new Answer() {
            private int count = 0;

            public Object answer(InvocationOnMock invocation) {
                if (++count == 1) {
                    return "authorization";
                }

                return "authorization2";
            }
        });

        when(idamService.generateServiceAuthorization()).thenAnswer(new Answer() {
            private int count = 0;

            public Object answer(InvocationOnMock invocation) {
                if (++count == 1) {
                    return "serviceAuthorization";
                }

                return "serviceAuthorization2";
            }
        });

        when(coreCaseDataApi.startForCaseworker(
            eq("authorization2"),
            eq("serviceAuthorization2"),
            any(),
            anyString(),
            anyString(),
            anyString()))
            .thenReturn(StartEventResponse.builder().build());

        when(coreCaseDataApi.submitForCaseworker(
            eq("authorization2"),
            eq("serviceAuthorization2"),
            any(),
            anyString(),
            anyString(),
            eq(true),
            any(CaseDataContent.class)))
            .thenReturn(CaseDetails.builder().build());

        createCcdService.createCcdCase(caseData);

        verify(coreCaseDataApi).submitForCaseworker(
            eq("authorization2"),
            eq("serviceAuthorization2"),
            any(),
            anyString(),
            anyString(),
            eq(true),
            any(CaseDataContent.class));
    }

    @Test
    public void givenCreateCcdApiFailsWhenSubmittingForCaseWorker_shouldRetryAndRecover() {
        when(coreCaseDataApi.startForCaseworker(
            eq("authorization"),
            eq("serviceAuthorization"),
            any(),
            anyString(),
            anyString(),
            anyString()))
            .thenReturn(StartEventResponse.builder().build());

        when(coreCaseDataApi.submitForCaseworker(
            eq("authorization"),
            eq("serviceAuthorization"),
            any(),
            anyString(),
            anyString(),
            eq(true),
            any(CaseDataContent.class)))
            .thenThrow(new RuntimeException())
            .thenThrow(new RuntimeException())
            .thenThrow(new RuntimeException());

        when(idamService.getIdamOauth2Token()).thenReturn("authorization2");
        when(idamService.generateServiceAuthorization()).thenReturn("serviceAuthorization2");

        when(coreCaseDataApi.startForCaseworker(
            eq("authorization2"),
            eq("serviceAuthorization2"),
            any(),
            anyString(),
            anyString(),
            anyString()))
            .thenReturn(StartEventResponse.builder().build());

        when(coreCaseDataApi.submitForCaseworker(
            eq("authorization2"),
            eq("serviceAuthorization2"),
            any(),
            anyString(),
            anyString(),
            eq(true),
            any(CaseDataContent.class)))
            .thenReturn(CaseDetails.builder().build());

        createCcdService.createCcdCase(caseData);

        verify(coreCaseDataApi).submitForCaseworker(
                eq("authorization2"),
                eq("serviceAuthorization2"),
                any(),
                anyString(),
                anyString(),
                eq(true),
                any(CaseDataContent.class));
    }
}
