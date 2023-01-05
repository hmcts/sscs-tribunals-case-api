package uk.gov.hmcts.reform.sscs.callback;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ACTION_HEARING_RECORDING_REQUEST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionhearingrecordingrequest.ActionHearingRecordingRequestAboutToStartHandlerTest.getHearingRecording;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionhearingrecordingrequest.ActionHearingRecordingRequestAboutToSubmitHandlerTest.getOtherPartyHearingRecordingReqUi;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionhearingrecordingrequest.ActionHearingRecordingRequestAboutToSubmitHandlerTest.getProcessHearingRecordingRequest;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionhearingrecordingrequest.ActionHearingRecordingRequestMidEventHandlerTest.*;
import static uk.gov.hmcts.reform.sscs.model.PartyItemList.OTHER_PARTY;
import static uk.gov.hmcts.reform.sscs.model.RequestStatus.GRANTED;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.domain.wrapper.pdf.PdfState;
import uk.gov.hmcts.reform.sscs.model.PartyItemList;
import uk.gov.hmcts.reform.sscs.service.FooterService;

@SpringBootTest
@AutoConfigureMockMvc
@RunWith(JUnitParamsRunner.class)
public class ActionHearingRecordingIt extends AbstractEventIt {

    @MockBean
    private FooterService footerService;

    private SscsCaseData sscsCaseData;

    @Before
    public void setup() throws IOException {
        openMocks(this);
        super.setup();

        Hearing hearing1 = Hearing.builder().value(
                HearingDetails.builder().hearingId(HEARING.getValue().getHearingId()).venue(Venue.builder().name("venue 1 name").build())
                        .hearingDate("2021-01-20")
                        .time("15:15").build()).build();


        SscsHearingRecording recording1 = SscsHearingRecording.builder().value(SscsHearingRecordingDetails.builder().hearingId("1").build()).build();
        DynamicListItem selectedHearing = new DynamicListItem("1", "Venue Name 12:00:00 01 Jan 2021");
        sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("1")
                .state(State.WITH_DWP)
                .interlocReviewState(InterlocReviewState.REVIEW_BY_TCW)
                .hearings(List.of(hearing1))
                .sscsHearingRecordingCaseData(SscsHearingRecordingCaseData.builder()
                        .selectHearingDetails(new DynamicList(selectedHearing, List.of(selectedHearing)))
                        .sscsHearingRecordings(List.of(recording1))
                        .build())
                .appeal(Appeal.builder()
                        .mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build())
                        .rep(Representative.builder()
                                .hasRepresentative(YES.getValue())
                                .organisation("org")
                                .build())
                        .build())
                .jointParty(JointParty.builder().hasJointParty(YES).build())
                .build();


