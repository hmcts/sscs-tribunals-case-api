package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ACTION_FURTHER_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_FURTHER_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.MAKE_CASE_URGENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.POST_HEARING_OTHER;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPDATE_CASE_ONLY;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.REVIEW_BY_JUDGE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_JUDGE;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.InternalCaseDocumentData;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearingRequestType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@SuppressWarnings("unchecked")
class ActionFurtherEvidenceSubmittedCallbackHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

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

    @BeforeEach
    void setUp() {
        openMocks(this);
        handler = new ActionFurtherEvidenceSubmittedCallbackHandler(ccdService, updateCcdCaseService, ccdClient, sscsCcdConvertService, idamService, false, false, false);
    }


    @ParameterizedTest
    @MethodSource(value = "generateCanHandleScenarios")
    void givenCanHandleIsCalled_shouldReturnCorrectResult(CallbackType callbackType,
                                                                 Callback<SscsCaseData> callback,
                                                                 boolean expectedResult) {
        boolean actualResult = handler.canHandle(callbackType, callback);
        assertEquals(expectedResult, actualResult);
    }

    private static Object[] generateCanHandleScenarios() {
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

    private static Callback<SscsCaseData> buildCallback(String dynamicListItemCode, EventType eventType) {
        DynamicList dynamicList = new DynamicList(new DynamicListItem(dynamicListItemCode, "label"),
            Collections.singletonList(new DynamicListItem(dynamicListItemCode, "label")));
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .furtherEvidenceAction(dynamicList)
            .build();
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(123L, "sscs",
            State.INTERLOCUTORY_REVIEW_STATE, sscsCaseData, LocalDateTime.now(), "Benefit");
        CaseDetails<SscsCaseData> caseDetailsBefore = new CaseDetails<>(123L, "sscs",
            State.INTERLOCUTORY_REVIEW_STATE, SscsCaseData.builder().build(), LocalDateTime.now(), "Benefit");
        return new Callback<>(caseDetails, Optional.of(caseDetailsBefore), eventType, false);
    }

    @Test
    void givenIssueToAllParties_shouldUpdateCaseCorrectly() {
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

        given(updateCcdCaseService.triggerCaseEventV2(anyLong(), eq("issueFurtherEvidence"),
            anyString(), anyString(), eq(idamTokens)))
            .willReturn(SscsCaseDetails.builder().data(sscsCaseData).build());
        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verify(updateCcdCaseService).triggerCaseEventV2(anyLong(), eq("issueFurtherEvidence"),
            anyString(), anyString(), eq(idamTokens));
    }

    @ParameterizedTest
    @CsvSource({
        "informationReceivedForInterlocJudge, REVIEW_BY_JUDGE, interlocInformationReceivedActionFurtherEvidence",
        "informationReceivedForInterlocTcw, REVIEW_BY_TCW, interlocInformationReceivedActionFurtherEvidence",
        "sendToInterlocReviewByJudge, REVIEW_BY_JUDGE, validSendToInterloc",
        "sendToInterlocReviewByTcw, REVIEW_BY_TCW, validSendToInterloc",
        "adminActionCorrection, AWAITING_ADMIN_ACTION, correctionRequest"
    })
    void givenFurtherEvidenceActionSelectedOption_shouldTriggerEventAndUpdateCaseCorrectly(
        String furtherEvidenceActionSelectedOption, InterlocReviewState interlocReviewState, String eventType) {
        var idamTokens = IdamTokens.builder().build();
        given(idamService.getIdamTokens()).willReturn(idamTokens);

        var startEventResponse = StartEventResponse.builder()
            .caseDetails(
                uk.gov.hmcts.reform.ccd.client.model.CaseDetails.builder().build()
            ).build();

        given(ccdClient.startEvent(idamTokens, 123L, UPDATE_CASE_ONLY.getCcdType())).willReturn(startEventResponse);


        DynamicList originalSender = new DynamicList(new DynamicListItem("appellant", "Appellant (or Appointee)"), null);

        Callback<SscsCaseData> callback = buildCallback(furtherEvidenceActionSelectedOption, ACTION_FURTHER_EVIDENCE);
        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        sscsCaseData.setOriginalSender(originalSender);
        given(sscsCcdConvertService.getCaseData(startEventResponse.getCaseDetails().getData())).willReturn(sscsCaseData);

        given(updateCcdCaseService.updateCaseV2(anyLong(), eq(eventType), anyString(), anyString(),
            eq(idamTokens), any(Consumer.class)))
            .willReturn(SscsCaseDetails.builder().data(sscsCaseData).build());

        handler = new ActionFurtherEvidenceSubmittedCallbackHandler(ccdService, updateCcdCaseService, ccdClient, sscsCcdConvertService, idamService, true, false, false);

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        ArgumentCaptor<Consumer<SscsCaseDetails>> captor = ArgumentCaptor.forClass(Consumer.class);

        then(updateCcdCaseService).should(times(1))
            .updateCaseV2(eq(123L), eq(eventType), anyString(),
                anyString(), eq(idamTokens), captor.capture());

        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
        captor.getValue().accept(sscsCaseDetails);

        assertEquals(interlocReviewState, sscsCaseData.getInterlocReviewState());

        if (furtherEvidenceActionSelectedOption.equals("informationReceivedForInterlocJudge")
            || furtherEvidenceActionSelectedOption.equals("informationReceivedForInterlocTcw")) {
            assertThat(sscsCaseData.getInterlocReferralDate(), is(LocalDate.now()));
        }

        if (eventType.equals("validSendToInterloc")) {
            assertThat(sscsCaseData.getSelectWhoReviewsCase(),
                equalTo(new DynamicList(new DynamicListItem(interlocReviewState.getCcdDefinition(), null), null)));
            assertThat(sscsCaseData.getOriginalSender(),
                equalTo(originalSender));
        }
    }

    @ParameterizedTest
    @CsvSource({
        "SET_ASIDE, setAsideRequest",
        "CORRECTION, correctionRequest",
        "STATEMENT_OF_REASONS, sORRequest",
        "LIBERTY_TO_APPLY, libertyToApplyRequest",
        "PERMISSION_TO_APPEAL, permissionToAppealRequest"
    })
    void givenPostHearingAndFurtherEvidenceActionIsReviewByJudge_shouldTriggerEventAndUpdateCaseCorrectly(
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

        handler = new ActionFurtherEvidenceSubmittedCallbackHandler(ccdService, updateCcdCaseService, ccdClient, sscsCcdConvertService, idamService, true, true, false);

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        ArgumentCaptor<Consumer<SscsCaseDetails>> captor = ArgumentCaptor.forClass(Consumer.class);

        then(updateCcdCaseService).should(times(1))
            .updateCaseV2(eq(123L), eq(eventType), anyString(),
                anyString(), eq(idamTokens), captor.capture());

        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
        captor.getValue().accept(sscsCaseDetails);

        assertEquals(REVIEW_BY_JUDGE, sscsCaseData.getInterlocReviewState());
    }

    // TODO "Re-enable once new post hearings B types are added to the enum"
    // @ParameterizedTest
    // @CsvSource({})
    void givenPostHearingNotImplementedAndFurtherEvidenceActionIsReviewByJudge_shouldThrowException(
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

        ArgumentCaptor<Consumer<SscsCaseDetails>> captor = ArgumentCaptor.forClass(Consumer.class);

        given(updateCcdCaseService.updateCaseV2(anyLong(), eq(eventType), anyString(), anyString(),
            eq(idamTokens), captor.capture()))
            .willReturn(SscsCaseDetails.builder().data(sscsCaseData).build());

        handler = new ActionFurtherEvidenceSubmittedCallbackHandler(ccdService, updateCcdCaseService, ccdClient, sscsCcdConvertService, idamService, true, false, false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> handler.handle(SUBMITTED, callback, USER_AUTHORISATION));
        assertEquals(String.format("Post hearing request type is not implemented or recognised: %s", requestType), exception.getMessage());
    }

    @ParameterizedTest
    @EnumSource(value = PostHearingRequestType.class, names = {
        "LIBERTY_TO_APPLY",
        "PERMISSION_TO_APPEAL"
        // TODO add remaining post hearing B types once implemented
    })
    void givenPostHearingsBNotEnabledAndFurtherEvidenceActionIsReviewByJudge_shouldThrowException(
        PostHearingRequestType requestType) {

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

        handler = new ActionFurtherEvidenceSubmittedCallbackHandler(ccdService, updateCcdCaseService, ccdClient, sscsCcdConvertService, idamService, true, false, false);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> handler.handle(SUBMITTED, callback, USER_AUTHORISATION));
        assertEquals("Post hearings B is not enabled", exception.getMessage());
    }

    @Test
    void givenFurtherEvidenceActionSelectedOptionAndUrgentCaseFlagNotSet_shouldTriggerUrgentCaseEventAndUpdateCaseCorrectly() {
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
        var sscsDocument = SscsDocument.builder().id("1").value(
            SscsDocumentDetails.builder()
                .documentType(DocumentType.URGENT_HEARING_REQUEST.getValue())
                .documentFileName("bla.pdf")
                .documentLink(DocumentLink.builder().documentUrl("www.test.com").build())
                .documentDateAdded("2019-06-12T00:00:00.000")
                .controlNumber("123")
                .build()).build();

        sscsCaseData.setSscsDocument(List.of(sscsDocument));
        sscsCaseData.setUrgentCase(null);
        callback.getCaseDetailsBefore().orElse(callback.getCaseDetails()).getCaseData().setSscsDocument(List.of());

        given(sscsCcdConvertService.getCaseData(startEventResponse.getCaseDetails().getData())).willReturn(sscsCaseData);
        given(updateCcdCaseService.triggerCaseEventV2(anyLong(), eq(MAKE_CASE_URGENT.getCcdType()), anyString(), anyString(),
            eq(idamTokens)))
            .willReturn(SscsCaseDetails.builder().data(sscsCaseData).build());

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        then(updateCcdCaseService).should(times(1))
            .triggerCaseEventV2(eq(123L), eq(MAKE_CASE_URGENT.getCcdType()), anyString(),
                anyString(), eq(idamTokens));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void givenFurtherEvidenceActionSelectedOptionAndUrgentCaseFlagNotSetInternalDocument_shouldTriggerUrgentCaseEventIfFlagOn(boolean isInternalDocumentFlagOn) {
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
        var sscsDocument = SscsDocument.builder().id("1").value(
            SscsDocumentDetails.builder()
                .documentType(DocumentType.URGENT_HEARING_REQUEST.getValue())
                .documentFileName("bla.pdf")
                .documentLink(DocumentLink.builder().documentUrl("www.test.com").build())
                .documentDateAdded("2019-06-12T00:00:00.000")
                .controlNumber("123")
                .build()).build();
        sscsCaseData.setInternalCaseDocumentData(InternalCaseDocumentData.builder().sscsInternalDocument(List.of(sscsDocument)).build());
        sscsCaseData.setUrgentCase(null);

        given(sscsCcdConvertService.getCaseData(startEventResponse.getCaseDetails().getData())).willReturn(sscsCaseData);
        EventType expectedEvent = isInternalDocumentFlagOn ? MAKE_CASE_URGENT : ISSUE_FURTHER_EVIDENCE;
        given(updateCcdCaseService.triggerCaseEventV2(anyLong(), eq(expectedEvent.getCcdType()), anyString(), anyString(),
            eq(idamTokens)))
            .willReturn(SscsCaseDetails.builder().data(sscsCaseData).build());
        handler = new ActionFurtherEvidenceSubmittedCallbackHandler(ccdService, updateCcdCaseService, ccdClient, sscsCcdConvertService, idamService, false, false, isInternalDocumentFlagOn);

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        then(updateCcdCaseService).should(times(1))
            .triggerCaseEventV2(eq(123L), eq(expectedEvent.getCcdType()), anyString(),
                anyString(), eq(idamTokens));
    }

    @Test
    void givenFurtherEvidenceActionSelectedOptionAndUrgentCaseFlagIsSet_shouldNotTriggerUrgentCaseEventAndUpdateCaseCorrectly() {
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
    void givenFurtherEvidenceOtherDocSelectedAndOldUrgentDocIsPresent_shouldNotTriggerUrgentCaseEventAndUpdateCaseCorrectly() {
        Callback<SscsCaseData> callback = buildCallback(FurtherEvidenceActionDynamicListItems.OTHER_DOCUMENT_MANUAL.code, ACTION_FURTHER_EVIDENCE);
        var idamTokens = IdamTokens.builder().build();
        given(idamService.getIdamTokens()).willReturn(idamTokens);

        var startEventResponse = StartEventResponse.builder()
            .caseDetails(
                uk.gov.hmcts.reform.ccd.client.model.CaseDetails.builder().build()
            ).build();

        given(ccdClient.startEvent(idamTokens, 123L, UPDATE_CASE_ONLY.getCcdType())).willReturn(startEventResponse);

        var sscsCaseData = callback.getCaseDetails().getCaseData();
        sscsCaseData.setUrgentCase("No");

        var oldUrgentDoc = SscsDocument.builder().id("1").value(
            SscsDocumentDetails.builder()
                .documentType(DocumentType.URGENT_HEARING_REQUEST.getValue())
                .documentFileName("uhr.pdf")
                .documentLink(DocumentLink.builder().documentUrl("www.test.com").build())
                .documentDateAdded("2019-05-12T00:00:00.000")
                .controlNumber("124")
                .build()).build();

        var sscsDocument = SscsDocument.builder().id("2").value(
            SscsDocumentDetails.builder()
                .documentType(DocumentType.OTHER_EVIDENCE.getValue())
                .documentFileName("test.pdf")
                .documentLink(DocumentLink.builder().documentUrl("www.test.com").build())
                .documentDateAdded("2019-06-12T00:00:00.000")
                .controlNumber("123")
                .build()).build();

        sscsCaseData.setSscsDocument(List.of(sscsDocument, oldUrgentDoc));
        callback.getCaseDetailsBefore().orElse(callback.getCaseDetails()).getCaseData().setSscsDocument(List.of(oldUrgentDoc));


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
    void givenFurtherEvidenceActionSelectedOptionWithManualDocument_shouldUpdateCaseCorrectly() {
        Callback<SscsCaseData> callback = buildCallback(FurtherEvidenceActionDynamicListItems.OTHER_DOCUMENT_MANUAL.code, ACTION_FURTHER_EVIDENCE);

        var idamTokens = IdamTokens.builder().build();
        given(idamService.getIdamTokens()).willReturn(idamTokens);

        var startEventResponse = StartEventResponse.builder()
            .caseDetails(
                uk.gov.hmcts.reform.ccd.client.model.CaseDetails.builder().build()
            ).build();

        given(ccdClient.startEvent(idamTokens, 123L, UPDATE_CASE_ONLY.getCcdType())).willReturn(startEventResponse);

        var sscsCaseData = callback.getCaseDetails().getCaseData();
        sscsCaseData.setSscsDocument(List.of());
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
    void givenFurtherEvidenceActionPostponementRequest_shouldTriggerEventAndUpdateCaseCorrectly() {
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
        sscsCaseData.setSscsDocument((Collections.singletonList(SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentType(DocumentType.POSTPONEMENT_REQUEST.getValue())
                .build())
            .build())));

        given(sscsCcdConvertService.getCaseData(startEventResponse.getCaseDetails().getData())).willReturn(sscsCaseData);

        ArgumentCaptor<Consumer<SscsCaseDetails>> captor = ArgumentCaptor.forClass(Consumer.class);

        String eventType = "validSendToInterloc";
        given(updateCcdCaseService.updateCaseV2(anyLong(), eq(eventType), anyString(), anyString(),
            eq(idamTokens), any(Consumer.class)))
            .willReturn(SscsCaseDetails.builder().data(sscsCaseData).build());

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        then(updateCcdCaseService).should(times(1))
            .updateCaseV2(eq(123L), eq(eventType), eq(ActionFurtherEvidenceSubmittedCallbackHandler.TCW_REVIEW_SEND_TO_JUDGE),
                anyString(), eq(idamTokens), captor.capture());

        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
        captor.getValue().accept(sscsCaseDetails);
        assertEquals(InterlocReviewState.REVIEW_BY_TCW, sscsCaseData.getInterlocReviewState());
        assertEquals(InterlocReferralReason.REVIEW_POSTPONEMENT_REQUEST, sscsCaseData.getInterlocReferralReason());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void givenFurtherEvidenceActionPostponementRequestInternalDocument_shouldTriggerEventAndUpdateCaseCorrectly(boolean isInternalDocumentFlagOn) {
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
        sscsCaseData.setInternalCaseDocumentData(InternalCaseDocumentData.builder().sscsInternalDocument((Collections.singletonList(SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                    .documentType(DocumentType.POSTPONEMENT_REQUEST.getValue())
                    .build())
                .build())))
            .build());

        given(sscsCcdConvertService.getCaseData(startEventResponse.getCaseDetails().getData())).willReturn(sscsCaseData);

        String eventType = "validSendToInterloc";

        given(updateCcdCaseService.updateCaseV2(anyLong(), eq(eventType), anyString(), anyString(),
            eq(idamTokens), any(Consumer.class)))
            .willReturn(SscsCaseDetails.builder().data(sscsCaseData).build());
        handler = new ActionFurtherEvidenceSubmittedCallbackHandler(ccdService, updateCcdCaseService, ccdClient, sscsCcdConvertService, idamService, false, false, isInternalDocumentFlagOn);
        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);
        ArgumentCaptor<Consumer<SscsCaseDetails>> captor = ArgumentCaptor.forClass(Consumer.class);
        then(updateCcdCaseService).should(times(1))
            .updateCaseV2(eq(123L), eq(eventType), eq(ActionFurtherEvidenceSubmittedCallbackHandler.TCW_REVIEW_SEND_TO_JUDGE),
                anyString(), eq(idamTokens), captor.capture());

        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
        captor.getValue().accept(sscsCaseDetails);
        assertEquals(InterlocReviewState.REVIEW_BY_TCW, sscsCaseData.getInterlocReviewState());
        assertEquals(isInternalDocumentFlagOn ? InterlocReferralReason.REVIEW_POSTPONEMENT_REQUEST : null, sscsCaseData.getInterlocReferralReason());
    }

    @Test
    void givenPostHearingOtherAndFurtherEvidenceActionIsReviewByJudge_shouldTriggerEventAndUpdateCaseCorrectly() {
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

        handler = new ActionFurtherEvidenceSubmittedCallbackHandler(ccdService, updateCcdCaseService, ccdClient, sscsCcdConvertService, idamService, true, true, false);

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        ArgumentCaptor<Consumer<SscsCaseDetails>> captor = ArgumentCaptor.forClass(Consumer.class);

        then(updateCcdCaseService).should(times(1))
            .updateCaseV2(eq(123L), eq(POST_HEARING_OTHER.getCcdType()), anyString(),
                anyString(), eq(idamTokens), captor.capture());

        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
        captor.getValue().accept(sscsCaseDetails);
        assertEquals(REVIEW_BY_JUDGE, sscsCaseData.getInterlocReviewState());
    }

    @Test
    void givenPostHearingOtherAndFurtherEvidenceActionIsReviewByJudgeInternalDocumentFlagOn_shouldTriggerEventAndUpdateCaseCorrectly() {
        Callback<SscsCaseData> callback = buildCallback(SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getCode(), ACTION_FURTHER_EVIDENCE);

        var idamTokens = IdamTokens.builder().build();
        given(idamService.getIdamTokens()).willReturn(idamTokens);

        var startEventResponse = StartEventResponse.builder()
            .caseDetails(
                uk.gov.hmcts.reform.ccd.client.model.CaseDetails.builder().build()
            ).build();

        given(ccdClient.startEvent(idamTokens, 123L, UPDATE_CASE_ONLY.getCcdType())).willReturn(startEventResponse);

        var sscsCaseData = callback.getCaseDetails().getCaseData();
        sscsCaseData.setInternalCaseDocumentData(InternalCaseDocumentData.builder().sscsInternalDocument(List.of(SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                    .documentType(DocumentType.POST_HEARING_OTHER.getValue())
                    .build())
                .build()))
            .build());

        given(sscsCcdConvertService.getCaseData(startEventResponse.getCaseDetails().getData())).willReturn(sscsCaseData);

        given(updateCcdCaseService.updateCaseV2(anyLong(), eq(POST_HEARING_OTHER.getCcdType()), anyString(), anyString(),
            eq(idamTokens), any(Consumer.class)))
            .willReturn(SscsCaseDetails.builder().data(sscsCaseData).build());

        handler = new ActionFurtherEvidenceSubmittedCallbackHandler(ccdService, updateCcdCaseService, ccdClient, sscsCcdConvertService, idamService, true, true, true);

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        ArgumentCaptor<Consumer<SscsCaseDetails>> captor = ArgumentCaptor.forClass(Consumer.class);

        then(updateCcdCaseService).should(times(1))
            .updateCaseV2(eq(123L), eq(POST_HEARING_OTHER.getCcdType()), anyString(),
                anyString(), eq(idamTokens), captor.capture());

        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
        captor.getValue().accept(sscsCaseDetails);
        assertEquals(REVIEW_BY_JUDGE, sscsCaseData.getInterlocReviewState());
    }


}
