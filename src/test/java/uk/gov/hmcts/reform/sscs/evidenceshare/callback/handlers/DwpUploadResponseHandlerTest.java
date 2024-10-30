package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.RESPONSE_SUBMITTED_DWP;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DWP_UPLOAD_RESPONSE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.AWAITING_ADMIN_ACTION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.util.function.Consumer;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.exception.RequiredFieldMissingException;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;


@RunWith(JUnitParamsRunner.class)
public class DwpUploadResponseHandlerTest {
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Mock
    private IdamService idamService;
    @Mock
    private UpdateCcdCaseService updateCcdCaseService;

    private DwpUploadResponseHandler handler;

    @Captor
    private ArgumentCaptor<Consumer<SscsCaseDetails>> consumerArgumentCaptor;

    @Before
    public void setup() {
        handler = new DwpUploadResponseHandler(updateCcdCaseService, idamService);
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
        handler = new DwpUploadResponseHandler(updateCcdCaseService, idamService);
        assertFalse(handler.canHandle(CallbackType.SUBMITTED, HandlerHelper.buildTestCallbackForGivenData(SscsCaseData.builder().createdInGapsFrom(State.VALID_APPEAL.getId()).build(), INTERLOCUTORY_REVIEW_STATE, DWP_UPLOAD_RESPONSE)));
    }

