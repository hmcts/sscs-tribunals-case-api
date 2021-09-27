package uk.gov.hmcts.reform.sscs.ccd.presubmit.requesthearingrecording;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@RunWith(JUnitParamsRunner.class)
public class RequestHearingRecordingAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    RequestHearingRecordingAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new RequestHearingRecordingAboutToSubmitHandler();

        when(callback.getEvent()).thenReturn(EventType.DWP_REQUEST_HEARING_RECORDING);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        DynamicListItem item1 = new DynamicListItem("an_id1", "\t\n"
                + "venue 1 name 15:15 20 January 2020");
        DynamicListItem item2 = new DynamicListItem("an_id2", "\t\n"
                + "venue 2 name 15:15 20 February 2020");
        DynamicListItem item3 = new DynamicListItem("an_id3", "\t\n"
                + "venue 1 name 15:15 20 March 2020");

        List<DynamicListItem> validHearings = new ArrayList<>();
        validHearings.add(item1);
        validHearings.add(item2);
        validHearings.add(item3);

        DynamicList dynamicList = new DynamicList(item1, validHearings);

        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
                .sscsHearingRecordingCaseData(SscsHearingRecordingCaseData.builder().requestableHearingDetails(dynamicList).build()).build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

    }

    @Test
    public void givenANonRequestHearingRecordingEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenASelectedHearingRecording_thenAddToNewRequestList() {
        SscsHearingRecording recording1 = SscsHearingRecording.builder().value(SscsHearingRecordingDetails.builder().hearingId("an_id1").build()).build();
        sscsCaseData.getSscsHearingRecordingCaseData().setSscsHearingRecordings(Arrays.asList(recording1));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(YesNo.YES, response.getData().getSscsHearingRecordingCaseData().getHearingRecordingRequestOutstanding());
        assertEquals(1, response.getData().getSscsHearingRecordingCaseData().getRequestedHearings().size());
        assertEquals("an_id1", response.getData().getSscsHearingRecordingCaseData().getRequestedHearings().get(0).getValue().getSscsHearingRecording().getHearingId());
    }

    @Test
    public void givenASelectedHearingRecording_thenAddToExistingRequestList() {
        SscsHearingRecording recording1 = SscsHearingRecording.builder().value(SscsHearingRecordingDetails.builder().hearingId("an_id1").build()).build();
        sscsCaseData.getSscsHearingRecordingCaseData().setSscsHearingRecordings(Arrays.asList(recording1));

        HearingRecordingRequest hearingRecordingRequest = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails.builder()
                .requestingParty("dwp").build()).build();
        List<HearingRecordingRequest> hearingRecordingRequests = new ArrayList<>();
        hearingRecordingRequests.add(hearingRecordingRequest);
        sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearings(hearingRecordingRequests);
        sscsCaseData.getSscsHearingRecordingCaseData().setHearingRecordingRequestOutstanding(YesNo.YES);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(YesNo.YES, response.getData().getSscsHearingRecordingCaseData().getHearingRecordingRequestOutstanding());
        assertEquals(2, response.getData().getSscsHearingRecordingCaseData().getRequestedHearings().size());
        assertEquals("an_id1", response.getData().getSscsHearingRecordingCaseData().getRequestedHearings().get(1).getValue().getSscsHearingRecording().getHearingId());
    }


}
