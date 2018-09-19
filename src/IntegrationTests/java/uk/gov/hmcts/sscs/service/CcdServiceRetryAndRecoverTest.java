package uk.gov.hmcts.sscs.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.sscs.ccd.config.CcdRequestDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CcdServiceRetryAndRecoverTest {

    private static final String CASE_REF = "SC068/17/00013";

    @MockBean
    private CoreCaseDataApi coreCaseDataApi;

    @MockBean
    private IdamService idamService;

    @MockBean
    private CcdRequestDetails ccdRequestDetails;

    @Autowired
    private CcdService ccdService;

    private SscsCaseData caseData;

    private IdamTokens idamTokens;

    @Before
    public void setUp() {
        when(ccdRequestDetails.getCaseTypeId()).thenReturn("1");
        when(ccdRequestDetails.getJurisdictionId()).thenReturn("3");

        when(coreCaseDataApi.searchForCaseworker(
                eq("authorization"),
                eq("serviceAuthorization"),
                any(),
                anyString(),
                anyString(),
                any()))
                .thenThrow(new RuntimeException());

        caseData = SscsCaseData.builder()
            .caseReference("SC068/17/00004")
            .appeal(Appeal.builder().build())
            .build();

        Map<String, Object> data = new HashMap<>();
        data.put("caseReference", CASE_REF);

        when(coreCaseDataApi.searchForCaseworker(
                eq("authorization2"),
                eq("serviceAuthorization2"),
                any(),
                anyString(),
                anyString(),
                any()))
                .thenReturn(Collections.singletonList(
                        CaseDetails.builder().id(10L).data(data).build()
                ));

        setupMocks();

        idamTokens = idamService.getIdamTokens();
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

        ccdService.createCase(caseData, idamTokens);

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
            .thenThrow(new RuntimeException());

        ccdService.createCase(caseData, idamTokens);

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
    public void givenFindCaseDataByAppealNumberFails_shouldRetryAndRecover() {
        SscsCaseDetails result = ccdService.findCaseByAppealNumber(CASE_REF, idamTokens);
        verifySearchForCaseworkersWereCalled();
        assertEquals(CASE_REF, result.getData().getCaseReference());
    }

    @Test
    public void givenFindCaseDetailsByAppealNumberFails_shouldRetryAndRecover() {
        SscsCaseDetails result = ccdService.findCaseByAppealNumber(CASE_REF, idamTokens);
        verifySearchForCaseworkersWereCalled();
        assertTrue(result.getId() == 10L);
    }

    @Test
    public void givenFindCaseDetailsByByNinoAndBenefitTypeAndMrnDateFails_shouldRetryAndRecover() {
        SscsCaseDetails result = ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(SscsCaseData.builder()
                .generatedNino("JT01234567B")
                .appeal(Appeal.builder()
                    .mrnDetails(MrnDetails.builder().mrnDate("02/08/2018").build())
                    .benefitType(BenefitType.builder().code("JSA").build()).build()).build(), idamTokens);

        verifySearchForCaseworkersWereCalled();

        assertTrue(result.getId() == 10L);
    }

    private void verifySearchForCaseworkersWereCalled() {

        verify(coreCaseDataApi, times(3))
                .searchForCaseworker(
                        eq("authorization"),
                        eq("serviceAuthorization"),
                        any(),
                        anyString(),
                        anyString(),
                        any());

        verify(coreCaseDataApi)
                .searchForCaseworker(
                        eq("authorization2"),
                        eq("serviceAuthorization2"),
                        any(),
                        anyString(),
                        anyString(),
                        any());

    }

    @Test
    public void givenUpdateCcdApiFailsWhenStartCaseWorker_shouldRetryAndRecover() {
        when(coreCaseDataApi.startEventForCaseWorker(
                eq("authorization"),
                eq("serviceAuthorization"),
                any(),
                anyString(),
                anyString(),
                anyString(),
                anyString()))
                .thenThrow(new RuntimeException());

        ccdService.updateCase(caseData, 1L, "appealReceived", "", "", idamTokens);

        verify(coreCaseDataApi).submitEventForCaseWorker(
                eq("authorization2"),
                eq("serviceAuthorization2"),
                any(),
                anyString(),
                anyString(),
                anyString(),
                eq(true),
                any(CaseDataContent.class));
    }

    @Test
    public void givenUpdateCcdApiFailsWhenSubmittingForCaseWorker_shouldRetryAndRecover() {
        when(coreCaseDataApi.startEventForCaseWorker(
                eq("authorization"),
                eq("serviceAuthorization"),
                any(),
                anyString(),
                anyString(),
                anyString(),
                anyString()))
                .thenReturn(StartEventResponse.builder().build());

        when(coreCaseDataApi.submitEventForCaseWorker(
                eq("authorization"),
                eq("serviceAuthorization"),
                any(),
                anyString(),
                anyString(),
                anyString(),
                eq(true),
                any(CaseDataContent.class)))
                .thenThrow(new RuntimeException());

        ccdService.updateCase(caseData, 1L, "appealReceived", "", "", idamTokens);

        verify(coreCaseDataApi).submitEventForCaseWorker(
                eq("authorization2"),
                eq("serviceAuthorization2"),
                any(),
                anyString(),
                anyString(),
                anyString(),
                eq(true),
                any(CaseDataContent.class));
    }

    private void setupMocks() {

        when(idamService.getIdamTokens()).thenAnswer(new Answer() {
            private int count = 0;

            public Object answer(InvocationOnMock invocation) {
                if (++count == 1) {
                    return IdamTokens.builder().idamOauth2Token("authorization").serviceAuthorization("serviceAuthorization").build();
                }
                return IdamTokens.builder().idamOauth2Token("authorization2").serviceAuthorization("serviceAuthorization2").build();

            }
        });

        when(coreCaseDataApi.startEventForCaseWorker(
                eq("authorization2"),
                eq("serviceAuthorization2"),
                any(),
                anyString(),
                anyString(),
                anyString(),
                anyString()))
                .thenReturn(StartEventResponse.builder().build());

        when(coreCaseDataApi.submitEventForCaseWorker(
                eq("authorization2"),
                eq("serviceAuthorization2"),
                any(),
                anyString(),
                anyString(),
                anyString(),
                eq(true),
                any(CaseDataContent.class)))
                .thenReturn(CaseDetails.builder().build());

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
    }
}
