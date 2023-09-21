package uk.gov.hmcts.reform.sscs.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ADMIN_RESTORE_CASES;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.INTERLOC_INFORMATION_RECEIVED;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import junitparams.JUnitParamsRunner;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase.AdjournCaseCcdService;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase.AdjournCasePreviewService;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.PipWriteFinalDecisionPreviewDecisionService;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeService;
import uk.gov.hmcts.reform.sscs.service.admin.RestoreCasesService2;
import uk.gov.hmcts.reform.sscs.service.admin.RestoreCasesStatus;

@RunWith(JUnitParamsRunner.class)
public class CcdMideventCallbackControllerTest {

    // begin: needed to use spring runner and junitparamsRunner together
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    public static final String JURISDICTION = "SSCS";
    public static final long ID = 1234L;

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    // end

    private MockMvc mockMvc;

    @SuppressWarnings("PMD.UnusedPrivateField")
    @MockBean
    private AuthorisationService authorisationService;

    @MockBean
    private SscsCaseCallbackDeserializer deserializer;

    @MockBean
    private PipWriteFinalDecisionPreviewDecisionService writeFinalDecisionPreviewDecisionService;

    @MockBean
    private AdjournCasePreviewService adjournCasePreviewService;

    @Mock
    private AdjournCaseCcdService adjournCaseCcdService;

    @MockBean
    private RestoreCasesService2 restoreCasesService2;

    @Before
    public void setUp() {
        DynamicListItem listItem = new DynamicListItem("", "");

        DynamicList dynamicList = new DynamicList(listItem, List.of(listItem));

        when(writeFinalDecisionPreviewDecisionService.getBenefitType()).thenReturn("PIP");

        when(adjournCaseCcdService.getVenueDynamicListForRpcName(any())).thenReturn(dynamicList);

        DecisionNoticeService decisionNoticeService = new DecisionNoticeService(new ArrayList<>(), new ArrayList<>(),
                Collections.singletonList(writeFinalDecisionPreviewDecisionService));

        CcdMideventCallbackController controller = new CcdMideventCallbackController(authorisationService, deserializer, decisionNoticeService,
                adjournCasePreviewService, adjournCaseCcdService, restoreCasesService2);
        mockMvc = standaloneSetup(controller)
            .setMessageConverters(new ByteArrayHttpMessageConverter(), new StringHttpMessageConverter(),
                new ResourceHttpMessageConverter(false), new SourceHttpMessageConverter<>(),
                new AllEncompassingFormHttpMessageConverter(),
                new MappingJackson2HttpMessageConverter())
            .build();
    }

    @Test
    public void handleCcdMidEventPreviewFinalDecisionAndUpdateCaseData() throws Exception {
        String path = Objects.requireNonNull(getClass().getClassLoader().getResource("sya/allDetailsForGeneratePdf.json")).getFile();
        String content = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        SscsCaseData sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build()).build();
        when(deserializer.deserialize(content)).thenReturn(new Callback<>(
                new CaseDetails<>(ID, JURISDICTION, State.INTERLOCUTORY_REVIEW_STATE, sscsCaseData, LocalDateTime.now(), "Benefit"),
                Optional.empty(), INTERLOC_INFORMATION_RECEIVED, false));

        PreSubmitCallbackResponse<SscsCaseData> response =
            new PreSubmitCallbackResponse<>(SscsCaseData.builder().interlocReviewState(InterlocReviewState.WELSH_TRANSLATION).build());
        when(writeFinalDecisionPreviewDecisionService.preview(any(),eq(DocumentType.DRAFT_DECISION_NOTICE), anyString(), eq(false), eq(false), eq(false)))
                .thenReturn(response);

