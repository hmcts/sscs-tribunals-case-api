package uk.gov.hmcts.reform.sscs.ccd.presubmit.readytolist;

import static java.time.LocalDateTime.now;
import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.RESPONSE_RECEIVED;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingState;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.Venue;
import uk.gov.hmcts.reform.sscs.model.hearings.HearingRequest;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;
import uk.gov.hmcts.reform.sscs.service.hmc.topic.HearingMessageService;
import uk.gov.hmcts.reform.sscs.service.servicebus.SendCallbackHandler;

@ExtendWith(MockitoExtension.class)
public class ReadyToListSubmittedHandlerTest {
    
    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String CASE_ID = "1234";

    @Mock
    private RegionalProcessingCenterService regionalProcessingCenterService;
    @Mock
    private HearingMessageService hearingsMessageService;
    @Mock
    private SendCallbackHandler sendCallbackHandler;

    private SscsCaseData caseData;
    private ReadyToListSubmittedHandler handler;

    private Callback<SscsCaseData> callback;
    private CaseDetails<SscsCaseData> caseDetails;

    @BeforeEach
    public void setUp() {
        caseData = SscsCaseData.builder()
                .ccdCaseId(CASE_ID)
                .createdInGapsFrom(State.READY_TO_LIST.getId())
                .appeal(Appeal.builder().build())
                .build();
        caseDetails =
                new CaseDetails<>(1234L, "SSCS", RESPONSE_RECEIVED, caseData, now(), "Benefit");
        callback = new Callback<>(caseDetails, empty(), READY_TO_LIST, false);

        handler = new ReadyToListSubmittedHandler(regionalProcessingCenterService, hearingsMessageService, sendCallbackHandler);
    }

