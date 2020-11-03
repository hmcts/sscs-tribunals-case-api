package uk.gov.hmcts.reform.sscs.service.admin;

import static org.mockito.ArgumentMatchers.any;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException.UnprocessableEntity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.elasticsearch.search.builder.SearchSourceBuilder;
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

    private String date = "2020-08-28";

    @Before
    public void setup() {
        restoreCasesService = new RestoreCasesService(ccdService, idamService, new ObjectMapper());
        Mockito.when(idamService.getIdamTokens()).thenReturn(idamTokens);
    }

    @Test
    public void testRestoreNextBatchOfCasesWhenAllReturnedCasesMatchExpectedConditions() {
        List<SscsCaseDetails> cases = new ArrayList<>();
        cases.add(SscsCaseDetails.builder().id(1L).state("responseReceived").data(SscsCaseData.builder().createdInGapsFrom("readyToList").dwpFurtherInfo("No").build()).build());
        cases.add(SscsCaseDetails.builder().id(2L).state("responseReceived").data(SscsCaseData.builder().createdInGapsFrom("readyToList").dwpFurtherInfo("No").build()).build());

        Mockito.when(ccdService.findCaseByQuery(any(SearchSourceBuilder.class), Mockito.eq(idamTokens))).thenReturn(cases);

        final RestoreCasesStatus status = restoreCasesService.restoreNextBatchOfCases(date);

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

        Assert.assertFalse(status.isCompleted());
        Assert.assertTrue(status.isOk());
        Assert.assertEquals("RestoreCasesStatus{processedCount=2, successCount=2, failureCount=0, failureIds=[], completed=false}", status.toString());

    }

    @Test
    public void testRestoreNextBatchOfCasesWhenAllReturnedCasesMatchExpectedConditionsWillNotBeAffectedWhenOneHasUnexpectedCaseDataState() {
        List<SscsCaseDetails> cases = new ArrayList<>();
        cases.add(SscsCaseDetails.builder().id(1L).state("responseReceived").data(SscsCaseData.builder().createdInGapsFrom("readyToList").state(State.DORMANT_APPEAL_STATE).dwpFurtherInfo("No").build()).build());
        cases.add(SscsCaseDetails.builder().id(2L).state("responseReceived").data(SscsCaseData.builder().createdInGapsFrom("readyToList").dwpFurtherInfo("No").build()).build());

        Mockito.when(ccdService.findCaseByQuery(any(SearchSourceBuilder.class), Mockito.eq(idamTokens))).thenReturn(cases);

        final RestoreCasesStatus status = restoreCasesService.restoreNextBatchOfCases(date);

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

        Assert.assertFalse(status.isCompleted());
        Assert.assertTrue(status.isOk());
        Assert.assertEquals("RestoreCasesStatus{processedCount=2, successCount=2, failureCount=0, failureIds=[], completed=false}", status.toString());

    }

    @Test
    public void testRestoreNextBatchOfCasesWhenNoReturnedCases() {
        String date = "2020-08-28";

        final List<SscsCaseDetails> cases = new ArrayList<>();
        Mockito.when(ccdService.findCaseByQuery(any(SearchSourceBuilder.class), Mockito.eq(idamTokens))).thenReturn(cases);

        RestoreCasesStatus status = restoreCasesService.restoreNextBatchOfCases(date);

        ArgumentCaptor<SscsCaseData> sscsCaseDataCaptor = ArgumentCaptor.forClass(SscsCaseData.class);
        ArgumentCaptor<Long> caseIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> eventTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> summaryCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> descriptionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<IdamTokens> idamTokensCaptor = ArgumentCaptor.forClass(IdamTokens.class);

        Mockito.verify(ccdService, Mockito.times(0)).updateCase(sscsCaseDataCaptor.capture(), caseIdCaptor.capture(),
            eventTypeCaptor.capture(),
            summaryCaptor.capture(), descriptionCaptor.capture(), idamTokensCaptor.capture());

        Assert.assertTrue(status.isCompleted());
        Assert.assertTrue(status.isOk());
        Assert.assertEquals("RestoreCasesStatus{processedCount=0, successCount=0, failureCount=0, failureIds=[], completed=true}", status.toString());

    }

    @Test
    public void testRestoreNextBatchOfCasesWhenAllReturnedCasesMatchExpectedConditionsAndUpdateCaseThrowsUnprocessableEntityExceptionForOne() {
        List<SscsCaseDetails> cases = new ArrayList<>();
        cases.add(SscsCaseDetails.builder().id(1L).state("responseReceived").data(SscsCaseData.builder().createdInGapsFrom("readyToList").dwpFurtherInfo("No").build()).build());
        cases.add(SscsCaseDetails.builder().id(2L).state("responseReceived").data(SscsCaseData.builder().createdInGapsFrom("readyToList").dwpFurtherInfo("No").build()).build());

        Mockito.when(ccdService.findCaseByQuery(any(SearchSourceBuilder.class), Mockito.eq(idamTokens))).thenReturn(cases);

        UnprocessableEntity unprocessableEntity = Mockito.mock(UnprocessableEntity.class);
        Mockito.when(unprocessableEntity.getMessage()).thenReturn("some exception message");

        Mockito.when(ccdService.updateCase(any(), Mockito.eq(2L), Mockito.anyString(),
            Mockito.anyString(), Mockito.anyString(), Mockito.eq(idamTokens))).thenThrow(unprocessableEntity);

        final RestoreCasesStatus status = restoreCasesService.restoreNextBatchOfCases(date);

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

        Assert.assertFalse(status.isCompleted());
        Assert.assertFalse(status.isOk());
        Assert.assertEquals("RestoreCasesStatus{processedCount=2, successCount=1, failureCount=1, failureIds=[2], completed=false}", status.toString());

    }

    @Test
    public void testRestoreNextBatchOfCasesWhenAllReturnedCasesMatchExpectedConditionsAndUpdateCaseThrowsRuntimeExceptionForOne() {
        List<SscsCaseDetails> cases = new ArrayList<>();
        cases.add(SscsCaseDetails.builder().id(1L).state("responseReceived").data(SscsCaseData.builder().createdInGapsFrom("readyToList").dwpFurtherInfo("No").build()).build());
        cases.add(SscsCaseDetails.builder().id(2L).state("responseReceived").data(SscsCaseData.builder().createdInGapsFrom("readyToList").dwpFurtherInfo("No").build()).build());

        Mockito.when(ccdService.findCaseByQuery(any(SearchSourceBuilder.class), Mockito.eq(idamTokens))).thenReturn(cases);

        Mockito.when(ccdService.updateCase(any(), Mockito.eq(2L), Mockito.anyString(),
            Mockito.anyString(), Mockito.anyString(), Mockito.eq(idamTokens))).thenThrow(new RuntimeException("some exception message"));

        final RestoreCasesStatus status = restoreCasesService.restoreNextBatchOfCases(date);

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

        Assert.assertFalse(status.isCompleted());
        Assert.assertFalse(status.isOk());
        Assert.assertEquals("RestoreCasesStatus{processedCount=2, successCount=1, failureCount=1, failureIds=[2], completed=false}", status.toString());

    }

    @Test
    public void testRestoreNextBatchOfCasesWhenAllReturnedCasesMatchExpectedConditionsButOneIsNonDigital() {
        List<SscsCaseDetails> cases = new ArrayList<>();
        cases.add(SscsCaseDetails.builder().id(1L).state("responseReceived").data(SscsCaseData.builder().createdInGapsFrom("readyToList").dwpFurtherInfo("No").build()).build());
        cases.add(SscsCaseDetails.builder().id(2L).state("responseReceived").data(SscsCaseData.builder().createdInGapsFrom("somethingElse").dwpFurtherInfo("No").build()).build());

        Mockito.when(ccdService.findCaseByQuery(any(SearchSourceBuilder.class), Mockito.eq(idamTokens))).thenReturn(cases);

        restoreCasesService.restoreNextBatchOfCases(date);

        ArgumentCaptor<SscsCaseData> sscsCaseDataCaptor = ArgumentCaptor.forClass(SscsCaseData.class);
        ArgumentCaptor<Long> caseIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> eventTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> summaryCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> descriptionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<IdamTokens> idamTokensCaptor = ArgumentCaptor.forClass(IdamTokens.class);

        Mockito.verify(ccdService, Mockito.times(1)).updateCase(sscsCaseDataCaptor.capture(), caseIdCaptor.capture(),
            eventTypeCaptor.capture(),
            summaryCaptor.capture(), descriptionCaptor.capture(), idamTokensCaptor.capture());

        Assert.assertEquals(Arrays.asList(1L), caseIdCaptor.getAllValues());
        Assert.assertEquals(Arrays.asList("readyToList"), eventTypeCaptor.getAllValues());
        Assert.assertEquals(Arrays.asList("Ready to list"), summaryCaptor.getAllValues());
        Assert.assertEquals(Arrays.asList("Ready to list event triggered"), descriptionCaptor.getAllValues());
        Assert.assertEquals(Arrays.asList(idamTokens), idamTokensCaptor.getAllValues());
    }

    @Test
    public void testRestoreNextBatchOfCasesWhenAllReturnedCasesMatchExpectedConditionsButOneHasNoCaseData() {
        List<SscsCaseDetails> cases = new ArrayList<>();
        cases.add(SscsCaseDetails.builder().id(1L).state("responseReceived").build());
        cases.add(SscsCaseDetails.builder().id(2L).state("responseReceived").data(SscsCaseData.builder().createdInGapsFrom("readyToList").dwpFurtherInfo("No").build()).build());

        Mockito.when(ccdService.findCaseByQuery(any(SearchSourceBuilder.class), Mockito.eq(idamTokens))).thenReturn(cases);

        restoreCasesService.restoreNextBatchOfCases(date);

        ArgumentCaptor<SscsCaseData> sscsCaseDataCaptor = ArgumentCaptor.forClass(SscsCaseData.class);
        ArgumentCaptor<Long> caseIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> eventTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> summaryCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> descriptionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<IdamTokens> idamTokensCaptor = ArgumentCaptor.forClass(IdamTokens.class);

        Mockito.verify(ccdService, Mockito.times(1)).updateCase(sscsCaseDataCaptor.capture(), caseIdCaptor.capture(),
            eventTypeCaptor.capture(),
            summaryCaptor.capture(), descriptionCaptor.capture(), idamTokensCaptor.capture());

        Assert.assertEquals(Arrays.asList(2L), caseIdCaptor.getAllValues());
        Assert.assertEquals(Arrays.asList("readyToList"), eventTypeCaptor.getAllValues());
        Assert.assertEquals(Arrays.asList("Ready to list"), summaryCaptor.getAllValues());
        Assert.assertEquals(Arrays.asList("Ready to list event triggered"), descriptionCaptor.getAllValues());
        Assert.assertEquals(Arrays.asList(idamTokens), idamTokensCaptor.getAllValues());
    }

    @Test
    public void testRestoreNextBatchOfCasesWhenAllReturnedCasesMatchExpectedConditionsButOneHasNoCreatedInGapsFrom() {
        List<SscsCaseDetails> cases = new ArrayList<>();
        cases.add(SscsCaseDetails.builder().id(1L).state("responseReceived").data(SscsCaseData.builder().createdInGapsFrom("readyToList").dwpFurtherInfo("No").build()).build());
        cases.add(SscsCaseDetails.builder().id(2L).state("responseReceived").data(SscsCaseData.builder().dwpFurtherInfo("No").build()).build());

        Mockito.when(ccdService.findCaseByQuery(any(SearchSourceBuilder.class), Mockito.eq(idamTokens))).thenReturn(cases);

        restoreCasesService.restoreNextBatchOfCases(date);

        ArgumentCaptor<SscsCaseData> sscsCaseDataCaptor = ArgumentCaptor.forClass(SscsCaseData.class);
        ArgumentCaptor<Long> caseIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> eventTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> summaryCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> descriptionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<IdamTokens> idamTokensCaptor = ArgumentCaptor.forClass(IdamTokens.class);

        Mockito.verify(ccdService, Mockito.times(1)).updateCase(sscsCaseDataCaptor.capture(), caseIdCaptor.capture(),
            eventTypeCaptor.capture(),
            summaryCaptor.capture(), descriptionCaptor.capture(), idamTokensCaptor.capture());

        Assert.assertEquals(Arrays.asList(1L), caseIdCaptor.getAllValues());
        Assert.assertEquals(Arrays.asList("readyToList"), eventTypeCaptor.getAllValues());
        Assert.assertEquals(Arrays.asList("Ready to list"), summaryCaptor.getAllValues());
        Assert.assertEquals(Arrays.asList("Ready to list event triggered"), descriptionCaptor.getAllValues());
        Assert.assertEquals(Arrays.asList(idamTokens), idamTokensCaptor.getAllValues());
    }

    @Test(expected = IllegalStateException.class)
    public void testRestoreNextBatchOfCasesThrowsIllegalStateExceptionWhenAReturnedCaseHasIncorrectDwpFurtherInfo() {
        final String date = "2020-08-28";

        List<SscsCaseDetails> cases = new ArrayList<>();
        cases.add(SscsCaseDetails.builder().id(1L).state("responseReceived").data(SscsCaseData.builder().createdInGapsFrom("readyToList").dwpFurtherInfo("No").build()).build());
        cases.add(SscsCaseDetails.builder().id(2L).state("responseReceived").data(SscsCaseData.builder().createdInGapsFrom("readyToList").dwpFurtherInfo("No").build()).build());
        cases.add(SscsCaseDetails.builder().id(3L).state("responseReceived").data(SscsCaseData.builder().createdInGapsFrom("readyToList").dwpFurtherInfo("Something else").build()).build());

        Map<String, String> searchCriteria = new HashMap<>();
        searchCriteria.put("last_state_modified_date", date);
        searchCriteria.put("case.dwpFurtherInfo", "No");
        searchCriteria.put("state", "responseReceived");

        Mockito.when(ccdService.findCaseByQuery(any(SearchSourceBuilder.class), Mockito.eq(idamTokens))).thenReturn(cases);

        restoreCasesService.restoreNextBatchOfCases(date);

    }

    @Test(expected = IllegalStateException.class)
    public void testRestoreNextBatchOfCasesThrowsIllegalStateExceptionWhenAReturnedCaseHasIncorrectCaseDetailsState() {
        final String date = "2020-08-28";

        List<SscsCaseDetails> cases = new ArrayList<>();
        cases.add(SscsCaseDetails.builder().id(1L).state("responseReceived").data(SscsCaseData.builder().createdInGapsFrom("readyToList").dwpFurtherInfo("No").build()).build());
        cases.add(SscsCaseDetails.builder().id(2L).state("responseReceived").data(SscsCaseData.builder().createdInGapsFrom("readyToList").dwpFurtherInfo("No").build()).build());
        cases.add(SscsCaseDetails.builder().id(3L).state("somethingElse").data(SscsCaseData.builder().createdInGapsFrom("readyToList").dwpFurtherInfo("No").build()).build());

        Mockito.when(ccdService.findCaseByQuery(any(SearchSourceBuilder.class), Mockito.eq(idamTokens))).thenReturn(cases);

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
