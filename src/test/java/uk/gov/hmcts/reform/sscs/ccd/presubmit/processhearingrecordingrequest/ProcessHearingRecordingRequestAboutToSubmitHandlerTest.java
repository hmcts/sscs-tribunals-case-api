package uk.gov.hmcts.reform.sscs.ccd.presubmit.processhearingrecordingrequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.model.PartyItemList;


@RunWith(JUnitParamsRunner.class)
public class ProcessHearingRecordingRequestAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    ProcessHearingRecordingRequestAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new ProcessHearingRecordingRequestAboutToSubmitHandler();

        when(callback.getEvent()).thenReturn(EventType.PROCESS_HEARING_RECORDING_REQUEST);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
                .sscsHearingRecordingCaseData(SscsHearingRecordingCaseData.builder().build()).build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    private ProcessHearingRecordingRequest getDwpProcessHearingRecordingRequests(RequestStatus status, String hearingId) {
        DynamicListItem dynamicListItem = new DynamicListItem(status.getValue(), status.getValue());
        return ProcessHearingRecordingRequest.builder().value(ProcessHearingRecordingRequestDetails.builder()
                .dwp(new DynamicList(dynamicListItem, Collections.emptyList()))
                .appellant(new DynamicList(dynamicListItem, Collections.emptyList()))
                .hearingId(hearingId).build()).build();
    }

    @Test
    public void givenAGrantedFromRequestedDwpHearingRecording_thenRemoveFromRequestedListAndAddToReleasedListAndDwpStatusReleased() {
        SscsHearingRecording recording1 = SscsHearingRecording.builder().value(SscsHearingRecordingDetails.builder()
                .hearingId("an_id1").build()).build();
        HearingRecordingRequest request1 = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails
                .builder().sscsHearingRecordingList(Arrays.asList(recording1))
                .requestingParty(PartyItemList.DWP.getCode()).build()).build();
        sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearings(new ArrayList<>(Arrays.asList(request1)));
        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequests(Arrays
                .asList(getDwpProcessHearingRecordingRequests(RequestStatus.GRANTED, "an_id1")));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        SscsHearingRecordingCaseData sscsHearingRecordingCaseDataResponse = response.getData()
                .getSscsHearingRecordingCaseData();
        assertThat("Check RequestedHearings has been reduced",
                sscsHearingRecordingCaseDataResponse.getRequestedHearings(), is(empty()));
        assertThat("Check DwpReleasedHearings is populated",
                sscsHearingRecordingCaseDataResponse.getDwpReleasedHearings(), is(not(empty())));
        assertThat("Check DwpReleasedHearings has the correct Hearing",
                sscsHearingRecordingCaseDataResponse.getDwpReleasedHearings().get(0).getValue()
                        .getSscsHearingRecordingList().get(0).getValue().getHearingId(), is("an_id1"));
        assertThat("Check DwpState is PROCESSED", sscsCaseData.getDwpState(),
                is(DwpState.HEARING_RECORDING_PROCESSED.getId()));
    }

    @Test
    public void givenARefusedFromRequestedDwpHearingRecording_thenRemoveFromRequestedListAndDoNotAddToReleasedListAndDwpStatusReleased() {
        SscsHearingRecording recording1 = SscsHearingRecording.builder().value(SscsHearingRecordingDetails.builder()
                .hearingId("an_id1").build()).build();
        HearingRecordingRequest request1 = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails
                .builder().sscsHearingRecordingList(Arrays.asList(recording1))
                .requestingParty(PartyItemList.DWP.getCode()).build()).build();
        sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearings(new ArrayList<>(Arrays.asList(request1)));
        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequests(Arrays
                .asList(getDwpProcessHearingRecordingRequests(RequestStatus.REFUSED, "an_id1")));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        SscsHearingRecordingCaseData sscsHearingRecordingCaseDataResponse = response.getData()
                .getSscsHearingRecordingCaseData();
        assertThat("Check RequestedHearings has been reduced",
                sscsHearingRecordingCaseDataResponse.getRequestedHearings(), is(empty()));
        assertThat("Check ReleasedHearings has not been populated",
                sscsHearingRecordingCaseDataResponse.getDwpReleasedHearings(), is(empty()));
        assertThat("Check RefusedHearings was populated",
                sscsHearingRecordingCaseDataResponse.getRefusedHearings(), is(not(empty())));
        assertThat("Check DwpState is PROCESSED", sscsCaseData.getDwpState(),
                is(DwpState.HEARING_RECORDING_PROCESSED.getId()));
    }

    @Test
    public void givenARefusedFromGrantedDwpHearingRecording_thenRemoveFromReleasedListAndDwpStatusRefused() {
        SscsHearingRecording recording1 = SscsHearingRecording.builder().value(SscsHearingRecordingDetails.builder()
                .hearingId("an_id1").build()).build();
        HearingRecordingRequest request1 = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails
                .builder().sscsHearingRecordingList(Arrays.asList(recording1))
                .requestingParty(PartyItemList.DWP.getCode()).build()).build();
        sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearings(new ArrayList<>(Arrays.asList(request1)));
        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequests(Arrays
                .asList(getDwpProcessHearingRecordingRequests(RequestStatus.REFUSED, "an_id1")));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        SscsHearingRecordingCaseData sscsHearingRecordingCaseDataResponse = response.getData()
                .getSscsHearingRecordingCaseData();
        assertThat("Check RequestedHearings has been reduced",
                sscsHearingRecordingCaseDataResponse.getDwpReleasedHearings(), is(empty()));
        assertThat("Check DwpRefusedHearings is not populated",
                sscsHearingRecordingCaseDataResponse.getRefusedHearings(), is(not(empty())));
        assertThat("Check RefusedHearings has the correct Hearing",
                sscsHearingRecordingCaseDataResponse.getRefusedHearings().get(0).getValue()
                        .getSscsHearingRecordingList().get(0).getValue().getHearingId(), is("an_id1"));
        assertThat("Check DwpState is PROCESSED", sscsCaseData.getDwpState(),
                is(DwpState.HEARING_RECORDING_PROCESSED.getId()));
    }

    @Test
    public void givenAGrantedFromRefusedDwpHearingRecording_thenRemoveFromRequestedListAndAddToReleasedListAndDwpStatusReleased() {
        SscsHearingRecording recording1 = SscsHearingRecording.builder().value(SscsHearingRecordingDetails.builder()
                .hearingId("an_id1").build()).build();
        HearingRecordingRequest request1 = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails
                .builder().sscsHearingRecordingList(Arrays.asList(recording1))
                .requestingParty(PartyItemList.DWP.getCode()).build()).build();
        sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearings(new ArrayList<>(Arrays.asList(request1)));
        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequests(Arrays
                .asList(getDwpProcessHearingRecordingRequests(RequestStatus.GRANTED, "an_id1")));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        SscsHearingRecordingCaseData sscsHearingRecordingCaseDataResponse = response.getData()
                .getSscsHearingRecordingCaseData();
        assertThat("Check RefusedHearings has been reduced",
                sscsHearingRecordingCaseDataResponse.getRefusedHearings(), is(empty()));
        assertThat("Check DwpReleasedHearings is populated",
                sscsHearingRecordingCaseDataResponse.getDwpReleasedHearings(), is(not(empty())));
        assertThat("Check DwpReleasedHearings has the correct Hearing",
                sscsHearingRecordingCaseDataResponse.getDwpReleasedHearings().get(0).getValue()
                        .getSscsHearingRecordingList().get(0).getValue().getHearingId(), is("an_id1"));
        assertThat("Check DwpState is PROCESSED", sscsCaseData.getDwpState(),
                is(DwpState.HEARING_RECORDING_PROCESSED.getId()));
    }

    @Test
    public void givenARequestedDwpHearingRecording_thenDoNotAddToReleasedOrRefused() {
        SscsHearingRecording recording1 = SscsHearingRecording.builder().value(SscsHearingRecordingDetails.builder()
                .hearingId("an_id1").build()).build();
        HearingRecordingRequest request1 = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails
                .builder().sscsHearingRecordingList(Arrays.asList(recording1))
                .requestingParty(PartyItemList.DWP.getCode()).build()).build();
        sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearings(new ArrayList<>(Arrays.asList(request1)));
        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequests(Arrays
                .asList(getDwpProcessHearingRecordingRequests(RequestStatus.REQUESTED, "an_id1")));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        SscsHearingRecordingCaseData sscsHearingRecordingCaseDataResponse = response.getData()
                .getSscsHearingRecordingCaseData();
        assertThat("Check RefusedHearings has been reduced",
                sscsHearingRecordingCaseDataResponse.getRefusedHearings(), anyOf(nullValue(), empty()));
        assertThat("Check DwpReleasedHearings is populated",
                sscsHearingRecordingCaseDataResponse.getDwpReleasedHearings(), anyOf(nullValue(), empty()));
        assertThat("Check RequestedHearings has the correct Hearing",
                sscsHearingRecordingCaseDataResponse.getRequestedHearings().get(0).getValue()
                        .getSscsHearingRecordingList().get(0).getValue().getHearingId(), is("an_id1"));
        assertThat("Check DwpState is not PROCESSED", sscsCaseData.getDwpState(),
                is(not(DwpState.HEARING_RECORDING_PROCESSED.getId())));
    }
}