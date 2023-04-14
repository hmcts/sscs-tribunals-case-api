package uk.gov.hmcts.reform.sscs.ccd.presubmit.posthearingreview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;

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

@ExtendWith(MockitoExtension.class)
class PostHearingReviewAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private PostHearingReviewAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData caseData;

    @BeforeEach
    void setUp() {
        handler = new PostHearingReviewAboutToSubmitHandler(true);

        caseData = SscsCaseData.builder()
            .schedulingAndListingFields(SchedulingAndListingFields.builder()
                .hearingRoute(LIST_ASSIST).build())
            .ccdCaseId("1234")
            .documentGeneration(DocumentGeneration.builder()
                .directionNoticeContent("Body Content")
                .build())
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
        handler = new PostHearingReviewAboutToSubmitHandler(false);
        when(callback.getEvent()).thenReturn(POST_HEARING_REVIEW);
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
    void givenPreviewDocumentHasBeenSetAndCorrectionsRefused_thenAddToDocumentsTab() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        caseData.getDocumentStaging().setPreviewDocument(DocumentLink.builder().documentUrl("document url test").build());
        caseData.getPostHearing().getCorrection().setAction(CorrectionActions.REFUSE);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();

        List<SscsDocument> documents = response.getData().getSscsDocument();
        assertThat(documents)
            .hasSize(1)
            .element(0)
            .extracting(doc -> doc.getValue().getDocumentType())
            .isEqualTo(DocumentType.CORRECTION_REFUSED.getValue());
    }

    @Test
    void givenPreviewDocumentHasBeenSetAndSetAsideRefused_thenAddToDocumentsTab() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        caseData.getDocumentStaging().setPreviewDocument(DocumentLink.builder().documentUrl("document url test").build());
        caseData.getPostHearing().getSetAside().setAction(SetAsideActions.REFUSE);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        List<SscsDocument> documents = response.getData().getSscsDocument();
        assertThat(documents).hasSize(1);
        assertThat(documents.get(0).getValue().getDocumentType()).isEqualTo(DocumentType.SET_ASIDE_REFUSED.getValue());
    }
    
    @Test
    void givenHearingIsNull_thenCaseStatusNotChanged() {
        caseData.setState(State.DORMANT_APPEAL_STATE);
        handler.updateCaseStatus(caseData);
        assertThat(caseData.getState()).isEqualTo(State.DORMANT_APPEAL_STATE);
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
