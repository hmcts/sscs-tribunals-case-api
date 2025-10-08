package uk.gov.hmcts.reform.sscs.ccd.presubmit.updatenotlistable;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.READY_TO_LIST;

import feign.FeignException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

public class UpdateNotListableSubmittedHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private UpdateNotListableSubmittedHandler handler;

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

    @BeforeEach
    public void setUp() {
        openMocks(this);
        handler = new UpdateNotListableSubmittedHandler(updateCcdCaseService, idamService);

        when(callback.getEvent()).thenReturn(EventType.UPDATE_NOT_LISTABLE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getId()).thenReturn(123L);
        sscsCaseData = SscsCaseData.builder().build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());

    }

    @Test
    public void givenANonUpdateNotListableEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @ParameterizedTest
    @EnumSource(value = CallbackType.class, mode = EnumSource.Mode.EXCLUDE, names = {"SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenShouldReadyToListBeTriggeredYes_thenTriggerReadyToListEvent() {
        sscsCaseData.setShouldReadyToListBeTriggered(YesNo.YES);
        sscsCaseData.setSchedulingAndListingFields(SchedulingAndListingFields.builder().hearingRoute(HearingRoute.LIST_ASSIST).build());
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

        assertNull(sscsCaseData.getShouldReadyToListBeTriggered());
    }

    @Test
    public void givenShouldReadyToListBeTriggeredNo_thenDoNotTriggerReadyToListEvent() {
        sscsCaseData.setShouldReadyToListBeTriggered(YesNo.NO);
        sscsCaseData.setSchedulingAndListingFields(SchedulingAndListingFields.builder().hearingRoute(HearingRoute.LIST_ASSIST).build());
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertEquals(Collections.EMPTY_SET, response.getErrors());

        verify(updateCcdCaseService, never()).updateCaseV2(
            any(),
            any(),
            any(),
            any(),
            any(IdamTokens.class),
            any());
        assertEquals(YesNo.NO, sscsCaseData.getShouldReadyToListBeTriggered());
    }

    @Test
    public void givenShouldReadyToListBeTriggeredYesButGaps_thenTriggerReadyToListEvent() {
        sscsCaseData.setShouldReadyToListBeTriggered(YesNo.YES);
        sscsCaseData.setSchedulingAndListingFields(SchedulingAndListingFields.builder().hearingRoute(HearingRoute.GAPS).build());
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

        assertEquals(YesNo.YES, sscsCaseData.getShouldReadyToListBeTriggered());
    }

    @Test
    public void givenShouldReadyToListBeTriggeredNull_thenDoNotTriggerReadyToListEvent() {
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        sscsCaseData.setSchedulingAndListingFields(SchedulingAndListingFields.builder().hearingRoute(HearingRoute.LIST_ASSIST).build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertEquals(Collections.EMPTY_SET, response.getErrors());

        verify(updateCcdCaseService, never()).updateCaseV2(
            any(),
            any(),
            any(),
            any(),
            any(IdamTokens.class),
            any());
        assertNull(sscsCaseData.getShouldReadyToListBeTriggered());
    }

    @Test
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertThrows(IllegalStateException.class, () -> handler.handle(SUBMITTED, callback, USER_AUTHORISATION));
    }

    @Test
    public void givenSubmittedEventAndShouldBeTriggered_thenThrowExceptionWithResponseBodyWhenReadyToListEventFailsToUpdate() {
        sscsCaseData.setShouldReadyToListBeTriggered(YesNo.YES);
        sscsCaseData.setSchedulingAndListingFields(SchedulingAndListingFields.builder().hearingRoute(HearingRoute.LIST_ASSIST).build());
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
            any());

        assertThatExceptionOfType(FeignException.class).isThrownBy(
            () -> handler.handle(SUBMITTED, callback, USER_AUTHORISATION));
    }
}
