package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.time.LocalDate;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;


@RunWith(JUnitParamsRunner.class)
public class InterlocServiceHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

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
    @Parameters({
        "INTERLOC_SEND_TO_TCW", "TCW_DIRECTION_ISSUED", "INTERLOC_INFORMATION_RECEIVED", "JUDGE_DIRECTION_ISSUED",
        "TCW_REFER_TO_JUDGE", "NON_COMPLIANT", "NON_COMPLIANT_SEND_TO_INTERLOC", "REINSTATE_APPEAL",
        "TCW_DECISION_APPEAL_TO_PROCEED", "JUDGE_DECISION_APPEAL_TO_PROCEED", "UPLOAD_FURTHER_EVIDENCE",
        "SEND_TO_ADMIN"
    })
    public void givenEvent_thenCanHandleReturnTrue(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({
        "TCW_DIRECTION_ISSUED, awaitingInformation", "JUDGE_DECISION_APPEAL_TO_PROCEED, none",
        "TCW_DECISION_APPEAL_TO_PROCEED, none", "UPLOAD_FURTHER_EVIDENCE, interlocutoryReview",
        "SEND_TO_ADMIN, awaitingAdminAction"
    })
    public void givenEvent_thenSetInterlocReviewStateToExpected(EventType eventType,
                                                                String expectedInterlocReviewState) {
        when(callback.getEvent()).thenReturn(eventType);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState(), is(expectedInterlocReviewState));

    }

    @Test(expected = IllegalStateException.class)
    public void throwExceptionIfCannotHandleEventType() {
        when(callback.getEvent()).thenReturn(EventType.CASE_UPDATED);

        sscsCaseData = SscsCaseData.builder().interlocReviewState("someValue").build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

    @Test
    public void checkInterlocDateIsSet() {
        when(callback.getEvent()).thenReturn(EventType.NON_COMPLIANT);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReferralDate(), is(LocalDate.now().toString()));
    }
}