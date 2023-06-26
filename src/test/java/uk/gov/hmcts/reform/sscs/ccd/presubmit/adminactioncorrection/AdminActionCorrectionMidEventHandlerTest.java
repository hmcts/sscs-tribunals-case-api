package uk.gov.hmcts.reform.sscs.ccd.presubmit.adminactioncorrection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
class AdminActionCorrectionMidEventHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private static final String URL = "http://dm-store/documents/123";
    private static final String TEMPLATE_ID = "TB-SCS-GNO-ENG-00091.docx";

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData caseData;

    private AdminActionCorrectionMidEventHandler handler;
    @Mock
    private DocumentConfiguration documentConfiguration;
    @Mock
    private GenerateFile generateFile;
    private ArgumentCaptor<GenerateFileParams> capture;



    @BeforeEach
    void setUp() {
        handler = new AdminActionCorrectionMidEventHandler(documentConfiguration, generateFile, true);

        caseData = SscsCaseData.builder()
            .ccdCaseId("1234")
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
            .build();

        capture = ArgumentCaptor.forClass(GenerateFileParams.class);

    }

    @Test
    void givenAValidMidEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(ADMIN_ACTION_CORRECTION);
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
        handler = new AdminActionCorrectionMidEventHandler(documentConfiguration, generateFile, false);
        when(callback.getEvent()).thenReturn(ADMIN_ACTION_CORRECTION);
        assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
    }

    @Test
    void givenBodyCorrection_doNothing() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        caseData.getPostHearing().getCorrection().setAdminCorrectionType(AdminCorrectionType.BODY);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void givenHeaderCorrection_andFinalDecisionWasGenerated_regenerateCorrectedDocument() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        when(generateFile.assemble(any())).thenReturn(URL);
        when(documentConfiguration.getDocuments()).thenReturn(new HashMap<>(Map.of(
            LanguagePreference.ENGLISH,  new HashMap<>(Map.of(
                DECISION_ISSUED, TEMPLATE_ID)
            ))
        ));

        caseData.getPostHearing().getCorrection().setAdminCorrectionType(AdminCorrectionType.HEADER);
        caseData.setFinalDecisionNoticeGenerated(YesNo.YES);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();

        DocumentLink previewDocument = response.getData().getDocumentStaging().getPreviewDocument();
        assertThat(previewDocument).isNotNull();

        String expectedFilename = String.format("Corrected decision notice issued on %s.pdf",
            LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));

        assertThat(previewDocument.getDocumentFilename()).isEqualTo(expectedFilename);
        assertThat(previewDocument.getDocumentBinaryUrl()).isEqualTo(URL + "/binary");
        assertThat(previewDocument.getDocumentUrl()).isEqualTo(URL);

        verify(generateFile, times(1)).assemble(any());

        verify(generateFile, atLeastOnce()).assemble(capture.capture());

        var value = capture.getValue();
        NoticeIssuedTemplateBody payload = (NoticeIssuedTemplateBody) value.getFormPayload();
        assertThat(payload.getImage()).isEqualTo(NoticeIssuedTemplateBody.ENGLISH_IMAGE);
        assertThat(payload.getNoticeType()).isEqualTo("CORRECTED DECISION NOTICE");
        assertThat(payload.getAppellantFullName()).isEqualTo("Appellant Lastname");
        assertThat(value.getTemplateId()).isEqualTo(TEMPLATE_ID);
    }

    @Test
    void givenHeaderCorrection_andFinalDecisionWasUploaded_doNotGenerateNotice() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        caseData.getPostHearing().getCorrection().setAdminCorrectionType(AdminCorrectionType.HEADER);
        caseData.setFinalDecisionNoticeGenerated(YesNo.NO);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        DocumentLink previewDocument = response.getData().getDocumentStaging().getPreviewDocument();
        assertThat(previewDocument).isNull();

        verifyNoInteractions(generateFile);
    }

    @Test
    void givenOtherPageId_doNothing() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        caseData.getPostHearing().getCorrection().setAdminCorrectionType(AdminCorrectionType.HEADER);

        when(callback.getPageId()).thenReturn("test page id");

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }


    @ParameterizedTest
    @EnumSource(AdminCorrectionType.class)
    void whenAdminCorrectionTypeExists_shouldReturnWithoutError(AdminCorrectionType adminCorrectionType) {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        caseData.getPostHearing().getCorrection().setAdminCorrectionType(adminCorrectionType);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void whenAdminCorrectionTypeIsNull_shouldReturnError() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        caseData.getPostHearing().getCorrection().setAdminCorrectionType(null);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors())
            .hasSize(1)
            .containsExactly("adminCorrectionType unexpectedly null for case: 1234");
    }

}