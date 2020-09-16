package uk.gov.hmcts.reform.sscs.service.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@RunWith(MockitoJUnitRunner.class)
public class RestoreCasesServiceTest {

    private RestoreCasesService restoreCasesService;

    @Mock
    private CcdService ccdService;

    @Mock
    private IdamService idamService;

    @Mock
    private IdamTokens idamTokens;

    @Before
    public void setup() {
        restoreCasesService = new RestoreCasesService(ccdService, idamService, new ObjectMapper());
        Mockito.when(idamService.getIdamTokens()).thenReturn(idamTokens);
    }

    @Test
    public void testRestoreNextBatchOfCasesCreatesExpectedCriteria() {
        String date = "2020-08-28";

        restoreCasesService.restoreNextBatchOfCases(date);

        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(ccdService, Mockito.times(1)).findCaseBy(captor.capture(), Mockito.eq(idamTokens));

        Map<String, String> searchCriteria = captor.getValue();
        Assert.assertEquals(3, searchCriteria.size());

        Assert.assertEquals("responseReceived", searchCriteria.get("state"));
        Assert.assertEquals("No", searchCriteria.get("case.dwpFurtherInfo"));
        Assert.assertEquals(date, searchCriteria.get("last_state_modified_date"));
    }

    @Test
    public void testRestoreNextBatchOfCasesWhenAllReturnedCasesMatchExpectedConditions() {
        String date = "2020-08-28";

        List<SscsCaseDetails> cases = new ArrayList<>();
        cases.add(SscsCaseDetails.builder().id(1L).state("responseReceived").data(SscsCaseData.builder().state(State.RESPONSE_RECEIVED).dwpFurtherInfo("No").build()).build());
        cases.add(SscsCaseDetails.builder().id(2L).state("responseReceived").data(SscsCaseData.builder().state(State.RESPONSE_RECEIVED).dwpFurtherInfo("No").build()).build());

        Map<String, String> searchCriteria = new HashMap<>();
        searchCriteria.put("last_state_modified_date", date);
        searchCriteria.put("case.dwpFurtherInfo", "No");
        searchCriteria.put("state", "responseReceived");

        Mockito.when(ccdService.findCaseBy(Mockito.eq(searchCriteria), Mockito.eq(idamTokens))).thenReturn(cases);

        restoreCasesService.restoreNextBatchOfCases(date);

        ArgumentCaptor<SscsCaseData> sscsCaseDataCaptor = ArgumentCaptor.forClass(SscsCaseData.class);
        ArgumentCaptor<Long> caseIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> eventTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> summaryCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> descriptionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<IdamTokens> idamTokensCaptor = ArgumentCaptor.forClass(IdamTokens.class);

        Mockito.verify(ccdService, Mockito.times(2)).updateCase(sscsCaseDataCaptor.capture(), caseIdCaptor.capture(),
            eventTypeCaptor.capture(),
            summaryCaptor.capture(), descriptionCaptor.capture(), idamTokensCaptor.capture());

        Assert.assertEquals(Arrays.asList(1L, 2L), caseIdCaptor.getAllValues());
        Assert.assertEquals(Arrays.asList("readyToList", "readyToList"), eventTypeCaptor.getAllValues());
        Assert.assertEquals(Arrays.asList("Ready to list", "Ready to list"), summaryCaptor.getAllValues());
        Assert.assertEquals(Arrays.asList("Ready to list event triggered", "Ready to list event triggered"), descriptionCaptor.getAllValues());
        Assert.assertEquals(Arrays.asList(idamTokens, idamTokens), idamTokensCaptor.getAllValues());
    }

    @Test(expected = IllegalStateException.class)
    public void testRestoreNextBatchOfCasesThrowsIllegalStateExceptionWhenAReturnedCaseHasIncorrectDwpFurtherInfo() {
        final String date = "2020-08-28";

        List<SscsCaseDetails> cases = new ArrayList<>();
        cases.add(SscsCaseDetails.builder().id(1L).state("responseReceived").data(SscsCaseData.builder().state(State.RESPONSE_RECEIVED).dwpFurtherInfo("No").build()).build());
        cases.add(SscsCaseDetails.builder().id(2L).state("responseReceived").data(SscsCaseData.builder().state(State.RESPONSE_RECEIVED).dwpFurtherInfo("No").build()).build());
        cases.add(SscsCaseDetails.builder().id(3L).state("responseReceived").data(SscsCaseData.builder().state(State.RESPONSE_RECEIVED).dwpFurtherInfo("Something else").build()).build());

        Map<String, String> searchCriteria = new HashMap<>();
        searchCriteria.put("last_state_modified_date", date);
        searchCriteria.put("case.dwpFurtherInfo", "No");
        searchCriteria.put("state", "responseReceived");

        Mockito.when(ccdService.findCaseBy(Mockito.eq(searchCriteria), Mockito.eq(idamTokens))).thenReturn(cases);

        restoreCasesService.restoreNextBatchOfCases(date);

    }

