package uk.gov.hmcts.reform.sscs.ccd.presubmit.adminactioncorrection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ADMIN_ACTION_CORRECTION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.READY_TO_LIST;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.service.FooterService;

@ExtendWith(MockitoExtension.class)
class AdminActionCorrectionAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private AdminActionCorrectionAboutToSubmitHandler handler;
    @Mock
    private FooterService footerService;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData caseData;

    private DocumentLink docLink;

    @BeforeEach
    void setUp() {
        handler = new AdminActionCorrectionAboutToSubmitHandler(footerService, true);

        docLink = DocumentLink.builder()
                .documentFilename("z.pdf")
                .documentUrl("url")
                .documentBinaryUrl("url/binary")
                .build();
        SscsDocument draftDecisionNotice = SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                        .documentType(DRAFT_DECISION_NOTICE.getValue())
                        .build())
                .build();
        caseData = SscsCaseData.builder()
                .finalDecisionCaseData(SscsFinalDecisionCaseData.builder()
                        .writeFinalDecisionPreviewDocument(docLink)
                        .build())
                .sscsDocument(new ArrayList<>(List.of(draftDecisionNotice)))
                .ccdCaseId("1234")
                .build();
        caseData.setAppeal(Appeal.builder().hearingOptions(HearingOptions.builder().build()).build());
        caseData.getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());
    }

    @Test
    void givenAValidAboutToSubmitEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(ADMIN_ACTION_CORRECTION);
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
        handler = new AdminActionCorrectionAboutToSubmitHandler(footerService, false);
        when(callback.getEvent()).thenReturn(ADMIN_ACTION_CORRECTION);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @Test
    void shouldReturnWithoutError() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void givenHeaderCorrection_shouldUpdatePreviousStateWhenCurrentStateIsNotReadyToListOrWithFta() {
        when(callback.getEvent()).thenReturn(ADMIN_ACTION_CORRECTION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        caseData.setPreviousState(State.VOID_STATE);
        caseData.setState(State.APPEAL_CREATED);
        Correction correction = caseData.getPostHearing().getCorrection();
        correction.setAdminCorrectionType(AdminCorrectionType.HEADER);
        correction.setIsCorrectionFinalDecisionInProgress(YesNo.YES);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(footerService).createFooterAndAddDocToCase(docLink,
                caseData,
                DocumentType.CORRECTED_DECISION_NOTICE,
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
                null,
                null,
                null);
    }
}