package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ACTION_FURTHER_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@RunWith(JUnitParamsRunner.class)
public class ActionFurtherEvidenceReceivedHandlerTest {

    private ActionFurtherEvidenceReceivedHandler handler;

    @Before
    public void setUp() {
        handler = new ActionFurtherEvidenceReceivedHandler();
    }


    @Test
    @Parameters(method = "generateCanHandleScenarios")
    public void givenCanHandleIsCalled_shouldReturnCorrectResult(CallbackType callbackType,
                                                                 Callback<SscsCaseData> callback,
                                                                 boolean expectedResult) {
        boolean actualResult = handler.canHandle(callbackType, callback);

        assertEquals(expectedResult, actualResult);
    }

    private Object[] generateCanHandleScenarios() {
        Callback<SscsCaseData> callbacWithRightEventAndRightField =
            buildCallback("issueFurtherEvidence", ACTION_FURTHER_EVIDENCE);
        Callback<SscsCaseData> callbacWithRightEventAndWrongField =
            buildCallback("otherDocumentManual", ACTION_FURTHER_EVIDENCE);
        Callback<SscsCaseData> callbacWithWrongEventAndRightField =
            buildCallback("informationReceivedForInterloc", APPEAL_RECEIVED);

        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(123L, "sscs",
            State.INTERLOCUTORY_REVIEW_STATE, SscsCaseData.builder().build(), LocalDateTime.now());

        Callback<SscsCaseData> callbackWithRightEventAndNullField = new Callback<>(caseDetails, Optional.empty(),
            ACTION_FURTHER_EVIDENCE);

        return new Object[]{
            new Object[]{ABOUT_TO_SUBMIT, callbacWithRightEventAndRightField, true},
            new Object[]{SUBMITTED, callbacWithRightEventAndRightField, false},
            new Object[]{ABOUT_TO_SUBMIT, callbacWithRightEventAndWrongField, false},
            new Object[]{ABOUT_TO_SUBMIT, callbacWithWrongEventAndRightField, false},
            new Object[]{ABOUT_TO_SUBMIT, callbackWithRightEventAndNullField, false}
        };
    }

    private Callback<SscsCaseData> buildCallback(String dynamicListItemCode, EventType eventType) {
        DynamicList dynamicList = new DynamicList(new DynamicListItem(dynamicListItemCode, "label"),
            Collections.singletonList(new DynamicListItem(dynamicListItemCode, "label")));
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .furtherEvidenceAction(dynamicList)
            .build();
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(123L, "sscs",
            State.INTERLOCUTORY_REVIEW_STATE, sscsCaseData, LocalDateTime.now());
        return new Callback<>(caseDetails, Optional.empty(), eventType);
    }

    @Test
    public void givenHandleMethodIsCalled_shouldUpdateCaseCorrectly() {
        Callback<SscsCaseData> callback = buildCallback("issueFurtherEvidence",
            ACTION_FURTHER_EVIDENCE);


        PreSubmitCallbackResponse<SscsCaseData> updated = handler.handle(ABOUT_TO_SUBMIT, callback);

        assertEquals("furtherEvidenceReceived", updated.getData().getDwpFurtherEvidenceStates());
    }
}