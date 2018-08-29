package uk.gov.hmcts.sscs.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.sscs.model.ccd.CaseData;
import uk.gov.hmcts.sscs.model.ccd.CcdUtil;
import uk.gov.hmcts.sscs.service.ccd.ReadCoreCaseDataService;
import uk.gov.hmcts.sscs.service.idam.IdamService;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ReadCcdServiceRetryAndRecoverTest {

    private static final String CASE_REF = "SC068/17/00013";

    @MockBean
    private CoreCaseDataApi coreCaseDataApi;

    @MockBean
    private IdamService idamService;

    @Autowired
    private ReadCoreCaseDataService readCcdService;

    @Before
    public void setup() {
        when(coreCaseDataApi.searchForCaseworker(
                eq("authorization"),
                eq("serviceAuthorization"),
                any(),
                anyString(),
                anyString(),
                any()))
                .thenThrow(new RuntimeException());

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
    }

    @Test
    public void givenFindCaseDataByAppealNumberFails_shouldRetryAndRecover() {
        CaseData result = readCcdService.getCcdCaseDataByAppealNumber(CASE_REF);
        verifySearchForCaseworkersWereCalled();
        assertEquals(CASE_REF, CcdUtil.getCaseData(result).getCaseReference());
    }

    @Test
    public void givenFindCaseDetailsByAppealNumberFails_shouldRetryAndRecover() {
        CaseDetails result = readCcdService.getCcdCaseDetailsByAppealNumber(CASE_REF);
        verifySearchForCaseworkersWereCalled();
        assertTrue(result.getId() == 10L);
    }

    @Test
    public void givenFindCaseDetailsByByNinoAndBenefitTypeAndMrnDateFails_shouldRetryAndRecover() {
        CaseDetails result = readCcdService.getCcdCaseByNinoAndBenefitTypeAndMrnDate("JT01234567B", "JSA", "02/08/2018");

        verifySearchForCaseworkersWereCalled();

        assertTrue(result.getId() == 10L);
    }

    private void verifySearchForCaseworkersWereCalled() {

        verify(coreCaseDataApi)
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
}
