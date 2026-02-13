package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority.LATEST;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType.AT_38;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType.DWP_EVIDENCE_BUNDLE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.RESPONSE_SUBMITTED_DWP;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DWP_UPLOAD_RESPONSE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.AWAITING_ADMIN_ACTION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.REVIEW_BY_JUDGE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.INTERLOCUTORY_REVIEW_STATE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.NOT_LISTABLE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.RESPONSE_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.WITH_DWP;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.JointParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.exception.RequiredFieldMissingException;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@ExtendWith(MockitoExtension.class)
class DwpUploadResponseHandlerTest {

    @Mock
    private IdamService idamService;
    @Mock
    private UpdateCcdCaseService updateCcdCaseService;

    private DwpUploadResponseHandler handler;

    @Captor
    private ArgumentCaptor<Consumer<SscsCaseDetails>> consumerArgumentCaptor;

    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setup() {
        Logger logger = (Logger) LoggerFactory.getLogger(DwpUploadResponseHandler.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        handler = new DwpUploadResponseHandler(updateCcdCaseService, idamService);
    }

    private void assertLogWritten(EventType eventType, long caseId) {
        String message = "Updated case v2 with dwp load response event " + eventType + " for id " + caseId;
        assertThat(listAppender.list.stream().map(ILoggingEvent::getFormattedMessage).toList())
            .contains(message);
    }

    private void acceptAndAssertLog(EventType eventType, SscsCaseDetails sscsCaseDetails, long caseId) {
        consumerArgumentCaptor.getValue().accept(sscsCaseDetails);
        assertLogWritten(eventType, caseId);
    }

    @Nested
    class GeneralValidationTests {
        @Test
        void givenCallbackIsNull_whenHandleIsCalled_shouldThrowException() {
            assertThatThrownBy(() -> handler.handle(CallbackType.SUBMITTED, null)).isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @EnumSource(value = CallbackType.class, names = {"ABOUT_TO_START", "MID_EVENT", "ABOUT_TO_SUBMIT"})
        void givenCallbackIsNotSubmitted_willThrowAnException(CallbackType callbackType) {
            assertThatThrownBy(() -> handler.handle(callbackType,
                HandlerHelper.buildTestCallbackForGivenData(SscsCaseData.builder().build(), INTERLOCUTORY_REVIEW_STATE,
                    DWP_UPLOAD_RESPONSE))).isInstanceOf(IllegalStateException.class);
        }

        @ParameterizedTest
        @EnumSource(value = EventType.class, names = {"REISSUE_FURTHER_EVIDENCE", "EVIDENCE_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
        void givenEventTypeIsNotIssueFurtherEvidence_willThrowAnException(EventType eventType) {
            assertThatThrownBy(() -> handler.handle(CallbackType.SUBMITTED,
                HandlerHelper.buildTestCallbackForGivenData(SscsCaseData.builder().build(), INTERLOCUTORY_REVIEW_STATE,
                    eventType))).isInstanceOf(IllegalStateException.class);
        }

        @Test
        void givenAppealIsNullEvidence_willThrowAnException() {
            assertThatThrownBy(() -> handler.handle(CallbackType.SUBMITTED, HandlerHelper.buildTestCallbackForGivenData(
                SscsCaseData.builder().createdInGapsFrom(State.READY_TO_LIST.getId()).appeal(null).build(),
                INTERLOCUTORY_REVIEW_STATE, DWP_UPLOAD_RESPONSE))).isInstanceOf(IllegalStateException.class);
        }

        @Test
        void givenBenefitCodeIsNullEvidence_willThrowAnException() {
            Callback<SscsCaseData> sscsCaseData = HandlerHelper.buildTestCallbackForGivenData(
                SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(State.READY_TO_LIST.getId()).dwpFurtherInfo("No")
                    .elementsDisputedIsDecisionDisputedByOthers("No").appeal(Appeal.builder().benefitType(null).build()).build(),
                INTERLOCUTORY_REVIEW_STATE, DWP_UPLOAD_RESPONSE);
            assertThatThrownBy(() -> handler.handle(CallbackType.SUBMITTED, sscsCaseData)).isInstanceOf(
                IllegalStateException.class);
        }

        @Test
        void givenCaseDataInCallbackIsNull_shouldThrowException() {
            assertThatThrownBy(() -> handler.handle(CallbackType.SUBMITTED,
                HandlerHelper.buildTestCallbackForGivenData(null, INTERLOCUTORY_REVIEW_STATE, DWP_UPLOAD_RESPONSE))).isInstanceOf(
                RequiredFieldMissingException.class);
        }

        @Test
        void givenCallbackIsNull_whenCanHandleIsCalled_shouldThrowException() {
            assertThatThrownBy(() -> handler.canHandle(CallbackType.SUBMITTED, null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        void givenCallbackIsOkay_thenCanHandleIsTrue() {
            assertThat(handler.canHandle(CallbackType.SUBMITTED, HandlerHelper.buildTestCallbackForGivenData(
                SscsCaseData.builder().createdInGapsFrom(State.READY_TO_LIST.getId())
                    .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build()).build(),
                INTERLOCUTORY_REVIEW_STATE, DWP_UPLOAD_RESPONSE))).isTrue();
        }

        @Test
        void givenCallbackIsOkay_butCreatedInGapsIsValidAppeal_thenCanHandleIsFalse() {
            handler = new DwpUploadResponseHandler(updateCcdCaseService, idamService);
            assertThat(handler.canHandle(CallbackType.SUBMITTED, HandlerHelper.buildTestCallbackForGivenData(
                SscsCaseData.builder().createdInGapsFrom(State.VALID_APPEAL.getId()).build(), INTERLOCUTORY_REVIEW_STATE,
                DWP_UPLOAD_RESPONSE))).isFalse();
        }

        @Test
        void getPriorityReturnsLatest() {
            assertThat(handler.getPriority()).isEqualTo(LATEST);
        }

        @Test
        void givenDwpDocumentsContainsEvidenceBundle_shouldTriggerDwpResponseReceivedEvent() {
            SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(READY_TO_LIST.getId())
                .benefitCode(Benefit.CHILD_SUPPORT.getShortName()).dwpFurtherInfo("No")
                .elementsDisputedIsDecisionDisputedByOthers("Yes").appeal(
                    Appeal.builder().benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build())
                        .build()).dwpDocuments(singletonList(
                    DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_EVIDENCE_BUNDLE.getValue()).build())
                        .build())).build();
            final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(sscsCaseData, WITH_DWP,
                DWP_UPLOAD_RESPONSE);

            when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
            when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(
                SscsCaseDetails.builder().id(1L).data(callback.getCaseDetails().getCaseData()).build());

            handler.handle(CallbackType.SUBMITTED, callback);

            verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.DWP_RESPOND.getCcdType()), anyString(), anyString(), any(), consumerArgumentCaptor.capture());

            SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
            acceptAndAssertLog(EventType.DWP_RESPOND, sscsCaseDetails, callback.getCaseDetails().getId());
        }

        @Test
        void givenDwpDocumentsDoesNotContainEvidenceBundle_shouldTriggerDwpResponseReceivedEvent() {
            SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(READY_TO_LIST.getId())
                .benefitCode(Benefit.CHILD_SUPPORT.getShortName()).dwpFurtherInfo("No")
                .elementsDisputedIsDecisionDisputedByOthers("Yes").appeal(
                    Appeal.builder().benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build())
                        .build()).dwpDocuments(singletonList(
                    DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(AT_38.getValue()).build()).build()))
                .build();
            final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(sscsCaseData, WITH_DWP,
                DWP_UPLOAD_RESPONSE);

            handler.handle(CallbackType.SUBMITTED, callback);

            verifyNoInteractions(updateCcdCaseService);
        }

