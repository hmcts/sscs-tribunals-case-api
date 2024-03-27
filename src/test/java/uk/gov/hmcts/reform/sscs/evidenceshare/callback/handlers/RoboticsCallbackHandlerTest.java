package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CASE_UPDATED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.NOT_LISTABLE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.RoboticsService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;

@RunWith(JUnitParamsRunner.class)
public class RoboticsCallbackHandlerTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private RoboticsService roboticsService;

    @Mock
    private CcdService ccdService;

    @Mock
    private IdamService idamService;

    @Mock
    private RegionalProcessingCenterService regionalProcessingCenterService;

    @Mock
    private CaseDetails caseDetails;

    @Mock
    private SscsCaseDetails sscsCaseDetails;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SscsCaseData caseData;

    private RoboticsCallbackHandler handler;

    private final LocalDateTime now = LocalDateTime.now();

    @Before
    public void setUp() {
        when(callback.getEvent()).thenReturn(EventType.VALID_APPEAL_CREATED);

        handler = new RoboticsCallbackHandler(roboticsService, ccdService, idamService, regionalProcessingCenterService);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        when(caseData.isTranslationWorkOutstanding()).thenReturn(Boolean.FALSE);
        when(ccdService.getByCaseId(any(), any())).thenReturn(sscsCaseDetails);
        when(sscsCaseDetails.getData()).thenReturn(caseData);
    }

    @Test
    @Parameters({"VALID_APPEAL", "INTERLOC_VALID_APPEAL", "READY_TO_LIST", "VALID_APPEAL_CREATED", "RESEND_CASE_TO_GAPS2", "APPEAL_TO_PROCEED"})
    public void givenAValidRoboticsEvent_thenReturnTrue(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);

        assertTrue(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    public void givenANonRoboticsEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(handler.canHandle(SUBMITTED, callback));
    }


    @Test
    @Parameters({"VALID_APPEAL", "INTERLOC_VALID_APPEAL", "VALID_APPEAL_CREATED", "APPEAL_TO_PROCEED"})
    public void givenARoboticsRequestAndCreatedInGapsMatchesState_thenSendCaseToRoboticsAndSetSentToGapsDateAndDoNotTriggerUpdateCaseEvent(EventType eventType) {
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails(READY_TO_LIST, READY_TO_LIST.getId());
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), eventType, false);

        handler.handle(SUBMITTED, callback);

        verify(roboticsService).sendCaseToRobotics(any());

        assertEquals(LocalDate.now().toString(), callback.getCaseDetails().getCaseData().getDateCaseSentToGaps());
        verify(ccdService).getByCaseId(any(), any());
        verifyNoMoreInteractions(ccdService);
    }

    @Test
    @Parameters({"READY_TO_LIST", "RESEND_CASE_TO_GAPS2"})
    public void givenARoboticsRequestAndCreatedInGapsMatchesState_thenSendCaseToRoboticsAndSetSentToGapsDateAndTriggerUpdateCaseEvent(EventType eventType) {
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails(READY_TO_LIST, READY_TO_LIST.getId());
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), eventType, false);

        handler.handle(SUBMITTED, callback);

        verify(roboticsService).sendCaseToRobotics(any());

        assertEquals(LocalDate.now().toString(), callback.getCaseDetails().getCaseData().getDateCaseSentToGaps());

        ArgumentCaptor<String> capture = ArgumentCaptor.forClass(String.class);
        verify(ccdService).updateCase(any(), any(), capture.capture(), any(), any(), any());

        assertEquals(CASE_UPDATED.getCcdType(), capture.getValue());
    }

    @Test
    public void givenARoboticsRequestAndCreatedInGapsDoesNotMatchState_thenDoNotSendCaseToRobotics() {
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails(READY_TO_LIST, VALID_APPEAL.getId());
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.VALID_APPEAL_CREATED, false);

        handler.handle(SUBMITTED, callback);

        verifyNoInteractions(roboticsService);
    }

    @Test
    public void givenARoboticsRequestAndEventIsReissuetoGaps2_thenSendCaseToRobotics() {
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails(APPEAL_CREATED, READY_TO_LIST.getId());
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.RESEND_CASE_TO_GAPS2, false);

        handler.handle(SUBMITTED, callback);

        verify(roboticsService).sendCaseToRobotics(any());
    }

    @Test
    public void givenARoboticsRequestAndCreatedInGapsFieldIsBlank_thenSendCaseToRobotics() {
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails(APPEAL_CREATED, null);
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.VALID_APPEAL_CREATED, false);

        handler.handle(SUBMITTED, callback);

        verify(roboticsService).sendCaseToRobotics(any());
    }

    @Test
    public void givenARoboticsRequestAndCreatedInGapsFieldIsBlankAndAlreadySentInLast24Hours_thenDoNotSendCaseToRobotics() {

        when(caseData.getDateTimeSentToGaps()).thenReturn(Optional.of(LocalDateTime.now().minusHours(23)));

        CaseDetails<SscsCaseData> caseDetails = getCaseDetails(APPEAL_CREATED, null);
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.VALID_APPEAL_CREATED, false);

        handler.handle(SUBMITTED, callback);

        verifyNoInteractions(roboticsService);
    }

    @Test
    public void givenARoboticsRequestAndCreatedInGapsFieldIsBlankAndAlreadySentOutsideLast24Hours_thenDoSendCaseToRobotics() {

        when(caseData.getDateTimeSentToGaps()).thenReturn(Optional.of(LocalDateTime.now().minusHours(25)));

        CaseDetails<SscsCaseData> caseDetails = getCaseDetails(APPEAL_CREATED, null);
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.VALID_APPEAL_CREATED, false);

        handler.handle(SUBMITTED, callback);

        verify(roboticsService).sendCaseToRobotics(any());
    }

    private CaseDetails<SscsCaseData> getCaseDetails(State state, String createdInGapsFrom) {
        SscsCaseData caseData = SscsCaseData.builder()
            .ccdCaseId("123")
            .appeal(Appeal.builder().build())
            .createdInGapsFrom(createdInGapsFrom)
            .build();

        return new CaseDetails<>(123L, "jurisdiction", state, caseData, now, "Benefit");
    }

    @Test
    public void givenSendToDwpWithDocTranslated_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.SEND_TO_DWP);

        assertTrue(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    public void givenDocTranslationOutstanding_thenReturnFalse() {
        when(caseData.isTranslationWorkOutstanding()).thenReturn(Boolean.TRUE);
        assertFalse(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    public void givenResendCaseToGaps2WithDocTranslationOutstanding_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.RESEND_CASE_TO_GAPS2);
        when(caseData.isTranslationWorkOutstanding()).thenReturn(Boolean.TRUE);
        assertTrue(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    public void givenARoboticsRequestFromReviewConfidentialityRequestAndStateIsNotResponseReceivedAndConfidentialityRequestGranted_thenDoNotSendCaseToRobotics() {
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails(WITH_DWP, READY_TO_LIST.getId());
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.REVIEW_CONFIDENTIALITY_REQUEST, false);
        caseDetails.getCaseData().setConfidentialityRequestOutcomeJointParty(DatedRequestOutcome.builder().requestOutcome(RequestOutcome.GRANTED).build());

        assertFalse(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    public void givenARoboticsRequestFromReviewConfidentialityRequestAndStateIsResponseReceivedAndConfidentialityRequestRefused_thenDoNotSendCaseToRobotics() {
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails(RESPONSE_RECEIVED, READY_TO_LIST.getId());
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.REVIEW_CONFIDENTIALITY_REQUEST, false);
        caseDetails.getCaseData().setConfidentialityRequestOutcomeJointParty(DatedRequestOutcome.builder().requestOutcome(RequestOutcome.REFUSED).build());

        assertFalse(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    public void givenARoboticsRequestFromDwpRaiseExceptionAndStateIsWithDwp_thenSendCaseToRobotics() {
        handler = new RoboticsCallbackHandler(roboticsService, ccdService, idamService, regionalProcessingCenterService);

        CaseDetails<SscsCaseData> caseDetails = getCaseDetails(WITH_DWP, READY_TO_LIST.getId());
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.DWP_RAISE_EXCEPTION, false);
        caseDetails.getCaseData().setIsProgressingViaGaps("Yes");

        handler.handle(SUBMITTED, callback);

        verify(roboticsService).sendCaseToRobotics(any());

        assertEquals(LocalDate.now().toString(), callback.getCaseDetails().getCaseData().getDateCaseSentToGaps());

        ArgumentCaptor<String> capture = ArgumentCaptor.forClass(String.class);
        verify(ccdService).updateCase(any(), any(), capture.capture(), any(), any(), any());

        assertEquals(NOT_LISTABLE.getCcdType(), capture.getValue());
    }

    @Test
    public void givenRequestSentToRobotics_andGapSwitchOverFeatureEnabled_andHearingRouteIsListAssist_thenDoesNotRunFurther() {
        ReflectionTestUtils.setField(handler, "gapsSwitchOverFeature", true);

        when(caseData.getSchedulingAndListingFields().getHearingRoute()).thenReturn(HearingRoute.LIST_ASSIST);

        CaseDetails<SscsCaseData> caseDetails = getCaseDetails(APPEAL_CREATED, null);
        caseDetails.getCaseData().getSchedulingAndListingFields().setHearingRoute(HearingRoute.LIST_ASSIST);
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.RESEND_CASE_TO_GAPS2, false);
        handler.handle(SUBMITTED, callback);

        ArgumentCaptor<String> summaryCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> descriptionCaptor = ArgumentCaptor.forClass(String.class);
        verifyNoInteractions(roboticsService);
        verify(ccdService).updateCase(any(), any(), any(), summaryCaptor.capture(), descriptionCaptor.capture(), any());

        assertEquals("Case sent to List Assist", summaryCaptor.getValue());
        assertEquals("Updated case with sent to List Assist", descriptionCaptor.getValue());
    }
}
