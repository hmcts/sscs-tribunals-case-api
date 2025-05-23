package uk.gov.hmcts.reform.sscs.ccd.presubmit.writestatementofreasons;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.SOR_WRITE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.IN_CHAMBERS;
import static uk.gov.hmcts.reform.sscs.utility.StringUtils.getGramaticallyJoinedStrings;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CollectionItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentGeneration;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.JudicialUserPanel;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearingReviewType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Venue;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.client.JudicialUserBase;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.service.JudicialRefDataService;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;
import uk.gov.hmcts.reform.sscs.service.VenueDataLoader;

@ExtendWith(MockitoExtension.class)
class WriteStatementOfReasonsMidEventHandlerTest {
    public static final String URL = "http://dm-store/documents/123";
    private static final String USER_AUTHORISATION = "Bearer token";
    protected static final String LOGGED_IN_JUDGE_NAME = "Judge Full Name";
    protected static final String ORIGINAL_JUDGE_NAME = "test judge name name";
    public static final String CASE_ID = "123123";
    public static final String GENERATE_DOCUMENT = "generateDocument";
    public static final String TEMPLATE_ID = "template.docx";
    private static final Venue VENUE = Venue.builder().name("venue name").build();

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private ArgumentCaptor<GenerateFileParams> capture;

    @Mock
    private GenerateFile generateFile;

    @Mock
    private DocumentConfiguration documentConfiguration;

    @Mock
    private JudicialRefDataService judicialRefDataService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private VenueDataLoader venueDataLoader;

    private WriteStatementOfReasonsPreviewService service;

    private SscsCaseData caseData;

    private WriteStatementOfReasonsMidEventHandler handler;

    @BeforeEach
    void setUp() {
        service = new WriteStatementOfReasonsPreviewService(generateFile, userDetailsService, TEMPLATE_ID,
                documentConfiguration, venueDataLoader, judicialRefDataService);
        handler = new WriteStatementOfReasonsMidEventHandler(service, true, true);

        caseData = SscsCaseData.builder()
            .ccdCaseId(CASE_ID)
            .documentGeneration(DocumentGeneration.builder()
                .generateNotice(YES)
                .build())
            .appeal(Appeal.builder().appellant(Appellant.builder()
                .name(Name.builder().firstName("APPELLANT").lastName("Last'NamE").build())
                .identity(Identity.builder().build()).build()).build())
            .schedulingAndListingFields(SchedulingAndListingFields.builder()
                .hearingRoute(LIST_ASSIST)
                .build())
                .hearings(List.of(Hearing.builder()
                        .value(HearingDetails.builder()
                                .hearingDate(LocalDate.now().toString())
                                .venue(VENUE)
                                .venueId("123")
                                .build())
                        .build()))
            .build();

        capture = ArgumentCaptor.forClass(GenerateFileParams.class);
    }

