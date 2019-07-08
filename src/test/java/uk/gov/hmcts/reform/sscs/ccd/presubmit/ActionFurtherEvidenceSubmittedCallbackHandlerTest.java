package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.ActionFurtherEvidenceSubmittedCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@RunWith(JUnitParamsRunner.class)
public class ActionFurtherEvidenceSubmittedCallbackHandlerTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();
    @Mock
    private CcdService ccdService;
    @Mock
    private IdamService idamService;

    private ActionFurtherEvidenceSubmittedCallbackHandler handler;

    @Before
    public void setUp() {
        handler = new ActionFurtherEvidenceSubmittedCallbackHandler(ccdService, idamService);
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
            buildCallback("informationReceivedForInterloc", ACTION_FURTHER_EVIDENCE);
        Callback<SscsCaseData> callbacWithRightEventAndWrongField =
            buildCallback("otherDocumentManual", ACTION_FURTHER_EVIDENCE);
        Callback<SscsCaseData> callbacWithWrongEventAndRightField =
            buildCallback("informationReceivedForInterloc", APPEAL_RECEIVED);

        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(123L, "sscs",
            State.INTERLOCUTORY_REVIEW_STATE, SscsCaseData.builder().build(), LocalDateTime.now());

        Callback<SscsCaseData> callbacWithRightEventAndNullField = new Callback<>(caseDetails, Optional.empty(),
            ACTION_FURTHER_EVIDENCE);

        return new Object[]{
            new Object[]{SUBMITTED, callbacWithRightEventAndRightField, true},
            new Object[]{ABOUT_TO_SUBMIT, callbacWithRightEventAndRightField, false},
            new Object[]{SUBMITTED, callbacWithRightEventAndWrongField, false},
            new Object[]{SUBMITTED, callbacWithWrongEventAndRightField, false},
            new Object[]{SUBMITTED, callbacWithRightEventAndNullField, false}
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
        Callback<SscsCaseData> callback = buildCallback("informationReceivedForInterloc",
            ACTION_FURTHER_EVIDENCE);

        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder().build());

        ArgumentCaptor<SscsCaseData> captor = ArgumentCaptor.forClass(SscsCaseData.class);

        given(ccdService.updateCase(captor.capture(), anyLong(), eq("interlocInformationReceived"),
            anyString(), anyString(), any(IdamTokens.class)))
            .willReturn(SscsCaseDetails.builder().data(SscsCaseData.builder().build()).build());

        handler.handle(SUBMITTED, callback);

        assertEquals("interlocutoryReview", captor.getValue().getInterlocReviewState());
    }
}