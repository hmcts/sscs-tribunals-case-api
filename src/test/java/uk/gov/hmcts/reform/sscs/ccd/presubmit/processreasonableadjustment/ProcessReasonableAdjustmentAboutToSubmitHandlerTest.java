package uk.gov.hmcts.reform.sscs.ccd.presubmit.processreasonableadjustment;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
public class ProcessReasonableAdjustmentAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private ProcessReasonableAdjustmentAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new ProcessReasonableAdjustmentAboutToSubmitHandler();

        when(callback.getEvent()).thenReturn(EventType.PROCESS_REASONABLE_ADJUSTMENT);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder().appellant(Appellant.builder().build()).build()).build();
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonProcessReasonableAdjustmentEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenValidCallback_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenAProcessReasonableAdjustmentEventWithNoReasonableAdjustmentsToProcess_thenShowAnError() {
        List<Correspondence> correspondenceList = new ArrayList<>();

        sscsCaseData.setReasonableAdjustmentsOutstanding(YesNo.YES);
        ReasonableAdjustmentsLetters reasonableAdjustmentsLetters =
                new ReasonableAdjustmentsLetters(correspondenceList, correspondenceList, correspondenceList, correspondenceList, correspondenceList);
        sscsCaseData.setReasonableAdjustmentsLetters(reasonableAdjustmentsLetters);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("No reasonable adjustment correspondence has been generated on this case", response.getErrors().toArray()[0]);
    }

    @Test
    public void givenAProcessReasonableAdjustmentEventWithAllDocsActioned_thenClearReasonableAdjustmentsOutstandingFlag() {
        List<Correspondence> correspondenceList = new ArrayList<>();
        correspondenceList.add(Correspondence.builder().value(CorrespondenceDetails.builder().reasonableAdjustmentStatus(ReasonableAdjustmentStatus.ACTIONED).build()).build());

        sscsCaseData.setReasonableAdjustmentsOutstanding(YesNo.YES);
        ReasonableAdjustmentsLetters reasonableAdjustmentsLetters =
                new ReasonableAdjustmentsLetters(correspondenceList, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        sscsCaseData.setReasonableAdjustmentsLetters(reasonableAdjustmentsLetters);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(YesNo.NO, response.getData().getReasonableAdjustmentsOutstanding());
    }

    @Test
    public void givenARepProcessReasonableAdjustmentEventWithAllDocsActioned_thenClearReasonableAdjustmentsOutstandingFlag() {
        List<Correspondence> correspondenceList = new ArrayList<>();
        correspondenceList.add(Correspondence.builder().value(CorrespondenceDetails.builder().reasonableAdjustmentStatus(ReasonableAdjustmentStatus.ACTIONED).build()).build());

        sscsCaseData.setReasonableAdjustmentsOutstanding(YesNo.YES);
        ReasonableAdjustmentsLetters reasonableAdjustmentsLetters =
                new ReasonableAdjustmentsLetters(Collections.emptyList(), correspondenceList, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        sscsCaseData.setReasonableAdjustmentsLetters(reasonableAdjustmentsLetters);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(YesNo.NO, response.getData().getReasonableAdjustmentsOutstanding());
    }

    @Test
    public void givenAnAppointeeProcessReasonableAdjustmentEventWithAllDocsActioned_thenClearReasonableAdjustmentsOutstandingFlag() {
        List<Correspondence> correspondenceList = new ArrayList<>();
        correspondenceList.add(Correspondence.builder().value(CorrespondenceDetails.builder().reasonableAdjustmentStatus(ReasonableAdjustmentStatus.ACTIONED).build()).build());

        sscsCaseData.setReasonableAdjustmentsOutstanding(YesNo.YES);
        ReasonableAdjustmentsLetters reasonableAdjustmentsLetters =
                new ReasonableAdjustmentsLetters(Collections.emptyList(), Collections.emptyList(), correspondenceList, Collections.emptyList(), Collections.emptyList());
        sscsCaseData.setReasonableAdjustmentsLetters(reasonableAdjustmentsLetters);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(YesNo.NO, response.getData().getReasonableAdjustmentsOutstanding());
    }

    @Test
    public void givenAJointPartyProcessReasonableAdjustmentEventWithAllDocsActioned_thenClearReasonableAdjustmentsOutstandingFlag() {
        List<Correspondence> correspondenceList = new ArrayList<>();
        correspondenceList.add(Correspondence.builder().value(CorrespondenceDetails.builder().reasonableAdjustmentStatus(ReasonableAdjustmentStatus.ACTIONED).build()).build());

        sscsCaseData.setReasonableAdjustmentsOutstanding(YesNo.YES);
        ReasonableAdjustmentsLetters reasonableAdjustmentsLetters =
                new ReasonableAdjustmentsLetters(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), correspondenceList, Collections.emptyList());
        sscsCaseData.setReasonableAdjustmentsLetters(reasonableAdjustmentsLetters);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(YesNo.NO, response.getData().getReasonableAdjustmentsOutstanding());
    }

    @Test
    public void givenAOtherPartyProcessReasonableAdjustmentEventWithAllDocsActioned_thenClearReasonableAdjustmentsOutstandingFlag() {
        List<Correspondence> correspondenceList = new ArrayList<>();
        correspondenceList.add(Correspondence.builder().value(CorrespondenceDetails.builder().reasonableAdjustmentStatus(ReasonableAdjustmentStatus.ACTIONED).build()).build());

        sscsCaseData.setReasonableAdjustmentsOutstanding(YesNo.YES);
        ReasonableAdjustmentsLetters reasonableAdjustmentsLetters =
                new ReasonableAdjustmentsLetters(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), correspondenceList);
        sscsCaseData.setReasonableAdjustmentsLetters(reasonableAdjustmentsLetters);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(YesNo.NO, response.getData().getReasonableAdjustmentsOutstanding());
    }

    @Test
    public void givenAProcessReasonableAdjustmentEventWithOtherPartyDocsStillRequired_thenDoNotClearReasonableAdjustmentsOutstandingFlag() {
        List<Correspondence> correspondenceList = new ArrayList<>();
        correspondenceList.add(Correspondence.builder().value(CorrespondenceDetails.builder().reasonableAdjustmentStatus(ReasonableAdjustmentStatus.ACTIONED).build()).build());
        correspondenceList.add(Correspondence.builder().value(CorrespondenceDetails.builder().reasonableAdjustmentStatus(ReasonableAdjustmentStatus.REQUIRED).build()).build());

        sscsCaseData.setReasonableAdjustmentsOutstanding(YesNo.YES);
        ReasonableAdjustmentsLetters reasonableAdjustmentsLetters =
                ReasonableAdjustmentsLetters.builder().otherParty(correspondenceList).build();
        sscsCaseData.setReasonableAdjustmentsLetters(reasonableAdjustmentsLetters);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertTrue(response.getErrors().isEmpty());
        assertEquals(YesNo.YES, response.getData().getReasonableAdjustmentsOutstanding());
    }

    @Test
    public void givenAProcessReasonableAdjustmentEventWithSomeDocsStillRequired_thenDoNotClearReasonableAdjustmentsOutstandingFlag() {
        List<Correspondence> correspondenceList = new ArrayList<>();
        correspondenceList.add(Correspondence.builder().value(CorrespondenceDetails.builder().reasonableAdjustmentStatus(ReasonableAdjustmentStatus.ACTIONED).build()).build());
        correspondenceList.add(Correspondence.builder().value(CorrespondenceDetails.builder().reasonableAdjustmentStatus(ReasonableAdjustmentStatus.REQUIRED).build()).build());

        sscsCaseData.setReasonableAdjustmentsOutstanding(YesNo.YES);
        ReasonableAdjustmentsLetters reasonableAdjustmentsLetters =
                new ReasonableAdjustmentsLetters(correspondenceList, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        sscsCaseData.setReasonableAdjustmentsLetters(reasonableAdjustmentsLetters);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(YesNo.YES, response.getData().getReasonableAdjustmentsOutstanding());
    }


    @Test
    public void givenAProcessReasonableAdjustmentEventWithSomeDocsWithNullStatus_thenDoNotClearReasonableAdjustmentsOutstandingFlag() {
        List<Correspondence> correspondenceList = new ArrayList<>();
        correspondenceList.add(Correspondence.builder().value(CorrespondenceDetails.builder().reasonableAdjustmentStatus(ReasonableAdjustmentStatus.ACTIONED).build()).build());
        correspondenceList.add(Correspondence.builder().value(CorrespondenceDetails.builder().reasonableAdjustmentStatus(null).build()).build());

        sscsCaseData.setReasonableAdjustmentsOutstanding(YesNo.YES);
        ReasonableAdjustmentsLetters reasonableAdjustmentsLetters =
                new ReasonableAdjustmentsLetters(correspondenceList, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        sscsCaseData.setReasonableAdjustmentsLetters(reasonableAdjustmentsLetters);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(YesNo.YES, response.getData().getReasonableAdjustmentsOutstanding());
    }


    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

}