    @Test
    void givenAValidMidEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(SOR_WRITE);
        assertThat(handler.canHandle(MID_EVENT, callback)).isTrue();
    }

    @Test
    void givenAInvalidEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(READY_TO_LIST);
        assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
    }

    @Test
    void givenAInvalidCallbackType_thenReturnFalse() {
        assertThat(handler.canHandle(SUBMITTED, callback)).isFalse();
    }

    @Test
    void givenPostHearingsEnabledFalse_thenReturnFalse() {
        handler = new WriteStatementOfReasonsMidEventHandler(service, false, false);
        when(callback.getEvent()).thenReturn(SOR_WRITE);
        assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = YesNo.class, names = "NO")
    @NullSource
    void givenGenerateNoticeIsNoOrNull_doNothing(YesNo value) {
        caseData.getDocumentGeneration().setGenerateNotice(value);

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        when(callback.getPageId()).thenReturn(GENERATE_DOCUMENT);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void givenGenerateNoticeYes_generateNotice() {
        when(generateFile.assemble(any())).thenReturn(URL);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        when(callback.getPageId()).thenReturn(GENERATE_DOCUMENT);
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(LOGGED_IN_JUDGE_NAME);

        caseData.getDocumentGeneration().setBodyContent("Something");
        caseData.getDocumentGeneration().setSignedBy("A name");
        caseData.getDocumentGeneration().setSignedRole("A role");
        caseData.getPostHearing().setReviewType(PostHearingReviewType.SET_ASIDE);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        DocumentLink previewDocument = response.getData().getDocumentStaging().getPreviewDocument();
        assertThat(previewDocument).isNotNull();

        String expectedFilename = String.format("Statement of Reasons issued on %s.pdf",
            LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));

        assertThat(previewDocument.getDocumentFilename()).isEqualTo(expectedFilename);
        assertThat(previewDocument.getDocumentBinaryUrl()).isEqualTo(URL + "/binary");
        assertThat(previewDocument.getDocumentUrl()).isEqualTo(URL);

        verify(generateFile, times(1)).assemble(any());

        verify(generateFile, atLeastOnce()).assemble(capture.capture());

        var value = capture.getValue();
        NoticeIssuedTemplateBody payload = (NoticeIssuedTemplateBody) value.getFormPayload();
        assertThat(payload.getImage()).isEqualTo(NoticeIssuedTemplateBody.ENGLISH_IMAGE);
        assertThat(payload.getNoticeType()).isEqualTo("STATEMENT OF REASONS");
        assertThat(payload.getAppellantFullName()).isEqualTo("APPELLANT Last'NamE");
        assertThat(payload.getNoticeBody()).isEqualTo("Something");
        assertThat(payload.getUserName()).isEqualTo("A name");
        assertThat(payload.getUserRole()).isEqualTo("A role");
        assertThat(value.getTemplateId()).isEqualTo(TEMPLATE_ID);
        assertThat(caseData.getPostHearing().getReviewType()).isNull();
    }

    @Test
    void givenOtherPageId_doNothing() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        when(callback.getPageId()).thenReturn("something else");

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void givenNoValidHearingsOnCase_thenSetToInChambers() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getPageId()).thenReturn(GENERATE_DOCUMENT);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(LOGGED_IN_JUDGE_NAME);
        caseData.setHearings(null);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();

        verify(generateFile, atLeastOnce()).assemble(capture.capture());
        NoticeIssuedTemplateBody payload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        assertThat(payload.getHeldAt()).isEqualTo(IN_CHAMBERS);
    }

    @Test
    void givenCorrectionInProgressAndFinalDecisionJudgeExists_thenJudgeNameIsOriginalJudgeName() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getPageId()).thenReturn(GENERATE_DOCUMENT);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        caseData.getPostHearing().getCorrection().setIsCorrectionFinalDecisionInProgress(YES);

        caseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        caseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision("2018-10-10");
        caseData.getSscsFinalDecisionCaseData().setFinalDecisionJudge(ORIGINAL_JUDGE_NAME);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        verify(userDetailsService, atMostOnce()).buildLoggedInUserName(USER_AUTHORISATION);

        assertThat(response.getData().getSscsFinalDecisionCaseData().getFinalDecisionJudge()).isEqualTo(ORIGINAL_JUDGE_NAME);
    }

    @Test
    void givenNoValidHearingsOnCase_thenSetFinalDecisionHeldBefore() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getPageId()).thenReturn(GENERATE_DOCUMENT);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(LOGGED_IN_JUDGE_NAME);
        caseData.setHearings(null);

        caseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDisabilityQualifiedPanelMemberName("PM1");
        caseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionMedicallyQualifiedPanelMemberName("PM2");

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();

        verify(generateFile, atLeastOnce()).assemble(capture.capture());
        NoticeIssuedTemplateBody payload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        assertThat(payload.getHeldBefore()).isNotNull();
        assertThat(payload.getHeldBefore()).isEqualTo("Judge Full Name, PM1 and PM2");
    }

    @Test
    void givenPanelIsEmpty_thenDontUpdateHeldBefore() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getPageId()).thenReturn(GENERATE_DOCUMENT);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(LOGGED_IN_JUDGE_NAME);
        caseData.getLatestHearing().getValue().setPanel(new JudicialUserPanel());

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();

        verify(generateFile, atLeastOnce()).assemble(capture.capture());
        NoticeIssuedTemplateBody payload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        assertThat(payload.getHeldBefore()).isNull();
    }

    @Test
    void givenPanelIsSet_thenCorrectlyUpdateHeldBefore() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getPageId()).thenReturn(GENERATE_DOCUMENT);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(LOGGED_IN_JUDGE_NAME);
        caseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDisabilityQualifiedPanelMemberName("PM1");

        List<String> listOfNames = List.of("panel member 1", "panel member 2");
        caseData.getLatestHearing().getValue().setPanel(new JudicialUserPanel(new JudicialUserBase("1234", "1234"), List.of(new CollectionItem<>("", new JudicialUserBase("12345", "12345")))));
        when(judicialRefDataService.getAllJudicialUsersFullNames(caseData.getLatestHearing().getValue().getPanel().getAllPanelMembers())).thenReturn(listOfNames);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();

        verify(generateFile, atLeastOnce()).assemble(capture.capture());
        NoticeIssuedTemplateBody payload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        assertThat(payload.getHeldBefore()).isEqualTo(getGramaticallyJoinedStrings(listOfNames));
    }

    @Test
    void givenVenueName_thenUpdateHeldAt() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getPageId()).thenReturn(GENERATE_DOCUMENT);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        when(venueDataLoader.getGapVenueName(VENUE, "123")).thenReturn(VENUE.getName());
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(LOGGED_IN_JUDGE_NAME);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();

        verify(generateFile, atLeastOnce()).assemble(capture.capture());
        NoticeIssuedTemplateBody payload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        assertThat(payload.getHeldAt()).isEqualTo(VENUE.getName());
    }
}
