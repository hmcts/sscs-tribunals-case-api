package uk.gov.hmcts.reform.sscs.ccd.presubmit.readytolist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingState.CREATE_HEARING;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;

@RunWith(JUnitParamsRunner.class)
public class ReadyToListSubmittedHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private ReadyToListSubmittedHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private ListAssistHearingMessageHelper listAssistHearingMessageHelper;

    private SscsCaseData sscsCaseData;

    private static final String CASE_ID = "1234";

    @Before
    public void setUp() {
        openMocks(this);

        handler = new ReadyToListSubmittedHandler(listAssistHearingMessageHelper);

        ReflectionTestUtils.setField(handler, "gapsSwitchOverFeature", false);

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
    public void givenANonCallbackType_thenReturnTrue() {
        assertTrue(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    @Parameters({"APPEAL_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    public void givenANonHandleEvidenceEvent_thenReturnFalse(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertFalse(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_SUBMIT", "MID_EVENT", "ABOUT_TO_START"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenListAssist_shouldAddErrorIfMessageFailedToSend() {

        ReflectionTestUtils.setField(handler, "gapsSwitchOverFeature", true);

        when(listAssistHearingMessageHelper.sendHearingMessage(
            CASE_ID,
            LIST_ASSIST,
            CREATE_HEARING))
            .thenReturn(false);

        sscsCaseData = sscsCaseData.toBuilder()
            .region("TEST")
            .schedulingAndListingFields(SchedulingAndListingFields.builder()
                .hearingRoute(LIST_ASSIST)
                .build())
            .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(SUBMITTED,
            callback, USER_AUTHORISATION);

        verifyMessagingServiceCalled();

        assertThat(response.getData().getSchedulingAndListingFields().getHearingState()).isNull();

        assertThat(response.getErrors())
            .as("An unsuccessfully sent message should result in an errors.").hasSize(1);
        assertThat(response.getErrors())
            .contains("An error occurred during message publish. Please try again.");
    }

    @Test
    public void givenListAssist_shouldSuccessfullySendAHearingRequestMessage() {
        ReflectionTestUtils.setField(handler, "gapsSwitchOverFeature", true);

        when(listAssistHearingMessageHelper.sendHearingMessage(
            CASE_ID,
            LIST_ASSIST,
            CREATE_HEARING))
            .thenReturn(true);

        sscsCaseData = sscsCaseData.toBuilder()
            .region("TEST")
            .schedulingAndListingFields(SchedulingAndListingFields.builder()
                .hearingRoute(LIST_ASSIST)
                .build())
            .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(SUBMITTED, callback,
            USER_AUTHORISATION);

        verifyMessagingServiceCalled();

        assertThat(response.getData().getSchedulingAndListingFields().getHearingState()).isEqualTo(CREATE_HEARING);

        assertThat(response.getErrors())
            .as("A successfully sent message should not result in any errors.").isEmpty();
    }

    @Test
    public void givenListAssistButFeatureDisabled_shouldDoNothing() {
        sscsCaseData = sscsCaseData.toBuilder()
            .region("TEST")
            .schedulingAndListingFields(SchedulingAndListingFields.builder()
                .hearingRoute(LIST_ASSIST)
                .build())
            .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(SUBMITTED,
            callback, USER_AUTHORISATION);

        verifyNoInteractions(listAssistHearingMessageHelper);

        assertThat(response.getData().getSchedulingAndListingFields().getHearingState()).isNull();
    }

    private void verifyMessagingServiceCalled() {
        verify(listAssistHearingMessageHelper)
            .sendHearingMessage(CASE_ID, LIST_ASSIST, CREATE_HEARING);
    }
}
