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
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;
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
    private static final String TEMPLATE_ID = "TB-SCS-GNO-ENG-grant-refuse-set-aside.docx";

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
        handler = new PostHearingReviewMidEventHandler(documentConfiguration, generateFile, true);

        PostHearing postHearing = PostHearing.builder()
            .reviewType(PostHearingReviewType.SET_ASIDE)
            .setAside(SetAside.builder()
                .action(SetAsideActions.GRANT)
                .build())
            .build();

        caseData = SscsCaseData.builder()
            .documentGeneration(DocumentGeneration.builder()
                .generateNotice(YES)
                .build())
            .appeal(Appeal.builder().appellant(Appellant.builder()
                    .name(Name.builder().firstName("APPELLANT").lastName("LastNamE").build())
                    .identity(Identity.builder().build()).build()).build())
            .directionDueDate(LocalDate.now().plusDays(1).toString())
            .schedulingAndListingFields(SchedulingAndListingFields.builder()
                .hearingRoute(LIST_ASSIST)
                .build())
            .postHearing(postHearing)
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
        handler = new PostHearingReviewMidEventHandler(documentConfiguration, generateFile, false);
        when(callback.getEvent()).thenReturn(POST_HEARING_REVIEW);
        assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
    }

    @Test
    void givenLanguagePreferenceIsEnglish_NoticeIsGeneratedAndPopulatedInPreviewDocumentField() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        when(generateFile.assemble(any())).thenReturn(URL);
        when(callback.getPageId()).thenReturn(PAGE_ID_GENERATE_NOTICE);

        when(documentConfiguration.getDocuments()).thenReturn(new HashMap<>(Map.of(
            LanguagePreference.ENGLISH,  new HashMap<>(Map.of(
                SET_ASIDE_GRANTED, TEMPLATE_ID)
            ))
        ));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        DocumentLink previewDocument = response.getData().getDocumentStaging().getPreviewDocument();
        assertThat(previewDocument).isNotNull();

        String expectedFilename = String.format("Set Aside Application granted on %s.pdf",
            LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));

        assertThat(previewDocument.getDocumentFilename()).isEqualTo(expectedFilename);
        assertThat(previewDocument.getDocumentBinaryUrl()).isEqualTo(URL + "/binary");
        assertThat(previewDocument.getDocumentUrl()).isEqualTo(URL);

        verify(generateFile, times(1)).assemble(any());

        verify(generateFile, atLeastOnce()).assemble(capture.capture());

        var value = capture.getValue();
        NoticeIssuedTemplateBody payload = (NoticeIssuedTemplateBody) value.getFormPayload();
        assertThat(payload.getImage()).isEqualTo(NoticeIssuedTemplateBody.ENGLISH_IMAGE);
        assertThat(payload.getNoticeType()).isEqualTo("SET ASIDE DECISION NOTICE");
        assertThat(payload.getAppellantFullName()).isEqualTo("Appellant Lastname");
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
