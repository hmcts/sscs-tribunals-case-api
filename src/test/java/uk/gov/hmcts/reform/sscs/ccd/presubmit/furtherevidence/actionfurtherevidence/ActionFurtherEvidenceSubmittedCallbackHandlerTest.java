package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
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
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@RunWith(JUnitParamsRunner.class)
public class ActionFurtherEvidenceSubmittedCallbackHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();
    @Mock
    private CcdService ccdService;
    @Mock
    private IdamService idamService;

    private ActionFurtherEvidenceSubmittedCallbackHandler handler;

    @Before
    public void setUp() {
        handler = new ActionFurtherEvidenceSubmittedCallbackHandler(ccdService, idamService, false, false);
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
        Callback<SscsCaseData> callbackWithValidEventAndInformationReceivedForInterlocTcwOption =
                buildCallback("informationReceivedForInterlocTcw", ACTION_FURTHER_EVIDENCE);
        Callback<SscsCaseData> callbackWithValidEventAndInformationReceivedForInterlocJudgeOption =
                buildCallback("informationReceivedForInterlocJudge", ACTION_FURTHER_EVIDENCE);
        Callback<SscsCaseData> callbackWithValidEventAndIssueFurtherEvidenceOption =
                buildCallback("issueFurtherEvidence", ACTION_FURTHER_EVIDENCE);
        Callback<SscsCaseData> callbackWithValidEventAndOtherDocumentManual =
                buildCallback("otherDocumentManual", ACTION_FURTHER_EVIDENCE);
        Callback<SscsCaseData> callbackWithWrongEventAndValidOption =
                buildCallback("informationReceivedForInterlocJudge", APPEAL_RECEIVED);
        Callback<SscsCaseData> callbackWithValidEventAndSendToInterlocReviewByJudgeOption =
                buildCallback("sendToInterlocReviewByJudge", ACTION_FURTHER_EVIDENCE);
        Callback<SscsCaseData> callbackWithValidEventAndSendToInterlocReviewByTcwOption =
                buildCallback("sendToInterlocReviewByTcw", ACTION_FURTHER_EVIDENCE);

        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(123L, "sscs",
                State.INTERLOCUTORY_REVIEW_STATE, SscsCaseData.builder().build(), LocalDateTime.now(), "Benefit");

        Callback<SscsCaseData> callbackWithRightEventAndNullField = new Callback<>(caseDetails, Optional.empty(),
                ACTION_FURTHER_EVIDENCE, false);

        return new Object[]{
            new Object[]{SUBMITTED, callbackWithValidEventAndInformationReceivedForInterlocTcwOption, true},
            new Object[]{SUBMITTED, callbackWithValidEventAndInformationReceivedForInterlocJudgeOption, true},
            new Object[]{SUBMITTED, callbackWithValidEventAndIssueFurtherEvidenceOption, true},
            new Object[]{ABOUT_TO_SUBMIT, callbackWithValidEventAndInformationReceivedForInterlocTcwOption, false},
            new Object[]{SUBMITTED, callbackWithValidEventAndOtherDocumentManual, true},
            new Object[]{SUBMITTED, callbackWithWrongEventAndValidOption, false},
            new Object[]{SUBMITTED, callbackWithRightEventAndNullField, false},
            new Object[]{SUBMITTED, callbackWithValidEventAndSendToInterlocReviewByJudgeOption, true},
            new Object[]{SUBMITTED, callbackWithValidEventAndSendToInterlocReviewByTcwOption, true}};
    }

    private Callback<SscsCaseData> buildCallback(String dynamicListItemCode, EventType eventType) {
        DynamicList dynamicList = new DynamicList(new DynamicListItem(dynamicListItemCode, "label"),
                Collections.singletonList(new DynamicListItem(dynamicListItemCode, "label")));
        SscsCaseData sscsCaseData = SscsCaseData.builder()
                .furtherEvidenceAction(dynamicList)
                .build();
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(123L, "sscs",
                State.INTERLOCUTORY_REVIEW_STATE, sscsCaseData, LocalDateTime.now(), "Benefit");
        return new Callback<>(caseDetails, Optional.empty(), eventType, false);
    }

    @Test
    public void givenIssueToAllParties_shouldUpdateCaseCorrectly() {
        Callback<SscsCaseData> callback = buildCallback("issueFurtherEvidence",
                ACTION_FURTHER_EVIDENCE);

        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder().build());

        ArgumentCaptor<SscsCaseData> captor = ArgumentCaptor.forClass(SscsCaseData.class);

        given(ccdService.updateCase(captor.capture(), anyLong(), eq("issueFurtherEvidence"),
                anyString(), anyString(), any(IdamTokens.class)))
                .willReturn(SscsCaseDetails.builder().data(SscsCaseData.builder().build()).build());

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertNull(captor.getValue().getInterlocReviewState());
    }

    @Test
    @Parameters({
        "informationReceivedForInterlocJudge, REVIEW_BY_JUDGE, interlocInformationReceivedActionFurtherEvidence",
        "informationReceivedForInterlocTcw, REVIEW_BY_TCW, interlocInformationReceivedActionFurtherEvidence",
        "sendToInterlocReviewByJudge, REVIEW_BY_JUDGE, validSendToInterloc",
        "sendToInterlocReviewByTcw, REVIEW_BY_TCW, validSendToInterloc",
        "adminActionCorrection, AWAITING_ADMIN_ACTION, correctionRequest"
    })
    public void givenFurtherEvidenceActionSelectedOption_shouldTriggerEventAndUpdateCaseCorrectly(
            String furtherEvidenceActionSelectedOption, InterlocReviewState interlocReviewState, String eventType) {

        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder().build());

        ArgumentCaptor<SscsCaseData> captor = ArgumentCaptor.forClass(SscsCaseData.class);

        given(ccdService.updateCase(captor.capture(), anyLong(), eq(eventType), anyString(), anyString(),
                any(IdamTokens.class)))
                .willReturn(SscsCaseDetails.builder().data(SscsCaseData.builder().build()).build());

        Callback<SscsCaseData> callback = buildCallback(furtherEvidenceActionSelectedOption, ACTION_FURTHER_EVIDENCE);

        handler = new ActionFurtherEvidenceSubmittedCallbackHandler(ccdService, idamService, true, false);
        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertEquals(interlocReviewState, captor.getValue().getInterlocReviewState());

        if (furtherEvidenceActionSelectedOption.equals("informationReceivedForInterlocJudge")
                || furtherEvidenceActionSelectedOption.equals("informationReceivedForInterlocTcw")) {
            assertThat(captor.getValue().getInterlocReferralDate(), is(LocalDate.now()));
        }

        then(ccdService).should(times(1))
                .updateCase(eq(callback.getCaseDetails().getCaseData()), eq(123L), eq(eventType), anyString(),
                        anyString(), any(IdamTokens.class));
    }

    @Test
    @Parameters({
        "SET_ASIDE, setAsideRequest",
        "CORRECTION, correctionRequest",
        "STATEMENT_OF_REASONS, sORRequest",
        "LIBERTY_TO_APPLY, libertyToApplyRequest"
    })
    public void givenPostHearingAndFurtherEvidenceActionIsReviewByJudge_shouldTriggerEventAndUpdateCaseCorrectly(
        PostHearingRequestType requestType, String eventType) {

        Callback<SscsCaseData> callback = buildCallback("sendToInterlocReviewByJudge", ACTION_FURTHER_EVIDENCE);
        callback.getCaseDetails().getCaseData().getPostHearing().setRequestType(requestType);

        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder().build());

        ArgumentCaptor<SscsCaseData> captor = ArgumentCaptor.forClass(SscsCaseData.class);

        given(ccdService.updateCase(captor.capture(), anyLong(), eq(eventType), anyString(), anyString(),
            any(IdamTokens.class)))
            .willReturn(SscsCaseDetails.builder().data(SscsCaseData.builder().build()).build());

        handler = new ActionFurtherEvidenceSubmittedCallbackHandler(ccdService, idamService, true, true);
        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertEquals(InterlocReviewState.REVIEW_BY_JUDGE, captor.getValue().getInterlocReviewState());

        then(ccdService).should(times(1))
            .updateCase(eq(callback.getCaseDetails().getCaseData()), eq(123L), eq(eventType), anyString(),
                anyString(), any(IdamTokens.class));
    }

    @Test
    @Parameters({
        "PERMISSION_TO_APPEAL, permissionToAppealRequest"
    })
    public void givenPostHearingNotImplementedAndFurtherEvidenceActionIsReviewByJudge_shouldThrowException(
        PostHearingRequestType requestType, String eventType) {

        Callback<SscsCaseData> callback = buildCallback("sendToInterlocReviewByJudge", ACTION_FURTHER_EVIDENCE);
        callback.getCaseDetails().getCaseData().getPostHearing().setRequestType(requestType);

        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder().build());

        ArgumentCaptor<SscsCaseData> captor = ArgumentCaptor.forClass(SscsCaseData.class);

        given(ccdService.updateCase(captor.capture(), anyLong(), eq(eventType), anyString(), anyString(),
            any(IdamTokens.class)))
            .willReturn(SscsCaseDetails.builder().data(SscsCaseData.builder().build()).build());

        handler = new ActionFurtherEvidenceSubmittedCallbackHandler(ccdService, idamService, true, true);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> handler.handle(SUBMITTED, callback, USER_AUTHORISATION));
        assertEquals(String.format("Post hearing request type is not implemented or recognised: %s", requestType), exception.getMessage());
    }

    @Test
    @Parameters({
        "LIBERTY_TO_APPLY, libertyToApplyRequest"
    })
    public void givenPostHearingsBNotEnabledAndFurtherEvidenceActionIsReviewByJudge_shouldThrowException(
        PostHearingRequestType requestType, String eventType) {

        Callback<SscsCaseData> callback = buildCallback("sendToInterlocReviewByJudge", ACTION_FURTHER_EVIDENCE);
        callback.getCaseDetails().getCaseData().getPostHearing().setRequestType(requestType);

        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder().build());

        ArgumentCaptor<SscsCaseData> captor = ArgumentCaptor.forClass(SscsCaseData.class);

        given(ccdService.updateCase(captor.capture(), anyLong(), eq(eventType), anyString(), anyString(),
            any(IdamTokens.class)))
            .willReturn(SscsCaseDetails.builder().data(SscsCaseData.builder().build()).build());

        handler = new ActionFurtherEvidenceSubmittedCallbackHandler(ccdService, idamService, true, false);
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> handler.handle(SUBMITTED, callback, USER_AUTHORISATION));
        assertEquals("Post hearings B is not enabled", exception.getMessage());
    }

    @Test
    public void givenFurtherEvidenceActionSelectedOptionAndUrgentCaseFlagNotSet_shouldTriggerUrgentCaseEventAndUpdateCaseCorrectly() {
        SscsDocument sscsDocument = SscsDocument.builder().value(
                SscsDocumentDetails.builder()
                        .documentType(DocumentType.URGENT_HEARING_REQUEST.getValue())
                        .documentFileName("bla.pdf")
                        .documentLink(DocumentLink.builder().documentUrl("www.test.com").build())
                        .documentDateAdded("2019-06-12T00:00:00.000")
                        .controlNumber("123")
                        .build()).build();

        Callback<SscsCaseData> callback = buildCallback(FurtherEvidenceActionDynamicListItems.OTHER_DOCUMENT_MANUAL.code, ACTION_FURTHER_EVIDENCE);

        callback.getCaseDetails().getCaseData().setSscsDocument(List.of(sscsDocument));
        callback.getCaseDetails().getCaseData().setUrgentCase(null);
        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder().build());

        ArgumentCaptor<SscsCaseData> captor = ArgumentCaptor.forClass(SscsCaseData.class);

        given(ccdService.updateCase(captor.capture(), anyLong(), eq(MAKE_CASE_URGENT.getCcdType()), anyString(), anyString(),
                any(IdamTokens.class)))
                .willReturn(SscsCaseDetails.builder().data(SscsCaseData.builder().build()).build());

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        then(ccdService).should(times(1))
                .updateCase(eq(callback.getCaseDetails().getCaseData()), eq(123L), eq(MAKE_CASE_URGENT.getCcdType()), anyString(),
                        anyString(), any(IdamTokens.class));
    }

    @Test
    public void givenFurtherEvidenceActionSelectedOptionAndUrgentCaseFlagIsSet_shouldNotTriggerUrgentCaseEventAndUpdateCaseCorrectly() {
        SscsDocument sscsDocument = SscsDocument.builder().value(
                SscsDocumentDetails.builder()
                        .documentType(DocumentType.URGENT_HEARING_REQUEST.getValue())
                        .documentFileName("bla.pdf")
                        .documentLink(DocumentLink.builder().documentUrl("www.test.com").build())
                        .documentDateAdded("2019-06-12T00:00:00.000")
                        .controlNumber("123")
                        .build()).build();

        Callback<SscsCaseData> callback = buildCallback(FurtherEvidenceActionDynamicListItems.OTHER_DOCUMENT_MANUAL.code, ACTION_FURTHER_EVIDENCE);

        callback.getCaseDetails().getCaseData().setSscsDocument(List.of(sscsDocument));
        callback.getCaseDetails().getCaseData().setUrgentCase("Yes");
        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder().build());

        ArgumentCaptor<SscsCaseData> captor = ArgumentCaptor.forClass(SscsCaseData.class);

        given(ccdService.updateCase(captor.capture(), anyLong(), eq(ISSUE_FURTHER_EVIDENCE.getCcdType()), anyString(), anyString(),
                any(IdamTokens.class)))
                .willReturn(SscsCaseDetails.builder().data(SscsCaseData.builder().build()).build());

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        then(ccdService).should(times(1))
                .updateCase(eq(callback.getCaseDetails().getCaseData()), eq(123L), eq(ISSUE_FURTHER_EVIDENCE.getCcdType()), anyString(),
                        anyString(), any(IdamTokens.class));
    }

    @Test
    public void givenFurtherEvidenceActionSelectedOptionWithManualDocument_shouldUpdateCaseCorrectly() {

        Callback<SscsCaseData> callback = buildCallback(FurtherEvidenceActionDynamicListItems.OTHER_DOCUMENT_MANUAL.code, ACTION_FURTHER_EVIDENCE);

        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder().build());

        ArgumentCaptor<SscsCaseData> captor = ArgumentCaptor.forClass(SscsCaseData.class);

        given(ccdService.updateCase(captor.capture(), anyLong(), eq(ISSUE_FURTHER_EVIDENCE.getCcdType()), anyString(), anyString(),
                any(IdamTokens.class)))
                .willReturn(SscsCaseDetails.builder().data(SscsCaseData.builder().build()).build());

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        then(ccdService).should(times(1))
                .updateCase(eq(callback.getCaseDetails().getCaseData()), eq(123L), eq(ISSUE_FURTHER_EVIDENCE.getCcdType()), eq("Actioned manually"),
                        eq("Actioned manually"), any(IdamTokens.class));
    }

    @Test
    public void givenFurtherEvidenceActionPostponementRequest_shouldTriggerEventAndUpdateCaseCorrectly() {

        String eventType = "validSendToInterloc";

        Callback<SscsCaseData> callback = buildCallback(
                FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_TCW.getCode(),
                ACTION_FURTHER_EVIDENCE);

        callback.getCaseDetails().getCaseData().setSscsDocument(Arrays.asList(SscsDocument.builder()
                .value(SscsDocumentDetails.builder().documentType(DocumentType.POSTPONEMENT_REQUEST.getValue()).build())
                .build()));

        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder().build());

        ArgumentCaptor<SscsCaseData> captor = ArgumentCaptor.forClass(SscsCaseData.class);

        given(ccdService.updateCase(captor.capture(), anyLong(), eq(eventType), anyString(), anyString(),
                any(IdamTokens.class)))
                .willReturn(SscsCaseDetails.builder().data(SscsCaseData.builder().build()).build());

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertEquals(InterlocReviewState.REVIEW_BY_TCW, captor.getValue().getInterlocReviewState());
        assertEquals(InterlocReferralReason.REVIEW_POSTPONEMENT_REQUEST, captor.getValue().getInterlocReferralReason());

        then(ccdService).should(times(1))
                .updateCase(eq(callback.getCaseDetails().getCaseData()), eq(123L), eq(eventType),
                        eq(ActionFurtherEvidenceSubmittedCallbackHandler.TCW_REVIEW_SEND_TO_JUDGE),
                        anyString(), any(IdamTokens.class));
    }
}
