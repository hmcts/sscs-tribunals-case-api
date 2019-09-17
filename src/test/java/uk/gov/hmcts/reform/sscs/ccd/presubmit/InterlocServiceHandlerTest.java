package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;


public class InterlocServiceHandlerTest {

    private InterlocServiceHandler handler;

    private SscsCaseData sscsCaseData;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private Callback<SscsCaseData> callback;

    @Before
    public void setUp() {
        initMocks(this);
        handler = new InterlocServiceHandler();

        sscsCaseData = SscsCaseData.builder().build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenAInterlocSendToTcwEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.INTERLOC_SEND_TO_TCW);

        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenATcwDirectionIssuedEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.TCW_DIRECTION_ISSUED);

        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenAInterlocInformationReceivedEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.INTERLOC_INFORMATION_RECEIVED);

        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenAJudgeDirectionIssued_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.JUDGE_DIRECTION_ISSUED);

        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenATcwReferToJudgeEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.TCW_REFER_TO_JUDGE);

        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenANonCompliantEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.NON_COMPLIANT);

        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenANonCompliantSendToInterlocEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.NON_COMPLIANT_SEND_TO_INTERLOC);

        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenAReinstateAppealEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.REINSTATE_APPEAL);

        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenATcwDecisionAppealToProceedEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.TCW_DECISION_APPEAL_TO_PROCEED);

        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenAJudgeDecisionAppealToProceedEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.JUDGE_DECISION_APPEAL_TO_PROCEED);

        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenAnUploadFurtherEvidenceEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.UPLOAD_FURTHER_EVIDENCE);

        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void setsCorrectInterlocReviewStatus() {
        when(callback.getEvent()).thenReturn(EventType.TCW_DIRECTION_ISSUED);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback);

        assertThat(response.getData().getInterlocReviewState(), is("awaitingInformation"));
    }

    @Test
    public void resetsInterlocReviewStatusJudgeDecisionAppealToProceed() {
        when(callback.getEvent()).thenReturn(EventType.JUDGE_DECISION_APPEAL_TO_PROCEED);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback);

        assertThat(response.getData().getInterlocReviewState(), is("none"));
    }

    @Test
    public void resetsInterlocReviewStatusTcwDecisionAppealToProceed() {
        when(callback.getEvent()).thenReturn(EventType.TCW_DECISION_APPEAL_TO_PROCEED);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback);

        assertThat(response.getData().getInterlocReviewState(), is("none"));
    }

    @Test
    public void setsCorrectInterlocReviewStatusForUploadFurtherEvidence() {
        when(callback.getEvent()).thenReturn(EventType.UPLOAD_FURTHER_EVIDENCE);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback);

        assertThat(response.getData().getInterlocReviewState(), is("interlocutoryReview"));
    }

    @Test(expected = IllegalStateException.class)
    public void throwExceptionIfCannotHandleEventType() {
        when(callback.getEvent()).thenReturn(EventType.CASE_UPDATED);

        sscsCaseData = SscsCaseData.builder().interlocReviewState("someValue").build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        handler.handle(ABOUT_TO_SUBMIT, callback);
    }

    @Test
    public void checkInterlocDateIsSet() {
        when(callback.getEvent()).thenReturn(EventType.NON_COMPLIANT);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback);

        assertThat(response.getData().getInterlocReferralDate(), is(LocalDate.now().toString()));
    }
}