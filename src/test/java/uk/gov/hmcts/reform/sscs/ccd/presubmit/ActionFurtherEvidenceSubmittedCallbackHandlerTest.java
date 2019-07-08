package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import org.junit.Test;
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

public class ActionFurtherEvidenceSubmittedCallbackHandlerTest {
    @Mock
    private CcdService ccdService;
    @Mock
    private IdamService idamService;

    @Test
    public void givenCanHandleIsCalled_shouldReturnCorrectResult() {
        ActionFurtherEvidenceSubmittedCallbackHandler handler =
            new ActionFurtherEvidenceSubmittedCallbackHandler(ccdService, idamService);

        boolean actualResult = handler.canHandle(CallbackType.SUBMITTED, buildCallback());

        assertTrue(actualResult);
    }

    private Callback<SscsCaseData> buildCallback() {
        DynamicList dynamicList = new DynamicList(new DynamicListItem("informationReceivedForInterloc", "label"),
            Collections.singletonList(new DynamicListItem("informationReceivedForInterloc", "label")));
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .furtherEvidenceAction(dynamicList)
            .build();
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(123L, "sscs",
            State.INTERLOCUTORY_REVIEW_STATE, sscsCaseData, LocalDateTime.now());
        return new Callback<>(caseDetails, Optional.empty(), EventType.ACTION_FURTHER_EVIDENCE);
    }
}