        mockMvc.perform(post("/ccdMidEventPreviewFinalDecision")
                .contentType(MediaType.APPLICATION_JSON)
                .header("ServiceAuthorization", "")
                .header("Authorization", "")
                .content(content))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"data\": {\"interlocReviewState\": \"welshTranslation\"}}"));
    }

    @Test
    public void handleCcdMidEventPreviewFinalDecisionAndAddErrorWithNullBenefitType() throws Exception {
        String path = Objects.requireNonNull(getClass().getClassLoader().getResource("sya/allDetailsForGeneratePdf.json")).getFile();
        String content = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        SscsCaseData sscsCaseData = SscsCaseData.builder().build();
        when(deserializer.deserialize(content)).thenReturn(new Callback<>(
                new CaseDetails<>(ID, JURISDICTION, State.INTERLOCUTORY_REVIEW_STATE, sscsCaseData, LocalDateTime.now(), "Benefit"),
                Optional.empty(), INTERLOC_INFORMATION_RECEIVED, false));

        PreSubmitCallbackResponse<SscsCaseData> response =
                new PreSubmitCallbackResponse<>(SscsCaseData.builder().interlocReviewState(InterlocReviewState.WELSH_TRANSLATION).build());
        when(writeFinalDecisionPreviewDecisionService.preview(any(),eq(DocumentType.DRAFT_DECISION_NOTICE), anyString(), eq(false), eq(false), eq(false)))
                .thenReturn(response);

        String expectedErrorsString = List.of("\"Unexpected error - benefit type is null\"").toString();

        String expectedJsonErrorsAndWarningsString = "{\"errors\": " + expectedErrorsString + "}, {\"warnings\" : []}";

        mockMvc.perform(post("/ccdMidEventPreviewFinalDecision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("ServiceAuthorization", "")
                        .header("Authorization", "")
                        .content(content))
                .andExpect(content().json(expectedJsonErrorsAndWarningsString));
    }

    @Test
    public void handleCcdMidEventAdminRestoreCasesWhenPathIsExtractedAndFailuresReturnedUncompleted() throws Exception {
        RestoreCasesStatus status = new RestoreCasesStatus(10, 6,
            Arrays.asList(1L, 2L, 3L, 4L), false);

        Mockito.when(restoreCasesService2.restoreCases(any())).thenReturn(status);

        Mockito.when(restoreCasesService2.getRestoreCaseFileName(anyString())).thenReturn("restore-cases1.csv");

        // We don't care what the content is for this test, as we are defining behaviour through the
        // restoreCasesService2 mock config above
        String path = Objects.requireNonNull(getClass().getClassLoader().getResource("sya/allDetailsForGeneratePdf.json")).getFile();
        String content = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        SscsCaseData sscsCaseData = SscsCaseData.builder().build();

        when(deserializer.deserialize(content)).thenReturn(new Callback<>(
            new CaseDetails<>(ID, JURISDICTION, State.INTERLOCUTORY_REVIEW_STATE, sscsCaseData, LocalDateTime.now(), "Benefit"),
            Optional.empty(), ADMIN_RESTORE_CASES, false));

        String expectedErrorsString = List.of("\"" + status + "\"").toString();

        String expectedJsonErrorsAndWarningsString = "{\"errors\": " + expectedErrorsString + "}, {\"warnings\" : []}";

        mockMvc.perform(post("/ccdMidEventAdminRestoreCases")
            .contentType(MediaType.APPLICATION_JSON)
            .header("ServiceAuthorization", "")
            .header("Authorization", "")
            .content(content))
            .andExpect(content().json(expectedJsonErrorsAndWarningsString));
    }

    @Test
    public void handleCcdMidEventAdminRestoreCasesWhenPathIsExtractedAndNoFailuresReturnedUncompleted() throws Exception {

        RestoreCasesStatus status = new RestoreCasesStatus(10, 10,
            Arrays.asList(), false);

        Mockito.when(restoreCasesService2.restoreCases(any())).thenReturn(status);

        Mockito.when(restoreCasesService2.getRestoreCaseFileName(anyString())).thenReturn("restore-cases1.csv");

        // We don't care what the content is for this test, as we are defining behaviour through the
        // restoreCasesService2 mock config above
        String path = getClass().getClassLoader().getResource("sya/allDetailsForGeneratePdf.json").getFile();
        String content = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        SscsCaseData sscsCaseData = SscsCaseData.builder().build();

        when(deserializer.deserialize(content)).thenReturn(new Callback<>(
            new CaseDetails<>(ID, JURISDICTION, State.INTERLOCUTORY_REVIEW_STATE, sscsCaseData, LocalDateTime.now(), "Benefit"),
            Optional.empty(), ADMIN_RESTORE_CASES, false));

        String expectedErrorsString = Arrays.asList("\"" + status.toString() + "\"").toString();

        String expectedJsonErrorsAndWarningsString = "{\"errors\": " + expectedErrorsString + "}, {\"warnings\" : []}";

        mockMvc.perform(post("/ccdMidEventAdminRestoreCases")
            .contentType(MediaType.APPLICATION_JSON)
            .header("ServiceAuthorization", "")
            .header("Authorization", "")
            .content(content))
            .andExpect(content().json(expectedJsonErrorsAndWarningsString));
    }

    @Test
    public void handleCcdMidEventAdminRestoreCasesWhenPathIsExtractedAndNoFailuresReturnedCompleted() throws Exception {

        RestoreCasesStatus status = new RestoreCasesStatus(10, 10,
            Arrays.asList(), true);

        Mockito.when(restoreCasesService2.restoreCases(any())).thenReturn(status);

        Mockito.when(restoreCasesService2.getRestoreCaseFileName(anyString())).thenReturn("restore-cases1.csv");

        // We don't care what the content is for this test, as we are defining behaviour through the
        // restoreCasesService2 mock config above
        String path = getClass().getClassLoader().getResource("sya/allDetailsForGeneratePdf.json").getFile();
        String content = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        SscsCaseData sscsCaseData = SscsCaseData.builder().build();

        when(deserializer.deserialize(content)).thenReturn(new Callback<>(
            new CaseDetails<>(ID, JURISDICTION, State.INTERLOCUTORY_REVIEW_STATE, sscsCaseData, LocalDateTime.now(), "Benefit"),
            Optional.empty(), ADMIN_RESTORE_CASES, false));

        String expectedWarningsString = Arrays.asList("\"" + status.toString() + "\"", "Completed - no more cases").toString();

        String expectedJsonErrorsAndWarningsString = "{\"warnings\": " + expectedWarningsString + "}, {\"errors\" : []}";

        mockMvc.perform(post("/ccdMidEventAdminRestoreCases")
            .contentType(MediaType.APPLICATION_JSON)
            .header("ServiceAuthorization", "")
            .header("Authorization", "")
            .content(content))
            .andExpect(content().json(expectedJsonErrorsAndWarningsString));
    }

    @Test
    public void handleCcdMidEventAdminRestoreCasesWhenPathIsExtractedAndRestoreNextBatchThrowsException() throws Exception {

        Mockito.when(restoreCasesService2.restoreCases(any())).thenThrow(new RuntimeException("anything"));

        Mockito.when(restoreCasesService2.getRestoreCaseFileName(anyString())).thenReturn("restore-cases1.csv");

        // We don't care what the content is for this test, as we are defining behaviour through the
        // restoreCasesService2 mock config above
        String path = getClass().getClassLoader().getResource("sya/allDetailsForGeneratePdf.json").getFile();
        String content = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        SscsCaseData sscsCaseData = SscsCaseData.builder().build();

        when(deserializer.deserialize(content)).thenReturn(new Callback<>(
            new CaseDetails<>(ID, JURISDICTION, State.INTERLOCUTORY_REVIEW_STATE, sscsCaseData, LocalDateTime.now(), "Benefit"),
            Optional.empty(), ADMIN_RESTORE_CASES, false));

        String expectedErrorsString = Arrays.asList("anything").toString();

        String expectedJsonErrorsAndWarningsString = "{\"errors\": " + expectedErrorsString + "}, {\"warnings\" : []}";

        mockMvc.perform(post("/ccdMidEventAdminRestoreCases")
            .contentType(MediaType.APPLICATION_JSON)
            .header("ServiceAuthorization", "")
            .header("Authorization", "")
            .content(content))
            .andExpect(content().json(expectedJsonErrorsAndWarningsString));
    }

    @Test
    public void handleCcdMidEventAdminRestoreCasesWhenPathExtractionThrowsException() throws Exception {

        Mockito.when(restoreCasesService2.getRestoreCaseFileName(anyString())).thenThrow(new RuntimeException("anything"));

        // We don't care what the content is for this test, as we are defining behaviour through the
        // restoreCasesService2 mock config above
        String path = getClass().getClassLoader().getResource("sya/allDetailsForGeneratePdf.json").getFile();
        String content = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        SscsCaseData sscsCaseData = SscsCaseData.builder().build();

        when(deserializer.deserialize(content)).thenReturn(new Callback<>(
            new CaseDetails<>(ID, JURISDICTION, State.INTERLOCUTORY_REVIEW_STATE, sscsCaseData, LocalDateTime.now(), "Benefit"),
            Optional.empty(), ADMIN_RESTORE_CASES, false));

        String expectedErrorsString = Arrays.asList("anything").toString();

        String expectedJsonErrorsAndWarningsString = "{\"errors\": " + expectedErrorsString + "}, {\"warnings\" : []}";

        mockMvc.perform(post("/ccdMidEventAdminRestoreCases")
            .contentType(MediaType.APPLICATION_JSON)
            .header("ServiceAuthorization", "")
            .header("Authorization", "")
            .content(content))
            .andExpect(content().json(expectedJsonErrorsAndWarningsString));

        Mockito.verify(restoreCasesService2, Mockito.times(1)).getRestoreCaseFileName(any());
        Mockito.verifyNoMoreInteractions(restoreCasesService2);

    }

    @Test
    public void handleCcdMidEventAdminRestoreCasesWhenPathIsExtractedAndFailuresReturnedCompleted() throws Exception {

        RestoreCasesStatus status = new RestoreCasesStatus(10, 6,
            Arrays.asList(1L, 2L, 3L, 4L), true);

        Mockito.when(restoreCasesService2.restoreCases(any())).thenReturn(status);

        Mockito.when(restoreCasesService2.getRestoreCaseFileName(anyString())).thenReturn("restore-cases1.csv");


        // We don't care what the content is for this test, as we are defining behaviour through the
        // restoreCasesService2 mock config above
        String path = getClass().getClassLoader().getResource("sya/allDetailsForGeneratePdf.json").getFile();
        String content = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        SscsCaseData sscsCaseData = SscsCaseData.builder().build();

        when(deserializer.deserialize(content)).thenReturn(new Callback<>(
            new CaseDetails<>(ID, JURISDICTION, State.INTERLOCUTORY_REVIEW_STATE, sscsCaseData, LocalDateTime.now(), "Benefit"),
            Optional.empty(), ADMIN_RESTORE_CASES, false));

        String expectedWarningsString = Arrays.asList("\"" + status.toString() + "\"", "Completed - no more cases").toString();

        String expectedJsonErrorsAndWarningsString = "{\"warnings\": " + expectedWarningsString + "}, {\"errors\" : []}";

        mockMvc.perform(post("/ccdMidEventAdminRestoreCases")
            .contentType(MediaType.APPLICATION_JSON)
            .header("ServiceAuthorization", "")
            .header("Authorization", "")
            .content(content))
            .andExpect(content().json(expectedJsonErrorsAndWarningsString));
    }


}
