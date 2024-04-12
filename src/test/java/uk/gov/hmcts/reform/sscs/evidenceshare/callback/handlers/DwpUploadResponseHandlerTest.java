package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DWP_UPLOAD_RESPONSE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.exception.RequiredFieldMissingException;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;


@RunWith(JUnitParamsRunner.class)
public class DwpUploadResponseHandlerTest {
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Mock
    private IdamService idamService;
    @Mock
    private CcdService ccdService;

    private DwpUploadResponseHandler handler;

    @Before
    public void setup() {
        handler = new DwpUploadResponseHandler(ccdService, idamService);
    }

    @Test(expected = NullPointerException.class)
    public void givenCallbackIsNull_whenHandleIsCalled_shouldThrowException() {
        handler.handle(CallbackType.SUBMITTED, null);
    }

    @Test(expected = IllegalStateException.class)
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "ABOUT_TO_SUBMIT"})
    public void givenCallbackIsNotSubmitted_willThrowAnException(CallbackType callbackType) {
        handler.handle(callbackType,
            HandlerHelper.buildTestCallbackForGivenData(null, INTERLOCUTORY_REVIEW_STATE, DWP_UPLOAD_RESPONSE));
    }

    @Test(expected = IllegalStateException.class)
    @Parameters({"REISSUE_FURTHER_EVIDENCE", "EVIDENCE_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    public void givenEventTypeIsNotIssueFurtherEvidence_willThrowAnException(EventType eventType) {
        handler.handle(CallbackType.SUBMITTED,
            HandlerHelper.buildTestCallbackForGivenData(null, INTERLOCUTORY_REVIEW_STATE, eventType));
    }

    @Test(expected = IllegalStateException.class)
    @Parameters({"REISSUE_FURTHER_EVIDENCE", "EVIDENCE_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    public void givenAppealIsNullEvidence_willThrowAnException(EventType eventType) {
        handler.handle(CallbackType.SUBMITTED, HandlerHelper.buildTestCallbackForGivenData(SscsCaseData.builder().createdInGapsFrom(State.READY_TO_LIST.getId()).appeal(null).build(), INTERLOCUTORY_REVIEW_STATE, DWP_UPLOAD_RESPONSE));
    }

    @Test(expected = IllegalStateException.class)
    @Parameters({"REISSUE_FURTHER_EVIDENCE", "EVIDENCE_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    public void givenBenefitCodeIsNullEvidence_willThrowAnException(EventType eventType) {
        Callback<SscsCaseData> sscsCaseData = HandlerHelper.buildTestCallbackForGivenData(
            SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(State.READY_TO_LIST.getId()).dwpFurtherInfo("No")
                .elementsDisputedIsDecisionDisputedByOthers("No").appeal(Appeal.builder()
                    .benefitType(null)
                    .build()).build(), INTERLOCUTORY_REVIEW_STATE, DWP_UPLOAD_RESPONSE);
        handler.handle(CallbackType.SUBMITTED, sscsCaseData);
    }

    @Test(expected = RequiredFieldMissingException.class)
    public void givenCaseDataInCallbackIsNull_shouldThrowException() {
        handler.handle(CallbackType.SUBMITTED,
            HandlerHelper.buildTestCallbackForGivenData(null, INTERLOCUTORY_REVIEW_STATE, DWP_UPLOAD_RESPONSE));
    }


    @Test(expected = NullPointerException.class)
    public void givenCallbackIsNull_whenCanHandleIsCalled_shouldThrowException() {
        handler.canHandle(CallbackType.SUBMITTED, null);
    }

    @Test
    public void givenCallbackIsOkay_thenCanHandleIsTrue() {
        assertTrue(handler.canHandle(CallbackType.SUBMITTED, HandlerHelper.buildTestCallbackForGivenData(SscsCaseData.builder().createdInGapsFrom(State.READY_TO_LIST.getId()).appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build()).build(), INTERLOCUTORY_REVIEW_STATE, DWP_UPLOAD_RESPONSE)));
    }

    @Test
    public void givenCallbackIsOkay_butCreatedInGapsIsValidAppeal_thenCanHandleIsFalse() {
        handler = new DwpUploadResponseHandler(ccdService, idamService);
        assertFalse(handler.canHandle(CallbackType.SUBMITTED, HandlerHelper.buildTestCallbackForGivenData(SscsCaseData.builder().createdInGapsFrom(State.VALID_APPEAL.getId()).build(), INTERLOCUTORY_REVIEW_STATE, DWP_UPLOAD_RESPONSE)));
    }

    @Test
    public void givenADwpUploadResponseEventWithDwpFurtherInfoIsNo_runReadyToListEvent() {
        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
            SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(State.READY_TO_LIST.getId()).dwpFurtherInfo("No")
                .elementsDisputedIsDecisionDisputedByOthers("No").appeal(Appeal.builder()
                    .benefitType(BenefitType.builder().code("PIP").build())
                    .build()).build(), INTERLOCUTORY_REVIEW_STATE, DWP_UPLOAD_RESPONSE);

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        when(ccdService.updateCase(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().id(1L)
            .data(callback.getCaseDetails().getCaseData()).build());

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(idamService).getIdamTokens();
        verify(ccdService).updateCase(eq(callback.getCaseDetails().getCaseData()),
            eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())), eq(EventType.READY_TO_LIST.getCcdType()), anyString(), anyString(), any());
    }

    @Test
    public void givenADwpUploadResponseEventWithDwpFurtherInfoIsYes_doNothing() {
        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
            SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(State.READY_TO_LIST.getId()).dwpFurtherInfo("Yes")
                .elementsDisputedIsDecisionDisputedByOthers(null).appeal(Appeal.builder()
                    .benefitType(BenefitType.builder().code("PIP").build())
                    .build()).build(), WITH_DWP, DWP_UPLOAD_RESPONSE);

        handler.handle(CallbackType.SUBMITTED, callback);

        verifyNoInteractions(ccdService);
    }

    @Test
    public void givenADwpUploadResponsePipEventWithDisputeIsNull_runReadyToListEvent() {
        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
            SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(State.READY_TO_LIST.getId()).dwpFurtherInfo("No")
                .elementsDisputedIsDecisionDisputedByOthers(null).appeal(Appeal.builder()
                    .benefitType(BenefitType.builder().code("PIP").build())
                    .build()).build(), WITH_DWP, DWP_UPLOAD_RESPONSE);

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        when(ccdService.updateCase(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().id(1L).state(State.READY_TO_LIST.getId())
            .data(callback.getCaseDetails().getCaseData()).build());

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(idamService).getIdamTokens();
        verify(ccdService).updateCase(eq(callback.getCaseDetails().getCaseData()),
            eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())), eq(EventType.READY_TO_LIST.getCcdType()), anyString(), anyString(), any());
    }

    @Test
    public void givenADwpUploadResponseEsaEventWithDisputeIsYes_runReadyToListEvent() {
        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
            SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(State.READY_TO_LIST.getId()).dwpFurtherInfo("No")
                .elementsDisputedIsDecisionDisputedByOthers(null).appeal(Appeal.builder()
                    .benefitType(BenefitType.builder().code("ESA").build())
                    .build()).build(), WITH_DWP, DWP_UPLOAD_RESPONSE);

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        when(ccdService.updateCase(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().id(1L).state(State.READY_TO_LIST.getId())
            .data(callback.getCaseDetails().getCaseData()).build());

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(idamService).getIdamTokens();
        verify(ccdService).updateCase(eq(callback.getCaseDetails().getCaseData()),
            eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())), eq(EventType.READY_TO_LIST.getCcdType()), anyString(), anyString(), any());
    }

    @Test
    public void givenADwpUploadResponseUcEventWithDisputeIsYes_runResponseReceivedEvent() {
        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
            SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(State.READY_TO_LIST.getId()).dwpFurtherInfo("No")
                .elementsDisputedIsDecisionDisputedByOthers("yes").appeal(Appeal.builder()
                    .benefitType(BenefitType.builder().code("UC").build())
                    .build()).build(), WITH_DWP, DWP_UPLOAD_RESPONSE);

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        when(ccdService.updateCase(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().id(1L).state(State.RESPONSE_RECEIVED.getId())
            .data(callback.getCaseDetails().getCaseData()).build());

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(idamService).getIdamTokens();
        verify(ccdService).updateCase(eq(callback.getCaseDetails().getCaseData()),
            eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())), eq(EventType.DWP_RESPOND.getCcdType()), anyString(), anyString(), any());
    }

    @Test
    public void givenADwpUploadResponseEventUcWithBothYes_runResponseReceivedEvent() {
        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
            SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(State.READY_TO_LIST.getId()).dwpFurtherInfo("Yes")
                .elementsDisputedIsDecisionDisputedByOthers("Yes").appeal(Appeal.builder()
                    .benefitType(BenefitType.builder().code("UC").build())
                    .build()).build(), WITH_DWP, DWP_UPLOAD_RESPONSE);


        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        when(ccdService.updateCase(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().id(1L)
            .data(callback.getCaseDetails().getCaseData()).build());

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(idamService).getIdamTokens();
        verify(ccdService).updateCase(eq(callback.getCaseDetails().getCaseData()),
            eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())), eq(EventType.DWP_RESPOND.getCcdType()), anyString(), anyString(), any());
    }

    @Test
    public void givenADwpUploadResponseEventUcWithBothNo_runResponseReceivedEvent() {
        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
            SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(State.READY_TO_LIST.getId()).dwpFurtherInfo("No")
                .elementsDisputedIsDecisionDisputedByOthers("No").appeal(Appeal.builder()
                    .benefitType(BenefitType.builder().code("UC").build())
                    .build()).build(), WITH_DWP, DWP_UPLOAD_RESPONSE);


        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        when(ccdService.updateCase(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().id(1L)
            .data(callback.getCaseDetails().getCaseData()).build());

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(idamService).getIdamTokens();
        verify(ccdService).updateCase(eq(callback.getCaseDetails().getCaseData()),
            eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())), eq(EventType.READY_TO_LIST.getCcdType()), anyString(), anyString(), any());
    }

    @Test
    public void givenADwpUploadResponseEventWithUrgentCaseNo_runReadyToListEvent() {
        handler = new DwpUploadResponseHandler(ccdService, idamService);
        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
            SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(State.READY_TO_LIST.getId()).dwpFurtherInfo("No")
                .elementsDisputedIsDecisionDisputedByOthers("No").appeal(Appeal.builder()
                    .benefitType(BenefitType.builder().code("PIP").build()).build())
                .urgentCase("No")
                .build(), INTERLOCUTORY_REVIEW_STATE, DWP_UPLOAD_RESPONSE);

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        when(ccdService.updateCase(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().id(1L)
            .data(callback.getCaseDetails().getCaseData()).build());

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(idamService).getIdamTokens();
        verify(ccdService).updateCase(eq(callback.getCaseDetails().getCaseData()),
            eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())), eq(EventType.READY_TO_LIST.getCcdType()), anyString(), anyString(), any());
    }

    @Test
    public void givenADwpUploadResponseEventWithUrgentCaseYes_runResponseReceivedEvent() {
        handler = new DwpUploadResponseHandler(ccdService, idamService);
        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
            SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(State.READY_TO_LIST.getId()).dwpFurtherInfo("No")
                .elementsDisputedIsDecisionDisputedByOthers("No").appeal(Appeal.builder()
                    .benefitType(BenefitType.builder().code("PIP").build()).build())
                .urgentCase("Yes")
                .build(), INTERLOCUTORY_REVIEW_STATE, DWP_UPLOAD_RESPONSE);

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        when(ccdService.updateCase(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().id(1L)
            .data(callback.getCaseDetails().getCaseData()).build());

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(idamService).getIdamTokens();
        verify(ccdService).updateCase(eq(callback.getCaseDetails().getCaseData()),
            eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())), eq(EventType.DWP_RESPOND.getCcdType()), anyString(), anyString(), any());
    }

    @Test
    public void givenADwpUploadResponseUcEventWithDisputeIsNoUrgentCaseFlagOnAndUrgentCaseYes_runResponseReceivedEvent() {
        handler = new DwpUploadResponseHandler(ccdService, idamService);
        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
            SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(State.READY_TO_LIST.getId()).dwpFurtherInfo("No")
                .elementsDisputedIsDecisionDisputedByOthers("No").appeal(Appeal.builder()
                    .benefitType(BenefitType.builder().code("UC").build())
                    .build()).urgentCase("Yes").build(), WITH_DWP, DWP_UPLOAD_RESPONSE);

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        when(ccdService.updateCase(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().id(1L).state(State.RESPONSE_RECEIVED.getId())
            .data(callback.getCaseDetails().getCaseData()).build());

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(idamService).getIdamTokens();
        verify(ccdService).updateCase(eq(callback.getCaseDetails().getCaseData()),
            eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())), eq(EventType.DWP_RESPOND.getCcdType()), anyString(), anyString(), any());
    }

    @Test
    public void givenADwpUploadResponseUcEventWithDisputeIsNoUrgentCaseFlagOnAndUrgentCaseNo_runResponseReceivedEvent() {
        handler = new DwpUploadResponseHandler(ccdService, idamService);
        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
            SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(State.READY_TO_LIST.getId()).dwpFurtherInfo("No")
                .elementsDisputedIsDecisionDisputedByOthers("No").appeal(Appeal.builder()
                    .benefitType(BenefitType.builder().code("UC").build())
                    .build()).urgentCase("No").build(), WITH_DWP, DWP_UPLOAD_RESPONSE);

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        when(ccdService.updateCase(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().id(1L).state(State.RESPONSE_RECEIVED.getId())
            .data(callback.getCaseDetails().getCaseData()).build());

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(idamService).getIdamTokens();
        verify(ccdService).updateCase(eq(callback.getCaseDetails().getCaseData()),
            eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())), eq(EventType.READY_TO_LIST.getCcdType()), anyString(), anyString(), any());
    }

    @Test
    public void givenADwpUploadResponseEventWithJointParty_runJointPartyAddedEvent() {
        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
            SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(State.READY_TO_LIST.getId()).dwpFurtherInfo("No")
                .jointParty(JointParty.builder().hasJointParty(YES).build()).elementsDisputedIsDecisionDisputedByOthers("No").appeal(Appeal.builder()
                    .benefitType(BenefitType.builder().code("UC").build())
                    .build()).build(), WITH_DWP, DWP_UPLOAD_RESPONSE);

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        when(ccdService.updateCase(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().id(1L)
            .data(callback.getCaseDetails().getCaseData()).build());

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(idamService, times(2)).getIdamTokens();
        verify(ccdService).updateCase(eq(callback.getCaseDetails().getCaseData()),
            eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())), eq(EventType.READY_TO_LIST.getCcdType()), anyString(), anyString(), any());
        verify(ccdService).updateCase(eq(callback.getCaseDetails().getCaseData()),
            eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())), eq(EventType.JOINT_PARTY_ADDED.getCcdType()), anyString(), anyString(), any());

    }

    @Test
    public void givenADwpUploadResponseEventWithIidbBenefitType_thenDoNothing() {
        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
            SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(State.READY_TO_LIST.getId()).dwpFurtherInfo("No")
                .appeal(Appeal.builder()
                    .benefitType(BenefitType.builder().code(Benefit.IIDB.getShortName()).build())
                    .build()).build(), INTERLOCUTORY_REVIEW_STATE, DWP_UPLOAD_RESPONSE);

        assertFalse(handler.canHandle(CallbackType.SUBMITTED, callback));
    }

    @Test
    public void givenADwpUploadResponseEventChildSupportAndContainsFurtherInformationIsYesThenResponseReceived() {
        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
            SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(State.READY_TO_LIST.getId()).dwpFurtherInfo("Yes")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build()).build())
                .build(), RESPONSE_RECEIVED, DWP_UPLOAD_RESPONSE);

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        when(ccdService.updateCase(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().id(1L)
            .data(callback.getCaseDetails().getCaseData()).build());

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(idamService).getIdamTokens();
        verify(ccdService).updateCase(eq(callback.getCaseDetails().getCaseData()),
            eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
            eq(EventType.DWP_RESPOND.getCcdType()), anyString(), anyString(), any());
    }

    @Test
    public void givenADwpUploadResponseEventChildSupportAndContainsFurtherInformationIsNoThenDoNotTriggerNotListable() {

        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
            SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(State.READY_TO_LIST.getId()).dwpFurtherInfo("No")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build()).build())
                .build(), RESPONSE_RECEIVED, DWP_UPLOAD_RESPONSE);

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(idamService, times(0)).getIdamTokens();
        verify(ccdService, times(0)).updateCase(eq(callback.getCaseDetails().getCaseData()),
            eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
            eq(EventType.NOT_LISTABLE.getCcdType()), anyString(), anyString(), any());
    }

    @Test
    @Parameters({"WITH_DWP", "READY_TO_LIST",})
    public void givenAStateForTheCaseAndWhenDwpUploadResponseForSscs2IsRunWhileContainsFurtherInformationIsNoThenDoNotUpdateState(State state) {
        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
            SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(State.READY_TO_LIST.getId()).dwpFurtherInfo("No")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build()).build())
                .build(), state, DWP_UPLOAD_RESPONSE);

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(idamService, times(0)).getIdamTokens();
        verify(ccdService, times(0)).updateCase(eq(callback.getCaseDetails().getCaseData()),
            eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
            eq(EventType.NOT_LISTABLE.getCcdType()), anyString(), anyString(), any());
    }

    @Test
    public void givenADwpUploadResponseEventTaxCreditAndContainsFurtherInformationIsYesThenResponseReceived() {
        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
            SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(State.READY_TO_LIST.getId()).dwpFurtherInfo("Yes")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("taxCredit").build()).build())
                .build(), RESPONSE_RECEIVED, DWP_UPLOAD_RESPONSE);

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        when(ccdService.updateCase(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().id(1L)
            .data(callback.getCaseDetails().getCaseData()).build());

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(idamService).getIdamTokens();
        verify(ccdService).updateCase(eq(callback.getCaseDetails().getCaseData()),
            eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
            eq(EventType.DWP_RESPOND.getCcdType()), anyString(), anyString(), any());
    }

    @Test
    public void givenADwpUploadResponseEventTaxCreditAndContainsFurtherInformationIsNoEditedEvidenceReasonIsPhmeThenResponseReceived() {

        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
            SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(State.READY_TO_LIST.getId()).dwpFurtherInfo("No").dwpEditedEvidenceReason("phme")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("taxCredit").build()).build())
                .build(), RESPONSE_RECEIVED, DWP_UPLOAD_RESPONSE);

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        when(ccdService.updateCase(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().id(1L)
            .data(callback.getCaseDetails().getCaseData()).build());

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(idamService).getIdamTokens();
        verify(ccdService).updateCase(eq(callback.getCaseDetails().getCaseData()),
            eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
            eq(EventType.DWP_RESPOND.getCcdType()), anyString(), anyString(), any());
    }

    @Test
    public void givenADwpUploadResponseEventTaxCreditAndContainsFurtherInformationIsNoEditedEvidenceReasonIsPhmeThenThenNotListable() {
        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
            SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(State.READY_TO_LIST.getId()).dwpFurtherInfo("No").dwpEditedEvidenceReason("childSupport")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("taxCredit").build()).build())
                .build(), RESPONSE_RECEIVED, DWP_UPLOAD_RESPONSE);

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        when(ccdService.updateCase(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().id(1L)
            .data(callback.getCaseDetails().getCaseData()).build());

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(idamService).getIdamTokens();
        verify(ccdService).updateCase(eq(callback.getCaseDetails().getCaseData()),
            eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())), eq(EventType.READY_TO_LIST.getCcdType()), anyString(), anyString(), any());
    }
}
