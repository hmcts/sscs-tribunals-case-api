package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionhearingrecordingrequest;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
public class ActionHearingRecordingRequestAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    ActionHearingRecordingRequestAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new ActionHearingRecordingRequestAboutToSubmitHandler();

        when(callback.getEvent()).thenReturn(EventType.ACTION_HEARING_RECORDING_REQUEST);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
                .sscsHearingRecordingCaseData(SscsHearingRecordingCaseData.builder().build()).build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    public static ProcessHearingRecordingRequest getProcessHearingRecordingRequest(RequestStatus status, String hearingId, String party) {

        DynamicListItem dwpItem = party.equals(PartyItemList.DWP.getCode()) ? new DynamicListItem(status.getValue(), status.getValue()) : null;
        DynamicListItem jointItem = party.equals(PartyItemList.JOINT_PARTY.getCode()) ? new DynamicListItem(status.getValue(), status.getValue()) : null;
        DynamicListItem appellantItem = party.equals(PartyItemList.APPELLANT.getCode()) ? new DynamicListItem(status.getValue(), status.getValue()) : null;
        DynamicListItem repItem = party.equals(PartyItemList.REPRESENTATIVE.getCode()) ? new DynamicListItem(status.getValue(), status.getValue()) : null;

        return ProcessHearingRecordingRequest.builder()
                .dwp(new DynamicList(dwpItem, Collections.emptyList()))
                .appellant(new DynamicList(appellantItem, Collections.emptyList()))
                .jointParty(new DynamicList(jointItem, Collections.emptyList()))
                .rep(new DynamicList(repItem, Collections.emptyList()))
                .hearingId(hearingId).build();
    }

    public static List<OtherPartyHearingRecordingReqUi> getOtherPartyHearingRecordingReqUi(RequestStatus status, String otherPartyId, String party) {
        return List.of(OtherPartyHearingRecordingReqUi.builder()
            .value(OtherPartyHearingRecordingReqUiDetails.builder()
                .otherPartyId(otherPartyId)
                .hearingRecordingStatus(new DynamicList(new DynamicListItem(status.getValue(), status.getValue()), Collections.emptyList()))
                .requestingParty(party)
                .build())
            .build());
    }

    @Test
    public void givenAGrantedFromRequestedDwpHearingRecording_thenRemoveFromRequestedListAndAddToReleasedListAndDwpStatusReleased() {
        SscsHearingRecordingDetails recording1 = SscsHearingRecordingDetails.builder().hearingId("an_id1").build();
        HearingRecordingRequest request1 = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails
                .builder().sscsHearingRecording(recording1)
                .requestingParty(PartyItemList.DWP.getCode()).build()).build();
        sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearings(new ArrayList<>(Arrays.asList(request1)));
        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequest(getProcessHearingRecordingRequest(RequestStatus.GRANTED, "an_id1",
                        PartyItemList.DWP.getCode()));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        SscsHearingRecordingCaseData sscsHearingRecordingCaseDataResponse = response.getData()
                .getSscsHearingRecordingCaseData();
        assertThat("Check RequestedHearings has been reduced",
                sscsHearingRecordingCaseDataResponse.getRequestedHearings(), is(empty()));
        assertThat("Check DwpReleasedHearings is populated",
                sscsHearingRecordingCaseDataResponse.getDwpReleasedHearings(), is(not(empty())));
        assertThat("Check DwpReleasedHearings has the correct Hearing",
                sscsHearingRecordingCaseDataResponse.getDwpReleasedHearings().get(0).getValue()
                        .getSscsHearingRecording().getHearingId(), is("an_id1"));
        assertThat("Check DwpReleasedHearings has the correct approved date",
                sscsHearingRecordingCaseDataResponse.getDwpReleasedHearings().get(0).getValue()
                        .getDateApproved(), is(LocalDate.now().toString()));
        assertThat("Check DwpState is PROCESSED", response.getData().getDwpState(),
                is(DwpState.HEARING_RECORDING_PROCESSED));
    }

    @Test
    public void givenARefusedFromRequestedDwpHearingRecording_thenRemoveFromRequestedListAndDoNotAddToReleasedListAndDwpStatusReleased() {
        SscsHearingRecordingDetails recording1 = SscsHearingRecordingDetails.builder().hearingId("an_id1").build();
        HearingRecordingRequest request1 = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails
                .builder().sscsHearingRecording(recording1)
                .requestingParty(PartyItemList.DWP.getCode()).build()).build();
        sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearings(new ArrayList<>(Arrays.asList(request1)));
        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequest(
                getProcessHearingRecordingRequest(RequestStatus.REFUSED, "an_id1",
                        PartyItemList.DWP.getCode()));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        SscsHearingRecordingCaseData sscsHearingRecordingCaseDataResponse = response.getData()
                .getSscsHearingRecordingCaseData();
        assertThat("Check RequestedHearings has been reduced",
                sscsHearingRecordingCaseDataResponse.getRequestedHearings(), is(empty()));
        assertThat("Check ReleasedHearings has not been populated",
                sscsHearingRecordingCaseDataResponse.getDwpReleasedHearings(), is(empty()));
        assertThat("Check RefusedHearings was populated",
                sscsHearingRecordingCaseDataResponse.getRefusedHearings(), is(not(empty())));
        assertThat("Check RefusedHearings has no approved date",
                sscsHearingRecordingCaseDataResponse.getRefusedHearings().get(0).getValue()
                        .getDateApproved(), is(nullValue()));
        assertThat("Check DwpState is PROCESSED", sscsCaseData.getDwpState(),
                is(DwpState.HEARING_RECORDING_PROCESSED));
    }

    @Test
    public void givenARefusedFromGrantedDwpHearingRecording_thenRemoveFromReleasedListAndDwpStatusRefused() {
        SscsHearingRecordingDetails recording1 = SscsHearingRecordingDetails.builder().hearingId("an_id1").build();
        HearingRecordingRequest request1 = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails
                .builder().dateApproved(LocalDate.now().toString()).sscsHearingRecording(recording1)
                .requestingParty(PartyItemList.DWP.getCode()).build()).build();
        sscsCaseData.getSscsHearingRecordingCaseData().setDwpReleasedHearings(new ArrayList<>(Arrays.asList(request1)));
        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequest(
                getProcessHearingRecordingRequest(RequestStatus.REFUSED, "an_id1",
                        PartyItemList.DWP.getCode()));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        SscsHearingRecordingCaseData sscsHearingRecordingCaseDataResponse = response.getData()
                .getSscsHearingRecordingCaseData();
        assertThat("Check RequestedHearings has been reduced",
                sscsHearingRecordingCaseDataResponse.getDwpReleasedHearings(), is(empty()));
        assertThat("Check DwpRefusedHearings is not populated",
                sscsHearingRecordingCaseDataResponse.getRefusedHearings(), is(not(empty())));
        assertThat("Check RefusedHearings has the correct Hearing",
                sscsHearingRecordingCaseDataResponse.getRefusedHearings().get(0).getValue()
                        .getSscsHearingRecording().getHearingId(), is("an_id1"));
        assertThat("Check RefusedHearings has no approved date",
                sscsHearingRecordingCaseDataResponse.getRefusedHearings().get(0).getValue()
                        .getDateApproved(), is(nullValue()));
        assertThat("Check DwpState is PROCESSED", sscsCaseData.getDwpState(),
                is(DwpState.HEARING_RECORDING_PROCESSED));
    }

    @Test
    public void givenAGrantedFromRefusedDwpHearingRecording_thenRemoveFromRequestedListAndAddToReleasedListAndDwpStatusReleased() {
        SscsHearingRecordingDetails recording1 = SscsHearingRecordingDetails.builder().hearingId("an_id1").build();
        HearingRecordingRequest request1 = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails
                .builder().sscsHearingRecording(recording1)
                .requestingParty(PartyItemList.DWP.getCode()).build()).build();
        sscsCaseData.getSscsHearingRecordingCaseData().setRefusedHearings(new ArrayList<>(Arrays.asList(request1)));
        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequest(
                getProcessHearingRecordingRequest(RequestStatus.GRANTED, "an_id1",
                        PartyItemList.DWP.getCode()));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        SscsHearingRecordingCaseData sscsHearingRecordingCaseDataResponse = response.getData()
                .getSscsHearingRecordingCaseData();
        assertThat("Check RefusedHearings has been reduced",
                sscsHearingRecordingCaseDataResponse.getRefusedHearings(), is(empty()));
        assertThat("Check DwpReleasedHearings is populated",
                sscsHearingRecordingCaseDataResponse.getDwpReleasedHearings(), is(not(empty())));
        assertThat("Check DwpReleasedHearings has the correct Hearing",
                sscsHearingRecordingCaseDataResponse.getDwpReleasedHearings().get(0).getValue()
                        .getSscsHearingRecording().getHearingId(), is("an_id1"));
        assertThat("Check DwpState is PROCESSED", sscsCaseData.getDwpState(),
                is(DwpState.HEARING_RECORDING_PROCESSED));
    }

    @Test
    public void givenARequestedDwpHearingRecording_thenDoNotAddToReleasedOrRefused() {
        SscsHearingRecordingDetails recording1 = SscsHearingRecordingDetails.builder().hearingId("an_id1").build();
        HearingRecordingRequest request1 = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails
                .builder().sscsHearingRecording(recording1)
                .requestingParty(PartyItemList.DWP.getCode()).build()).build();
        sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearings(new ArrayList<>(Arrays.asList(request1)));
        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequest(
                getProcessHearingRecordingRequest(RequestStatus.REQUESTED, "an_id1",
                        PartyItemList.DWP.getCode()));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        SscsHearingRecordingCaseData sscsHearingRecordingCaseDataResponse = response.getData()
                .getSscsHearingRecordingCaseData();
        assertThat("Check RefusedHearings has been reduced",
                sscsHearingRecordingCaseDataResponse.getRefusedHearings(), anyOf(nullValue(), empty()));
        assertThat("Check DwpReleasedHearings is populated",
                sscsHearingRecordingCaseDataResponse.getDwpReleasedHearings(), anyOf(nullValue(), empty()));
        assertThat("Check RequestedHearings has the correct Hearing",
                sscsHearingRecordingCaseDataResponse.getRequestedHearings().get(0).getValue()
                        .getSscsHearingRecording().getHearingId(), is("an_id1"));
        assertThat("Check DwpState is not PROCESSED", sscsCaseData.getDwpState(),
                is(not(DwpState.HEARING_RECORDING_PROCESSED)));
    }

    @Test
    @Parameters({"appellant", "representative", "jointParty"})
    public void givenAGrantedFromVoidCitizenHearingRecording_thenAddToReleasedList(String party) {
        SscsHearingRecording recording1 = getHearingRecording();

        sscsCaseData.getSscsHearingRecordingCaseData().setSscsHearingRecordings(Arrays.asList(recording1));
        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequest(
                getProcessHearingRecordingRequest(RequestStatus.GRANTED, "an_id1", party));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        SscsHearingRecordingCaseData sscsHearingRecordingCaseDataResponse = response.getData()
                .getSscsHearingRecordingCaseData();
        assertThat("Check RequestedHearings is empty",
                sscsHearingRecordingCaseDataResponse.getRequestedHearings(), is(empty()));
        assertThat("Check DwpReleasedHearings is populated",
                sscsHearingRecordingCaseDataResponse.getCitizenReleasedHearings(), is(not(empty())));
        assertThat("Check DwpReleasedHearings has the correct Hearing",
                sscsHearingRecordingCaseDataResponse.getCitizenReleasedHearings().get(0).getValue()
                        .getSscsHearingRecording().getHearingId(), is("an_id1"));
        assertThat("Check DwpReleasedHearings has the correct approved date",
                sscsHearingRecordingCaseDataResponse.getCitizenReleasedHearings().get(0).getValue()
                        .getDateApproved(), is(LocalDate.now().toString()));
    }

    @Test
    @Parameters({"otherParty", "otherPartyRep"})
    public void givenAGrantedFromVoidCitizenHearingRecordingAndOtherPartyRequest_thenAddToReleasedList(String party) {
        SscsHearingRecording recording1 = getHearingRecording();

        sscsCaseData.getSscsHearingRecordingCaseData().setSscsHearingRecordings(Arrays.asList(recording1));
        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequest(
                getProcessHearingRecordingRequest(RequestStatus.GRANTED, "an_id1", party));
        sscsCaseData.getSscsHearingRecordingCaseData().setOtherPartyHearingRecordingReqUi(
                getOtherPartyHearingRecordingReqUi(RequestStatus.GRANTED, "op_id1", party));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        SscsHearingRecordingCaseData sscsHearingRecordingCaseDataResponse = response.getData()
                .getSscsHearingRecordingCaseData();
        assertThat("Check RequestedHearings is empty",
                sscsHearingRecordingCaseDataResponse.getRequestedHearings(), is(empty()));
        assertThat("Check DwpReleasedHearings is populated",
                sscsHearingRecordingCaseDataResponse.getCitizenReleasedHearings(), is(not(empty())));
        assertThat("Check DwpReleasedHearings has the correct Hearing",
                sscsHearingRecordingCaseDataResponse.getCitizenReleasedHearings().get(0).getValue()
                        .getSscsHearingRecording().getHearingId(), is("an_id1"));
        assertThat("Check DwpReleasedHearings has the correct approved date",
                sscsHearingRecordingCaseDataResponse.getCitizenReleasedHearings().get(0).getValue()
                        .getDateApproved(), is(LocalDate.now().toString()));
        assertThat("Check DwpReleasedHearings has the correct other party id",
                sscsHearingRecordingCaseDataResponse.getCitizenReleasedHearings().get(0).getValue()
                        .getOtherPartyId(), is("op_id1"));
    }

    @Test
    @Parameters({"appellant", "representative", "jointParty"})
    public void givenAGrantedFromRequestedCitizenHearingRecording_thenRemoveFromRequestedListAndAddToReleasedList(String party) {
        SscsHearingRecordingDetails recording1 = SscsHearingRecordingDetails.builder().hearingId("an_id1").build();
        HearingRecordingRequest request1 = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails
                .builder().sscsHearingRecording(recording1)
                .requestingParty(party).build()).build();
        sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearings(new ArrayList<>(Arrays.asList(request1)));
        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequest(
                getProcessHearingRecordingRequest(RequestStatus.GRANTED, "an_id1", party));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        SscsHearingRecordingCaseData sscsHearingRecordingCaseDataResponse = response.getData()
                .getSscsHearingRecordingCaseData();
        assertThat("Check RequestedHearings has been reduced",
                sscsHearingRecordingCaseDataResponse.getRequestedHearings(), is(empty()));
        assertThat("Check DwpReleasedHearings is populated",
                sscsHearingRecordingCaseDataResponse.getCitizenReleasedHearings(), is(not(empty())));
        assertThat("Check DwpReleasedHearings has the correct Hearing",
                sscsHearingRecordingCaseDataResponse.getCitizenReleasedHearings().get(0).getValue()
                        .getSscsHearingRecording().getHearingId(), is("an_id1"));
        assertThat("Check DwpReleasedHearings has the correct approved date",
                sscsHearingRecordingCaseDataResponse.getCitizenReleasedHearings().get(0).getValue()
                        .getDateApproved(), is(LocalDate.now().toString()));
    }

    @Test
    @Parameters({"otherParty", "otherPartyRep"})
    public void givenAGrantedFromRequestedCitizenHearingRecordingForOtherPartyRequest_thenRemoveFromRequestedListAndAddToReleasedList(String party) {
        SscsHearingRecordingDetails recording1 = SscsHearingRecordingDetails.builder().hearingId("an_id1").build();
        HearingRecordingRequest request1 = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails
                .builder().sscsHearingRecording(recording1)
                .requestingParty(party)
                .otherPartyId("op_id1").build()).build();
        sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearings(new ArrayList<>(Arrays.asList(request1)));
        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequest(
                getProcessHearingRecordingRequest(RequestStatus.GRANTED, "an_id1", party));
        sscsCaseData.getSscsHearingRecordingCaseData().setOtherPartyHearingRecordingReqUi(
                getOtherPartyHearingRecordingReqUi(RequestStatus.GRANTED, "op_id1", party));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        SscsHearingRecordingCaseData sscsHearingRecordingCaseDataResponse = response.getData()
                .getSscsHearingRecordingCaseData();
        assertThat("Check RequestedHearings has been reduced",
                sscsHearingRecordingCaseDataResponse.getRequestedHearings(), is(empty()));
        assertThat("Check DwpReleasedHearings is populated",
                sscsHearingRecordingCaseDataResponse.getCitizenReleasedHearings(), is(not(empty())));
        assertThat("Check DwpReleasedHearings has the correct Hearing",
                sscsHearingRecordingCaseDataResponse.getCitizenReleasedHearings().get(0).getValue()
                        .getSscsHearingRecording().getHearingId(), is("an_id1"));
        assertThat("Check DwpReleasedHearings has the correct approved date",
                sscsHearingRecordingCaseDataResponse.getCitizenReleasedHearings().get(0).getValue()
                        .getDateApproved(), is(LocalDate.now().toString()));
        assertThat("Check DwpReleasedHearings has the correct other party id",
                sscsHearingRecordingCaseDataResponse.getCitizenReleasedHearings().get(0).getValue()
                        .getOtherPartyId(), is("op_id1"));
    }

    @Test
    @Parameters({"appellant", "representative", "jointParty"})
    public void givenARefusedFromRequestedCitizenHearingRecording_thenRemoveFromRequestedListAndDoNotAddToReleasedList(String party) {
        SscsHearingRecordingDetails recording1 = SscsHearingRecordingDetails.builder().hearingId("an_id1").build();
        HearingRecordingRequest request1 = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails
                .builder().sscsHearingRecording(recording1)
                .requestingParty(party).build()).build();
        sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearings(new ArrayList<>(Arrays.asList(request1)));
        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequest(
                getProcessHearingRecordingRequest(RequestStatus.REFUSED, "an_id1", party));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        SscsHearingRecordingCaseData sscsHearingRecordingCaseDataResponse = response.getData()
                .getSscsHearingRecordingCaseData();
        assertThat("Check RequestedHearings has been reduced",
                sscsHearingRecordingCaseDataResponse.getRequestedHearings(), is(empty()));
        assertThat("Check ReleasedHearings has not been populated",
                sscsHearingRecordingCaseDataResponse.getCitizenReleasedHearings(), is(empty()));
        assertThat("Check RefusedHearings was populated",
                sscsHearingRecordingCaseDataResponse.getRefusedHearings(), is(not(empty())));
        assertThat("Check RefusedHearings has no approved date",
                sscsHearingRecordingCaseDataResponse.getRefusedHearings().get(0).getValue()
                        .getDateApproved(), is(nullValue()));
    }

    @Test
    @Parameters({"otherParty", "otherPartyRep"})
    public void givenARefusedFromRequestedCitizenHearingRecordingForOtherPartyRequest_thenRemoveFromRequestedListAndDoNotAddToReleasedList(String party) {
        SscsHearingRecordingDetails recording1 = SscsHearingRecordingDetails.builder().hearingId("an_id1").build();
        HearingRecordingRequest request1 = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails
                .builder().sscsHearingRecording(recording1)
                .requestingParty(party)
                .otherPartyId("op_id1").build()).build();
        sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearings(new ArrayList<>(Arrays.asList(request1)));
        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequest(
                getProcessHearingRecordingRequest(RequestStatus.REFUSED, "an_id1", party));
        sscsCaseData.getSscsHearingRecordingCaseData().setOtherPartyHearingRecordingReqUi(
                getOtherPartyHearingRecordingReqUi(RequestStatus.REFUSED, "op_id1", party));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        SscsHearingRecordingCaseData sscsHearingRecordingCaseDataResponse = response.getData()
                .getSscsHearingRecordingCaseData();
        assertThat("Check RequestedHearings has been reduced",
                sscsHearingRecordingCaseDataResponse.getRequestedHearings(), is(empty()));
        assertThat("Check ReleasedHearings has not been populated",
                sscsHearingRecordingCaseDataResponse.getCitizenReleasedHearings(), is(empty()));
        assertThat("Check RefusedHearings was populated",
                sscsHearingRecordingCaseDataResponse.getRefusedHearings(), is(not(empty())));
        assertThat("Check RefusedHearings has no approved date",
                sscsHearingRecordingCaseDataResponse.getRefusedHearings().get(0).getValue()
                        .getDateApproved(), is(nullValue()));
        assertThat("Check RefusedHearings has the correct other party id",
                sscsHearingRecordingCaseDataResponse.getRefusedHearings().get(0).getValue()
                        .getOtherPartyId(), is("op_id1"));
    }

    @Test
    @Parameters({"appellant", "representative", "jointParty"})
    public void givenARefusedFromGrantedCitizenHearingRecording_thenRemoveFromReleasedList(String party) {
        SscsHearingRecordingDetails recording1 = SscsHearingRecordingDetails.builder().hearingId("an_id1").build();
        HearingRecordingRequest request1 = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails
                .builder().dateApproved(LocalDate.now().toString()).sscsHearingRecording(recording1)
                .requestingParty(party).build()).build();
        sscsCaseData.getSscsHearingRecordingCaseData().setCitizenReleasedHearings(new ArrayList<>(Arrays.asList(request1)));
        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequest(
                getProcessHearingRecordingRequest(RequestStatus.REFUSED, "an_id1", party));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        SscsHearingRecordingCaseData sscsHearingRecordingCaseDataResponse = response.getData()
                .getSscsHearingRecordingCaseData();
        assertThat("Check RequestedHearings has been reduced",
                sscsHearingRecordingCaseDataResponse.getCitizenReleasedHearings(), is(empty()));
        assertThat("Check DwpRefusedHearings is not populated",
                sscsHearingRecordingCaseDataResponse.getRefusedHearings(), is(not(empty())));
        assertThat("Check RefusedHearings has the correct Hearing",
                sscsHearingRecordingCaseDataResponse.getRefusedHearings().get(0).getValue()
                        .getSscsHearingRecording().getHearingId(), is("an_id1"));
        assertThat("Check RefusedHearings has no approved date",
                sscsHearingRecordingCaseDataResponse.getRefusedHearings().get(0).getValue()
                        .getDateApproved(), is(nullValue()));
    }

    @Test
    @Parameters({"otherParty", "otherPartyRep"})
    public void givenARefusedFromGrantedCitizenHearingRecordingForOtherPartyRequest_thenRemoveFromReleasedList(String party) {
        SscsHearingRecordingDetails recording1 = SscsHearingRecordingDetails.builder().hearingId("an_id1").build();
        HearingRecordingRequest request1 = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails
                .builder().dateApproved(LocalDate.now().toString()).sscsHearingRecording(recording1)
                .requestingParty(party)
                .otherPartyId("op_id1").build()).build();
        sscsCaseData.getSscsHearingRecordingCaseData().setCitizenReleasedHearings(new ArrayList<>(Arrays.asList(request1)));
        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequest(
                getProcessHearingRecordingRequest(RequestStatus.REFUSED, "an_id1", party));
        sscsCaseData.getSscsHearingRecordingCaseData().setOtherPartyHearingRecordingReqUi(
                getOtherPartyHearingRecordingReqUi(RequestStatus.REFUSED, "op_id1", party));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        SscsHearingRecordingCaseData sscsHearingRecordingCaseDataResponse = response.getData()
                .getSscsHearingRecordingCaseData();
        assertThat("Check RequestedHearings has been reduced",
                sscsHearingRecordingCaseDataResponse.getCitizenReleasedHearings(), is(empty()));
        assertThat("Check DwpRefusedHearings is not populated",
                sscsHearingRecordingCaseDataResponse.getRefusedHearings(), is(not(empty())));
        assertThat("Check RefusedHearings has the correct Hearing",
                sscsHearingRecordingCaseDataResponse.getRefusedHearings().get(0).getValue()
                        .getSscsHearingRecording().getHearingId(), is("an_id1"));
        assertThat("Check RefusedHearings has no approved date",
                sscsHearingRecordingCaseDataResponse.getRefusedHearings().get(0).getValue()
                        .getDateApproved(), is(nullValue()));
        assertThat("Check RefusedHearings has the correct other party id",
                sscsHearingRecordingCaseDataResponse.getRefusedHearings().get(0).getValue()
                        .getOtherPartyId(), is("op_id1"));
    }

    @Test
    @Parameters({"appellant", "representative", "jointParty"})
    public void givenAGrantedFromRefusedCitizenHearingRecording_thenRemoveFromRequestedListAndAddToReleasedList(String party) {
        SscsHearingRecordingDetails recording1 = SscsHearingRecordingDetails.builder().hearingId("an_id1").build();
        HearingRecordingRequest request1 = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails
                .builder().sscsHearingRecording(recording1)
                .requestingParty(party).build()).build();
        sscsCaseData.getSscsHearingRecordingCaseData().setRefusedHearings(new ArrayList<>(Arrays.asList(request1)));
        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequest(
                getProcessHearingRecordingRequest(RequestStatus.GRANTED, "an_id1", party));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        SscsHearingRecordingCaseData sscsHearingRecordingCaseDataResponse = response.getData()
                .getSscsHearingRecordingCaseData();
        assertThat("Check RefusedHearings has been reduced",
                sscsHearingRecordingCaseDataResponse.getRefusedHearings(), is(empty()));
        assertThat("Check DwpReleasedHearings is populated",
                sscsHearingRecordingCaseDataResponse.getCitizenReleasedHearings(), is(not(empty())));
        assertThat("Check DwpReleasedHearings has the correct Hearing",
                sscsHearingRecordingCaseDataResponse.getCitizenReleasedHearings().get(0).getValue()
                        .getSscsHearingRecording().getHearingId(), is("an_id1"));
    }

    @Test
    @Parameters({"otherParty", "otherPartyRep"})
    public void givenAGrantedFromRefusedCitizenHearingRecordingForOtherPartyRequest_thenRemoveFromRequestedListAndAddToReleasedList(String party) {
        SscsHearingRecordingDetails recording1 = SscsHearingRecordingDetails.builder().hearingId("an_id1").build();
        HearingRecordingRequest request1 = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails
                .builder().sscsHearingRecording(recording1)
                .requestingParty(party)
                .otherPartyId("op_id1").build()).build();
        sscsCaseData.getSscsHearingRecordingCaseData().setRefusedHearings(new ArrayList<>(Arrays.asList(request1)));
        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequest(
                getProcessHearingRecordingRequest(RequestStatus.GRANTED, "an_id1", party));
        sscsCaseData.getSscsHearingRecordingCaseData().setOtherPartyHearingRecordingReqUi(
                getOtherPartyHearingRecordingReqUi(RequestStatus.GRANTED, "op_id1", party));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        SscsHearingRecordingCaseData sscsHearingRecordingCaseDataResponse = response.getData()
                .getSscsHearingRecordingCaseData();
        assertThat("Check RefusedHearings has been reduced",
                sscsHearingRecordingCaseDataResponse.getRefusedHearings(), is(empty()));
        assertThat("Check DwpReleasedHearings is populated",
                sscsHearingRecordingCaseDataResponse.getCitizenReleasedHearings(), is(not(empty())));
        assertThat("Check DwpReleasedHearings has the correct Hearing",
                sscsHearingRecordingCaseDataResponse.getCitizenReleasedHearings().get(0).getValue()
                        .getSscsHearingRecording().getHearingId(), is("an_id1"));
        assertThat("Check RefusedHearings has the correct other party id",
                sscsHearingRecordingCaseDataResponse.getCitizenReleasedHearings().get(0).getValue()
                        .getOtherPartyId(), is("op_id1"));

    }

    @Test
    @Parameters({"appellant", "representative", "jointParty"})
    public void givenARequestedCitizenHearingRecording_thenDoNotAddToReleasedOrRefused(String party) {
        SscsHearingRecordingDetails recording1 = SscsHearingRecordingDetails.builder().hearingId("an_id1").build();
        HearingRecordingRequest request1 = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails
                .builder().sscsHearingRecording(recording1)
                .requestingParty(party).build()).build();
        sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearings(new ArrayList<>(Arrays.asList(request1)));
        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequest(
                getProcessHearingRecordingRequest(RequestStatus.REQUESTED, "an_id1", party));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        SscsHearingRecordingCaseData sscsHearingRecordingCaseDataResponse = response.getData()
                .getSscsHearingRecordingCaseData();
        assertThat("Check RefusedHearings has been reduced",
                sscsHearingRecordingCaseDataResponse.getRefusedHearings(), anyOf(nullValue(), empty()));
        assertThat("Check DwpReleasedHearings is populated",
                sscsHearingRecordingCaseDataResponse.getCitizenReleasedHearings(), anyOf(nullValue(), empty()));
        assertThat("Check RequestedHearings has the correct Hearing",
                sscsHearingRecordingCaseDataResponse.getRequestedHearings().get(0).getValue()
                        .getSscsHearingRecording().getHearingId(), is("an_id1"));
    }

    @Test
    public void whenHearingRecordingRequestWithNoHearingRecordingsIsActioned_thenDoNotThrowAnError() {
        SscsHearingRecording recording1 = getHearingRecording();
        HearingRecordingRequest hearingRecordingRequest = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails
                .builder().sscsHearingRecording(null)
                .requestingParty(PartyItemList.APPELLANT.getCode()).build()).build();

        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequest(
                getProcessHearingRecordingRequest(
                        RequestStatus.GRANTED, "an_id1", PartyItemList.APPELLANT.getCode()));
        sscsCaseData.getSscsHearingRecordingCaseData().setSscsHearingRecordings(singletonList(recording1));
        sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearings(singletonList(hearingRecordingRequest));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
    }


    private SscsHearingRecording getHearingRecording() {
        return SscsHearingRecording.builder()
                .value(SscsHearingRecordingDetails.builder().hearingId("an_id1").build())
                .build();
    }

}