    @Test
    public void givenADwpUploadResponseEventWithDwpFurtherInfoIsNo_runReadyToListEvent() {
        SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(READY_TO_LIST.getId()).dwpFurtherInfo("No")
                .elementsDisputedIsDecisionDisputedByOthers("No").appeal(Appeal.builder()
                        .benefitType(BenefitType.builder().code("PIP").build())
                        .build()).build();
        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
                sscsCaseData, INTERLOCUTORY_REVIEW_STATE, DWP_UPLOAD_RESPONSE);

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().id(1L)
            .data(callback.getCaseDetails().getCaseData()).build());

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(idamService).getIdamTokens();
        verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.READY_TO_LIST.getCcdType()), anyString(), anyString(), any(), consumerArgumentCaptor.capture());
        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
        consumerArgumentCaptor.getValue().accept(sscsCaseDetails);
        assertEquals(RESPONSE_SUBMITTED_DWP, sscsCaseData.getDwpState());
    }

    @Test
    public void givenADwpUploadResponseEventWithDwpFurtherInfoIsYes_doNothing() {
        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
            SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(State.READY_TO_LIST.getId()).dwpFurtherInfo("Yes")
                .elementsDisputedIsDecisionDisputedByOthers(null).appeal(Appeal.builder()
                    .benefitType(BenefitType.builder().code("PIP").build())
                    .build()).build(), WITH_DWP, DWP_UPLOAD_RESPONSE);

        handler.handle(CallbackType.SUBMITTED, callback);

        verifyNoInteractions(updateCcdCaseService);
    }

    @Test
    public void givenADwpUploadResponsePipEventWithDisputeIsNull_runReadyToListEvent() {
        SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(READY_TO_LIST.getId()).dwpFurtherInfo("No")
                .elementsDisputedIsDecisionDisputedByOthers(null).appeal(Appeal.builder()
                        .benefitType(BenefitType.builder().code("PIP").build())
                        .build()).build();
        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
                sscsCaseData, WITH_DWP, DWP_UPLOAD_RESPONSE);

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().id(1L).state(State.READY_TO_LIST.getId())
            .data(callback.getCaseDetails().getCaseData()).build());

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(idamService).getIdamTokens();
        verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.READY_TO_LIST.getCcdType()), anyString(), anyString(), any(), consumerArgumentCaptor.capture());

        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
        consumerArgumentCaptor.getValue().accept(sscsCaseDetails);
        assertEquals(RESPONSE_SUBMITTED_DWP, sscsCaseData.getDwpState());
    }

    @Test
    public void givenADwpUploadResponseEsaEventWithDisputeIsYes_runReadyToListEvent() {
        SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(READY_TO_LIST.getId()).dwpFurtherInfo("No")
                .elementsDisputedIsDecisionDisputedByOthers(null).appeal(Appeal.builder()
                        .benefitType(BenefitType.builder().code("ESA").build())
                        .build()).build();
        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
                sscsCaseData, WITH_DWP, DWP_UPLOAD_RESPONSE);

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().id(1L).state(State.READY_TO_LIST.getId())
            .data(callback.getCaseDetails().getCaseData()).build());

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(idamService).getIdamTokens();
        verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
            eq(EventType.READY_TO_LIST.getCcdType()), anyString(), anyString(), any(), consumerArgumentCaptor.capture());

        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
        consumerArgumentCaptor.getValue().accept(sscsCaseDetails);
        assertEquals(RESPONSE_SUBMITTED_DWP, sscsCaseData.getDwpState());
    }

    @Test
    public void givenADwpUploadResponseUcEventWithDisputeIsYes_runResponseReceivedEvent() {
        SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(READY_TO_LIST.getId()).dwpFurtherInfo("No")
                .elementsDisputedIsDecisionDisputedByOthers("yes").appeal(Appeal.builder()
                        .benefitType(BenefitType.builder().code("UC").build())
                        .build()).build();
        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
                sscsCaseData, WITH_DWP, DWP_UPLOAD_RESPONSE);

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().id(1L).state(State.RESPONSE_RECEIVED.getId())
            .data(callback.getCaseDetails().getCaseData()).build());

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(idamService).getIdamTokens();
        verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.DWP_RESPOND.getCcdType()), anyString(), anyString(), any(), consumerArgumentCaptor.capture());

        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
        consumerArgumentCaptor.getValue().accept(sscsCaseDetails);
        assertEquals(RESPONSE_SUBMITTED_DWP, sscsCaseData.getDwpState());
    }

    @Test
    public void givenADwpUploadResponseEventUcWithBothYes_runResponseReceivedEvent() {
        SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(READY_TO_LIST.getId()).dwpFurtherInfo("Yes")
                .elementsDisputedIsDecisionDisputedByOthers("Yes").appeal(Appeal.builder()
                        .benefitType(BenefitType.builder().code("UC").build())
                        .build()).build();
        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
                sscsCaseData, WITH_DWP, DWP_UPLOAD_RESPONSE);


        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().id(1L)
            .data(callback.getCaseDetails().getCaseData()).build());

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(idamService).getIdamTokens();
        verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.DWP_RESPOND.getCcdType()), anyString(), anyString(), any(), consumerArgumentCaptor.capture());

        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
        consumerArgumentCaptor.getValue().accept(sscsCaseDetails);
        assertEquals(RESPONSE_SUBMITTED_DWP, sscsCaseData.getDwpState());
    }

    @Test
    public void givenADwpUploadResponseEventUcWithBothNo_runResponseReceivedEvent() {
        SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(READY_TO_LIST.getId()).dwpFurtherInfo("No")
                .elementsDisputedIsDecisionDisputedByOthers("No").appeal(Appeal.builder()
                        .benefitType(BenefitType.builder().code("UC").build())
                        .build()).build();
        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
                sscsCaseData, WITH_DWP, DWP_UPLOAD_RESPONSE);


        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().id(1L)
            .data(callback.getCaseDetails().getCaseData()).build());

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(idamService).getIdamTokens();
        verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                 eq(EventType.READY_TO_LIST.getCcdType()), anyString(), anyString(), any(), consumerArgumentCaptor.capture());

        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
        consumerArgumentCaptor.getValue().accept(sscsCaseDetails);
        assertEquals(RESPONSE_SUBMITTED_DWP, sscsCaseData.getDwpState());
    }

    @Test
    public void givenADwpUploadResponseEventWithUrgentCaseNo_runReadyToListEvent() {
        handler = new DwpUploadResponseHandler(updateCcdCaseService, idamService);
        SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(READY_TO_LIST.getId()).dwpFurtherInfo("No")
                .elementsDisputedIsDecisionDisputedByOthers("No").appeal(Appeal.builder()
                        .benefitType(BenefitType.builder().code("PIP").build()).build())
                .urgentCase("No")
                .build();
        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
                sscsCaseData, INTERLOCUTORY_REVIEW_STATE, DWP_UPLOAD_RESPONSE);

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().id(1L)
            .data(callback.getCaseDetails().getCaseData()).build());

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(idamService).getIdamTokens();
        verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.READY_TO_LIST.getCcdType()), anyString(), anyString(), any(), consumerArgumentCaptor.capture());
        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
        consumerArgumentCaptor.getValue().accept(sscsCaseDetails);
        assertEquals(RESPONSE_SUBMITTED_DWP, sscsCaseData.getDwpState());
    }

    @Test
    public void givenADwpUploadResponseEventWithUrgentCaseYes_runResponseReceivedEvent() {
        handler = new DwpUploadResponseHandler(updateCcdCaseService, idamService);
        SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(READY_TO_LIST.getId()).dwpFurtherInfo("No")
                .elementsDisputedIsDecisionDisputedByOthers("No").appeal(Appeal.builder()
                        .benefitType(BenefitType.builder().code("PIP").build()).build())
                .urgentCase("Yes")
                .build();
        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
                sscsCaseData, INTERLOCUTORY_REVIEW_STATE, DWP_UPLOAD_RESPONSE);

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().id(1L)
            .data(callback.getCaseDetails().getCaseData()).build());

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(idamService).getIdamTokens();
        verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.DWP_RESPOND.getCcdType()), anyString(), anyString(), any(), consumerArgumentCaptor.capture());

        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
        consumerArgumentCaptor.getValue().accept(sscsCaseDetails);
        assertEquals(RESPONSE_SUBMITTED_DWP, sscsCaseData.getDwpState());
    }

    @Test
    public void givenADwpUploadResponseUcEventWithDisputeIsNoUrgentCaseFlagOnAndUrgentCaseYes_runResponseReceivedEvent() {
        handler = new DwpUploadResponseHandler(updateCcdCaseService, idamService);
        SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(READY_TO_LIST.getId()).dwpFurtherInfo("No")
                .elementsDisputedIsDecisionDisputedByOthers("No").appeal(Appeal.builder()
                        .benefitType(BenefitType.builder().code("UC").build())
                        .build()).urgentCase("Yes").build();
        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
                sscsCaseData, WITH_DWP, DWP_UPLOAD_RESPONSE);

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().id(1L).state(State.RESPONSE_RECEIVED.getId())
            .data(callback.getCaseDetails().getCaseData()).build());

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(idamService).getIdamTokens();
        verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.DWP_RESPOND.getCcdType()), anyString(), anyString(), any(), consumerArgumentCaptor.capture());

        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
        consumerArgumentCaptor.getValue().accept(sscsCaseDetails);
        assertEquals(RESPONSE_SUBMITTED_DWP, sscsCaseData.getDwpState());
    }

    @Test
    public void givenADwpUploadResponseUcEventWithDisputeIsNoUrgentCaseFlagOnAndUrgentCaseNo_runResponseReceivedEvent() {
        handler = new DwpUploadResponseHandler(updateCcdCaseService, idamService);
        SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(READY_TO_LIST.getId()).dwpFurtherInfo("No")
                .elementsDisputedIsDecisionDisputedByOthers("No").appeal(Appeal.builder()
                        .benefitType(BenefitType.builder().code("UC").build())
                        .build()).urgentCase("No").build();
        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
                sscsCaseData, WITH_DWP, DWP_UPLOAD_RESPONSE);

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().id(1L).state(State.RESPONSE_RECEIVED.getId())
            .data(callback.getCaseDetails().getCaseData()).build());

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(idamService).getIdamTokens();
        verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.READY_TO_LIST.getCcdType()), anyString(), anyString(), any(), consumerArgumentCaptor.capture());

        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
        consumerArgumentCaptor.getValue().accept(sscsCaseDetails);
        assertEquals(RESPONSE_SUBMITTED_DWP, sscsCaseData.getDwpState());
    }

    @Test
    public void givenADwpUploadResponseEventWithJointParty_runJointPartyAddedEvent() {
        SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(READY_TO_LIST.getId()).dwpFurtherInfo("No")
                .jointParty(JointParty.builder().hasJointParty(YES).build()).elementsDisputedIsDecisionDisputedByOthers("No").appeal(Appeal.builder()
                        .benefitType(BenefitType.builder().code("UC").build())
                        .build()).build();
        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
                sscsCaseData, WITH_DWP, DWP_UPLOAD_RESPONSE);

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().id(1L)
            .data(callback.getCaseDetails().getCaseData()).build());

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(idamService, times(2)).getIdamTokens();
        verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.READY_TO_LIST.getCcdType()), anyString(), anyString(), any(), consumerArgumentCaptor.capture());

        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
        consumerArgumentCaptor.getValue().accept(sscsCaseDetails);
        assertEquals(RESPONSE_SUBMITTED_DWP, sscsCaseData.getDwpState());

        verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.JOINT_PARTY_ADDED.getCcdType()), anyString(), anyString(), any(), consumerArgumentCaptor.capture());

        consumerArgumentCaptor.getValue().accept(sscsCaseDetails);
        assertNotNull(sscsCaseData.getJointParty());
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
        SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(READY_TO_LIST.getId()).dwpFurtherInfo("Yes")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build()).build())
                .build();
        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
                sscsCaseData, RESPONSE_RECEIVED, DWP_UPLOAD_RESPONSE);

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().id(1L)
            .data(callback.getCaseDetails().getCaseData()).build());

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(idamService).getIdamTokens();
        verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.DWP_RESPOND.getCcdType()),
             anyString(), anyString(), any(), consumerArgumentCaptor.capture());

        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
        consumerArgumentCaptor.getValue().accept(sscsCaseDetails);

        assertEquals(RESPONSE_SUBMITTED_DWP, sscsCaseData.getDwpState());
        assertEquals(AWAITING_ADMIN_ACTION, sscsCaseData.getInterlocReviewState());
    }

    @Test
    public void givenADwpUploadResponseEventChildSupportAndContainsFurtherInformationIsNoThenDoNotTriggerNotListable() {

        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
            SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(State.READY_TO_LIST.getId()).dwpFurtherInfo("No")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build()).build())
                .build(), RESPONSE_RECEIVED, DWP_UPLOAD_RESPONSE);

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(idamService, times(0)).getIdamTokens();
        verify(updateCcdCaseService, times(0)).updateCase(eq(callback.getCaseDetails().getCaseData()),
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
        verify(updateCcdCaseService, times(0)).updateCase(eq(callback.getCaseDetails().getCaseData()),
            eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
            eq(EventType.NOT_LISTABLE.getCcdType()), anyString(), anyString(), any());
    }

    @Test
    public void givenADwpUploadResponseEventTaxCreditAndContainsFurtherInformationIsYesThenResponseReceived() {
        SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(READY_TO_LIST.getId()).dwpFurtherInfo("Yes")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("taxCredit").build()).build())
                .build();
        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
                sscsCaseData, RESPONSE_RECEIVED, DWP_UPLOAD_RESPONSE);

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().id(1L)
            .data(callback.getCaseDetails().getCaseData()).build());

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(idamService).getIdamTokens();
        verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
            eq(EventType.DWP_RESPOND.getCcdType()), anyString(), anyString(), any(), consumerArgumentCaptor.capture());

        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
        consumerArgumentCaptor.getValue().accept(sscsCaseDetails);
        assertEquals(RESPONSE_SUBMITTED_DWP, sscsCaseData.getDwpState());
    }

    @Test
    public void givenADwpUploadResponseEventTaxCreditAndContainsFurtherInformationIsNoEditedEvidenceReasonIsPhmeThenResponseReceived() {

        SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(READY_TO_LIST.getId()).dwpFurtherInfo("No").dwpEditedEvidenceReason("phme")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("taxCredit").build()).build())
                .build();
        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
                sscsCaseData, RESPONSE_RECEIVED, DWP_UPLOAD_RESPONSE);

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().id(1L)
            .data(callback.getCaseDetails().getCaseData()).build());

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(idamService).getIdamTokens();
        verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
            eq(EventType.DWP_RESPOND.getCcdType()), anyString(), anyString(), any(), consumerArgumentCaptor.capture());

        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
        consumerArgumentCaptor.getValue().accept(sscsCaseDetails);
        assertEquals(RESPONSE_SUBMITTED_DWP, sscsCaseData.getDwpState());
    }

    @Test
    public void givenADwpUploadResponseEventTaxCreditAndContainsFurtherInformationIsNoEditedEvidenceReasonIsPhmeThenThenNotListable() {
        SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(READY_TO_LIST.getId()).dwpFurtherInfo("No").dwpEditedEvidenceReason("childSupport")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("taxCredit").build()).build())
                .build();

        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
                sscsCaseData, RESPONSE_RECEIVED, DWP_UPLOAD_RESPONSE);

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().id(1L)
            .data(callback.getCaseDetails().getCaseData()).build());

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(idamService).getIdamTokens();
        verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.READY_TO_LIST.getCcdType()), anyString(), anyString(), any(), consumerArgumentCaptor.capture());

        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
        consumerArgumentCaptor.getValue().accept(sscsCaseDetails);
        assertEquals(RESPONSE_SUBMITTED_DWP, sscsCaseData.getDwpState());
    }

    @Test
    public void givenADwpUploadResponseEventWithIbcaCase_runResponseReceivedEvent() {
        handler = new DwpUploadResponseHandler(updateCcdCaseService, idamService);
        SscsCaseData sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("1")
                .createdInGapsFrom(READY_TO_LIST.getId())
                .elementsDisputedIsDecisionDisputedByOthers("No")
                .appeal(Appeal.builder()
                        .benefitType(BenefitType.builder()
                                .code("infectedBloodAppeal")
                                .build()
                        )
                        .build()
                )
                .build();
        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
                sscsCaseData, NOT_LISTABLE, DWP_UPLOAD_RESPONSE);

        final IdamTokens tokens = IdamTokens.builder().build();

        when(idamService.getIdamTokens()).thenReturn(tokens);
        when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any()))
                .thenReturn(SscsCaseDetails.builder()
                        .id(1L)
                        .data(callback.getCaseDetails().getCaseData())
                        .build());

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(idamService).getIdamTokens();
        verify(updateCcdCaseService).updateCaseV2(
                eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.DWP_RESPOND.getCcdType()),
                eq("Response received."),
                eq("IBC case must move to responseReceived."),
                eq(tokens),
                consumerArgumentCaptor.capture()
        );

        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
        consumerArgumentCaptor.getValue().accept(sscsCaseDetails);
        assertEquals(RESPONSE_SUBMITTED_DWP, sscsCaseData.getDwpState());
    }
}
