package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static org.junit.Assert.assertEquals;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@RunWith(JUnitParamsRunner.class)
public class ActionFurtherEvidenceSubmittedCallbackHandlerTest {
    @Mock
    private CcdService ccdService;
    @Mock
    private IdamService idamService;

    @Test
    @Parameters({
        "SUBMITTED,informationReceivedForInterloc,ACTION_FURTHER_EVIDENCE,true",
        "ABOUT_TO_SUBMIT,informationReceivedForInterloc,ACTION_FURTHER_EVIDENCE,false",
        "SUBMITTED,otherDocumentManual,ACTION_FURTHER_EVIDENCE,false",
        "SUBMITTED,informationReceivedForInterloc,APPEAL_RECEIVED,false"
    })
    public void givenCanHandleIsCalled_shouldReturnCorrectResult(CallbackType callbackType, String dynamicListItemCode,
                                                                 EventType eventType, boolean expectedResult) {
        ActionFurtherEvidenceSubmittedCallbackHandler handler =
            new ActionFurtherEvidenceSubmittedCallbackHandler(ccdService, idamService);

        boolean actualResult = handler.canHandle(callbackType,
            buildCallback(dynamicListItemCode, eventType));

        assertEquals(expectedResult, actualResult);
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
}