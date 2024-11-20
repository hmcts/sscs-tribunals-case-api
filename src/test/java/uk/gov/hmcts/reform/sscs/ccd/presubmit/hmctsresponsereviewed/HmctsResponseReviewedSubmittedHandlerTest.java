package uk.gov.hmcts.reform.sscs.ccd.presubmit.hmctsresponsereviewed;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
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
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
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
    private CcdService ccdService;

    @Mock
    private IdamService idamService;

    @BeforeEach
    public void setUp() {
        openMocks(this);
        handler = new HmctsResponseReviewedSubmittedHandler(ccdService, idamService);

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
    // JunitParamsRunnerToParameterized conversion not supported
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
        verify(ccdService).updateCase(any(SscsCaseData.class), eq(123L), eq(VALID_SEND_TO_INTERLOC.getCcdType()), eq("Send to interloc"), eq("Send a case to a Judge for review"), any(IdamTokens.class));
    }

    @Test
    public void givenAHmctsResponseReviewedSubmittedEventAndInterlocIsRequiredForTcw_thenTriggerValidSendToInterlocEvent() {

        sscsCaseData = sscsCaseData.toBuilder().isInterlocRequired("Yes").selectWhoReviewsCase(new DynamicList(new DynamicListItem("reviewByTcw", "Review by TCW"), null)).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertEquals(Collections.EMPTY_SET, response.getErrors());
        verify(ccdService).updateCase(any(SscsCaseData.class), eq(123L), eq(VALID_SEND_TO_INTERLOC.getCcdType()), eq("Send to interloc"), eq("Send a case to a TCW for review"), any(IdamTokens.class));
    }

    @Test
    public void givenAHmctsResponseReviewedSubmittedEventAndInterlocIsNotRequired_thenTriggerReadyToListEvent() {

        sscsCaseData = sscsCaseData.toBuilder().isInterlocRequired("No").build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertEquals(Collections.EMPTY_SET, response.getErrors());
        assertEquals(response.getData().getIgnoreCallbackWarnings(), YesNo.YES);
        verify(ccdService).updateCase(any(SscsCaseData.class), eq(123L), eq(READY_TO_LIST.getCcdType()), eq("Ready to list"), eq("Makes an appeal ready to list"), any(IdamTokens.class));
    }

    @Test
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        assertThrows(IllegalStateException.class, () -> {
            when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
            handler.handle(SUBMITTED, callback, USER_AUTHORISATION);
        });
    }

    @Test
    public void givenSubmittedEventAndInterlocIsNotRequired_thenThrowExceptionWithResponseBodyWhenReadyToListEventFailsToUpdate() {

        sscsCaseData = sscsCaseData.toBuilder().isInterlocRequired("No").build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        FeignException feignException = mock(FeignException.class);
        when(feignException.status()).thenReturn(422);
        when(feignException.responseBody()).thenReturn(Optional.of(ByteBuffer.wrap("ccd warning message".getBytes(StandardCharsets.UTF_8))));

        doThrow(feignException).when(ccdService).updateCase(any(SscsCaseData.class), eq(123L), eq(READY_TO_LIST.getCcdType()), eq("Ready to list"), eq("Makes an appeal ready to list"), any(IdamTokens.class));

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

        doThrow(feignException).when(ccdService).updateCase(any(SscsCaseData.class), eq(123L), eq(READY_TO_LIST.getCcdType()), eq("Ready to list"), eq("Makes an appeal ready to list"), any(IdamTokens.class));

        assertThatExceptionOfType(FeignException.class).isThrownBy(
                () ->  handler.handle(SUBMITTED, callback, USER_AUTHORISATION));
    }
}