    @Test(expected = IllegalStateException.class)
    public void testRestoreNextBatchOfCasesThrowsIllegalStateExceptionWhenAReturnedCaseHasIncorrectCaseDetailsState() {
        final String date = "2020-08-28";

        List<SscsCaseDetails> cases = new ArrayList<>();
        cases.add(SscsCaseDetails.builder().id(1L).state("responseReceived").data(SscsCaseData.builder().state(State.RESPONSE_RECEIVED).dwpFurtherInfo("No").build()).build());
        cases.add(SscsCaseDetails.builder().id(2L).state("responseReceived").data(SscsCaseData.builder().state(State.RESPONSE_RECEIVED).dwpFurtherInfo("No").build()).build());
        cases.add(SscsCaseDetails.builder().id(3L).state("somethingElse").data(SscsCaseData.builder().state(State.RESPONSE_RECEIVED).dwpFurtherInfo("No").build()).build());

        Map<String, String> searchCriteria = new HashMap<>();
        searchCriteria.put("last_state_modified_date", date);
        searchCriteria.put("case.dwpFurtherInfo", "No");
        searchCriteria.put("state", "responseReceived");

        Mockito.when(ccdService.findCaseBy(Mockito.eq(searchCriteria), Mockito.eq(idamTokens))).thenReturn(cases);

        restoreCasesService.restoreNextBatchOfCases(date);

    }

    @Test(expected = IllegalStateException.class)
    public void testRestoreNextBatchOfCasesThrowsIllegalStateExceptionWhenAReturnedCaseHasIncorrectCaseDataState() {
        final String date = "2020-08-28";

        List<SscsCaseDetails> cases = new ArrayList<>();
        cases.add(SscsCaseDetails.builder().id(1L).state("responseReceived").data(SscsCaseData.builder().state(State.RESPONSE_RECEIVED).dwpFurtherInfo("No").build()).build());
        cases.add(SscsCaseDetails.builder().id(2L).state("responseReceived").data(SscsCaseData.builder().state(State.RESPONSE_RECEIVED).dwpFurtherInfo("No").build()).build());
        cases.add(SscsCaseDetails.builder().id(3L).state("responseReceived").data(SscsCaseData.builder().state(State.APPEAL_CREATED).dwpFurtherInfo("No").build()).build());

        Map<String, String> searchCriteria = new HashMap<>();
        searchCriteria.put("last_state_modified_date", date);
        searchCriteria.put("case.dwpFurtherInfo", "No");
        searchCriteria.put("state", "responseReceived");

        Mockito.when(ccdService.findCaseBy(Mockito.eq(searchCriteria), Mockito.eq(idamTokens))).thenReturn(cases);

        restoreCasesService.restoreNextBatchOfCases(date);

    }


    @Test(expected = JsonProcessingException.class)
    public void testGetRestoreCasesDateWhenInvalidJson() throws JsonProcessingException {
        String json = "invalidJson";
        restoreCasesService.getRestoreCasesDate(json);
    }

    @Test(expected = IllegalStateException.class)
    public void testGetRestoreCasesDateWhenNoCaseDetails() throws JsonProcessingException {
        String json = "{}}";
        restoreCasesService.getRestoreCasesDate(json);
    }

    @Test
    public void testGetRestoreCasesDateWhenValidJsonWithRestoreCasesDate() throws JsonProcessingException {
        String json = "{\"case_details\" : {\"case_data\" : {\"restoreCasesDate\" : \"2020-08-28\"}}}";
        String date = restoreCasesService.getRestoreCasesDate(json);
        Assert.assertEquals("2020-08-28", date);
    }

    @Test(expected = IllegalStateException.class)
    public void testGetRestoreCasesDateWhenJsonWithoutRestoreCasesDate() throws JsonProcessingException {
        String json = "{\"case_details\" : {\"case_data\" : {}}}";
        String date = restoreCasesService.getRestoreCasesDate(json);
        Assert.assertEquals("2020-08-28", date);
    }

    @Test(expected = IllegalStateException.class)
    public void testGetRestoreCasesDateWhenJsonWithoutCaseData() throws JsonProcessingException {
        String json = "{\"case_details\" : {}}";
        String date = restoreCasesService.getRestoreCasesDate(json);
        Assert.assertEquals("2020-08-28", date);
    }

    @Test(expected = IllegalStateException.class)
    public void testGetRestoreCasesDateWhenJsonWithoutCaseDetails() throws JsonProcessingException {
        String json = "{}";
        String date = restoreCasesService.getRestoreCasesDate(json);
        Assert.assertEquals("2020-08-28", date);
    }

}
