package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@RunWith(JUnitParamsRunner.class)
public class ActionFurtherEvidenceAboutToStartHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private ActionFurtherEvidenceAboutToStartHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        initMocks(this);
        handler = new ActionFurtherEvidenceAboutToStartHandler();

        when(callback.getEvent()).thenReturn(EventType.ACTION_FURTHER_EVIDENCE);

        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().build()).build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        ReflectionTestUtils.setField(handler, "issueFurtherEvidenceFeature", false);
    }

    @Test
    public void givenAHandleEvidenceEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    public void givenANonHandleEvidenceEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    @Parameters({
        "true, any, 6, true, true, true, true, true, true",
        "true, null, 2, true, true, false, false, false, false",
        "false, null, 1, false, true, false, false, false, false",
        "false, any, 5, false, true, true, true, true, true"
    })
    public void GivenActionFurtherEvidenceAboutToStart_populateFurtherEvidenceDropdown(
        boolean issueFurtherEvidenceFeature,
        @Nullable String interlocReviewState,
        int expectedListItemSize,
        boolean expectedIssueFurtherEvidenceItem,
        boolean expectedOtherDocumentManualItem,
        boolean informationReceivedForInterlocJudgeItem,
        boolean informationReceivedForInterlocTcwItem,
        boolean sendToInterlocJudgeItem,
        boolean sendToInterlocTcwItem
    ) {

        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().build())
            .interlocReviewState(interlocReviewState).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        ReflectionTestUtils.setField(handler, "issueFurtherEvidenceFeature", issueFurtherEvidenceFeature);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals(expectedIssueFurtherEvidenceItem, "issueFurtherEvidence".equals(getItemCodeInList(
            response.getData().getFurtherEvidenceAction(), "issueFurtherEvidence")));

        assertEquals(expectedOtherDocumentManualItem, "otherDocumentManual".equals(getItemCodeInList(
            response.getData().getFurtherEvidenceAction(), "otherDocumentManual")));

        assertEquals(informationReceivedForInterlocJudgeItem, "informationReceivedForInterlocJudge".equals(
            getItemCodeInList(response.getData().getFurtherEvidenceAction(), "informationReceivedForInterlocJudge")));

        assertEquals(informationReceivedForInterlocTcwItem, "informationReceivedForInterlocTcw".equals(
            getItemCodeInList(response.getData().getFurtherEvidenceAction(), "informationReceivedForInterlocTcw")));

        assertEquals(sendToInterlocJudgeItem, "sendToInterlocReviewByJudge".equals(
            getItemCodeInList(response.getData().getFurtherEvidenceAction(), "sendToInterlocReviewByJudge")));

        assertEquals(sendToInterlocTcwItem, "sendToInterlocReviewByTcw".equals(
            getItemCodeInList(response.getData().getFurtherEvidenceAction(), "sendToInterlocReviewByTcw")));

        assertEquals(expectedListItemSize, response.getData().getFurtherEvidenceAction().getListItems().size());
    }

    private String getItemCodeInList(DynamicList dynamicList, String item) {
        return dynamicList.getListItems().stream()
            .filter(o -> item.equals(o.getCode()))
            .findFirst()
            .map(DynamicListItem::getCode)
            .orElse(null);
    }

    @Test
    public void populateOriginalSenderDropdown_whenCaseHasRep() {
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().rep(Representative.builder().hasRepresentative("Yes").build()).build()).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals("appellant", response.getData().getOriginalSender().getListItems().get(0).getCode());
        assertEquals("dwp", response.getData().getOriginalSender().getListItems().get(1).getCode());
        assertEquals("representative", response.getData().getOriginalSender().getListItems().get(2).getCode());
        assertEquals(3, response.getData().getOriginalSender().getListItems().size());
    }

    @Test
    public void populateOriginalSenderDropdown_whenCaseHasNoRep() {
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().rep(Representative.builder().hasRepresentative("No").build()).build()).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals("appellant", response.getData().getOriginalSender().getListItems().get(0).getCode());
        assertEquals("dwp", response.getData().getOriginalSender().getListItems().get(1).getCode());
        assertEquals(2, response.getData().getOriginalSender().getListItems().size());
    }

    @Test
    public void populateOriginalSenderDropdown_whenCaseHasRepIsNull() {
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().rep(Representative.builder().hasRepresentative(null).build()).build()).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals("appellant", response.getData().getOriginalSender().getListItems().get(0).getCode());
        assertEquals("dwp", response.getData().getOriginalSender().getListItems().get(1).getCode());
        assertEquals(2, response.getData().getOriginalSender().getListItems().size());
    }
}