    @ParameterizedTest
    @CsvSource({"APPEAL_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    public void givenANonHandleEvidenceEvent_thenReturnFalse(EventType eventType) {
        callback = new Callback<>(caseDetails, empty(), eventType, false);

        assertFalse(handler.canHandle(SUBMITTED, callback));
    }

    @ParameterizedTest
    @CsvSource({"ABOUT_TO_START", "MID_EVENT", "ABOUT_TO_SUBMIT"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }


    @Test
    public void returnAnErrorIfCreatedInGapsFromIsAtValidAppeal() {
        buildRegionalProcessingCentreMap(HearingRoute.GAPS);
        caseData.setRegion("TEST");
        caseData.setCreatedInGapsFrom(State.VALID_APPEAL.getId());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).contains("Case already created in GAPS at valid appeal.");
    }

    @Test
    public void returnAnErrorIfCreatedInGapsFromIsNull() {
        buildRegionalProcessingCentreMap(HearingRoute.GAPS);
        caseData.setCreatedInGapsFrom(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).contains("Case already created in GAPS at valid appeal.");
    }

    @Test
    public void givenAnRpcUsingListAssist_shouldSuccessfullySendAHearingRequestMessage() {
        buildRegionalProcessingCentreMap(HearingRoute.LIST_ASSIST);
        when(hearingsMessageService.sendMessage(any())).thenReturn(true);
        caseData.setRegion("TEST");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verifyMessagingServiceCalled();
        assertEquals(HearingRoute.LIST_ASSIST, response.getData().getSchedulingAndListingFields().getHearingRoute());
        assertEquals(HearingState.CREATE_HEARING, response.getData().getSchedulingAndListingFields().getHearingState());

        assertThat(response.getErrors())
                .as("A successfully sent message should not result in any errors.").isEmpty();
    }

    @Test
    public void givenAnIbcCase_shouldSuccessfullySendAHearingRequestMessageWithListAssist() {
        when(hearingsMessageService.sendMessage(any())).thenReturn(true);
        caseData.setBenefitCode("093");
        caseData.setRegion("TEST");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertEquals(HearingRoute.LIST_ASSIST, response.getData().getSchedulingAndListingFields().getHearingRoute());
        assertEquals(HearingState.CREATE_HEARING, response.getData().getSchedulingAndListingFields().getHearingState());

        assertThat(response.getErrors())
                .as("A successfully sent message should not result in any errors.").isEmpty();
    }

    @Test
    public void givenAnRpcUsingListAssistAndAnExistingGapsCase_shouldResolveToGaps() {
        caseData.setSchedulingAndListingFields(
                SchedulingAndListingFields.builder().hearingRoute(HearingRoute.GAPS).build()
        );
        callback = new Callback<>(caseDetails, empty(), READY_TO_LIST, true);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verifyNoInteractions(hearingsMessageService);
        assertEquals(HearingRoute.GAPS, response.getData().getSchedulingAndListingFields().getHearingRoute());
        assertEquals(HearingState.CREATE_HEARING, response.getData().getSchedulingAndListingFields().getHearingState());
        assertThat(response.getErrors())
                .as("A successfully sent message should not result in any errors.").isEmpty();
    }

    @Test
    public void givenAnRpcUsingListAssist_shouldAddErrorIfMessageFailedToSend() {
        buildRegionalProcessingCentreMap(HearingRoute.LIST_ASSIST);
        when(hearingsMessageService.sendMessage(any())).thenReturn(false);
        caseData.setRegion("TEST");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verifyMessagingServiceCalled();
        assertNull(response.getData().getSchedulingAndListingFields().getHearingRoute());
        assertNull(response.getData().getSchedulingAndListingFields().getHearingState());
        assertThat(response.getErrors())
                .as("An unsuccessfully sent message should result in an errors.").hasSize(1);
        assertThat(response.getErrors()).contains("An error occurred during message publish. Please try again.");
    }

    @Test
    public void givenAListAssistCaseIfAHearingExistsInTheFutureAndUserProceedsThenSendAHearingRequestMessage() {
        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
                .hearingDate(LocalDate.now().minusDays(10).toString())
                .start(now().minusDays(10))
                .hearingId(String.valueOf(1))
                .venue(Venue.builder().name("Venue 1").build())
                .time("12:00")
                .build()).build();
        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
                .hearingDate(LocalDate.now().plusDays(5).toString())
                .start(now().plusDays(5))
                .hearingId(String.valueOf(1))
                .venue(Venue.builder().name("Venue 1").build())
                .time("12:00")
                .build()).build();
        caseData.setHearings(List.of(hearing1, hearing2));
        callback = new Callback<>(caseDetails, empty(), READY_TO_LIST, true);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getWarnings()).isEmpty();
    }

    @Test
    public void respondWithNoErrorsIfCreatedFromGapsIsAtReadyToList() {
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), EventType.APPEAL_RECEIVED, false);

        assertThrows(IllegalStateException.class, () -> handler.handle(SUBMITTED, callback, USER_AUTHORISATION));
    }

    @Test
    public void givenRpcNotSet_HearingRouteShouldBeGaps() {
        caseData.setRegion("FakeRegion");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertEquals(HearingRoute.GAPS, response.getData().getSchedulingAndListingFields().getHearingRoute());
    }

    @Test
    public void givenIbcCase_HearingRoutesShouldBeListAssist() {
        caseData.setBenefitCode("093");
        caseData.setRegion("FakeRegion");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(SUBMITTED,
                callback, USER_AUTHORISATION);

        assertEquals(HearingRoute.LIST_ASSIST, response.getData().getSchedulingAndListingFields().getHearingRoute());
        assertEquals(HearingRoute.LIST_ASSIST, response.getData().getAppeal().getHearingOptions().getHearingRoute());
        assertEquals(HearingRoute.LIST_ASSIST, response.getData().getRegionalProcessingCenter().getHearingRoute());
        PreSubmitCallbackResponse<SscsCaseData> expectedResponse = HearingHandler
                .valueOf(HearingRoute.LIST_ASSIST.name()).handle(caseData, hearingsMessageService);
        assertEquals(expectedResponse.getData(), response.getData());
        assertEquals(expectedResponse.getErrors(), response.getErrors());
        assertEquals(expectedResponse.getWarnings(), response.getWarnings());
    }

    private void verifyMessagingServiceCalled() {
        verify(hearingsMessageService).sendMessage(HearingRequest.builder(CASE_ID)
                .hearingRoute(HearingRoute.LIST_ASSIST)
                .hearingState(HearingState.CREATE_HEARING)
                .build());
    }

    private void buildRegionalProcessingCentreMap(HearingRoute route) {
        Map<String, RegionalProcessingCenter> rpcMap = new HashMap<>();
        rpcMap.put("SSCS TEST", RegionalProcessingCenter.builder().hearingRoute(route)
                .name("TEST")
                .build());
        when(regionalProcessingCenterService.getRegionalProcessingCenterMap()).thenReturn(rpcMap);
    }
}
