package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import java.time.LocalDateTime;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

public class ValidateInterlocDecisionDocumentHandlerTest {

    private Callback<SscsCaseData> callback;
    private ValidateInterlocDecisionDocumentHandler validateInterlocDecisionDocumentHandler;

    @Before
    public void setUp() {
        callback = mock(Callback.class);
        validateInterlocDecisionDocumentHandler = new ValidateInterlocDecisionDocumentHandler();
    }

    @Test
    public void canHandleTcwDecisionAppealToProceedEvent() {
        when(callback.getEvent()).thenReturn(EventType.TCW_DECISION_APPEAL_TO_PROCEED);
        boolean canHandle = validateInterlocDecisionDocumentHandler.canHandle(CallbackType.ABOUT_TO_SUBMIT, callback);

        assertThat(canHandle, is(true));
    }

    @Test
    public void canHandleJudgeDecisionAppealToProceed() {
        when(callback.getEvent()).thenReturn(EventType.JUDGE_DECISION_APPEAL_TO_PROCEED);
        boolean canHandle = validateInterlocDecisionDocumentHandler.canHandle(CallbackType.ABOUT_TO_SUBMIT, callback);

        assertThat(canHandle, is(true));
    }

    @Test
    public void cannotHandleNonDecisionAppealToProceedEvent() {
        when(callback.getEvent()).thenReturn(EventType.UPLOAD_FURTHER_EVIDENCE);
        boolean canHandle = validateInterlocDecisionDocumentHandler.canHandle(CallbackType.ABOUT_TO_SUBMIT, callback);

        assertThat(canHandle, is(false));
    }

    @Test
    public void cannotHandleNonAboutToSubmitCallbackType() {
        when(callback.getEvent()).thenReturn(EventType.TCW_DECISION_APPEAL_TO_PROCEED);
        boolean canHandle = validateInterlocDecisionDocumentHandler.canHandle(CallbackType.ABOUT_TO_START, callback);

        assertThat(canHandle, is(false));
    }

    @Test
    public void handlesCallback() {
        SscsCaseData sscsCaseData = SscsCaseData.builder()
                .sscsInterlocDecisionDocument(SscsInterlocDecisionDocument.builder()
                        .documentLink(DocumentLink.builder()
                                .documentFilename("SomeDoc.pdf")
                                .build())
                        .build())
                .build();
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(
                1L,
                "sscs",
                State.INTERLOCUTORY_REVIEW_STATE,
                sscsCaseData,
                LocalDateTime.now()
        );
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        PreSubmitCallbackResponse<SscsCaseData> response =
                validateInterlocDecisionDocumentHandler.handle(CallbackType.ABOUT_TO_SUBMIT, callback);

        assertThat(response.getData(), is(sscsCaseData));
        assertThat(response.getErrors().isEmpty(), is(true));
    }

    @Test
    public void errorWhenHandlingCallbackAndInterlocDecisionDocumentHasNotBeenSet() {
        SscsCaseData sscsCaseData = SscsCaseData.builder().build();
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(
                1L,
                "sscs",
                State.INTERLOCUTORY_REVIEW_STATE,
                sscsCaseData,
                LocalDateTime.now()
        );
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        PreSubmitCallbackResponse<SscsCaseData> response =
                validateInterlocDecisionDocumentHandler.handle(CallbackType.ABOUT_TO_SUBMIT, callback);

        assertThat(response.getData(), is(sscsCaseData));
        assertThat(response.getErrors(), is(Sets.newHashSet("Interloc decision document must be set")));
    }

    @Test
    public void errorWhenHandlingCallbackAndInterlocDecisionDocumentIsNotPdf() {
        SscsCaseData sscsCaseData = SscsCaseData.builder()
                .sscsInterlocDecisionDocument(SscsInterlocDecisionDocument.builder()
                        .documentLink(DocumentLink.builder()
                                .documentFilename("SomeDoc.doc")
                                .build())
                        .build())
                .build();
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(
                1L,
                "sscs",
                State.INTERLOCUTORY_REVIEW_STATE,
                sscsCaseData,
                LocalDateTime.now()
        );
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        PreSubmitCallbackResponse<SscsCaseData> response =
                validateInterlocDecisionDocumentHandler.handle(CallbackType.ABOUT_TO_SUBMIT, callback);

        assertThat(response.getData(), is(sscsCaseData));
        assertThat(response.getErrors(), is(Sets.newHashSet("Interloc decision document must be a PDF")));
    }
}