        setJson(sscsCaseData, ACTION_HEARING_RECORDING_REQUEST);
        given(footerService.isReadablePdf(any())).willReturn(PdfState.OK);
    }



    @Test
    @Parameters({"REPRESENTATIVE", "APPELLANT", "DWP", "JOINT_PARTY"})
    public void midEventChangingRequestFromRefusedToGrantedReturnsWarning(PartyItemList party) throws Exception {

        setRefusedHearingsForParty(sscsCaseData, party, null);

        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequest(
                getProcessHearingRecordingRequestDetails(party, false)
        );
        setJson(sscsCaseData, ACTION_HEARING_RECORDING_REQUEST);

        PreSubmitCallbackResponse<SscsCaseData> response = assertResponseOkAndGetResult(MID_EVENT);

        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(1));
        assertThat(response.getWarnings().iterator().next(), is("Are you sure you want to change the request status"));
    }


    @Test
    public void midEventGivenCaseWithOtherPartiesAndExistingHearingRecordingRequestForSelectedHearing_thenBuildTheOtherPartyUi() throws Exception {

        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequest(ProcessHearingRecordingRequest.builder()
                .hearingId(HEARING.getValue().getHearingId())
                .build());

        List<HearingRecordingRequest> otherPartyHearingRecordingReq = new ArrayList<>();

        otherPartyHearingRecordingReq.add(HearingRecordingRequest.builder().value(HearingRecordingRequestDetails.builder()
                .otherPartyId("1")
                .status(GRANTED.getLabel())
                .requestingParty(OTHER_PARTY.getCode())
                .sscsHearingRecording(SscsHearingRecordingDetails.builder().hearingId("1").build()).build()).build());

        sscsCaseData.getSscsHearingRecordingCaseData().setCitizenReleasedHearings(otherPartyHearingRecordingReq);

        List<CcdValue<OtherParty>> otherParties = List.of(buildOtherPartyWithAppointeeAndRep("1", "2", "3"));
        sscsCaseData.setOtherParties(otherParties);

        setJson(sscsCaseData, ACTION_HEARING_RECORDING_REQUEST);

        PreSubmitCallbackResponse<SscsCaseData> response = assertResponseOkAndGetResult(MID_EVENT, "selectHearing");


        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(0));
        assertThat(response.getData().getSscsHearingRecordingCaseData().getOtherPartyHearingRecordingReqUi().size(), is(2));
        assertThat(response.getData().getSscsHearingRecordingCaseData().getOtherPartyHearingRecordingReqUi().get(0).getValue().getOtherPartyName(), is("Harry Kane"));
        assertThat(response.getData().getSscsHearingRecordingCaseData().getOtherPartyHearingRecordingReqUi().get(0).getValue().getHearingRecordingStatus().getValue().getCode(), is("Granted"));
        assertThat(response.getData().getSscsHearingRecordingCaseData().getOtherPartyHearingRecordingReqUi().get(0).getValue().getHearingRecordingStatus().getListItems().size(), is(2));
        assertThat(response.getData().getSscsHearingRecordingCaseData().getOtherPartyHearingRecordingReqUi().get(1).getValue().getOtherPartyName(), is("Wendy Smith - Representative"));
        assertThat(response.getData().getSscsHearingRecordingCaseData().getOtherPartyHearingRecordingReqUi().get(1).getValue().getHearingRecordingStatus().getValue().getCode(), is(""));
    }

    @Test
    public void aboutToStartGivenThereAreNoHearingRecordings_ReturnError() throws Exception {
        sscsCaseData.getSscsHearingRecordingCaseData().setSscsHearingRecordings(null);
        setJson(sscsCaseData, ACTION_HEARING_RECORDING_REQUEST);
        PreSubmitCallbackResponse<SscsCaseData> response = assertResponseOkAndGetResult(ABOUT_TO_START);
        assertEquals(1, response.getErrors().size());
        assertEquals("No hearing recordings on this case", response.getErrors().toArray()[0]);
    }

    @Test
    public void aboutToStartGivenThereAreMultipleHearingWithRecordings_thenAddToDynamicList() throws Exception {
        SscsHearingRecording recording1 = getHearingRecording("1");
        SscsHearingRecording recording2 = getHearingRecording("2");
        sscsCaseData.getSscsHearingRecordingCaseData().setSscsHearingRecordings(Arrays.asList(recording1, recording2));

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
                        .hearingDate("2021-01-01").time("12:00").hearingId("1").venue(Venue.builder().name("Venue Name").build()).build()).build(),
                Hearing.builder().value(HearingDetails.builder()
                        .hearingDate("2021-02-01").time("13:00").hearingId("2").venue(Venue.builder().name("Venue Name2").build()).build()).build()));

        setJson(sscsCaseData, ACTION_HEARING_RECORDING_REQUEST);
        PreSubmitCallbackResponse<SscsCaseData> response = assertResponseOkAndGetResult(ABOUT_TO_START);

        assertEquals(0, response.getErrors().size());
        assertEquals(2, response.getData().getSscsHearingRecordingCaseData().getSelectHearingDetails().getListItems().size());
        assertEquals("Venue Name2 13:00:00 01 Feb 2021", response.getData().getSscsHearingRecordingCaseData().getSelectHearingDetails().getListItems().get(0).getLabel());
        assertEquals("2", response.getData().getSscsHearingRecordingCaseData().getSelectHearingDetails().getListItems().get(0).getCode());
        assertEquals("Venue Name 12:00:00 01 Jan 2021", response.getData().getSscsHearingRecordingCaseData().getSelectHearingDetails().getListItems().get(1).getLabel());
        assertEquals("1", response.getData().getSscsHearingRecordingCaseData().getSelectHearingDetails().getListItems().get(1).getCode());
    }


    @Test
    @Parameters({"appellant", "representative", "jointParty"})
    public void aboutToSubmitGivenAGrantedFromRequestedCitizenHearingRecording_thenRemoveFromRequestedListAndAddToReleasedList(String party) throws Exception {
        SscsHearingRecordingDetails recording1 = SscsHearingRecordingDetails.builder().hearingId("1").build();
        HearingRecordingRequest request1 = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails
                .builder().sscsHearingRecording(recording1)
                .requestingParty(party).build()).build();
        sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearings(List.of(request1));
        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequest(
                getProcessHearingRecordingRequest(RequestStatus.GRANTED, "1", party));

        setJson(sscsCaseData, ACTION_HEARING_RECORDING_REQUEST);
        PreSubmitCallbackResponse<SscsCaseData> response = assertResponseOkAndGetResult(ABOUT_TO_SUBMIT);

        SscsHearingRecordingCaseData sscsHearingRecordingCaseDataResponse = response.getData()
                .getSscsHearingRecordingCaseData();
        assertThat("Check RequestedHearings has been reduced",
                sscsHearingRecordingCaseDataResponse.getRequestedHearings(), is(empty()));
        assertThat("Check DwpReleasedHearings is populated",
                sscsHearingRecordingCaseDataResponse.getCitizenReleasedHearings(), is(not(empty())));
        assertThat("Check DwpReleasedHearings has the correct Hearing",
                sscsHearingRecordingCaseDataResponse.getCitizenReleasedHearings().get(0).getValue()
                        .getSscsHearingRecording().getHearingId(), is("1"));
        assertThat("Check DwpReleasedHearings has the correct approved date",
                sscsHearingRecordingCaseDataResponse.getCitizenReleasedHearings().get(0).getValue()
                        .getDateApproved(), is(LocalDate.now().toString()));
    }

    @Test
    @Parameters({"otherParty", "otherPartyRep"})
    public void aboutToSubmitGivenAGrantedFromRequestedCitizenHearingRecordingForOtherPartyRequest_thenRemoveFromRequestedListAndAddToReleasedList(String party) throws Exception {
        SscsHearingRecordingDetails recording1 = SscsHearingRecordingDetails.builder().hearingId("1").build();
        HearingRecordingRequest request1 = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails
                .builder().sscsHearingRecording(recording1)
                .requestingParty(party)
                .otherPartyId("op_id1").build()).build();
        sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearings(List.of(request1));
        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequest(
                getProcessHearingRecordingRequest(RequestStatus.GRANTED, "1", party));
        sscsCaseData.getSscsHearingRecordingCaseData().setOtherPartyHearingRecordingReqUi(
                getOtherPartyHearingRecordingReqUi(RequestStatus.GRANTED, "op_id1", party));

        setJson(sscsCaseData, ACTION_HEARING_RECORDING_REQUEST);
        PreSubmitCallbackResponse<SscsCaseData> response = assertResponseOkAndGetResult(ABOUT_TO_SUBMIT);

        SscsHearingRecordingCaseData sscsHearingRecordingCaseDataResponse = response.getData()
                .getSscsHearingRecordingCaseData();
        assertThat("Check RequestedHearings has been reduced",
                sscsHearingRecordingCaseDataResponse.getRequestedHearings(), is(empty()));
        assertThat("Check DwpReleasedHearings is populated",
                sscsHearingRecordingCaseDataResponse.getCitizenReleasedHearings(), is(not(empty())));
        assertThat("Check DwpReleasedHearings has the correct Hearing",
                sscsHearingRecordingCaseDataResponse.getCitizenReleasedHearings().get(0).getValue()
                        .getSscsHearingRecording().getHearingId(), is("1"));
        assertThat("Check DwpReleasedHearings has the correct approved date",
                sscsHearingRecordingCaseDataResponse.getCitizenReleasedHearings().get(0).getValue()
                        .getDateApproved(), is(LocalDate.now().toString()));
        assertThat("Check DwpReleasedHearings has the correct other party id",
                sscsHearingRecordingCaseDataResponse.getCitizenReleasedHearings().get(0).getValue()
                        .getOtherPartyId(), is("op_id1"));
    }


    @Test
    public void aboutToSubmitGivenAGrantedFromRequestedDwpHearingRecording_thenRemoveFromRequestedListAndAddToReleasedListAndDwpStatusReleased() throws Exception {
        SscsHearingRecordingDetails recording1 = SscsHearingRecordingDetails.builder().hearingId("1").build();
        HearingRecordingRequest request1 = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails
                .builder().sscsHearingRecording(recording1)
                .requestingParty(PartyItemList.DWP.getCode()).build()).build();
        sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearings(List.of(request1));
        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequest(
                getProcessHearingRecordingRequest(RequestStatus.GRANTED, HEARING.getValue().getHearingId(),
                PartyItemList.DWP.getCode()));

        setJson(sscsCaseData, ACTION_HEARING_RECORDING_REQUEST);
        PreSubmitCallbackResponse<SscsCaseData> response = assertResponseOkAndGetResult(ABOUT_TO_SUBMIT);

        SscsHearingRecordingCaseData sscsHearingRecordingCaseDataResponse = response.getData()
                .getSscsHearingRecordingCaseData();
        assertThat("Check RequestedHearings has been reduced",
                sscsHearingRecordingCaseDataResponse.getRequestedHearings(), is(empty()));
        assertThat("Check DwpReleasedHearings is populated",
                sscsHearingRecordingCaseDataResponse.getDwpReleasedHearings(), is(not(empty())));
        assertThat("Check DwpReleasedHearings has the correct Hearing",
                sscsHearingRecordingCaseDataResponse.getDwpReleasedHearings().get(0).getValue()
                        .getSscsHearingRecording().getHearingId(), is("1"));
        assertThat("Check DwpReleasedHearings has the correct approved date",
                sscsHearingRecordingCaseDataResponse.getDwpReleasedHearings().get(0).getValue()
                        .getDateApproved(), is(LocalDate.now().toString()));
        assertThat("Check DwpState is PROCESSED", response.getData().getDwpState(),
                is(DwpState.HEARING_RECORDING_PROCESSED));
    }

}
