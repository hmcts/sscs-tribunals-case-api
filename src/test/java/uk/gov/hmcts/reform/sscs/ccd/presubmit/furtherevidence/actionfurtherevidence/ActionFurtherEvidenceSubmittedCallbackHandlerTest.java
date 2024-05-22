package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_JUDGE;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
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
    private UpdateCcdCaseService updateCcdCaseService;

    @Mock
    private CcdClient ccdClient;

    @Mock
    private SscsCcdConvertService sscsCcdConvertService;

    @Mock
    private IdamService idamService;

    private ActionFurtherEvidenceSubmittedCallbackHandler handler;

    @Before
    public void setUp() {
        handler = new ActionFurtherEvidenceSubmittedCallbackHandler(ccdService, updateCcdCaseService, ccdClient, sscsCcdConvertService, idamService, false, false);
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

        var idamTokens = IdamTokens.builder().build();
        given(idamService.getIdamTokens()).willReturn(idamTokens);

        var startEventResponse = StartEventResponse.builder()
            .caseDetails(
                uk.gov.hmcts.reform.ccd.client.model.CaseDetails.builder().build()
            ).build();

        given(ccdClient.startEvent(idamTokens, 123L, UPDATE_CASE_ONLY.getCcdType())).willReturn(startEventResponse);
        var sscsCaseData = callback.getCaseDetails().getCaseData();
        given(sscsCcdConvertService.getCaseData(startEventResponse.getCaseDetails().getData())).willReturn(sscsCaseData);

        ArgumentCaptor<Consumer<SscsCaseData>> captor = ArgumentCaptor.forClass(Consumer.class);

        given(updateCcdCaseService.triggerCaseEventV2(anyLong(), eq("issueFurtherEvidence"),
            anyString(), anyString(), eq(idamTokens)))
            .willReturn(SscsCaseDetails.builder().data(sscsCaseData).build());

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verify(updateCcdCaseService).triggerCaseEventV2(anyLong(), eq("issueFurtherEvidence"),
            anyString(), anyString(), eq(idamTokens));
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
        var idamTokens = IdamTokens.builder().build();
        given(idamService.getIdamTokens()).willReturn(idamTokens);

        var startEventResponse = StartEventResponse.builder()
            .caseDetails(
                uk.gov.hmcts.reform.ccd.client.model.CaseDetails.builder().build()
            ).build();

        given(ccdClient.startEvent(idamTokens, 123L, UPDATE_CASE_ONLY.getCcdType())).willReturn(startEventResponse);

        Callback<SscsCaseData> callback = buildCallback(furtherEvidenceActionSelectedOption, ACTION_FURTHER_EVIDENCE);
        var sscsCaseData = callback.getCaseDetails().getCaseData();
        given(sscsCcdConvertService.getCaseData(startEventResponse.getCaseDetails().getData())).willReturn(sscsCaseData);

        given(updateCcdCaseService.updateCaseV2(anyLong(), eq(eventType), anyString(), anyString(),
            eq(idamTokens), any(Consumer.class)))
            .willReturn(SscsCaseDetails.builder().data(sscsCaseData).build());

        handler = new ActionFurtherEvidenceSubmittedCallbackHandler(ccdService, updateCcdCaseService, ccdClient, sscsCcdConvertService, idamService, true, false);

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        ArgumentCaptor<Consumer<SscsCaseData>> captor = ArgumentCaptor.forClass(Consumer.class);

        then(updateCcdCaseService).should(times(1))
            .updateCaseV2(eq(123L), eq(eventType), anyString(),
                anyString(), eq(idamTokens), captor.capture());

        captor.getValue().accept(sscsCaseData);

        assertEquals(interlocReviewState, sscsCaseData.getInterlocReviewState());

        if (furtherEvidenceActionSelectedOption.equals("informationReceivedForInterlocJudge")
            || furtherEvidenceActionSelectedOption.equals("informationReceivedForInterlocTcw")) {
            assertThat(sscsCaseData.getInterlocReferralDate(), is(LocalDate.now()));
        }
    }

    @Test
    @Parameters({
        "SET_ASIDE, setAsideRequest",
        "CORRECTION, correctionRequest",
        "STATEMENT_OF_REASONS, sORRequest",
        "LIBERTY_TO_APPLY, libertyToApplyRequest",
        "PERMISSION_TO_APPEAL, permissionToAppealRequest"
    })
    public void givenPostHearingAndFurtherEvidenceActionIsReviewByJudge_shouldTriggerEventAndUpdateCaseCorrectly(
        PostHearingRequestType requestType, String eventType) {

        Callback<SscsCaseData> callback = buildCallback("sendToInterlocReviewByJudge", ACTION_FURTHER_EVIDENCE);

        var idamTokens = IdamTokens.builder().build();
        given(idamService.getIdamTokens()).willReturn(idamTokens);

        var startEventResponse = StartEventResponse.builder()
            .caseDetails(
                uk.gov.hmcts.reform.ccd.client.model.CaseDetails.builder().build()
            ).build();

        given(ccdClient.startEvent(idamTokens, 123L, UPDATE_CASE_ONLY.getCcdType())).willReturn(startEventResponse);
        var sscsCaseData = callback.getCaseDetails().getCaseData();
        sscsCaseData.setPostHearing(PostHearing.builder().requestType(requestType).build());

        given(sscsCcdConvertService.getCaseData(startEventResponse.getCaseDetails().getData())).willReturn(sscsCaseData);

        given(updateCcdCaseService.updateCaseV2(anyLong(), eq(eventType), anyString(), anyString(),
            eq(idamTokens), any(Consumer.class)))
            .willReturn(SscsCaseDetails.builder().data(sscsCaseData).build());

        handler = new ActionFurtherEvidenceSubmittedCallbackHandler(ccdService, updateCcdCaseService, ccdClient, sscsCcdConvertService, idamService, true, true);

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        ArgumentCaptor<Consumer<SscsCaseData>> captor = ArgumentCaptor.forClass(Consumer.class);

        then(updateCcdCaseService).should(times(1))
            .updateCaseV2(eq(123L), eq(eventType), anyString(),
                anyString(), eq(idamTokens), captor.capture());

        captor.getValue().accept(sscsCaseData);

        assertEquals(InterlocReviewState.REVIEW_BY_JUDGE, sscsCaseData.getInterlocReviewState());
    }

    @Ignore("Re-enable once new post hearings B types are added to the enum")
    @Test
    @Parameters({ // TODO add remaining post hearing B types once implemented
    })
    public void givenPostHearingNotImplementedAndFurtherEvidenceActionIsReviewByJudge_shouldThrowException(
        PostHearingRequestType requestType, String eventType) {

        Callback<SscsCaseData> callback = buildCallback("sendToInterlocReviewByJudge", ACTION_FURTHER_EVIDENCE);

        var idamTokens = IdamTokens.builder().build();
        given(idamService.getIdamTokens()).willReturn(idamTokens);

        var startEventResponse = StartEventResponse.builder()
            .caseDetails(
                uk.gov.hmcts.reform.ccd.client.model.CaseDetails.builder().build()
            ).build();

        given(ccdClient.startEvent(idamTokens, 123L, UPDATE_CASE_ONLY.getCcdType())).willReturn(startEventResponse);

        var sscsCaseData = callback.getCaseDetails().getCaseData();
        sscsCaseData.setPostHearing(PostHearing.builder().requestType(requestType).build());
        given(sscsCcdConvertService.getCaseData(startEventResponse.getCaseDetails().getData())).willReturn(sscsCaseData);

        given(idamService.getIdamTokens()).willReturn(idamTokens);

        ArgumentCaptor<Consumer<SscsCaseData>> captor = ArgumentCaptor.forClass(Consumer.class);

        given(updateCcdCaseService.updateCaseV2(anyLong(), eq(eventType), anyString(), anyString(),
            eq(idamTokens), captor.capture()))
            .willReturn(SscsCaseDetails.builder().data(sscsCaseData).build());

        handler = new ActionFurtherEvidenceSubmittedCallbackHandler(ccdService, updateCcdCaseService, ccdClient, sscsCcdConvertService, idamService, true, false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> handler.handle(SUBMITTED, callback, USER_AUTHORISATION));
        assertEquals(String.format("Post hearing request type is not implemented or recognised: %s", requestType), exception.getMessage());
    }

    @Test
    @Parameters({
        "LIBERTY_TO_APPLY, libertyToApplyRequest",
        "PERMISSION_TO_APPEAL, permissionToAppealRequest"
        // TODO add remaining post hearing B types once implemented
    })
    public void givenPostHearingsBNotEnabledAndFurtherEvidenceActionIsReviewByJudge_shouldThrowException(
        PostHearingRequestType requestType, String eventType) {

        Callback<SscsCaseData> callback = buildCallback("sendToInterlocReviewByJudge", ACTION_FURTHER_EVIDENCE);
        var idamTokens = IdamTokens.builder().build();
        given(idamService.getIdamTokens()).willReturn(idamTokens);

        var startEventResponse = StartEventResponse.builder()
            .caseDetails(
                uk.gov.hmcts.reform.ccd.client.model.CaseDetails.builder().build()
            ).build();

        given(ccdClient.startEvent(idamTokens, 123L, UPDATE_CASE_ONLY.getCcdType())).willReturn(startEventResponse);

        var sscsCaseData = callback.getCaseDetails().getCaseData();
        sscsCaseData.setPostHearing(PostHearing.builder().requestType(requestType).build());

        given(sscsCcdConvertService.getCaseData(startEventResponse.getCaseDetails().getData())).willReturn(sscsCaseData);

        handler = new ActionFurtherEvidenceSubmittedCallbackHandler(ccdService, updateCcdCaseService, ccdClient, sscsCcdConvertService, idamService, true, false);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> handler.handle(SUBMITTED, callback, USER_AUTHORISATION));
        assertEquals("Post hearings B is not enabled", exception.getMessage());
    }

    @Test
    public void givenFurtherEvidenceActionSelectedOptionAndUrgentCaseFlagNotSet_shouldTriggerUrgentCaseEventAndUpdateCaseCorrectly() {
        Callback<SscsCaseData> callback = buildCallback(FurtherEvidenceActionDynamicListItems.OTHER_DOCUMENT_MANUAL.code, ACTION_FURTHER_EVIDENCE);
        var idamTokens = IdamTokens.builder().build();
        given(idamService.getIdamTokens()).willReturn(idamTokens);

        var startEventResponse = StartEventResponse.builder()
            .caseDetails(
                uk.gov.hmcts.reform.ccd.client.model.CaseDetails.builder().build()
            ).build();

        given(ccdClient.startEvent(idamTokens, 123L, UPDATE_CASE_ONLY.getCcdType())).willReturn(startEventResponse);
        given(idamService.getIdamTokens()).willReturn(idamTokens);

        var sscsCaseData = callback.getCaseDetails().getCaseData();
        var sscsDocument = SscsDocument.builder().value(
            SscsDocumentDetails.builder()
                .documentType(DocumentType.URGENT_HEARING_REQUEST.getValue())
                .documentFileName("bla.pdf")
                .documentLink(DocumentLink.builder().documentUrl("www.test.com").build())
                .documentDateAdded("2019-06-12T00:00:00.000")
                .controlNumber("123")
                .build()).build();

        sscsCaseData.setSscsDocument(List.of(sscsDocument));
        sscsCaseData.setUrgentCase(null);

        given(sscsCcdConvertService.getCaseData(startEventResponse.getCaseDetails().getData())).willReturn(sscsCaseData);
        given(updateCcdCaseService.triggerCaseEventV2(anyLong(), eq(MAKE_CASE_URGENT.getCcdType()), anyString(), anyString(),
            eq(idamTokens)))
            .willReturn(SscsCaseDetails.builder().data(sscsCaseData).build());

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        then(updateCcdCaseService).should(times(1))
            .triggerCaseEventV2(eq(123L), eq(MAKE_CASE_URGENT.getCcdType()), anyString(),
                anyString(), eq(idamTokens));
    }

    @Test
    public void givenFurtherEvidenceActionSelectedOptionAndUrgentCaseFlagIsSet_shouldNotTriggerUrgentCaseEventAndUpdateCaseCorrectly() {
        Callback<SscsCaseData> callback = buildCallback(FurtherEvidenceActionDynamicListItems.OTHER_DOCUMENT_MANUAL.code, ACTION_FURTHER_EVIDENCE);
        var idamTokens = IdamTokens.builder().build();
        given(idamService.getIdamTokens()).willReturn(idamTokens);

        var startEventResponse = StartEventResponse.builder()
            .caseDetails(
                uk.gov.hmcts.reform.ccd.client.model.CaseDetails.builder().build()
            ).build();

        given(ccdClient.startEvent(idamTokens, 123L, UPDATE_CASE_ONLY.getCcdType())).willReturn(startEventResponse);

        var sscsCaseData = callback.getCaseDetails().getCaseData();
        sscsCaseData.setUrgentCase("Yes");

        var sscsDocument = SscsDocument.builder().value(
            SscsDocumentDetails.builder()
                .documentType(DocumentType.URGENT_HEARING_REQUEST.getValue())
                .documentFileName("bla.pdf")
                .documentLink(DocumentLink.builder().documentUrl("www.test.com").build())
                .documentDateAdded("2019-06-12T00:00:00.000")
                .controlNumber("123")
                .build()).build();

        sscsCaseData.setSscsDocument(List.of(sscsDocument));

        given(sscsCcdConvertService.getCaseData(startEventResponse.getCaseDetails().getData())).willReturn(sscsCaseData);

        given(updateCcdCaseService.triggerCaseEventV2(anyLong(), eq(ISSUE_FURTHER_EVIDENCE.getCcdType()), anyString(), anyString(),
            eq(idamTokens)))
            .willReturn(SscsCaseDetails.builder().data(sscsCaseData).build());

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        then(updateCcdCaseService).should(times(1))
            .triggerCaseEventV2(eq(123L), eq(ISSUE_FURTHER_EVIDENCE.getCcdType()), anyString(),
                anyString(), eq(idamTokens));
    }

    @Test
    public void givenFurtherEvidenceActionSelectedOptionWithManualDocument_shouldUpdateCaseCorrectly() {
        Callback<SscsCaseData> callback = buildCallback(FurtherEvidenceActionDynamicListItems.OTHER_DOCUMENT_MANUAL.code, ACTION_FURTHER_EVIDENCE);

        var idamTokens = IdamTokens.builder().build();
        given(idamService.getIdamTokens()).willReturn(idamTokens);

        var startEventResponse = StartEventResponse.builder()
            .caseDetails(
                uk.gov.hmcts.reform.ccd.client.model.CaseDetails.builder().build()
            ).build();

        given(ccdClient.startEvent(idamTokens, 123L, UPDATE_CASE_ONLY.getCcdType())).willReturn(startEventResponse);

        var sscsCaseData = callback.getCaseDetails().getCaseData();
        given(sscsCcdConvertService.getCaseData(startEventResponse.getCaseDetails().getData())).willReturn(sscsCaseData);

        given(updateCcdCaseService.triggerCaseEventV2(anyLong(), eq(ISSUE_FURTHER_EVIDENCE.getCcdType()), anyString(), anyString(),
            eq(idamTokens)))
            .willReturn(SscsCaseDetails.builder().data(sscsCaseData).build());

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        then(updateCcdCaseService).should(times(1))
            .triggerCaseEventV2(eq(123L), eq(ISSUE_FURTHER_EVIDENCE.getCcdType()), eq("Actioned manually"),
                eq("Actioned manually"), eq(idamTokens));
    }

    @Test
    public void givenFurtherEvidenceActionPostponementRequest_shouldTriggerEventAndUpdateCaseCorrectly() {
        Callback<SscsCaseData> callback = buildCallback(
            FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_TCW.getCode(),
            ACTION_FURTHER_EVIDENCE);

        var idamTokens = IdamTokens.builder().build();
        given(idamService.getIdamTokens()).willReturn(idamTokens);

        var startEventResponse = StartEventResponse.builder()
            .caseDetails(
                uk.gov.hmcts.reform.ccd.client.model.CaseDetails.builder().build()
            ).build();

        given(ccdClient.startEvent(idamTokens, 123L, UPDATE_CASE_ONLY.getCcdType())).willReturn(startEventResponse);

        var sscsCaseData = callback.getCaseDetails().getCaseData();
        sscsCaseData.setSscsDocument((Arrays.asList(SscsDocument.builder()
            .value(SscsDocumentDetails.builder().documentType(DocumentType.POSTPONEMENT_REQUEST.getValue()).build()).build())));

        given(sscsCcdConvertService.getCaseData(startEventResponse.getCaseDetails().getData())).willReturn(sscsCaseData);

        ArgumentCaptor<Consumer<SscsCaseData>> captor = ArgumentCaptor.forClass(Consumer.class);

        String eventType = "validSendToInterloc";
        given(updateCcdCaseService.updateCaseV2(anyLong(), eq(eventType), anyString(), anyString(),
            eq(idamTokens), any(Consumer.class)))
            .willReturn(SscsCaseDetails.builder().data(sscsCaseData).build());

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        then(updateCcdCaseService).should(times(1))
            .updateCaseV2(eq(123L), eq(eventType), eq(ActionFurtherEvidenceSubmittedCallbackHandler.TCW_REVIEW_SEND_TO_JUDGE),
                anyString(), eq(idamTokens), captor.capture());

        captor.getValue().accept(sscsCaseData);
        assertEquals(InterlocReviewState.REVIEW_BY_TCW, sscsCaseData.getInterlocReviewState());
        assertEquals(InterlocReferralReason.REVIEW_POSTPONEMENT_REQUEST, sscsCaseData.getInterlocReferralReason());
    }

    @Test
    public void givenPostHearingOtherAndFurtherEvidenceActionIsReviewByJudge_shouldTriggerEventAndUpdateCaseCorrectly() {
        Callback<SscsCaseData> callback = buildCallback(SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getCode(), ACTION_FURTHER_EVIDENCE);

        var idamTokens = IdamTokens.builder().build();
        given(idamService.getIdamTokens()).willReturn(idamTokens);

        var startEventResponse = StartEventResponse.builder()
            .caseDetails(
                uk.gov.hmcts.reform.ccd.client.model.CaseDetails.builder().build()
            ).build();

        given(ccdClient.startEvent(idamTokens, 123L, UPDATE_CASE_ONLY.getCcdType())).willReturn(startEventResponse);

        var sscsCaseData = callback.getCaseDetails().getCaseData();

        sscsCaseData.setSscsDocument(List.of(SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentType(DocumentType.POST_HEARING_OTHER.getValue())
                .build())
            .build()));

        given(sscsCcdConvertService.getCaseData(startEventResponse.getCaseDetails().getData())).willReturn(sscsCaseData);

        given(updateCcdCaseService.updateCaseV2(anyLong(), eq(POST_HEARING_OTHER.getCcdType()), anyString(), anyString(),
            eq(idamTokens), any(Consumer.class)))
            .willReturn(SscsCaseDetails.builder().data(sscsCaseData).build());

        handler = new ActionFurtherEvidenceSubmittedCallbackHandler(ccdService, updateCcdCaseService, ccdClient, sscsCcdConvertService, idamService, true, true);

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        ArgumentCaptor<Consumer<SscsCaseData>> captor = ArgumentCaptor.forClass(Consumer.class);

        then(updateCcdCaseService).should(times(1))
            .updateCaseV2(eq(123L), eq(POST_HEARING_OTHER.getCcdType()), anyString(),
                anyString(), eq(idamTokens), captor.capture());

        captor.getValue().accept(sscsCaseData);
        assertEquals(InterlocReviewState.REVIEW_BY_JUDGE, sscsCaseData.getInterlocReviewState());
    }


}
