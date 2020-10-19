package uk.gov.hmcts.reform.sscs.ccd.presubmit.validsendtointerloc;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@RunWith(JUnitParamsRunner.class)
public class ValidSendToInterlocAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private ValidSendToInterlocAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    private SscsCaseData sscsCaseData;


    @Before
    public void setUp() {
        openMocks(this);
        handler = new ValidSendToInterlocAboutToSubmitHandler();

        when(callback.getEvent()).thenReturn(EventType.VALID_SEND_TO_INTERLOC);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder().build())
                .selectWhoReviewsCase(new DynamicList(new DynamicListItem("reviewByTcw", "Review by TCW"), null))
                .directionDueDate("01/02/2020")
                .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    @Parameters({"APPEAL_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    public void givenANonHandleEvidenceEvent_thenReturnFalse(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    @Parameters({"reviewByTcw, Review by TCW", "reviewByJudge, Review by Judge"})
    public void setsEvidenceHandledFlagToNoForDocumentSelected(String code, String selectedLabel) {

        sscsCaseData = sscsCaseData.toBuilder()
                .selectWhoReviewsCase(new DynamicList(
                        new DynamicListItem(code, selectedLabel),
                        Arrays.asList(new DynamicListItem("reviewByJudge", "Review by Judge"),
                                new DynamicListItem("reviewByTcw", "Review by TCW"))
                ))
                .reissueFurtherEvidenceDocument(new DynamicList(new DynamicListItem(code, selectedLabel), null)).build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(Collections.EMPTY_SET, response.getErrors());

        assertEquals(code, response.getData().getInterlocReviewState());
        assertEquals(LocalDate.now().toString(), response.getData().getInterlocReferralDate());
        assertNull(response.getData().getSelectWhoReviewsCase());
        assertNull(response.getData().getDirectionDueDate());
    }

    @Test
    @Parameters({"VALID_SEND_TO_INTERLOC", "ADMIN_SEND_TO_INTERLOCUTORY_REVIEW_STATE"})
    public void returnAnErrorIfNoSelectedDocument(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);

        sscsCaseData = sscsCaseData.toBuilder().selectWhoReviewsCase(null).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertEquals("Must select who reviews the appeal.", response.getErrors().toArray()[0]);
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

}
