package uk.gov.hmcts.reform.sscs.ccd.presubmit.readytolist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.readytolist.ReadyToListAboutToSubmitHandler.EXISTING_HEARING_WARNING;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.readytolist.ReadyToListAboutToSubmitHandler.GAPS_CASE_WARNING;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.model.hearings.HearingRequest;
import uk.gov.hmcts.reform.sscs.model.servicebus.NoOpMessagingService;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;
import uk.gov.hmcts.reform.sscs.service.hmc.topic.HearingMessageService;
import uk.gov.hmcts.reform.sscs.service.hmc.topic.HearingMessagingServiceFactory;

@RunWith(JUnitParamsRunner.class)
public class ReadyToListAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private ReadyToListAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private RegionalProcessingCenterService regionalProcessingCenterService;

    @Mock
    private HearingMessagingServiceFactory hearingMessagingServiceFactory;

    @Mock
    private HearingMessageService hearingsMessageService;

    private SscsCaseData sscsCaseData;

    private static final String CASE_ID = "1234";

    @Before
    public void setUp() {
        openMocks(this);

        when(hearingMessagingServiceFactory.getMessagingService(HearingRoute.GAPS))
            .thenReturn(new NoOpMessagingService());
        when(hearingMessagingServiceFactory.getMessagingService(HearingRoute.LIST_ASSIST))
            .thenReturn(hearingsMessageService);

        handler = new ReadyToListAboutToSubmitHandler(false, regionalProcessingCenterService,
            hearingMessagingServiceFactory);

        when(callback.getEvent()).thenReturn(EventType.READY_TO_LIST);

        sscsCaseData = SscsCaseData.builder()
                .ccdCaseId(CASE_ID)
                .createdInGapsFrom(State.READY_TO_LIST.getId())
                .appeal(Appeal.builder().build())
                .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    @Parameters({"APPEAL_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    public void givenANonHandleEvidenceEvent_thenReturnFalse(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }


    @Test
    public void returnAnErrorIfCreatedInGapsFromIsAtValidAppeal() {
        buildRegionalProcessingCentreMap(HearingRoute.GAPS);
        sscsCaseData = sscsCaseData.toBuilder().region("TEST").createdInGapsFrom(State.VALID_APPEAL.getId()).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("Case already created in GAPS at valid appeal.", response.getErrors().toArray()[0]);
    }

    @Test
    public void returnAnErrorIfCreatedInGapsFromIsNull() {
        buildRegionalProcessingCentreMap(HearingRoute.GAPS);
        sscsCaseData = sscsCaseData.toBuilder().region("TEST").createdInGapsFrom(null).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);


        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("Case already created in GAPS at valid appeal.", response.getErrors().toArray()[0]);
    }

    @Test
    public void givenAnRpcUsingListAssist_shouldSuccessfullySendAHearingRequestMessage() {
        buildRegionalProcessingCentreMap(HearingRoute.LIST_ASSIST);
        when(hearingsMessageService.sendMessage(any())).thenReturn(true);

        handler = new ReadyToListAboutToSubmitHandler(true, regionalProcessingCenterService,
            hearingMessagingServiceFactory);

        sscsCaseData = sscsCaseData.toBuilder().region("TEST").build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback,
            USER_AUTHORISATION);

        verifyMessagingServiceCalled();

        assertThat(response.getData().getSchedulingAndListingFields().getHearingRoute()).isEqualTo(HearingRoute.LIST_ASSIST);
        assertThat(response.getData().getSchedulingAndListingFields().getHearingState()).isEqualTo(HearingState.CREATE_HEARING);

        assertThat(response.getErrors())
            .as("A successfully sent message should not result in any errors.").isEmpty();
    }

    @Test
    public void givenAnIbcCase_shouldSuccessfullySendAHearingRequestMessageWithListAssist() {
        buildRegionalProcessingCentreMap(HearingRoute.LIST_ASSIST);
        when(hearingsMessageService.sendMessage(any())).thenReturn(true);

        handler = new ReadyToListAboutToSubmitHandler(true, regionalProcessingCenterService,
            hearingMessagingServiceFactory);

        sscsCaseData = sscsCaseData.toBuilder().benefitCode("093").region("TEST").build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback,
            USER_AUTHORISATION);

        assertThat(response.getData().getSchedulingAndListingFields().getHearingRoute()).isEqualTo(HearingRoute.LIST_ASSIST);
        assertThat(response.getData().getSchedulingAndListingFields().getHearingState()).isEqualTo(HearingState.CREATE_HEARING);

        assertThat(response.getErrors())
            .as("A successfully sent message should not result in any errors.").isEmpty();
    }

    @Test
    public void givenAnRpcUsingListAssistAndAnExistingGapsCase_shouldResolveToGaps() {
        buildRegionalProcessingCentreMap(HearingRoute.LIST_ASSIST);
        when(hearingsMessageService.sendMessage(any())).thenReturn(true);

        handler = new ReadyToListAboutToSubmitHandler(true, regionalProcessingCenterService,
            hearingMessagingServiceFactory);

        sscsCaseData = sscsCaseData.toBuilder().schedulingAndListingFields(SchedulingAndListingFields
                .builder()
                .hearingRoute(HearingRoute.GAPS).build())
            .region("TEST")
            .build();


        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(callback.isIgnoreWarnings()).thenReturn(true);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback,
            USER_AUTHORISATION);

        verifyNoInteractions(hearingsMessageService);

        assertThat(response.getData().getSchedulingAndListingFields().getHearingRoute())
            .isEqualTo(HearingRoute.GAPS);
        assertThat(response.getData().getSchedulingAndListingFields().getHearingState())
            .isEqualTo(HearingState.CREATE_HEARING);

        assertThat(response.getErrors())
            .as("A successfully sent message should not result in any errors.").isEmpty();
    }

    @Test
    public void givenAnRpcUsingListAssist_shouldAddErrorIfMessageFailedToSend() {
        buildRegionalProcessingCentreMap(HearingRoute.LIST_ASSIST);
        when(hearingsMessageService.sendMessage(any())).thenReturn(false);

        handler = new ReadyToListAboutToSubmitHandler(true, regionalProcessingCenterService,
            hearingMessagingServiceFactory);

        sscsCaseData = sscsCaseData.toBuilder().region("TEST").build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT,
            callback, USER_AUTHORISATION);

        verifyMessagingServiceCalled();

        assertThat(response.getData().getSchedulingAndListingFields().getHearingRoute()).isNull();
        assertThat(response.getData().getSchedulingAndListingFields().getHearingState()).isNull();

        assertThat(response.getErrors())
            .as("An unsuccessfully sent message should result in an errors.").hasSize(1);
        assertThat(response.getErrors())
            .contains("An error occurred during message publish. Please try again.");
    }

    @Test
    public void givenAnRpcUsingListAssistButFeatureDisabled_shouldDoNothing() {
        buildRegionalProcessingCentreMap(HearingRoute.LIST_ASSIST);
        when(hearingsMessageService.sendMessage(any())).thenReturn(true);

        handler = new ReadyToListAboutToSubmitHandler(false, regionalProcessingCenterService,
            hearingMessagingServiceFactory);

        sscsCaseData = sscsCaseData.toBuilder().region("TEST").build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT,
            callback, USER_AUTHORISATION);

        verifyNoInteractions(hearingsMessageService);

        assertThat(response.getData().getSchedulingAndListingFields().getHearingRoute()).isNull();
        assertThat(response.getData().getSchedulingAndListingFields().getHearingState()).isNull();
    }

    @Test
    public void givenAGapsCaseOnSubmitReturnWarning() {
        SchedulingAndListingFields schedulingAndListingFields = SchedulingAndListingFields.builder()
                .hearingRoute(HearingRoute.GAPS)
                .build();
        sscsCaseData = sscsCaseData.toBuilder()
                .schedulingAndListingFields(schedulingAndListingFields)
                .region("TEST")
                .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        handler = new ReadyToListAboutToSubmitHandler(false, regionalProcessingCenterService,
                hearingMessagingServiceFactory);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT,
                callback, USER_AUTHORISATION);

        MatcherAssert.assertThat(response.getErrors().size(), is(0));
        MatcherAssert.assertThat(response.getWarnings().size(), is(1));
        MatcherAssert.assertThat(response.getWarnings().iterator().next(), is(GAPS_CASE_WARNING));
    }

    @Test
    public void givenAGapsCaseOnSubmitIgnoreWarningIIgnoreWarningsFieldIsYes() {
        SchedulingAndListingFields schedulingAndListingFields = SchedulingAndListingFields.builder()
                .hearingRoute(HearingRoute.GAPS)
                .build();
        sscsCaseData = sscsCaseData.toBuilder()
                .schedulingAndListingFields(schedulingAndListingFields)
                .region("TEST")
                .build();
        sscsCaseData.setIgnoreCallbackWarnings(YES);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        handler = new ReadyToListAboutToSubmitHandler(false, regionalProcessingCenterService,
                hearingMessagingServiceFactory);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT,
                callback, USER_AUTHORISATION);

        MatcherAssert.assertThat(response.getErrors().size(), is(0));
        MatcherAssert.assertThat(response.getWarnings().size(), is(0));
    }

    @Test
    @Parameters({"YES", "NO"})
    public void givenAListAssistCaseIfAHearingExistsInTheFutureThenReturnWarning(YesNo ignoreCallbackWarnings) {
        HearingDetails hearingDetails1 = HearingDetails.builder()
                .hearingDate(LocalDate.now().minusDays(10).toString())
                .start(LocalDateTime.now().minusDays(10))
                .hearingId(String.valueOf(1))
                .venue(Venue.builder().name("Venue 1").build())
                .time("12:00")
                .build();
        Hearing hearing1 = Hearing.builder().value(hearingDetails1).build();

        HearingDetails hearingDetails2 = HearingDetails.builder()
                .hearingDate(LocalDate.now().plusDays(5).toString())
                .start(LocalDateTime.now().plusDays(5))
                .hearingId(String.valueOf(1))
                .venue(Venue.builder().name("Venue 1").build())
                .time("12:00")
                .build();
        Hearing hearing2 = Hearing.builder().value(hearingDetails2).build();

        sscsCaseData = sscsCaseData.toBuilder()
                .hearings(List.of(hearing1, hearing2))
                .region("TEST")
                .ignoreCallbackWarnings(ignoreCallbackWarnings)
                .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        handler = new ReadyToListAboutToSubmitHandler(false, regionalProcessingCenterService, hearingMessagingServiceFactory);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        MatcherAssert.assertThat(response.getErrors().size(), is(0));
        if (ignoreCallbackWarnings == NO) {
            MatcherAssert.assertThat(response.getWarnings().size(), is(1));
            assertTrue(response.getWarnings().contains(EXISTING_HEARING_WARNING));
        } else {
            MatcherAssert.assertThat(response.getWarnings().size(), is(0));
        }
    }

    @Test
    public void givenAListAssistCaseIfAHearingExistsInTheFutureAndUserProceedsThenSendAHearingRequestMessage() {
        HearingDetails hearingDetails1 = HearingDetails.builder()
                .hearingDate(LocalDate.now().minusDays(10).toString())
                .start(LocalDateTime.now().minusDays(10))
                .hearingId(String.valueOf(1))
                .venue(Venue.builder().name("Venue 1").build())
                .time("12:00")
                .build();
        Hearing hearing1 = Hearing.builder().value(hearingDetails1).build();

        HearingDetails hearingDetails2 = HearingDetails.builder()
                .hearingDate(LocalDate.now().plusDays(5).toString())
                .start(LocalDateTime.now().plusDays(5))
                .hearingId(String.valueOf(1))
                .venue(Venue.builder().name("Venue 1").build())
                .time("12:00")
                .build();
        Hearing hearing2 = Hearing.builder().value(hearingDetails2).build();

        sscsCaseData = sscsCaseData.toBuilder()
                .hearings(List.of(hearing1, hearing2))
                .region("TEST")
                .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(callback.isIgnoreWarnings()).thenReturn(true);
        handler = new ReadyToListAboutToSubmitHandler(false, regionalProcessingCenterService, hearingMessagingServiceFactory);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        MatcherAssert.assertThat(response.getErrors().size(), is(0));
        MatcherAssert.assertThat(response.getWarnings().size(), is(0));
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

    @Test
    public void respondWithNoErrorsIfCreatedFromGapsIsAtReadyToList() {
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

    @Test
    public void givenRpcNotSet_HearingRouteShouldBeGaps() {

        handler = new ReadyToListAboutToSubmitHandler(true, regionalProcessingCenterService,
                hearingMessagingServiceFactory);

        sscsCaseData = sscsCaseData.toBuilder().region("FakeRegion").build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT,
                callback, USER_AUTHORISATION);

        assertEquals(HearingRoute.GAPS, response.getData().getSchedulingAndListingFields().getHearingRoute());
    }

    @Test
    public void givenIbcCase_HearingRoutesShouldBeListAssist() {

        handler = new ReadyToListAboutToSubmitHandler(true, regionalProcessingCenterService,
            hearingMessagingServiceFactory);

        sscsCaseData = sscsCaseData.toBuilder().benefitCode("093").region("FakeRegion").build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT,
            callback, USER_AUTHORISATION);

        assertEquals(HearingRoute.LIST_ASSIST, response.getData().getSchedulingAndListingFields().getHearingRoute());
        assertEquals(HearingRoute.LIST_ASSIST, response.getData().getAppeal().getHearingOptions().getHearingRoute());
        assertEquals(HearingRoute.LIST_ASSIST, response.getData().getRegionalProcessingCenter().getHearingRoute());
        PreSubmitCallbackResponse<SscsCaseData> expectedResponse = HearingHandler
            .valueOf(HearingRoute.LIST_ASSIST.name()).handle(sscsCaseData, true,
            hearingMessagingServiceFactory.getMessagingService(HearingRoute.LIST_ASSIST));
        assertEquals(expectedResponse.getData(), response.getData());
        assertEquals(expectedResponse.getErrors(), response.getErrors());
        assertEquals(expectedResponse.getWarnings(), response.getWarnings());
    }
}
