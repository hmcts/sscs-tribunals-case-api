package uk.gov.hmcts.reform.sscs.ccd.presubmit.processhearingrecordingrequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

    private ProcessHearingRecordingRequest getProcessHearingRecordingRequest(RequestStatus status, String hearingId, String party) {

        DynamicListItem dwpItem = party.equals(PartyItemList.DWP.getCode()) ? new DynamicListItem(status.getValue(), status.getValue()) : null;
        DynamicListItem jointItem = party.equals(PartyItemList.JOINT_PARTY.getCode()) ? new DynamicListItem(status.getValue(), status.getValue()) : null;
        DynamicListItem appellantItem = party.equals(PartyItemList.APPELLANT.getCode()) ? new DynamicListItem(status.getValue(), status.getValue()) : null;
        DynamicListItem repItem = party.equals(PartyItemList.REPRESENTATIVE.getCode()) ? new DynamicListItem(status.getValue(), status.getValue()) : null;

        return ProcessHearingRecordingRequest.builder().value(ProcessHearingRecordingRequestDetails.builder()
                .dwp(new DynamicList(dwpItem, Collections.emptyList()))
                .appellant(new DynamicList(appellantItem, Collections.emptyList()))
                .jointParty(new DynamicList(jointItem, Collections.emptyList()))
                .rep(new DynamicList(repItem, Collections.emptyList()))
                .hearingId(hearingId).build()).build();
    }

    @Test
    public void givenAGrantedFromVoidDwpHearingRecording_thenAddToReleasedListAndDwpStatusProcessed() {
        SscsHearingRecording recording1 = SscsHearingRecording.builder().value(SscsHearingRecordingDetails.builder()
                .hearingId("an_id1").build()).build();

        sscsCaseData.getSscsHearingRecordingCaseData().setSscsHearingRecordings(Arrays.asList(recording1));
        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequests(Arrays
                .asList(getProcessHearingRecordingRequest(RequestStatus.GRANTED, "an_id1",
                        PartyItemList.DWP.getCode())));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        SscsHearingRecordingCaseData sscsHearingRecordingCaseDataResponse = response.getData()
                .getSscsHearingRecordingCaseData();
        assertThat("Check RequestedHearings is empty",
                sscsHearingRecordingCaseDataResponse.getRequestedHearings(), is(empty()));
        assertThat("Check DwpReleasedHearings is populated",
                sscsHearingRecordingCaseDataResponse.getDwpReleasedHearings(), is(not(empty())));
        assertThat("Check DwpReleasedHearings has the correct Hearing",
                sscsHearingRecordingCaseDataResponse.getDwpReleasedHearings().get(0).getValue()
                        .getSscsHearingRecordingList().get(0).getValue().getHearingId(), is("an_id1"));
        assertThat("Check DwpReleasedHearings has the correct approved date",
                sscsHearingRecordingCaseDataResponse.getDwpReleasedHearings().get(0).getValue()
                        .getDateApproved(), is(LocalDate.now().toString()));
        assertThat("Check DwpState is PROCESSED", sscsCaseData.getDwpState(),
                is(DwpState.HEARING_RECORDING_PROCESSED.getId()));
    }

    @Test
    public void givenAGrantedFromRequestedDwpHearingRecording_thenRemoveFromRequestedListAndAddToReleasedListAndDwpStatusProcessed() {
        SscsHearingRecording recording1 = SscsHearingRecording.builder().value(SscsHearingRecordingDetails.builder()
                .hearingId("an_id1").build()).build();
        HearingRecordingRequest request1 = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails
                .builder().sscsHearingRecordingList(Arrays.asList(recording1))
                .requestingParty(PartyItemList.DWP.getCode()).build()).build();
        sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearings(new ArrayList<>(Arrays.asList(request1)));
        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequests(Arrays
                .asList(getProcessHearingRecordingRequest(RequestStatus.GRANTED, "an_id1",
                        PartyItemList.DWP.getCode())));

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
        assertThat("Check DwpReleasedHearings has the correct approved date",
                sscsHearingRecordingCaseDataResponse.getDwpReleasedHearings().get(0).getValue()
                        .getDateApproved(), is(LocalDate.now().toString()));
        assertThat("Check DwpState is PROCESSED", sscsCaseData.getDwpState(),
                is(DwpState.HEARING_RECORDING_PROCESSED.getId()));
    }

    @Test
    public void givenARefusedFromRequestedDwpHearingRecording_thenRemoveFromRequestedListAndDoNotAddToReleasedListAndDwpStatusProcessed() {
        SscsHearingRecording recording1 = SscsHearingRecording.builder().value(SscsHearingRecordingDetails.builder()
                .hearingId("an_id1").build()).build();
        HearingRecordingRequest request1 = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails
                .builder().sscsHearingRecordingList(Arrays.asList(recording1))
                .requestingParty(PartyItemList.DWP.getCode()).build()).build();
        sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearings(new ArrayList<>(Arrays.asList(request1)));
        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequests(Arrays
                .asList(getProcessHearingRecordingRequest(RequestStatus.REFUSED, "an_id1",
                        PartyItemList.DWP.getCode())));

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
                is(DwpState.HEARING_RECORDING_PROCESSED.getId()));
    }

    @Test
    public void givenARefusedFromGrantedDwpHearingRecording_thenRemoveFromReleasedListAndDwpStatusProcessed() {
        SscsHearingRecording recording1 = SscsHearingRecording.builder().value(SscsHearingRecordingDetails.builder()
                .hearingId("an_id1").build()).build();
        HearingRecordingRequest request1 = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails
                .builder().dateApproved(LocalDate.now().toString()).sscsHearingRecordingList(Arrays.asList(recording1))
                .requestingParty(PartyItemList.DWP.getCode()).build()).build();
        sscsCaseData.getSscsHearingRecordingCaseData().setDwpReleasedHearings(new ArrayList<>(Arrays.asList(request1)));
        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequests(Arrays
                .asList(getProcessHearingRecordingRequest(RequestStatus.REFUSED, "an_id1",
                        PartyItemList.DWP.getCode())));

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
        assertThat("Check RefusedHearings has no approved date",
                sscsHearingRecordingCaseDataResponse.getRefusedHearings().get(0).getValue()
                        .getDateApproved(), is(nullValue()));
        assertThat("Check DwpState is PROCESSED", sscsCaseData.getDwpState(),
                is(DwpState.HEARING_RECORDING_PROCESSED.getId()));
    }

    @Test
    public void givenAGrantedFromRefusedDwpHearingRecording_thenRemoveFromRequestedListAndAddToReleasedListAndDwpStatusProcessed() {
        SscsHearingRecording recording1 = SscsHearingRecording.builder().value(SscsHearingRecordingDetails.builder()
                .hearingId("an_id1").build()).build();
        HearingRecordingRequest request1 = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails
                .builder().sscsHearingRecordingList(Arrays.asList(recording1))
                .requestingParty(PartyItemList.DWP.getCode()).build()).build();
        sscsCaseData.getSscsHearingRecordingCaseData().setRefusedHearings(new ArrayList<>(Arrays.asList(request1)));
        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequests(Arrays
                .asList(getProcessHearingRecordingRequest(RequestStatus.GRANTED, "an_id1",
                        PartyItemList.DWP.getCode())));

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
                .asList(getProcessHearingRecordingRequest(RequestStatus.REQUESTED, "an_id1",
                        PartyItemList.DWP.getCode())));

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

    @Test
    @Parameters({"appellant", "representative", "jointParty"})
    public void givenAGrantedFromVoidCitizenHearingRecording_thenAddToReleasedList(String party) {
        SscsHearingRecording recording1 = SscsHearingRecording.builder().value(SscsHearingRecordingDetails.builder()
                .hearingId("an_id1").build()).build();

        sscsCaseData.getSscsHearingRecordingCaseData().setSscsHearingRecordings(Arrays.asList(recording1));
        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequests(Arrays
                .asList(getProcessHearingRecordingRequest(RequestStatus.GRANTED, "an_id1", party)));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        SscsHearingRecordingCaseData sscsHearingRecordingCaseDataResponse = response.getData()
                .getSscsHearingRecordingCaseData();
        assertThat("Check RequestedHearings is empty",
                sscsHearingRecordingCaseDataResponse.getRequestedHearings(), is(empty()));
        assertThat("Check DwpReleasedHearings is populated",
                sscsHearingRecordingCaseDataResponse.getCitizenReleasedHearings(), is(not(empty())));
        assertThat("Check DwpReleasedHearings has the correct Hearing",
                sscsHearingRecordingCaseDataResponse.getCitizenReleasedHearings().get(0).getValue()
                        .getSscsHearingRecordingList().get(0).getValue().getHearingId(), is("an_id1"));
        assertThat("Check DwpReleasedHearings has the correct approved date",
                sscsHearingRecordingCaseDataResponse.getCitizenReleasedHearings().get(0).getValue()
                        .getDateApproved(), is(LocalDate.now().toString()));
    }

    @Test
    @Parameters({"appellant", "representative", "jointParty"})
    public void givenAGrantedFromRequestedCitizenHearingRecording_thenRemoveFromRequestedListAndAddToReleasedList(String party) {
        SscsHearingRecording recording1 = SscsHearingRecording.builder().value(SscsHearingRecordingDetails.builder()
                .hearingId("an_id1").build()).build();
        HearingRecordingRequest request1 = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails
                .builder().sscsHearingRecordingList(Arrays.asList(recording1))
                .requestingParty(party).build()).build();
        sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearings(new ArrayList<>(Arrays.asList(request1)));
        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequests(Arrays
                .asList(getProcessHearingRecordingRequest(RequestStatus.GRANTED, "an_id1", party)));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        SscsHearingRecordingCaseData sscsHearingRecordingCaseDataResponse = response.getData()
                .getSscsHearingRecordingCaseData();
        assertThat("Check RequestedHearings has been reduced",
                sscsHearingRecordingCaseDataResponse.getRequestedHearings(), is(empty()));
        assertThat("Check DwpReleasedHearings is populated",
                sscsHearingRecordingCaseDataResponse.getCitizenReleasedHearings(), is(not(empty())));
        assertThat("Check DwpReleasedHearings has the correct Hearing",
                sscsHearingRecordingCaseDataResponse.getCitizenReleasedHearings().get(0).getValue()
                        .getSscsHearingRecordingList().get(0).getValue().getHearingId(), is("an_id1"));
        assertThat("Check DwpReleasedHearings has the correct approved date",
                sscsHearingRecordingCaseDataResponse.getCitizenReleasedHearings().get(0).getValue()
                        .getDateApproved(), is(LocalDate.now().toString()));
    }

    @Test
    @Parameters({"appellant", "representative", "jointParty"})
    public void givenARefusedFromRequestedCitizenHearingRecording_thenRemoveFromRequestedListAndDoNotAddToReleasedList(String party) {
        SscsHearingRecording recording1 = SscsHearingRecording.builder().value(SscsHearingRecordingDetails.builder()
                .hearingId("an_id1").build()).build();
        HearingRecordingRequest request1 = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails
                .builder().sscsHearingRecordingList(Arrays.asList(recording1))
                .requestingParty(party).build()).build();
        sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearings(new ArrayList<>(Arrays.asList(request1)));
        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequests(Arrays
                .asList(getProcessHearingRecordingRequest(RequestStatus.REFUSED, "an_id1", party)));

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
    @Parameters({"appellant", "representative", "jointParty"})
    public void givenARefusedFromGrantedCitizenHearingRecording_thenRemoveFromReleasedList(String party) {
        SscsHearingRecording recording1 = SscsHearingRecording.builder().value(SscsHearingRecordingDetails.builder()
                .hearingId("an_id1").build()).build();
        HearingRecordingRequest request1 = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails
                .builder().dateApproved(LocalDate.now().toString()).sscsHearingRecordingList(Arrays.asList(recording1))
                .requestingParty(party).build()).build();
        sscsCaseData.getSscsHearingRecordingCaseData().setCitizenReleasedHearings(new ArrayList<>(Arrays.asList(request1)));
        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequests(Arrays
                .asList(getProcessHearingRecordingRequest(RequestStatus.REFUSED, "an_id1", party)));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        SscsHearingRecordingCaseData sscsHearingRecordingCaseDataResponse = response.getData()
                .getSscsHearingRecordingCaseData();
        assertThat("Check RequestedHearings has been reduced",
                sscsHearingRecordingCaseDataResponse.getCitizenReleasedHearings(), is(empty()));
        assertThat("Check DwpRefusedHearings is not populated",
                sscsHearingRecordingCaseDataResponse.getRefusedHearings(), is(not(empty())));
        assertThat("Check RefusedHearings has the correct Hearing",
                sscsHearingRecordingCaseDataResponse.getRefusedHearings().get(0).getValue()
                        .getSscsHearingRecordingList().get(0).getValue().getHearingId(), is("an_id1"));
        assertThat("Check RefusedHearings has no approved date",
                sscsHearingRecordingCaseDataResponse.getRefusedHearings().get(0).getValue()
                        .getDateApproved(), is(nullValue()));
    }

    @Test
    @Parameters({"appellant", "representative", "jointParty"})
    public void givenAGrantedFromRefusedCitizenHearingRecording_thenRemoveFromRequestedListAndAddToReleasedList(String party) {
        SscsHearingRecording recording1 = SscsHearingRecording.builder().value(SscsHearingRecordingDetails.builder()
                .hearingId("an_id1").build()).build();
        HearingRecordingRequest request1 = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails
                .builder().sscsHearingRecordingList(Arrays.asList(recording1))
                .requestingParty(party).build()).build();
        sscsCaseData.getSscsHearingRecordingCaseData().setRefusedHearings(new ArrayList<>(Arrays.asList(request1)));
        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequests(Arrays
                .asList(getProcessHearingRecordingRequest(RequestStatus.GRANTED, "an_id1", party)));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        SscsHearingRecordingCaseData sscsHearingRecordingCaseDataResponse = response.getData()
                .getSscsHearingRecordingCaseData();
        assertThat("Check RefusedHearings has been reduced",
                sscsHearingRecordingCaseDataResponse.getRefusedHearings(), is(empty()));
        assertThat("Check DwpReleasedHearings is populated",
                sscsHearingRecordingCaseDataResponse.getCitizenReleasedHearings(), is(not(empty())));
        assertThat("Check DwpReleasedHearings has the correct Hearing",
                sscsHearingRecordingCaseDataResponse.getCitizenReleasedHearings().get(0).getValue()
                        .getSscsHearingRecordingList().get(0).getValue().getHearingId(), is("an_id1"));
    }

    @Test
    @Parameters({"appellant", "representative", "jointParty"})
    public void givenARequestedCitizenHearingRecording_thenDoNotAddToReleasedOrRefused(String party) {
        SscsHearingRecording recording1 = SscsHearingRecording.builder().value(SscsHearingRecordingDetails.builder()
                .hearingId("an_id1").build()).build();
        HearingRecordingRequest request1 = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails
                .builder().sscsHearingRecordingList(Arrays.asList(recording1))
                .requestingParty(party).build()).build();
        sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearings(new ArrayList<>(Arrays.asList(request1)));
        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequests(Arrays
                .asList(getProcessHearingRecordingRequest(RequestStatus.REQUESTED, "an_id1", party)));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        SscsHearingRecordingCaseData sscsHearingRecordingCaseDataResponse = response.getData()
                .getSscsHearingRecordingCaseData();
        assertThat("Check RefusedHearings has been reduced",
                sscsHearingRecordingCaseDataResponse.getRefusedHearings(), anyOf(nullValue(), empty()));
        assertThat("Check DwpReleasedHearings is populated",
                sscsHearingRecordingCaseDataResponse.getCitizenReleasedHearings(), anyOf(nullValue(), empty()));
        assertThat("Check RequestedHearings has the correct Hearing",
                sscsHearingRecordingCaseDataResponse.getRequestedHearings().get(0).getValue()
                        .getSscsHearingRecordingList().get(0).getValue().getHearingId(), is("an_id1"));
    }
}