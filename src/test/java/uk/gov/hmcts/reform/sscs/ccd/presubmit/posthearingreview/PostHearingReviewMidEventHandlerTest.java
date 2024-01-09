package uk.gov.hmcts.reform.sscs.ccd.presubmit.posthearingreview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DECISION_ISSUED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.POST_HEARING_REVIEW;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.posthearingreview.PostHearingReviewMidEventHandler.PAGE_ID_GENERATE_NOTICE;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;

@ExtendWith(MockitoExtension.class)
class PostHearingReviewMidEventHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private static final String URL = "http://dm-store/documents/123";
    private static final String TEMPLATE_ID = "TB-SCS-GNO-ENG-00091.docx";

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData caseData;

    private ArgumentCaptor<GenerateFileParams> capture;

    @Mock
    private GenerateFile generateFile;

    @Mock
    private DocumentConfiguration documentConfiguration;

    private PostHearingReviewMidEventHandler handler;


    @BeforeEach
    void setUp() {
        handler = new PostHearingReviewMidEventHandler(documentConfiguration, generateFile, true, true);

        caseData = SscsCaseData.builder()
            .documentGeneration(DocumentGeneration.builder()
                .generateNotice(YES)
                .correctionGenerateNotice(YES)
                .statementOfReasonsGenerateNotice(YES)
                .libertyToApplyGenerateNotice(YES)
                .permissionToAppealGenerateNotice(YES)
                .build())
            .appeal(Appeal.builder().appellant(Appellant.builder()
                    .name(Name.builder().firstName("APPELLANT").lastName("Last'NamE").build())
                    .identity(Identity.builder().build()).build()).build())
            .directionDueDate(LocalDate.now().plusDays(1).toString())
            .schedulingAndListingFields(SchedulingAndListingFields.builder()
                .hearingRoute(LIST_ASSIST)
                .build())
            .build();

        capture = ArgumentCaptor.forClass(GenerateFileParams.class);
    }

    @Test
    void givenAValidMidEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(POST_HEARING_REVIEW);
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
        handler = new PostHearingReviewMidEventHandler(documentConfiguration, generateFile, false, false);
        when(callback.getEvent()).thenReturn(POST_HEARING_REVIEW);
        assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(
        value = PostHearingReviewType.class,
        names = {
            "SET_ASIDE",
            "STATEMENT_OF_REASONS",
            "LIBERTY_TO_APPLY",
            "PERMISSION_TO_APPEAL"
        })
    void givenLanguagePreferenceIsEnglish_NoticeIsGeneratedAndPopulatedInPreviewDocumentField(PostHearingReviewType postHearingReviewType) {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        when(generateFile.assemble(any())).thenReturn(URL);
        when(callback.getPageId()).thenReturn(PAGE_ID_GENERATE_NOTICE);

        when(documentConfiguration.getDocuments()).thenReturn(new HashMap<>(Map.of(
            LanguagePreference.ENGLISH,  new HashMap<>(Map.of(
                DECISION_ISSUED, TEMPLATE_ID)
            ))
        ));

        caseData.getPostHearing().setReviewType(postHearingReviewType);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        DocumentLink previewDocument = response.getData().getDocumentStaging().getPostHearingPreviewDocument();
        assertThat(previewDocument).isNotNull();

        String expectedFilename = String.format("%s Granted Decision Notice issued on %s.pdf",
            postHearingReviewType.getShortDescriptionEn(),
            LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));

        assertThat(previewDocument.getDocumentFilename()).isEqualTo(expectedFilename);
        assertThat(previewDocument.getDocumentBinaryUrl()).isEqualTo(URL + "/binary");
        assertThat(previewDocument.getDocumentUrl()).isEqualTo(URL);

        verify(generateFile, times(1)).assemble(any());

        verify(generateFile, atLeastOnce()).assemble(capture.capture());

        var value = capture.getValue();
        NoticeIssuedTemplateBody payload = (NoticeIssuedTemplateBody) value.getFormPayload();
        assertThat(payload.getImage()).isEqualTo(NoticeIssuedTemplateBody.ENGLISH_IMAGE);
        assertThat(payload.getNoticeType()).isEqualTo(postHearingReviewType.getDescriptionEn().toUpperCase() + " DECISION NOTICE");
        assertThat(payload.getAppellantFullName()).isEqualTo("APPELLANT Last'NamE");
        assertThat(value.getTemplateId()).isEqualTo(TEMPLATE_ID);
    }

    @Test
    void givenLanguagePreferenceIsEnglish_NoticeIsGeneratedAndPopulatedInPreviewDocumentFieldForPtaReview() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        when(generateFile.assemble(any())).thenReturn(URL);
        when(callback.getPageId()).thenReturn(PAGE_ID_GENERATE_NOTICE);

        when(documentConfiguration.getDocuments()).thenReturn(new HashMap<>(Map.of(
                LanguagePreference.ENGLISH,  new HashMap<>(Map.of(
                        DECISION_ISSUED, TEMPLATE_ID)
                ))
        ));

        caseData.getPostHearing().setReviewType(PostHearingReviewType.PERMISSION_TO_APPEAL);
        caseData.getPostHearing().getPermissionToAppeal().setAction(PermissionToAppealActions.REVIEW);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        DocumentLink previewDocument = response.getData().getDocumentStaging().getPostHearingPreviewDocument();
        assertThat(previewDocument).isNotNull();

        String expectedFilename = String.format("Review Granted issued on %s.pdf",
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));

        assertThat(previewDocument.getDocumentFilename()).isEqualTo(expectedFilename);
        assertThat(previewDocument.getDocumentBinaryUrl()).isEqualTo(URL + "/binary");
        assertThat(previewDocument.getDocumentUrl()).isEqualTo(URL);

        verify(generateFile, times(1)).assemble(any());

        verify(generateFile, atLeastOnce()).assemble(capture.capture());

        var value = capture.getValue();
        NoticeIssuedTemplateBody payload = (NoticeIssuedTemplateBody) value.getFormPayload();
        assertThat(payload.getImage()).isEqualTo(NoticeIssuedTemplateBody.ENGLISH_IMAGE);
        assertThat(payload.getNoticeType()).isEqualTo("REVIEW DECISION NOTICE");
        assertThat(payload.getAppellantFullName()).isEqualTo("APPELLANT Last'NamE");
        assertThat(value.getTemplateId()).isEqualTo(TEMPLATE_ID);
    }

    @Test
    void givenReviewTypeIsNull_NoticeUsesDefaultDecisionNoticeName() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        when(generateFile.assemble(any())).thenReturn(URL);
        when(callback.getPageId()).thenReturn(PAGE_ID_GENERATE_NOTICE);

        when(documentConfiguration.getDocuments()).thenReturn(new HashMap<>(Map.of(
                LanguagePreference.ENGLISH,  new HashMap<>(Map.of(
                        DECISION_ISSUED, TEMPLATE_ID)
                ))
        ));

        caseData.getPostHearing().setReviewType(null);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        DocumentLink previewDocument = response.getData().getDocumentStaging().getPostHearingPreviewDocument();
        assertThat(previewDocument).isNotNull();

        String expectedFilename = String.format("Decision Notice issued on %s.pdf",
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));

        assertThat(previewDocument.getDocumentFilename()).isEqualTo(expectedFilename);
        assertThat(previewDocument.getDocumentBinaryUrl()).isEqualTo(URL + "/binary");
        assertThat(previewDocument.getDocumentUrl()).isEqualTo(URL);

        verify(generateFile, times(1)).assemble(any());

        verify(generateFile, atLeastOnce()).assemble(capture.capture());

        var value = capture.getValue();
        NoticeIssuedTemplateBody payload = (NoticeIssuedTemplateBody) value.getFormPayload();
        assertThat(payload.getImage()).isEqualTo(NoticeIssuedTemplateBody.ENGLISH_IMAGE);
        assertThat(payload.getNoticeType()).isEqualTo("DECISION NOTICE");
        assertThat(payload.getAppellantFullName()).isEqualTo("APPELLANT Last'NamE");
        assertThat(value.getTemplateId()).isEqualTo(TEMPLATE_ID);
    }

    @ParameterizedTest
    @EnumSource(
        value = YesNo.class,
        names = {"NO"})
    @NullSource
    void givenGenerateNoticeNotYes_doNothing(YesNo value) {
        caseData.getDocumentGeneration().setGenerateNotice(value);

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        when(callback.getPageId()).thenReturn("generateNotice");

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();

        verifyNoInteractions(generateFile);
    }

    @Test
    void givenOtherPageId_doNothing() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        when(callback.getPageId()).thenReturn("test page id");

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();

        verifyNoInteractions(generateFile);
    }

}
