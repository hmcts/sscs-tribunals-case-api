package uk.gov.hmcts.reform.sscs.ccd.presubmit.hmctsresponsereviewed;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_SEND_TO_INTERLOC;

import feign.FeignException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Consumer;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@RunWith(JUnitParamsRunner.class)
public class HmctsResponseReviewedSubmittedHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private HmctsResponseReviewedSubmittedHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    private SscsCaseData sscsCaseData;

    @Mock
    private UpdateCcdCaseService updateCcdCaseService;

    @Mock
    private IdamService idamService;

    @Captor
    private ArgumentCaptor<Consumer<SscsCaseDetails>> consumerArgumentCaptor;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new HmctsResponseReviewedSubmittedHandler(updateCcdCaseService, idamService);

        when(callback.getEvent()).thenReturn(EventType.HMCTS_RESPONSE_REVIEWED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getId()).thenReturn(123L);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder().build())
                .selectWhoReviewsCase(new DynamicList(new DynamicListItem("reviewByTcw", "Review by TCW"), null))
                .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());

    }

    @Test
    public void givenANonHmctsResponseReviewedEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "ABOUT_TO_SUBMIT"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenAHmctsResponseReviewedSubmittedEventAndInterlocIsRequiredForJudge_thenTriggerValidSendToInterlocEvent() {

        sscsCaseData = sscsCaseData.toBuilder().isInterlocRequired("Yes").selectWhoReviewsCase(new DynamicList(new DynamicListItem("reviewByJudge", "Review by Judge"), null)).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertEquals(Collections.EMPTY_SET, response.getErrors());
        verify(updateCcdCaseService).triggerCaseEventV2(
                eq(123L),
                eq(VALID_SEND_TO_INTERLOC.getCcdType()),
                eq("Send to interloc"),
                eq("Send a case to a Judge for review"),
                any(IdamTokens.class));
    }

    @Test
    public void givenAHmctsResponseReviewedSubmittedEventAndInterlocIsRequiredForTcw_thenTriggerValidSendToInterlocEvent() {

        sscsCaseData = sscsCaseData.toBuilder().isInterlocRequired("Yes").selectWhoReviewsCase(new DynamicList(new DynamicListItem("reviewByTcw", "Review by TCW"), null)).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertEquals(Collections.EMPTY_SET, response.getErrors());
        verify(updateCcdCaseService).triggerCaseEventV2(
                eq(123L),
                eq(VALID_SEND_TO_INTERLOC.getCcdType()),
                eq("Send to interloc"),
                eq("Send a case to a TCW for review"),
                any(IdamTokens.class));
    }

    @Test
    public void givenAHmctsResponseReviewedSubmittedEventAndInterlocIsNotRequired_thenTriggerReadyToListEvent() {

        sscsCaseData = sscsCaseData.toBuilder().isInterlocRequired("No").build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertEquals(Collections.EMPTY_SET, response.getErrors());

        verify(updateCcdCaseService).updateCaseV2(
                eq(123L),
                eq(READY_TO_LIST.getCcdType()),
                eq("Ready to list"),
                eq("Makes an appeal ready to list"),
                any(IdamTokens.class),
                consumerArgumentCaptor.capture());

        Consumer<SscsCaseDetails> mutator = consumerArgumentCaptor.getValue();
        mutator.accept(SscsCaseDetails.builder().data(sscsCaseData).build());

        assertEquals(YesNo.YES, sscsCaseData.getIgnoreCallbackWarnings());
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);
    }

    @Test
    public void givenSubmittedEventAndInterlocIsNotRequired_thenThrowExceptionWithResponseBodyWhenReadyToListEventFailsToUpdate() {

        sscsCaseData = sscsCaseData.toBuilder().isInterlocRequired("No").build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        FeignException feignException = mock(FeignException.class);
        when(feignException.status()).thenReturn(422);
        when(feignException.responseBody()).thenReturn(Optional.of(ByteBuffer.wrap("ccd warning message".getBytes(StandardCharsets.UTF_8))));

        doThrow(feignException).when(updateCcdCaseService).updateCaseV2(
                eq(123L),
                eq(READY_TO_LIST.getCcdType()),
                eq("Ready to list"),
                eq("Makes an appeal ready to list"),
                any(IdamTokens.class),
                any(Consumer.class));

        assertThatExceptionOfType(FeignException.class).isThrownBy(
                () ->  handler.handle(SUBMITTED, callback, USER_AUTHORISATION));
    }

    @Test
    public void givenSubmittedEventAndInterlocIsNotRequired_thenThrowExceptionWithMessageWhenReadyToListEventFailsToUpdate() {

        sscsCaseData = sscsCaseData.toBuilder().isInterlocRequired("No").build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        FeignException feignException = mock(FeignException.class);
        when(feignException.status()).thenReturn(422);
        when(feignException.getMessage()).thenReturn("ccd error message");

        doThrow(feignException).when(updateCcdCaseService).updateCaseV2(
                eq(123L),
                eq(READY_TO_LIST.getCcdType()),
                eq("Ready to list"),
                eq("Makes an appeal ready to list"),
                any(IdamTokens.class),
                any(Consumer.class));

        assertThatExceptionOfType(FeignException.class).isThrownBy(
                () ->  handler.handle(SUBMITTED, callback, USER_AUTHORISATION));
    }
}
