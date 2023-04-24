package uk.gov.hmcts.reform.sscs.ccd.presubmit.posthearingreview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.POST_HEARING_REVIEW;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.service.FooterService;

@ExtendWith(MockitoExtension.class)
class PostHearingReviewAboutToSubmitHandlerTest {
    private static final String DOCUMENT_URL = "dm-store/documents/123";
    private static final String USER_AUTHORISATION = "Bearer token";
    private PostHearingReviewAboutToSubmitHandler handler;
    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    private SscsCaseData caseData;
    @Spy
    private DocumentConfiguration documentConfiguration;
    @Mock
    private GenerateFile generateFile;
    @Mock
    private FooterService footerService;

    @BeforeEach
    void setUp() {
        handler = new PostHearingReviewAboutToSubmitHandler(true,
            documentConfiguration,
            generateFile,
            footerService);

        PostHearing postHearing = PostHearing.builder()
            .reviewType(PostHearingReviewType.SET_ASIDE)
            .setAside(SetAside.builder()
                .action(SetAsideActions.GRANT)
                .build())
            .build();

        Name name = Name.builder()
            .title("Mr")
            .firstName("Joe")
            .lastName("Bloggs")
            .build();

        Identity identity = Identity.builder().nino("ABCD").build();

        Appellant appellant = Appellant.builder()
            .isAppointee(YesNo.NO.getValue())
            .name(name)
            .identity(identity)
            .build();

        Appeal appeal = Appeal.builder()
            .appellant(appellant)
            .build();

        caseData = SscsCaseData.builder()
            .schedulingAndListingFields(SchedulingAndListingFields.builder()
                .hearingRoute(LIST_ASSIST).build())
            .ccdCaseId("1234")
            .documentGeneration(DocumentGeneration.builder()
                .directionNoticeContent("Body Content")
                .build())
            .documentStaging(DocumentStaging.builder()
                .previewDocument(DocumentLink.builder()
                    .documentUrl(DOCUMENT_URL)
                    .documentBinaryUrl(DOCUMENT_URL + "/binary")
                    .documentFilename("decisionIssued.pdf")
                    .build())
                .build())
            .languagePreferenceWelsh(YesNo.NO.getValue())
            .appeal(appeal)
            .postHearing(postHearing)
            .build();
    }

    @Test
    void givenAValidAboutToSubmitEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(POST_HEARING_REVIEW);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isTrue();
    }

    @Test
    void givenAInvalidEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(READY_TO_LIST);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @Test
    void givenAInvalidCallbackType_thenReturnFalse() {
        assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
    }

    @Test
    void givenPostHearingsEnabledFalse_thenReturnFalse() {
        handler = new PostHearingReviewAboutToSubmitHandler(false, documentConfiguration, generateFile, footerService);
        when(callback.getEvent()).thenReturn(POST_HEARING_REVIEW);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @Test
    void shouldReturnWithoutError() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        Map<EventType, String> englishEventTypeDocs = new HashMap<>();
        englishEventTypeDocs.put(EventType.SET_ASIDE_GRANTED, "TB-SCS-GNO-ENG-grant-refuse-set-aside.docx");

        Map<LanguagePreference, Map<EventType, String>> documents = new HashMap<>();
        documents.put(LanguagePreference.ENGLISH, englishEventTypeDocs);
        documentConfiguration.setDocuments(documents);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void givenHearingIsNull_thenCaseStatusNotChanged() {
        caseData.setState(State.NOT_LISTABLE);
        handler.updateCaseStatus(caseData);
        assertThat(caseData.getState()).isEqualTo(State.NOT_LISTABLE);
    }

    @ParameterizedTest
    @CsvSource({"SET_ASIDE,SET_ASIDE_GRANTED,GRANT",
        "SET_ASIDE,SET_ASIDE_REFUSED,REFUSE",
        "CORRECTION,CORRECTION_GRANTED,GRANT",
        "CORRECTION,CORRECTION_REFUSED,REFUSE"})
    void givenSetAsideReviewType_documentTypeIsExpectedValue(PostHearingReviewType reviewType, EventType eventType, SetAsideActions action) {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        CorrectionActions correctionAction = action.getCcdDefinition().equals("grant") ? CorrectionActions.GRANT : CorrectionActions.REFUSE;

        PostHearing postHearing = PostHearing.builder()
            .reviewType(reviewType)
            .setAside(SetAside.builder()
                .action(action)
                .build())
            .correction(Correction.builder()
                .action(correctionAction)
                .build())
            .build();
        caseData.setPostHearing(postHearing);

        Map<EventType, String> englishEventTypeDocs = new HashMap<>();
        englishEventTypeDocs.put(eventType, "TB-SCS-GNO-ENG-grant-refuse-set-aside.docx");

        Map<LanguagePreference, Map<EventType, String>> documents = new HashMap<>();
        documents.put(LanguagePreference.ENGLISH, englishEventTypeDocs);
        documentConfiguration.setDocuments(documents);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String actionLabel = action.getCcdDefinition().equals(ProcessRequestAction.GRANT.getValue()) ? "granted" : "refused";
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        String expectedDocFileName = reviewType.getDescriptionEn() + " Application " + actionLabel + " on " + date + ".pdf";

        String docFileName = response.getData().getDocumentStaging().getPreviewDocument().getDocumentFilename();

        Assertions.assertEquals(expectedDocFileName, docFileName);
    }

    @ParameterizedTest
    @EnumSource(value = PostHearingReviewType.class, names = {"SET_ASIDE","CORRECTION"})
    void givenNullActionType_throwsNullPointerExceptionWhenHandlingPostHearingReview(PostHearingReviewType reviewType) {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PostHearing postHearing = PostHearing.builder()
            .reviewType(reviewType)
            .setAside(SetAside.builder()
                .action(null)
                .build())
            .build();
        caseData.setPostHearing(postHearing);

        assertThrows(NullPointerException.class,
            () -> {
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
            });
    }

    @ParameterizedTest
    @EnumSource(value = SetAsideActions.class, names = {"GRANT","REFUSE"})
    void givenNullReviewType_throwsNullPointerExceptionWhenHandlingPostHearingReview(SetAsideActions action) {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PostHearing postHearing = PostHearing.builder()
            .reviewType(null)
            .setAside(SetAside.builder()
                .action(action)
                .build())
            .build();
        caseData.setPostHearing(postHearing);

        assertThrows(NullPointerException.class,
            () -> {
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
            });
    }

    @Test
    void givenSetAsideStateIsNull_thenCaseStatusNotChanged() {
        caseData = SscsCaseData.builder()
            .state(State.DORMANT_APPEAL_STATE)
            .postHearing(PostHearing.builder()
                .setAside(SetAside.builder()
                    .action(null)
                    .build())
                .build())
            .build();

        handler.updateCaseStatus(caseData);
        assertThat(caseData.getState()).isEqualTo(State.DORMANT_APPEAL_STATE);
    }

    @Test
    void givenSetAsideState_thenCaseStatusChanged() {
        caseData = SscsCaseData.builder()
            .state(State.DORMANT_APPEAL_STATE)
            .postHearing(PostHearing.builder()
                .setAside(SetAside.builder()
                    .action(SetAsideActions.GRANT)
                    .build())
                .build())
            .build();

        handler.updateCaseStatus(caseData);
        assertThat(caseData.getState()).isEqualTo(State.NOT_LISTABLE);
    }
}
