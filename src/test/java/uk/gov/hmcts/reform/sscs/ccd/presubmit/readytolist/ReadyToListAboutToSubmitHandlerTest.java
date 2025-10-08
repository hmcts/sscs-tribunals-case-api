package uk.gov.hmcts.reform.sscs.ccd.presubmit.readytolist;

import static java.time.LocalDateTime.now;
import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.RESPONSE_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.readytolist.ReadyToListAboutToSubmitHandler.EXISTING_HEARING_WARNING;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.readytolist.ReadyToListAboutToSubmitHandler.GAPS_CASE_WARNING;

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
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingState;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.exception.GetCaseException;
import uk.gov.hmcts.reform.sscs.exception.TribunalsEventProcessingException;
import uk.gov.hmcts.reform.sscs.exception.UpdateCaseException;
import uk.gov.hmcts.reform.sscs.helper.SscsHelper;
import uk.gov.hmcts.reform.sscs.model.hearings.HearingRequest;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus;
import uk.gov.hmcts.reform.sscs.model.multi.hearing.CaseHearing;
import uk.gov.hmcts.reform.sscs.model.multi.hearing.HearingsGetResponse;
import uk.gov.hmcts.reform.sscs.service.HmcHearingApiService;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;
import uk.gov.hmcts.reform.sscs.service.hmc.topic.HearingRequestHandler;

@ExtendWith(MockitoExtension.class)
public class ReadyToListAboutToSubmitHandlerTest {
    
    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String CASE_ID = "1234";

    @Mock
    private RegionalProcessingCenterService regionalProcessingCenterService;
    @Mock
    private HearingRequestHandler hearingRequestHandler;
    @Mock
    private HmcHearingApiService hmcHearingApiService;
    @Mock
    private SscsHelper sscsHelper;

    private SscsCaseData caseData;
    private ReadyToListAboutToSubmitHandler handler;

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

