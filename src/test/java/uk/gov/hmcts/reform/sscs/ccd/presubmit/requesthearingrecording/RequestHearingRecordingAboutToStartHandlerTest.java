package uk.gov.hmcts.reform.sscs.ccd.presubmit.requesthearingrecording;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;

import java.util.Arrays;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@RunWith(JUnitParamsRunner.class)
public class RequestHearingRecordingAboutToStartHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    RequestHearingRecordingAboutToStartHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new RequestHearingRecordingAboutToStartHandler();

        when(callback.getEvent()).thenReturn(EventType.DWP_REQUEST_HEARING_RECORDING);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        Hearing hearing1 = Hearing.builder().value(
                HearingDetails.builder().hearingId("an_id1").venue(Venue.builder().name("venue 1 name").build())
                        .hearingDate("2021-01-20")
                        .time("15:15").build()).build();
        Hearing hearing2 = Hearing.builder().value(
                HearingDetails.builder().hearingId("an_id2").venue(Venue.builder().name("venue 2 name").build())
                        .hearingDate("2021-02-20")
                        .time("15:15").build()).build();
        Hearing hearing3 = Hearing.builder().value(
                HearingDetails.builder().hearingId("an_id3").venue(Venue.builder().name("venue 3 name").build())
                        .hearingDate("2021-03-20")
                        .time("15:15").build()).build();

        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").hearings(Arrays.asList(hearing1, hearing2, hearing3)).build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

    }

    @Ignore
    @Test
    public void givenANonRequestHearingRecordingEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Ignore
    @Test
    public void givenAHearingWithRecording_thenHearingInRequestableList() {
        sscsCaseData.setHearings(singletonList(Hearing.builder().value(
                HearingDetails.builder().hearingId("an_id").venue(Venue.builder().name("venue name").build())
                        .hearingDate("2021-03-20")
                        .time("15:15").build()).build()));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertEquals(1, response.getData().getHearingRecordingsData().getRequestableHearingDetails().getListItems().size());
    }

    @Ignore
    @Test
    public void givenThreeHearingsWithRecording_thenThreeHearingInRequestableList() {
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertEquals(3, response.getData().getHearingRecordingsData().getRequestableHearingDetails().getListItems().size());
    }

    @Ignore
    @Test
    public void givenAHearingsRequested_thenHearingInRequestedList() {
        HearingRecordingRequest recordingRequest = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails.builder().requestingParty("dwp").requestedHearing("an_id2").status("requested").build()).build();
        sscsCaseData.getHearingRecordingsData().setHearingRecordingRequests(singletonList(recordingRequest));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals(2, response.getData().getHearingRecordingsData().getRequestableHearingDetails().getListItems().size());
        assertEquals(1, response.getData().getHearingRecordingsData().getRequestedHearings().size());
    }

}
