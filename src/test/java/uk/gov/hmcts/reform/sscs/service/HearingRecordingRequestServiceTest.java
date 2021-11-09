package uk.gov.hmcts.reform.sscs.service;

import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.Arrays;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.model.PartyItemList;

@RunWith(JUnitParamsRunner.class)
public class HearingRecordingRequestServiceTest {

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    private HearingRecordingRequestService hearingRecordingRequestService;

    @Before
    public void setUp() {
        openMocks(this);
        hearingRecordingRequestService = new HearingRecordingRequestService();

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

        SscsHearingRecording recording1 = SscsHearingRecording.builder().value(SscsHearingRecordingDetails.builder().hearingId("an_id1").build()).build();
        SscsHearingRecording recording2 = SscsHearingRecording.builder().value(SscsHearingRecordingDetails.builder().hearingId("an_id2").build()).build();
        SscsHearingRecording recording3 = SscsHearingRecording.builder().value(SscsHearingRecordingDetails.builder().hearingId("an_id3").build()).build();

        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").hearings(Arrays.asList(hearing1, hearing2, hearing3)).build();
        sscsCaseData.getSscsHearingRecordingCaseData().setSscsHearingRecordings(Arrays.asList(recording1, recording2, recording3));

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    @Parameters({"DWP", "APPELLANT", "REPRESENTATIVE", "JOINT_PARTY"})
    public void givenAHearingWithRecordingAndASelectedPartyItem_thenHearingInRequestableListAndCorrectMessageDisplayed(PartyItemList partyItemList) {
        sscsCaseData.setHearings(singletonList(Hearing.builder().value(
                HearingDetails.builder().hearingId("an_id1").venue(Venue.builder().name("venue name").build())
                        .hearingDate("2021-03-20")
                        .time("15:15").build()).build()));

        PreSubmitCallbackResponse<SscsCaseData> response = hearingRecordingRequestService.buildHearingRecordingUi(new PreSubmitCallbackResponse<>(sscsCaseData), partyItemList);
        assertEquals(1, response.getData().getSscsHearingRecordingCaseData().getRequestableHearingDetails().getListItems().size());
        assertEquals("There are no outstanding " + partyItemList.getLabel() + " hearing recording requests on this case", response.getData().getSscsHearingRecordingCaseData().getRequestedHearingsTextList());
        assertEquals("No hearing recordings have been released to " + partyItemList.getLabel() + " on this case", response.getData().getSscsHearingRecordingCaseData().getReleasedHearingsTextList());
    }

    @Test
    @Parameters({"DWP", "APPELLANT", "REPRESENTATIVE", "JOINT_PARTY"})
    public void givenThreeHearingsWithRecordingAndASelectedPartyItem_thenThreeHearingInRequestableList(PartyItemList partyItemList) {
        PreSubmitCallbackResponse<SscsCaseData> response = hearingRecordingRequestService.buildHearingRecordingUi(new PreSubmitCallbackResponse<>(sscsCaseData), partyItemList);
        assertEquals(3, response.getData().getSscsHearingRecordingCaseData().getRequestableHearingDetails().getListItems().size());
    }

    @Test
    @Parameters({"DWP", "APPELLANT", "REPRESENTATIVE", "JOINT_PARTY"})
    public void givenAHearingsRequestedAndASelectedPartyItem_thenHearingInRequestedList(PartyItemList partyItemList) {
        SscsHearingRecording sscsHearingRecording = SscsHearingRecording.builder().value(SscsHearingRecordingDetails.builder().hearingId("an_id2").build()).build();
        HearingRecordingRequest recordingRequest = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails.builder()
                .requestingParty(partyItemList.getCode()).sscsHearingRecording(sscsHearingRecording.getValue()).build()).build();
        sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearings(singletonList(recordingRequest));
        PreSubmitCallbackResponse<SscsCaseData> response = hearingRecordingRequestService.buildHearingRecordingUi(new PreSubmitCallbackResponse<>(sscsCaseData), partyItemList);

        assertEquals(2, response.getData().getSscsHearingRecordingCaseData().getRequestableHearingDetails().getListItems().size());
        assertEquals(1, response.getData().getSscsHearingRecordingCaseData().getRequestedHearings().size());
    }

    @Test
    @Parameters({"DWP", "APPELLANT", "REPRESENTATIVE", "JOINT_PARTY"})
    public void givenAHearingsReleasedAndASelectedPartyItem_thenHearingInReleasedList(PartyItemList partyItemList) {
        SscsHearingRecording sscsHearingRecording = SscsHearingRecording.builder().value(SscsHearingRecordingDetails.builder().hearingId("an_id2").build()).build();
        HearingRecordingRequest recordingRequest = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails.builder()
                .requestingParty(partyItemList.getCode()).sscsHearingRecording(sscsHearingRecording.getValue()).build()).build();

        if (PartyItemList.DWP.equals(partyItemList)) {
            sscsCaseData.getSscsHearingRecordingCaseData().setDwpReleasedHearings(singletonList(recordingRequest));
        } else {
            sscsCaseData.getSscsHearingRecordingCaseData().setCitizenReleasedHearings(singletonList(recordingRequest));
        }

        PreSubmitCallbackResponse<SscsCaseData> response = hearingRecordingRequestService.buildHearingRecordingUi(new PreSubmitCallbackResponse<>(sscsCaseData), partyItemList);

        assertEquals(2, response.getData().getSscsHearingRecordingCaseData().getRequestableHearingDetails().getListItems().size());

        if (PartyItemList.DWP.equals(partyItemList)) {
            assertEquals(1, response.getData().getSscsHearingRecordingCaseData().getDwpReleasedHearings().size());
            assertNull(response.getData().getSscsHearingRecordingCaseData().getCitizenReleasedHearings());
        } else {
            assertEquals(1, response.getData().getSscsHearingRecordingCaseData().getCitizenReleasedHearings().size());
            assertNull(response.getData().getSscsHearingRecordingCaseData().getDwpReleasedHearings());
        }
    }

    @Test
    public void givenACaseWithNoHearings_thenAnErrorIsReturned() {
        sscsCaseData.setHearings(null);
        PreSubmitCallbackResponse<SscsCaseData> response = hearingRecordingRequestService.buildHearingRecordingUi(new PreSubmitCallbackResponse<>(sscsCaseData), PartyItemList.APPELLANT);

        if (response.getErrors().stream().findAny().isPresent()) {
            assertEquals("There are no hearings on this case", response.getErrors().stream().findAny().get());
        }
    }

    @Test
    public void givenACaseWithNoHearingRecordingsOnTheCase_thenAnErrorIsReturned() {

        PreSubmitCallbackResponse<SscsCaseData> response = hearingRecordingRequestService.buildHearingRecordingUi(new PreSubmitCallbackResponse<>(sscsCaseData), PartyItemList.APPELLANT);

        if (response.getErrors().stream().findAny().isPresent()) {
            assertEquals("There are no hearings with hearing recordings on this case", response.getErrors().stream().findAny().get());
        }
    }

}