        @Test
        void givenDwpEditedEvidenceReasonPhme_triggersResponseReceivedEvent() {
            SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(READY_TO_LIST.getId())
                .dwpFurtherInfo("No").dwpEditedEvidenceReason("phme")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build()).build();
            final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(sscsCaseData, WITH_DWP,
                DWP_UPLOAD_RESPONSE);

            when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
            when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(
                SscsCaseDetails.builder().id(1L).data(callback.getCaseDetails().getCaseData()).build());

            handler.handle(CallbackType.SUBMITTED, callback);

            verify(idamService).getIdamTokens();
            verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.DWP_RESPOND.getCcdType()), eq("Response received"),
                eq("Update to response received as an Admin has to review the case"), any(), consumerArgumentCaptor.capture());

            SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
            acceptAndAssertLog(EventType.DWP_RESPOND, sscsCaseDetails, callback.getCaseDetails().getId());
        }
    }

    @Nested
    class UcBenefitTests {
        @Test
        void givenADwpUploadResponseUcEventWithDisputeIsYes_runResponseReceivedEvent() {
            SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(READY_TO_LIST.getId())
                .dwpFurtherInfo("No").elementsDisputedIsDecisionDisputedByOthers("yes")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("UC").build()).build()).build();
            final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(sscsCaseData, WITH_DWP,
                DWP_UPLOAD_RESPONSE);

            when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
            when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(
                SscsCaseDetails.builder().id(1L).state(State.RESPONSE_RECEIVED.getId())
                    .data(callback.getCaseDetails().getCaseData()).build());

            handler.handle(CallbackType.SUBMITTED, callback);

            verify(idamService).getIdamTokens();
            verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.DWP_RESPOND.getCcdType()), anyString(), anyString(), any(), consumerArgumentCaptor.capture());

            SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
            acceptAndAssertLog(EventType.DWP_RESPOND, sscsCaseDetails, callback.getCaseDetails().getId());
            assertThat(sscsCaseData.getDwpState()).isEqualTo(RESPONSE_SUBMITTED_DWP);
        }

        @Test
        void givenADwpUploadResponseEventUcWithBothYes_runResponseReceivedEvent() {
            SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(READY_TO_LIST.getId())
                .dwpFurtherInfo("Yes").elementsDisputedIsDecisionDisputedByOthers("Yes")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("UC").build()).build()).build();
            final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(sscsCaseData, WITH_DWP,
                DWP_UPLOAD_RESPONSE);

            when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
            when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(
                SscsCaseDetails.builder().id(1L).data(callback.getCaseDetails().getCaseData()).build());

            handler.handle(CallbackType.SUBMITTED, callback);

            verify(idamService).getIdamTokens();
            verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.DWP_RESPOND.getCcdType()), anyString(), anyString(), any(), consumerArgumentCaptor.capture());

            SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
            acceptAndAssertLog(EventType.DWP_RESPOND, sscsCaseDetails, callback.getCaseDetails().getId());
            assertThat(sscsCaseData.getDwpState()).isEqualTo(RESPONSE_SUBMITTED_DWP);
        }

        @Test
        void givenADwpUploadResponseUcEventWithDisputeIsNoUrgentCaseFlagOnAndUrgentCaseYes_runResponseReceivedEvent() {
            handler = new DwpUploadResponseHandler(updateCcdCaseService, idamService);
            SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(READY_TO_LIST.getId())
                .dwpFurtherInfo("No").elementsDisputedIsDecisionDisputedByOthers("No")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("UC").build()).build()).urgentCase("Yes").build();
            final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(sscsCaseData, WITH_DWP,
                DWP_UPLOAD_RESPONSE);

            when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
            when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(
                SscsCaseDetails.builder().id(1L).state(State.RESPONSE_RECEIVED.getId())
                    .data(callback.getCaseDetails().getCaseData()).build());

            handler.handle(CallbackType.SUBMITTED, callback);

            verify(idamService).getIdamTokens();
            verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.DWP_RESPOND.getCcdType()), anyString(), anyString(), any(), consumerArgumentCaptor.capture());

            SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
            acceptAndAssertLog(EventType.DWP_RESPOND, sscsCaseDetails, callback.getCaseDetails().getId());
            assertThat(sscsCaseData.getDwpState()).isEqualTo(RESPONSE_SUBMITTED_DWP);
        }

        @Test
        void givenADwpUploadResponseUcEventWithFurtherInfoOnly_runResponseReceivedEventWithFurtherInfoDescription() {
            SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(READY_TO_LIST.getId())
                .dwpFurtherInfo("Yes").elementsDisputedIsDecisionDisputedByOthers("No")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("UC").build()).build()).build();
            final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(sscsCaseData, WITH_DWP,
                DWP_UPLOAD_RESPONSE);

            when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
            when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(
                SscsCaseDetails.builder().id(1L).data(callback.getCaseDetails().getCaseData()).build());

            handler.handle(CallbackType.SUBMITTED, callback);

            verify(idamService).getIdamTokens();
            verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.DWP_RESPOND.getCcdType()), eq("Response received"),
                eq("update to response received event as there is further information to assist the tribunal."), any(),
                consumerArgumentCaptor.capture());

            SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
            acceptAndAssertLog(EventType.DWP_RESPOND, sscsCaseDetails, callback.getCaseDetails().getId());
            assertThat(sscsCaseData.getDwpState()).isEqualTo(RESPONSE_SUBMITTED_DWP);
        }

        @Test
        void givenADwpUploadResponseUcEventWithFurtherInfoAndDispute_runResponseReceivedEventWithCombinedDescription() {
            SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(READY_TO_LIST.getId())
                .dwpFurtherInfo("Yes").elementsDisputedIsDecisionDisputedByOthers("Yes")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("UC").build()).build()).build();
            final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(sscsCaseData, WITH_DWP,
                DWP_UPLOAD_RESPONSE);

            when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
            when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(
                SscsCaseDetails.builder().id(1L).data(callback.getCaseDetails().getCaseData()).build());

            handler.handle(CallbackType.SUBMITTED, callback);

            verify(idamService).getIdamTokens();
            verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.DWP_RESPOND.getCcdType()), eq("Response received"),
                eq("update to response received event as there is further information to assist the tribunal and there is a dispute."),
                any(), consumerArgumentCaptor.capture());

            SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
            acceptAndAssertLog(EventType.DWP_RESPOND, sscsCaseDetails, callback.getCaseDetails().getId());
        }

        @Test
        void givenUcBenefitAndDisputeFlagPresent_shouldEvaluateDisputeFlag() {
            SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(READY_TO_LIST.getId())
                .benefitCode(Benefit.UC.getShortName()).dwpFurtherInfo("No").elementsDisputedIsDecisionDisputedByOthers("Yes")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code(Benefit.UC.getShortName()).build()).build())
                .build();
            final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(sscsCaseData, WITH_DWP,
                DWP_UPLOAD_RESPONSE);

            when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
            when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(
                SscsCaseDetails.builder().id(1L).data(callback.getCaseDetails().getCaseData()).build());

            handler.handle(CallbackType.SUBMITTED, callback);

            verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.DWP_RESPOND.getCcdType()), anyString(), anyString(), any(), consumerArgumentCaptor.capture());
        }

        @Test
        void givenUcDisputeFlagPresent_triggersResponseReceivedEventForDispute() {
            SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(READY_TO_LIST.getId())
                .dwpFurtherInfo("No").elementsDisputedIsDecisionDisputedByOthers("Yes")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("UC").build()).build()).build();
            final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(sscsCaseData, WITH_DWP,
                DWP_UPLOAD_RESPONSE);

            when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
            when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(
                SscsCaseDetails.builder().id(1L).data(callback.getCaseDetails().getCaseData()).build());

            handler.handle(CallbackType.SUBMITTED, callback);

            verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.DWP_RESPOND.getCcdType()), eq("Response received"),
                eq("update to response received event as there is a dispute."), any(), consumerArgumentCaptor.capture());

            SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
            acceptAndAssertLog(EventType.DWP_RESPOND, sscsCaseDetails, callback.getCaseDetails().getId());
        }

        @Test
        void givenADwpUploadResponseEventUcWithBothNo_runResponseReceivedEvent() {
            SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(READY_TO_LIST.getId())
                .dwpFurtherInfo("No").elementsDisputedIsDecisionDisputedByOthers("No")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("UC").build()).build()).build();
            final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(sscsCaseData, WITH_DWP,
                DWP_UPLOAD_RESPONSE);

            when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
            when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(
                SscsCaseDetails.builder().id(1L).data(callback.getCaseDetails().getCaseData()).build());

            handler.handle(CallbackType.SUBMITTED, callback);

            verify(idamService).getIdamTokens();
            verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.READY_TO_LIST.getCcdType()), anyString(), anyString(), any(), consumerArgumentCaptor.capture());

            SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
            acceptAndAssertLog(EventType.READY_TO_LIST, sscsCaseDetails, callback.getCaseDetails().getId());
            assertThat(sscsCaseData.getDwpState()).isEqualTo(RESPONSE_SUBMITTED_DWP);
            assertThat(sscsCaseData.getIgnoreCallbackWarnings()).isEqualTo(YES);
        }

        @Test
        void givenADwpUploadResponseUcEventWithDisputeIsNoUrgentCaseFlagOnAndUrgentCaseNo_runResponseReceivedEvent() {
            handler = new DwpUploadResponseHandler(updateCcdCaseService, idamService);
            SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(READY_TO_LIST.getId())
                .dwpFurtherInfo("No").elementsDisputedIsDecisionDisputedByOthers("No")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("UC").build()).build()).urgentCase("No").build();
            final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(sscsCaseData, WITH_DWP,
                DWP_UPLOAD_RESPONSE);

            when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
            when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(
                SscsCaseDetails.builder().id(1L).state(State.RESPONSE_RECEIVED.getId())
                    .data(callback.getCaseDetails().getCaseData()).build());

            handler.handle(CallbackType.SUBMITTED, callback);

            verify(idamService).getIdamTokens();
            verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.READY_TO_LIST.getCcdType()), anyString(), anyString(), any(), consumerArgumentCaptor.capture());

            SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
            acceptAndAssertLog(EventType.READY_TO_LIST, sscsCaseDetails, callback.getCaseDetails().getId());
            assertThat(sscsCaseData.getDwpState()).isEqualTo(RESPONSE_SUBMITTED_DWP);
        }

        @Test
        void givenADwpUploadResponseEventWithJointParty_runJointPartyAddedEvent() {
            JointParty jointParty = JointParty.builder().hasJointParty(YES).build();
            SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(READY_TO_LIST.getId())
                .dwpFurtherInfo("No").jointParty(jointParty).elementsDisputedIsDecisionDisputedByOthers("No")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("UC").build()).build()).build();
            final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(sscsCaseData, WITH_DWP,
                DWP_UPLOAD_RESPONSE);

            when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
            when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(
                SscsCaseDetails.builder().id(1L).data(callback.getCaseDetails().getCaseData()).build());

            handler.handle(CallbackType.SUBMITTED, callback);

            verify(idamService, times(2)).getIdamTokens();
            verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.READY_TO_LIST.getCcdType()), anyString(), anyString(), any(), consumerArgumentCaptor.capture());

            SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
            acceptAndAssertLog(EventType.READY_TO_LIST, sscsCaseDetails, callback.getCaseDetails().getId());
            assertThat(sscsCaseData.getDwpState()).isEqualTo(RESPONSE_SUBMITTED_DWP);

            verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.JOINT_PARTY_ADDED.getCcdType()), anyString(), anyString(), any(), consumerArgumentCaptor.capture());

            acceptAndAssertLog(EventType.JOINT_PARTY_ADDED, sscsCaseDetails, callback.getCaseDetails().getId());
            assertThat(sscsCaseData.getJointParty()).isEqualTo(jointParty);
        }

        @Test
        void givenUcDisputeFlagNotSet_triggersResponseReceivedEventForDispute() {
            SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(READY_TO_LIST.getId())
                .dwpFurtherInfo("No").elementsDisputedIsDecisionDisputedByOthers(null)
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("UC").build()).build()).build();
            final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(sscsCaseData, WITH_DWP,
                DWP_UPLOAD_RESPONSE);

            when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
            when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(
                SscsCaseDetails.builder().id(1L).data(callback.getCaseDetails().getCaseData()).build());

            handler.handle(CallbackType.SUBMITTED, callback);

            verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.READY_TO_LIST.getCcdType()), eq("ready to list"),
                eq("update to ready to list event as there is no further information to assist the tribunal and no dispute."),
                any(), consumerArgumentCaptor.capture());

            SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
            acceptAndAssertLog(EventType.READY_TO_LIST, sscsCaseDetails, callback.getCaseDetails().getId());
        }

        @Test
        void givenUcDisputeFlagSetToNo_triggersReadyToListEvent() {
            SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(READY_TO_LIST.getId())
                .dwpFurtherInfo("No").elementsDisputedIsDecisionDisputedByOthers("No")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("UC").build()).build()).build();
            final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(sscsCaseData, WITH_DWP,
                DWP_UPLOAD_RESPONSE);

            when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
            when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(
                SscsCaseDetails.builder().id(1L).data(callback.getCaseDetails().getCaseData()).build());

            handler.handle(CallbackType.SUBMITTED, callback);

            verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.READY_TO_LIST.getCcdType()), anyString(), anyString(), any(), consumerArgumentCaptor.capture());

            SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
            acceptAndAssertLog(EventType.READY_TO_LIST, sscsCaseDetails, callback.getCaseDetails().getId());
        }


    }

    @Nested
    class ChildSupportAndSscs5BenefitTests {
        @Test
        void givenADwpUploadResponseEventChildSupportAndContainsFurtherInformationIsYesThenResponseReceived() {
            SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(READY_TO_LIST.getId())
                .dwpFurtherInfo("Yes")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build()).build()).build();
            final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(sscsCaseData, RESPONSE_RECEIVED,
                DWP_UPLOAD_RESPONSE);

            when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
            when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(
                SscsCaseDetails.builder().id(1L).data(callback.getCaseDetails().getCaseData()).build());

            handler.handle(CallbackType.SUBMITTED, callback);

            verify(idamService).getIdamTokens();
            verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.DWP_RESPOND.getCcdType()), anyString(), anyString(), any(), consumerArgumentCaptor.capture());

            SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
            acceptAndAssertLog(EventType.DWP_RESPOND, sscsCaseDetails, callback.getCaseDetails().getId());

            assertThat(sscsCaseData.getDwpState()).isEqualTo(RESPONSE_SUBMITTED_DWP);
            assertThat(sscsCaseData.getInterlocReviewState()).isEqualTo(AWAITING_ADMIN_ACTION);
        }

        @Test
        void givenADwpUploadResponseEventChildSupportAndContainsFurtherInformationIsNoThenDoNotTriggerNotListable() {
            final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
                SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(State.READY_TO_LIST.getId()).dwpFurtherInfo("No")
                    .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build()).build()).build(),
                RESPONSE_RECEIVED, DWP_UPLOAD_RESPONSE);

            handler.handle(CallbackType.SUBMITTED, callback);

            verify(idamService, times(0)).getIdamTokens();
            verify(updateCcdCaseService, times(0)).updateCase(eq(callback.getCaseDetails().getCaseData()),
                eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())), eq(EventType.NOT_LISTABLE.getCcdType()),
                anyString(), anyString(), any());
        }

        @Test
        void givenADwpChildSupportLoadResponseEventWithDwpFurtherInfoIsNull_doNothing() {
            final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
                SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(State.READY_TO_LIST.getId()).dwpFurtherInfo(null)
                    .elementsDisputedIsDecisionDisputedByOthers(null).appeal(
                        Appeal.builder().benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build())
                            .build()).build(), WITH_DWP, DWP_UPLOAD_RESPONSE);

            handler.handle(CallbackType.SUBMITTED, callback);

            verifyNoInteractions(updateCcdCaseService);
        }

        @ParameterizedTest
        @EnumSource(value = State.class, names = {"WITH_DWP", "READY_TO_LIST"})
        void givenAStateForTheCaseAndWhenDwpUploadResponseForSscs2IsRunWhileContainsFurtherInformationIsNoThenDoNotUpdateState(
            State state) {
            final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
                SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(State.READY_TO_LIST.getId()).dwpFurtherInfo("No")
                    .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build()).build()).build(),
                state, DWP_UPLOAD_RESPONSE);

            handler.handle(CallbackType.SUBMITTED, callback);

            verify(idamService, times(0)).getIdamTokens();
            verify(updateCcdCaseService, times(0)).updateCase(eq(callback.getCaseDetails().getCaseData()),
                eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())), eq(EventType.NOT_LISTABLE.getCcdType()),
                anyString(), anyString(), any());
        }

        @Test
        void givenADwpUploadResponseEventTaxCreditAndContainsFurtherInformationIsYesThenResponseReceived() {
            SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(READY_TO_LIST.getId())
                .dwpFurtherInfo("Yes")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("taxCredit").build()).build()).build();
            final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(sscsCaseData, RESPONSE_RECEIVED,
                DWP_UPLOAD_RESPONSE);

            when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
            when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(
                SscsCaseDetails.builder().id(1L).data(callback.getCaseDetails().getCaseData()).build());

            handler.handle(CallbackType.SUBMITTED, callback);

            verify(idamService).getIdamTokens();
            verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.DWP_RESPOND.getCcdType()), anyString(), anyString(), any(), consumerArgumentCaptor.capture());

            SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
            acceptAndAssertLog(EventType.DWP_RESPOND, sscsCaseDetails, callback.getCaseDetails().getId());
            assertThat(sscsCaseData.getDwpState()).isEqualTo(RESPONSE_SUBMITTED_DWP);
        }

        @Test
        void givenADwpUploadResponseEventTaxCreditFurtherInfoNoEditedEvidencePhme_triggersResponseReceivedEvent() {
            SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(READY_TO_LIST.getId())
                .dwpFurtherInfo("No").dwpEditedEvidenceReason("phme")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("taxCredit").build()).build()).build();
            final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(sscsCaseData, RESPONSE_RECEIVED,
                DWP_UPLOAD_RESPONSE);

            when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
            when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(
                SscsCaseDetails.builder().id(1L).data(callback.getCaseDetails().getCaseData()).build());

            handler.handle(CallbackType.SUBMITTED, callback);

            verify(idamService).getIdamTokens();
            verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.DWP_RESPOND.getCcdType()), anyString(), anyString(), any(), consumerArgumentCaptor.capture());

            SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
            acceptAndAssertLog(EventType.DWP_RESPOND, sscsCaseDetails, callback.getCaseDetails().getId());
            assertThat(sscsCaseData.getDwpState()).isEqualTo(RESPONSE_SUBMITTED_DWP);
        }

        @Test
        void givenSscs5BenefitAndNoFurtherInfo_triggersReadyToListEvent() {
            SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(READY_TO_LIST.getId())
                .dwpFurtherInfo("No").dwpEditedEvidenceReason("other").benefitCode(Benefit.TAX_CREDIT.getShortName())
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("taxCredit").build()).build()).build();
            final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(sscsCaseData, RESPONSE_RECEIVED,
                DWP_UPLOAD_RESPONSE);

            when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
            when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(
                SscsCaseDetails.builder().id(1L).data(callback.getCaseDetails().getCaseData()).build());

            handler.handle(CallbackType.SUBMITTED, callback);

            verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.READY_TO_LIST.getCcdType()), anyString(), anyString(), any(), consumerArgumentCaptor.capture());
        }

        @Test
        void givenChildSupportAndNoFurtherInfo_whenBenefitTypeChanges_hitsReviewByJudgeBranch() {
            SscsCaseData baseCaseData = SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(READY_TO_LIST.getId())
                .dwpFurtherInfo("No").dwpEditedEvidenceReason("other")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build()).build()).build();
            SscsCaseData sscsCaseData = spy(baseCaseData);
            when(sscsCaseData.getBenefitType()).thenReturn(Optional.empty(), Optional.of(Benefit.TAX_CREDIT));

            final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(sscsCaseData, RESPONSE_RECEIVED,
                DWP_UPLOAD_RESPONSE);

            when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
            when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(
                SscsCaseDetails.builder().id(1L).data(callback.getCaseDetails().getCaseData()).build());

            handler.handle(CallbackType.SUBMITTED, callback);

            verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.DWP_RESPOND.getCcdType()), anyString(), anyString(), any(), consumerArgumentCaptor.capture());

            SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
            acceptAndAssertLog(EventType.DWP_RESPOND, sscsCaseDetails, callback.getCaseDetails().getId());
            assertThat(sscsCaseData.getInterlocReviewState()).isEqualTo(REVIEW_BY_JUDGE);
            assertThat(sscsCaseData.getDwpState()).isEqualTo(RESPONSE_SUBMITTED_DWP);
        }

        @Test
        void givenADwpUploadResponseEventTaxCreditAndContainsFurtherInformationIsNoEditedEvidenceReasonIsPhmeThenThenNotListable() {
            SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(READY_TO_LIST.getId())
                .dwpFurtherInfo("No").dwpEditedEvidenceReason("childSupport")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("taxCredit").build()).build()).build();

            final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(sscsCaseData, RESPONSE_RECEIVED,
                DWP_UPLOAD_RESPONSE);

            when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
            when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(
                SscsCaseDetails.builder().id(1L).data(callback.getCaseDetails().getCaseData()).build());

            handler.handle(CallbackType.SUBMITTED, callback);

            verify(idamService).getIdamTokens();
            verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.READY_TO_LIST.getCcdType()), anyString(), anyString(), any(), consumerArgumentCaptor.capture());

            SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
            acceptAndAssertLog(EventType.READY_TO_LIST, sscsCaseDetails, callback.getCaseDetails().getId());
            assertThat(sscsCaseData.getDwpState()).isEqualTo(RESPONSE_SUBMITTED_DWP);
        }
    }

    @Nested
    class PipEsaBenefitTests {
        @Test
        void givenADwpUploadResponseEventWithDwpFurtherInfoIsNo_runReadyToListEvent() {
            SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(READY_TO_LIST.getId())
                .dwpFurtherInfo("No").elementsDisputedIsDecisionDisputedByOthers("No")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build()).build();
            final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(sscsCaseData,
                INTERLOCUTORY_REVIEW_STATE, DWP_UPLOAD_RESPONSE);

            when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
            when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(
                SscsCaseDetails.builder().id(1L).data(callback.getCaseDetails().getCaseData()).build());

            handler.handle(CallbackType.SUBMITTED, callback);

            verify(idamService).getIdamTokens();
            verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.READY_TO_LIST.getCcdType()), anyString(), anyString(), any(), consumerArgumentCaptor.capture());
            SscsCaseData targetCaseData = SscsCaseData.builder().build();
            SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(targetCaseData).build();
            acceptAndAssertLog(EventType.READY_TO_LIST, sscsCaseDetails, callback.getCaseDetails().getId());
            assertThat(targetCaseData.getDwpState()).isEqualTo(RESPONSE_SUBMITTED_DWP);
            assertThat(sscsCaseData.getIgnoreCallbackWarnings()).isNull();
        }

        @Test
        void givenADwpUploadResponseEventWithDwpFurtherInfoIsYes_doNothing() {
            final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
                SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(State.READY_TO_LIST.getId()).dwpFurtherInfo("Yes")
                    .elementsDisputedIsDecisionDisputedByOthers(null)
                    .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build()).build(), WITH_DWP,
                DWP_UPLOAD_RESPONSE);

            handler.handle(CallbackType.SUBMITTED, callback);

            verifyNoInteractions(updateCcdCaseService);
        }

        @Test
        void givenADwpUploadResponsePipEventWithDisputeIsNull_runReadyToListEvent() {
            SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(READY_TO_LIST.getId())
                .dwpFurtherInfo("No").elementsDisputedIsDecisionDisputedByOthers(null)
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build()).build();
            final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(sscsCaseData, WITH_DWP,
                DWP_UPLOAD_RESPONSE);

            when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
            when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(
                SscsCaseDetails.builder().id(1L).state(State.READY_TO_LIST.getId()).data(callback.getCaseDetails().getCaseData())
                    .build());

            handler.handle(CallbackType.SUBMITTED, callback);

            verify(idamService).getIdamTokens();
            verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.READY_TO_LIST.getCcdType()), anyString(), anyString(), any(), consumerArgumentCaptor.capture());

            SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
            acceptAndAssertLog(EventType.READY_TO_LIST, sscsCaseDetails, callback.getCaseDetails().getId());
            assertThat(sscsCaseData.getDwpState()).isEqualTo(RESPONSE_SUBMITTED_DWP);
        }

        @Test
        void givenADwpUploadResponseEsaEventWithDisputeIsYes_runReadyToListEvent() {
            SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(READY_TO_LIST.getId())
                .dwpFurtherInfo("No").elementsDisputedIsDecisionDisputedByOthers(null)
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("ESA").build()).build()).build();
            final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(sscsCaseData, WITH_DWP,
                DWP_UPLOAD_RESPONSE);

            when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
            when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(
                SscsCaseDetails.builder().id(1L).state(State.READY_TO_LIST.getId()).data(callback.getCaseDetails().getCaseData())
                    .build());

            handler.handle(CallbackType.SUBMITTED, callback);

            verify(idamService).getIdamTokens();
            verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.READY_TO_LIST.getCcdType()), anyString(), anyString(), any(), consumerArgumentCaptor.capture());

            SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
            acceptAndAssertLog(EventType.READY_TO_LIST, sscsCaseDetails, callback.getCaseDetails().getId());
            assertThat(sscsCaseData.getDwpState()).isEqualTo(RESPONSE_SUBMITTED_DWP);
        }

        @Test
        void givenADwpUploadResponseEventWithUrgentCaseNo_runReadyToListEvent() {
            handler = new DwpUploadResponseHandler(updateCcdCaseService, idamService);
            SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(READY_TO_LIST.getId())
                .dwpFurtherInfo("No").elementsDisputedIsDecisionDisputedByOthers("No")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build()).urgentCase("No").build();
            final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(sscsCaseData,
                INTERLOCUTORY_REVIEW_STATE, DWP_UPLOAD_RESPONSE);

            when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
            when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(
                SscsCaseDetails.builder().id(1L).data(callback.getCaseDetails().getCaseData()).build());

            handler.handle(CallbackType.SUBMITTED, callback);

            verify(idamService).getIdamTokens();
            verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.READY_TO_LIST.getCcdType()), anyString(), anyString(), any(), consumerArgumentCaptor.capture());
            SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
            acceptAndAssertLog(EventType.READY_TO_LIST, sscsCaseDetails, callback.getCaseDetails().getId());
            assertThat(sscsCaseData.getDwpState()).isEqualTo(RESPONSE_SUBMITTED_DWP);
        }

        @Test
        void givenADwpUploadResponseEventWithUrgentCaseYes_runResponseReceivedEvent() {
            handler = new DwpUploadResponseHandler(updateCcdCaseService, idamService);
            SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(READY_TO_LIST.getId())
                .dwpFurtherInfo("No").elementsDisputedIsDecisionDisputedByOthers("No")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build()).urgentCase("Yes")
                .build();
            final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(sscsCaseData,
                INTERLOCUTORY_REVIEW_STATE, DWP_UPLOAD_RESPONSE);

            when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
            when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(
                SscsCaseDetails.builder().id(1L).data(callback.getCaseDetails().getCaseData()).build());

            handler.handle(CallbackType.SUBMITTED, callback);

            verify(idamService).getIdamTokens();
            verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.DWP_RESPOND.getCcdType()), anyString(), anyString(), any(), consumerArgumentCaptor.capture());

            SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
            acceptAndAssertLog(EventType.DWP_RESPOND, sscsCaseDetails, callback.getCaseDetails().getId());
            assertThat(sscsCaseData.getDwpState()).isEqualTo(RESPONSE_SUBMITTED_DWP);
        }

    }

    @Nested
    class InfectedBloodTests {
        @Test
        void givenADwpUploadResponseEventWithIidbBenefitType_thenDoNothing() {
            final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
                SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(State.READY_TO_LIST.getId()).dwpFurtherInfo("No")
                    .appeal(Appeal.builder().benefitType(BenefitType.builder().code(Benefit.IIDB.getShortName()).build()).build())
                    .build(), INTERLOCUTORY_REVIEW_STATE, DWP_UPLOAD_RESPONSE);

            assertThat(handler.canHandle(CallbackType.SUBMITTED, callback)).isFalse();
        }

        @Test
        void givenADwpUploadResponseEventWithIbcaCase_runResponseReceivedEvent() {
            handler = new DwpUploadResponseHandler(updateCcdCaseService, idamService);
            SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(READY_TO_LIST.getId())
                .elementsDisputedIsDecisionDisputedByOthers("No")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("infectedBloodCompensation").build()).build())
                .build();
            final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(sscsCaseData, NOT_LISTABLE,
                DWP_UPLOAD_RESPONSE);

            final IdamTokens tokens = IdamTokens.builder().build();

            when(idamService.getIdamTokens()).thenReturn(tokens);
            when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(
                SscsCaseDetails.builder().id(1L).data(callback.getCaseDetails().getCaseData()).build());

            handler.handle(CallbackType.SUBMITTED, callback);

            verify(idamService).getIdamTokens();
            verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.DWP_RESPOND.getCcdType()), eq("Response received."), eq("IBC case must move to responseReceived."),
                eq(tokens), consumerArgumentCaptor.capture());

            SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
            acceptAndAssertLog(EventType.DWP_RESPOND, sscsCaseDetails, callback.getCaseDetails().getId());
            assertThat(sscsCaseData.getDwpState()).isEqualTo(RESPONSE_SUBMITTED_DWP);
        }
    }
}
