package uk.gov.hmcts.reform.sscs.ccd.presubmit.requesthearingrecording;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;

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
import uk.gov.hmcts.reform.sscs.model.PartyItemList;
import uk.gov.hmcts.reform.sscs.service.HearingRecordingRequestService;

@RunWith(JUnitParamsRunner.class)
public class RequestHearingRecordingAboutToStartHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    RequestHearingRecordingAboutToStartHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Mock
    private HearingRecordingRequestService hearingRecordingRequestService;

    @BeforeEach
    public void setUp() {
        openMocks(this);
        handler = new RequestHearingRecordingAboutToStartHandler(hearingRecordingRequestService);

        when(callback.getEvent()).thenReturn(EventType.DWP_REQUEST_HEARING_RECORDING);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonRequestHearingRecordingEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    // JunitParamsRunnerToParameterized conversion not supported
    @Parameters({"ABOUT_TO_SUBMIT", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenARequestHearingRecordingEvent_thenBuildTheUi() {
        PreSubmitCallbackResponse response = new PreSubmitCallbackResponse(callback.getCaseDetails().getCaseData());

        when(hearingRecordingRequestService.buildHearingRecordingUi(any(PreSubmitCallbackResponse.class), eq(PartyItemList.DWP)))
                .thenReturn(response);

        PreSubmitCallbackResponse<SscsCaseData> result = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        verify(hearingRecordingRequestService).buildHearingRecordingUi(any(PreSubmitCallbackResponse.class), eq(PartyItemList.DWP));
        assertEquals(response, result);
    }

    @Test
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        assertThrows(IllegalStateException.class, () -> {
            when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
            handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        });
    }
}