        handler = new ReadyToListAboutToSubmitHandler(regionalProcessingCenterService, hearingRequestHandler, sscsHelper, hmcHearingApiService);
    }

    @ParameterizedTest
    @CsvSource({"APPEAL_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    public void givenANonHandleEvidenceEvent_thenReturnFalse(EventType eventType) {
        callback = new Callback<>(caseDetails, empty(), eventType, false);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @ParameterizedTest
    @CsvSource({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenAGapsCaseOnSubmitReturnWarning() {
        caseData.setSchedulingAndListingFields(
                SchedulingAndListingFields.builder().hearingRoute(HearingRoute.GAPS).build()
        );

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT,
                callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
        assertEquals(1, response.getWarnings().size());
        assertEquals(GAPS_CASE_WARNING, response.getWarnings().iterator().next());
    }

    @Test
    public void givenAGapsCaseOnSubmitIgnoreWarningIIgnoreWarningsFieldIsYes() {
        caseData.setSchedulingAndListingFields(
                SchedulingAndListingFields.builder().hearingRoute(HearingRoute.GAPS).build()
        );
        caseData.setIgnoreCallbackWarnings(YES);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT,
                callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
        assertEquals(0, response.getWarnings().size());
    }

    @Test
    public void givenAListAssistCaseIfAHearingIsListedThenReturnError() {
        caseData.getSchedulingAndListingFields().setHearingRoute(HearingRoute.LIST_ASSIST);
        caseData.setRegion("TEST");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
        assertFalse(response.getErrors().contains(EXISTING_HEARING_WARNING));
    }

    @Test
    public void returnAnErrorIfCreatedInGapsFromIsAtValidAppeal() {
        buildRegionalProcessingCentreMap(HearingRoute.GAPS);
        caseData.setRegion("TEST");
        caseData.setCreatedInGapsFrom(State.VALID_APPEAL.getId());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).contains("Case already created in GAPS at valid appeal.");
    }

    @Test
    public void returnAnErrorIfCreatedInGapsFromIsNull() {
        buildRegionalProcessingCentreMap(HearingRoute.GAPS);
        caseData.setCreatedInGapsFrom(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).contains("Case already created in GAPS at valid appeal.");
    }

    @Test
    public void givenAnRpcUsingListAssist_shouldSuccessfullySendAHearingRequestMessage()
            throws UpdateCaseException, TribunalsEventProcessingException, GetCaseException {
        buildRegionalProcessingCentreMap(HearingRoute.LIST_ASSIST);
        doNothing().when(hearingRequestHandler).handleHearingRequest(any());
        caseData.setRegion("TEST");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verifyMessagingServiceCalled();
        assertEquals(HearingRoute.LIST_ASSIST, response.getData().getSchedulingAndListingFields().getHearingRoute());
        assertEquals(HearingState.CREATE_HEARING, response.getData().getSchedulingAndListingFields().getHearingState());

        assertThat(response.getErrors())
                .as("A successfully sent message should not result in any errors.").isEmpty();
    }

    @Test
    public void givenAnIbcCase_shouldSuccessfullySendAHearingRequestMessageWithListAssist()
            throws UpdateCaseException, TribunalsEventProcessingException, GetCaseException {
        doNothing().when(hearingRequestHandler).handleHearingRequest(any());
        caseData.setBenefitCode("093");
        caseData.setRegion("TEST");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

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

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verifyNoInteractions(hearingRequestHandler);
        assertEquals(HearingRoute.GAPS, response.getData().getSchedulingAndListingFields().getHearingRoute());
        assertEquals(HearingState.CREATE_HEARING, response.getData().getSchedulingAndListingFields().getHearingState());
        assertThat(response.getErrors())
                .as("A successfully sent message should not result in any errors.").isEmpty();
    }

    @Test
    public void givenAnRpcUsingListAssist_shouldAddErrorIfMessageFailedToSend()
            throws UpdateCaseException, TribunalsEventProcessingException, GetCaseException {
        buildRegionalProcessingCentreMap(HearingRoute.LIST_ASSIST);
        doThrow(UpdateCaseException.class).when(hearingRequestHandler).handleHearingRequest(any());
        caseData.setRegion("TEST");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verifyMessagingServiceCalled();
        assertNull(response.getData().getSchedulingAndListingFields().getHearingRoute());
        assertNull(response.getData().getSchedulingAndListingFields().getHearingState());
        assertThat(response.getErrors())
                .as("An unsuccessfully sent message should result in an errors.").hasSize(1);
        assertThat(response.getErrors()).contains("An error occurred during message publish. Please try again.");
    }

    @Test
    public void givenAListAssistCaseIfAHearingExistsInTheFutureAndUserProceedsThenSendAHearingRequestMessage() {
        caseData.getSchedulingAndListingFields().setHearingRoute(HearingRoute.LIST_ASSIST);
        callback = new Callback<>(caseDetails, empty(), READY_TO_LIST, true);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getWarnings()).isEmpty();
    }

    @Test
    public void respondWithNoErrorsIfCreatedFromGapsIsAtReadyToList() {
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), EventType.APPEAL_RECEIVED, false);

        assertThrows(IllegalStateException.class, () -> handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION));
    }

    @Test
    public void givenRpcNotSet_HearingRouteShouldBeGaps() {
        caseData.setRegion("FakeRegion");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(HearingRoute.GAPS, response.getData().getSchedulingAndListingFields().getHearingRoute());
    }

    @Test
    public void givenIbcCase_HearingRoutesShouldBeListAssist() {
        caseData.setBenefitCode("093");
        caseData.setRegion("FakeRegion");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT,
                callback, USER_AUTHORISATION);

        assertEquals(HearingRoute.LIST_ASSIST, response.getData().getSchedulingAndListingFields().getHearingRoute());
        assertEquals(HearingRoute.LIST_ASSIST, response.getData().getAppeal().getHearingOptions().getHearingRoute());
        assertEquals(HearingRoute.LIST_ASSIST, response.getData().getRegionalProcessingCenter().getHearingRoute());
        PreSubmitCallbackResponse<SscsCaseData> expectedResponse = HearingHandler
                .valueOf(HearingRoute.LIST_ASSIST.name()).handle(caseData, hearingRequestHandler);
        assertEquals(expectedResponse.getData(), response.getData());
        assertEquals(expectedResponse.getErrors(), response.getErrors());
        assertEquals(expectedResponse.getWarnings(), response.getWarnings());
    }

    private void verifyMessagingServiceCalled()
            throws UpdateCaseException, TribunalsEventProcessingException, GetCaseException {
        verify(hearingRequestHandler).handleHearingRequest(HearingRequest.builder(CASE_ID)
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
