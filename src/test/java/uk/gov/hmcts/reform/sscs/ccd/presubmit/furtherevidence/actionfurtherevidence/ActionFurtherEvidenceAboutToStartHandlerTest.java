package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

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
        openMocks(this);
        handler = new ActionFurtherEvidenceAboutToStartHandler(false);

        when(callback.getEvent()).thenReturn(EventType.ACTION_FURTHER_EVIDENCE);

        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().build()).build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenAHandleEvidenceEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertThatThrownBy(() -> handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void givenANonHandleEvidenceEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    public void givenActionFurtherEvidenceAboutToStartPostHearingsFlagFalse_populateFurtherEvidenceDropdown() {
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().build()).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals("issueFurtherEvidence", getItemCodeInList(
                response.getData().getFurtherEvidenceAction(), "issueFurtherEvidence"));
        assertEquals("otherDocumentManual", getItemCodeInList(
                response.getData().getFurtherEvidenceAction(), "otherDocumentManual"));
        assertEquals("informationReceivedForInterlocJudge", getItemCodeInList(response.getData().getFurtherEvidenceAction(), "informationReceivedForInterlocJudge"));
        assertEquals("informationReceivedForInterlocTcw", getItemCodeInList(response.getData().getFurtherEvidenceAction(), "informationReceivedForInterlocTcw"));
        assertEquals("sendToInterlocReviewByJudge", getItemCodeInList(response.getData().getFurtherEvidenceAction(), "sendToInterlocReviewByJudge"));
        assertEquals("sendToInterlocReviewByTcw", getItemCodeInList(response.getData().getFurtherEvidenceAction(), "sendToInterlocReviewByTcw"));
        assertEquals(6, response.getData().getFurtherEvidenceAction().getListItems().size());
    }

    @Test
    public void givenActionFurtherEvidenceAboutToStartWithPostHearingsFlagEnabled_populateFurtherEvidenceDropdown() {
        handler = new ActionFurtherEvidenceAboutToStartHandler(true);
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().build()).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals("issueFurtherEvidence", getItemCodeInList(
            response.getData().getFurtherEvidenceAction(), "issueFurtherEvidence"));
        assertEquals("otherDocumentManual", getItemCodeInList(
            response.getData().getFurtherEvidenceAction(), "otherDocumentManual"));
        assertEquals("informationReceivedForInterlocJudge", getItemCodeInList(response.getData().getFurtherEvidenceAction(), "informationReceivedForInterlocJudge"));
        assertEquals("informationReceivedForInterlocTcw", getItemCodeInList(response.getData().getFurtherEvidenceAction(), "informationReceivedForInterlocTcw"));
        assertEquals("sendToInterlocReviewByJudge", getItemCodeInList(response.getData().getFurtherEvidenceAction(), "sendToInterlocReviewByJudge"));
        assertEquals("sendToInterlocReviewByTcw", getItemCodeInList(response.getData().getFurtherEvidenceAction(), "sendToInterlocReviewByTcw"));
        assertEquals("adminActionCorrection", getItemCodeInList(response.getData().getFurtherEvidenceAction(), "adminActionCorrection"));
        assertEquals(7, response.getData().getFurtherEvidenceAction().getListItems().size());
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
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().rep(Representative.builder().hasRepresentative(YES.getValue()).build()).build()).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals(4, response.getData().getOriginalSender().getListItems().size());
        assertEquals("appellant", response.getData().getOriginalSender().getListItems().get(0).getCode());
        assertEquals("representative", response.getData().getOriginalSender().getListItems().get(1).getCode());
        assertEquals("dwp", response.getData().getOriginalSender().getListItems().get(2).getCode());
        assertEquals("hmcts", response.getData().getOriginalSender().getListItems().get(3).getCode());
    }

    @Test
    public void populateOriginalSenderDropdown_whenCaseHasNoRep() {
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().rep(Representative.builder().hasRepresentative(NO.getValue()).build()).build()).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals(3, response.getData().getOriginalSender().getListItems().size());
        assertEquals("appellant", response.getData().getOriginalSender().getListItems().get(0).getCode());
        assertEquals("dwp", response.getData().getOriginalSender().getListItems().get(1).getCode());
        assertEquals("hmcts", response.getData().getOriginalSender().getListItems().get(2).getCode());
    }

    @Test
    public void populateOriginalSenderDropdown_whenCaseHasRepIsNullAndThereIsAJointParty() {
        sscsCaseData = SscsCaseData.builder().jointParty(JointParty.builder().hasJointParty(YES).build()).appeal(Appeal.builder().rep(Representative.builder().hasRepresentative(null).build()).build()).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals(4, response.getData().getOriginalSender().getListItems().size());
        assertEquals("appellant", response.getData().getOriginalSender().getListItems().get(0).getCode());
        assertEquals("jointParty", response.getData().getOriginalSender().getListItems().get(1).getCode());
        assertEquals("dwp", response.getData().getOriginalSender().getListItems().get(2).getCode());
        assertEquals("hmcts", response.getData().getOriginalSender().getListItems().get(3).getCode());
    }

    @Test
    public void populateOriginalSenderDropdown_whenCaseHasRepAndJointParty() {
        sscsCaseData = SscsCaseData.builder().jointParty(JointParty.builder().hasJointParty(YES).build()).appeal(Appeal.builder().rep(Representative.builder().hasRepresentative(YES.getValue()).build()).build()).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals(5, response.getData().getOriginalSender().getListItems().size());
        assertEquals("appellant", response.getData().getOriginalSender().getListItems().get(0).getCode());
        assertEquals("jointParty", response.getData().getOriginalSender().getListItems().get(1).getCode());
        assertEquals("representative", response.getData().getOriginalSender().getListItems().get(2).getCode());
        assertEquals("dwp", response.getData().getOriginalSender().getListItems().get(3).getCode());
        assertEquals("hmcts", response.getData().getOriginalSender().getListItems().get(4).getCode());
    }
}
