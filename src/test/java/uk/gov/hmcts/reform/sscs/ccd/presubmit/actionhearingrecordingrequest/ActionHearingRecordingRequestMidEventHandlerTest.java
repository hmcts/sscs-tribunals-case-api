package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionhearingrecordingrequest;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionhearingrecordingrequest.ActionHearingRecordingRequestAboutToStartHandlerTest.recording;
import static uk.gov.hmcts.reform.sscs.model.PartyItemList.*;
import static uk.gov.hmcts.reform.sscs.model.RequestStatus.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.idam.UserRole;
import uk.gov.hmcts.reform.sscs.model.PartyItemList;
import uk.gov.hmcts.reform.sscs.model.RequestStatus;
import uk.gov.hmcts.reform.sscs.service.actionhearingrecordingrequest.ActionHearingRecordingRequestService;

@RunWith(JUnitParamsRunner.class)
public class ActionHearingRecordingRequestMidEventHandlerTest {

    public static final Hearing HEARING = getHearing("1");
    private static final String USER_AUTHORISATION = "Bearer token";
    private final ActionHearingRecordingRequestMidEventHandler handler = new ActionHearingRecordingRequestMidEventHandler(new ActionHearingRecordingRequestService());

    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    @Mock
    private IdamService idamService;

    private SscsCaseData sscsCaseData;
    private final UserDetails userDetails = UserDetails.builder().roles(new ArrayList<>(asList("caseworker-sscs", UserRole.CTSC_CLERK.getValue()))).build();


    @Before
    public void setUp() {
        openMocks(this);

        DynamicListItem selectedHearing = new DynamicListItem("1", "Venue Name 12:00:00 01 Jan 2021");
        sscsCaseData = SscsCaseData.builder()
                .appeal(Appeal.builder()
                        .mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build())
                        .rep(Representative.builder()
                                .hasRepresentative(YES.getValue())
                                .organisation("org")
                                .build())
                        .build())
                .jointParty(JointParty.builder().hasJointParty(YES).build())
                .hearings(List.of(HEARING, getHearing("an_id2")))
                .sscsHearingRecordingCaseData(SscsHearingRecordingCaseData.builder()
                        .selectHearingDetails(new DynamicList(selectedHearing, Arrays.asList(selectedHearing)))
                        .sscsHearingRecordings(List.of(recording("1"), recording("an_id2")))
                        .build())
                .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getEvent()).thenReturn(EventType.ACTION_HEARING_RECORDING_REQUEST);
        when(callback.getPageId()).thenReturn("validateHearingRecordingRequest");
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(userDetails);
    }


    @Test
    @Parameters({"REPRESENTATIVE", "APPELLANT", "DWP", "JOINT_PARTY"})
    public void changingRequestFromGrantedToRefusedReturnsWarning(PartyItemList party) {

        setReleasedHearingsForParty(party, null);

        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequest(
                getProcessHearingRecordingRequestDetails(party, true)
        );
        final PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(1));
        assertThat(response.getWarnings().iterator().next(), is("Are you sure you want to change the request status"));
    }

    @Test
    @Parameters({"REPRESENTATIVE", "APPELLANT", "DWP", "JOINT_PARTY"})
    public void changingRequestFromRefusedToGrantedReturnsWarning(PartyItemList party) {

        setRefusedHearingsForParty(sscsCaseData, party, null);

        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequest(
                getProcessHearingRecordingRequestDetails(party, false)
        );
        final PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(1));
        assertThat(response.getWarnings().iterator().next(), is("Are you sure you want to change the request status"));
    }

    public static ProcessHearingRecordingRequest getProcessHearingRecordingRequestDetails(PartyItemList party, boolean setRefusedForParty) {
        return ProcessHearingRecordingRequest.builder()
                    .hearingId(HEARING.getValue().getHearingId())
                    .appellant(getDynamicList(party, APPELLANT, setRefusedForParty))
                    .dwp(getDynamicList(party, DWP, setRefusedForParty))
                    .jointParty(getDynamicList(party, JOINT_PARTY, setRefusedForParty))
                    .rep(getDynamicList(party, REPRESENTATIVE, setRefusedForParty))
                    .build();
    }

    @Test
    @Parameters({"APPELLANT", "DWP", "JOINT_PARTY", "REPRESENTATIVE"})
    public void changingFromRequestedToRefusedHasNoWarningsOrErrors(PartyItemList party) {
        sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearings(
                List.of(hearingRecordingRequest(party, null))
        );

        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequest(
                getProcessHearingRecordingRequestDetails(party, true)
        );
        final PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(0));
    }

    @Test
    public void givenCaseWithOtherPartiesAndNoExistingHearingRecordingRequests_thenBuildTheOtherPartyUi() {
        when(callback.getPageId()).thenReturn("selectHearing");

        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequest(ProcessHearingRecordingRequest.builder()
                .hearingId(HEARING.getValue().getHearingId())
                .build());

        List<CcdValue<OtherParty>> otherParties = Arrays.asList(buildOtherPartyWithAppointeeAndRep("1", "2", "3"));
        sscsCaseData.setOtherParties(otherParties);

        final PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(0));
        assertThat(response.getData().getSscsHearingRecordingCaseData().getOtherPartyHearingRecordingReqUi().size(), is(2));
        assertThat(response.getData().getSscsHearingRecordingCaseData().getOtherPartyHearingRecordingReqUi().get(0).getValue().getOtherPartyName(), is("Harry Kane"));
        assertThat(response.getData().getSscsHearingRecordingCaseData().getOtherPartyHearingRecordingReqUi().get(0).getValue().getHearingRecordingStatus().getValue().getCode(), is(""));
        assertThat(response.getData().getSscsHearingRecordingCaseData().getOtherPartyHearingRecordingReqUi().get(0).getValue().getOtherPartyId(), is("1"));
        assertThat(response.getData().getSscsHearingRecordingCaseData().getOtherPartyHearingRecordingReqUi().get(1).getValue().getOtherPartyName(), is("Wendy Smith - Representative"));
        assertThat(response.getData().getSscsHearingRecordingCaseData().getOtherPartyHearingRecordingReqUi().get(1).getValue().getHearingRecordingStatus().getValue().getCode(), is(""));
        assertThat(response.getData().getSscsHearingRecordingCaseData().getOtherPartyHearingRecordingReqUi().get(1).getValue().getOtherPartyId(), is("3"));
    }

    @Test
    public void givenCaseWithMultipleOtherPartiesAndNoExistingHearingRecordingRequests_thenBuildTheOtherPartyUi() {
        when(callback.getPageId()).thenReturn("selectHearing");

        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequest(ProcessHearingRecordingRequest.builder()
                .hearingId(HEARING.getValue().getHearingId())
                .build());

        CcdValue<OtherParty> otherParty2 = CcdValue.<OtherParty>builder()
                .value(OtherParty.builder().id("1").name(Name.builder().firstName("Hugo").lastName("Lloris").build()).build()).build();

        List<CcdValue<OtherParty>> otherParties = Arrays.asList(buildOtherPartyWithAppointeeAndRep(null, null, null), otherParty2);
        sscsCaseData.setOtherParties(otherParties);

        final PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(0));
        assertThat(response.getData().getSscsHearingRecordingCaseData().getOtherPartyHearingRecordingReqUi().size(), is(3));
        assertThat(response.getData().getSscsHearingRecordingCaseData().getOtherPartyHearingRecordingReqUi().get(0).getValue().getOtherPartyName(), is("Harry Kane"));
        assertThat(response.getData().getSscsHearingRecordingCaseData().getOtherPartyHearingRecordingReqUi().get(0).getValue().getHearingRecordingStatus().getValue().getCode(), is(""));
        assertThat(response.getData().getSscsHearingRecordingCaseData().getOtherPartyHearingRecordingReqUi().get(1).getValue().getOtherPartyName(), is("Wendy Smith - Representative"));
        assertThat(response.getData().getSscsHearingRecordingCaseData().getOtherPartyHearingRecordingReqUi().get(1).getValue().getHearingRecordingStatus().getValue().getCode(), is(""));
        assertThat(response.getData().getSscsHearingRecordingCaseData().getOtherPartyHearingRecordingReqUi().get(2).getValue().getOtherPartyName(), is("Hugo Lloris"));
        assertThat(response.getData().getSscsHearingRecordingCaseData().getOtherPartyHearingRecordingReqUi().get(2).getValue().getHearingRecordingStatus().getValue().getCode(), is(""));
    }

    @Test
    public void givenCaseWithOtherPartiesAndExistingHearingRecordingRequestForSelectedHearing_thenBuildTheOtherPartyUi() {
        when(callback.getPageId()).thenReturn("selectHearing");

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

        List<CcdValue<OtherParty>> otherParties = Arrays.asList(buildOtherPartyWithAppointeeAndRep("1", "2", "3"));
        sscsCaseData.setOtherParties(otherParties);

        final PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

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
    public void givenCaseWithOtherPartiesAndExistingHearingRecordingRequestForOtherPartyAppointeeAndRepForSelectedHearing_thenBuildTheOtherPartyUi() {
        when(callback.getPageId()).thenReturn("selectHearing");

        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequest(ProcessHearingRecordingRequest.builder()
                .hearingId(HEARING.getValue().getHearingId())
                .build());

        sscsCaseData.getSscsHearingRecordingCaseData().setCitizenReleasedHearings(List.of(HearingRecordingRequest.builder().value(HearingRecordingRequestDetails.builder()
                .otherPartyId("1")
                .status(GRANTED.getLabel())
                .requestingParty(OTHER_PARTY.getCode())
                .sscsHearingRecording(SscsHearingRecordingDetails.builder().hearingId("1").build()).build()).build()));

        sscsCaseData.getSscsHearingRecordingCaseData().setRefusedHearings(List.of(HearingRecordingRequest.builder().value(HearingRecordingRequestDetails.builder()
                .otherPartyId("3")
                .status(REFUSED.getLabel())
                .requestingParty(OTHER_PARTY_REPRESENTATIVE.getCode())
                .sscsHearingRecording(SscsHearingRecordingDetails.builder().hearingId("1").build()).build()).build()));

        List<CcdValue<OtherParty>> otherParties = Arrays.asList(buildOtherPartyWithAppointeeAndRep("1", "2", "3"));
        sscsCaseData.setOtherParties(otherParties);

        final PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(0));
        assertThat(response.getData().getSscsHearingRecordingCaseData().getOtherPartyHearingRecordingReqUi().size(), is(2));
        assertThat(response.getData().getSscsHearingRecordingCaseData().getOtherPartyHearingRecordingReqUi().get(0).getValue().getOtherPartyName(), is("Harry Kane"));
        assertThat(response.getData().getSscsHearingRecordingCaseData().getOtherPartyHearingRecordingReqUi().get(0).getValue().getHearingRecordingStatus().getValue().getCode(), is("Granted"));
        assertThat(response.getData().getSscsHearingRecordingCaseData().getOtherPartyHearingRecordingReqUi().get(1).getValue().getOtherPartyName(), is("Wendy Smith - Representative"));
        assertThat(response.getData().getSscsHearingRecordingCaseData().getOtherPartyHearingRecordingReqUi().get(1).getValue().getHearingRecordingStatus().getValue().getCode(), is("Refused"));
    }

    @Test
    public void givenCaseWithOtherPartiesAndExistingRequestedHearingRecordingRequestForSelectedHearing_thenBuildTheOtherPartyUi() {
        when(callback.getPageId()).thenReturn("selectHearing");

        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequest(ProcessHearingRecordingRequest.builder()
                .hearingId(HEARING.getValue().getHearingId())
                .build());


        sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearings(List.of(HearingRecordingRequest.builder().value(HearingRecordingRequestDetails.builder()
                .otherPartyId("1")
                .status(REQUESTED.getLabel())
                .requestingParty(OTHER_PARTY.getCode())
                .sscsHearingRecording(SscsHearingRecordingDetails.builder().hearingId("1").build()).build()).build()));

        List<CcdValue<OtherParty>> otherParties = Arrays.asList(buildOtherPartyWithAppointeeAndRep("1", "2", "3"));
        sscsCaseData.setOtherParties(otherParties);

        final PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(0));
        assertThat(response.getData().getSscsHearingRecordingCaseData().getOtherPartyHearingRecordingReqUi().size(), is(2));
        assertThat(response.getData().getSscsHearingRecordingCaseData().getOtherPartyHearingRecordingReqUi().get(0).getValue().getOtherPartyName(), is("Harry Kane"));
        assertThat(response.getData().getSscsHearingRecordingCaseData().getOtherPartyHearingRecordingReqUi().get(0).getValue().getHearingRecordingStatus().getValue().getCode(), is("Requested"));
        assertThat(response.getData().getSscsHearingRecordingCaseData().getOtherPartyHearingRecordingReqUi().get(0).getValue().getHearingRecordingStatus().getListItems().size(), is(3));
        assertThat(response.getData().getSscsHearingRecordingCaseData().getOtherPartyHearingRecordingReqUi().get(1).getValue().getOtherPartyName(), is("Wendy Smith - Representative"));
        assertThat(response.getData().getSscsHearingRecordingCaseData().getOtherPartyHearingRecordingReqUi().get(1).getValue().getHearingRecordingStatus().getValue().getCode(), is(""));
    }

    public static CcdValue<OtherParty> buildOtherPartyWithAppointeeAndRep(String id, String appointeeId, String repId) {
        return CcdValue.<OtherParty>builder()
                .value(OtherParty.builder()
                        .id(id)
                        .name(Name.builder().firstName("Harry").lastName("Kane").build())
                        .isAppointee(YES.getValue())
                        .appointee(Appointee.builder().id(appointeeId).name(Name.builder().firstName("Henry").lastName("Smith").build()).build())
                        .rep(Representative.builder().id(repId).name(Name.builder().firstName("Wendy").lastName("Smith").build()).hasRepresentative(YES.getValue()).build())
                        .build())
                .build();
    }

    @Test
    public void givenHearingRecordings_showProcessHearingRecordingRequests() {
        when(callback.getPageId()).thenReturn("selectHearing");

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        SscsCaseData responseData = response.getData();
        assertThat(responseData.getSscsHearingRecordingCaseData().getProcessHearingRecordingRequest().getHearingId(), is("1"));
        final ProcessHearingRecordingRequest processHearingRecordingRequest = responseData.getSscsHearingRecordingCaseData().getProcessHearingRecordingRequest();
        assertThat(processHearingRecordingRequest.getHearingId(), is(HEARING.getValue().getHearingId()));
        assertThat(processHearingRecordingRequest.getHearingTitle(), is("Hearing 1"));
        assertThat(processHearingRecordingRequest.getHearingInformation(), is("Venue 1 12:00:00 18 May 2021"));
        assertThat(processHearingRecordingRequest.getRecordings().size(), is(1));
        assertThat(processHearingRecordingRequest.getAppellant().getValue().getCode(), is(""));
        assertThat(processHearingRecordingRequest.getAppellant().getListItems().stream().map(DynamicListItem::getCode).collect(Collectors.toList()), is(List.of("Granted", "Refused")));
        assertThat(processHearingRecordingRequest.getDwp().getValue().getCode(), is(""));
        assertThat(processHearingRecordingRequest.getJointParty().getValue().getCode(), is(""));
        assertThat(processHearingRecordingRequest.getRep().getValue().getCode(), is(""));
    }

    @Test
    public void givenCaseWithOtherPartiesAndUserTriesToPressTheRemoveButtonForOtherPartyRequests_thenShowAnError() {

        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequest(ProcessHearingRecordingRequest.builder()
                .hearingId(HEARING.getValue().getHearingId())
                .build());

        List<CcdValue<OtherParty>> otherParties = Arrays.asList(buildOtherPartyWithAppointeeAndRep("1", "2", "3"));
        sscsCaseData.setOtherParties(otherParties);

        List<OtherPartyHearingRecordingReqUi> otherPartyHearingRecordingReqUi = new ArrayList<>();
        otherPartyHearingRecordingReqUi.add(OtherPartyHearingRecordingReqUi.builder().value(OtherPartyHearingRecordingReqUiDetails.builder().hearingRecordingStatus(new DynamicList(refusedSelectedIfTrueElseGranted(false), getListItems())).otherPartyId("1").otherPartyName("Harry Kane").build()).build());

        sscsCaseData.getSscsHearingRecordingCaseData().setOtherPartyHearingRecordingReqUi(otherPartyHearingRecordingReqUi);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getWarnings().size(), is(0));
        assertThat(response.getErrors().iterator().next(), is("Please do not use the remove buttons within this event. You may need to start again."));
    }

    @Test
    @Parameters({"OTHER_PARTY,1", "OTHER_PARTY_REPRESENTATIVE,3"})
    public void givenCaseWithOtherParties_changingRequestFromGrantedToRefusedReturnsWarning(PartyItemList party, String otherPartyId) {

        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequest(ProcessHearingRecordingRequest.builder()
                .hearingId(HEARING.getValue().getHearingId())
                .build());

        setReleasedHearingsForParty(party, otherPartyId);

        List<CcdValue<OtherParty>> otherParties = Arrays.asList(buildOtherPartyWithAppointeeAndRep("1", "2", "3"));
        sscsCaseData.setOtherParties(otherParties);

        List<OtherPartyHearingRecordingReqUi> otherPartyHearingRecordingReqUi = new ArrayList<>();
        otherPartyHearingRecordingReqUi.add(OtherPartyHearingRecordingReqUi.builder().value(OtherPartyHearingRecordingReqUiDetails.builder().hearingRecordingStatus(new DynamicList(refusedSelectedIfTrueElseGranted(OTHER_PARTY.equals(party)), getListItems())).otherPartyId("1").otherPartyName("Harry Kane").build()).build());
        otherPartyHearingRecordingReqUi.add(OtherPartyHearingRecordingReqUi.builder().value(OtherPartyHearingRecordingReqUiDetails.builder().hearingRecordingStatus(new DynamicList(refusedSelectedIfTrueElseGranted(OTHER_PARTY_REPRESENTATIVE.equals(party)), getListItems())).otherPartyId("3").otherPartyName("Harry Kane").build()).build());

        sscsCaseData.getSscsHearingRecordingCaseData().setOtherPartyHearingRecordingReqUi(otherPartyHearingRecordingReqUi);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(1));
        assertThat(response.getWarnings().iterator().next(), is("Are you sure you want to change the request status"));
    }

    @Test
    @Parameters({"OTHER_PARTY,1", "OTHER_PARTY_REPRESENTATIVE,3"})
    public void givenCaseWithOtherParties_changingRequestFromRefusedToGrantedReturnsWarning(PartyItemList party, String otherPartyId) {

        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequest(ProcessHearingRecordingRequest.builder()
                .hearingId(HEARING.getValue().getHearingId())
                .build());

        setRefusedHearingsForParty(sscsCaseData, party, otherPartyId);

        List<CcdValue<OtherParty>> otherParties = Arrays.asList(buildOtherPartyWithAppointeeAndRep("1", "2", "3"));
        sscsCaseData.setOtherParties(otherParties);

        List<OtherPartyHearingRecordingReqUi> otherPartyHearingRecordingReqUi = new ArrayList<>();
        otherPartyHearingRecordingReqUi.add(OtherPartyHearingRecordingReqUi.builder().value(OtherPartyHearingRecordingReqUiDetails.builder().hearingRecordingStatus(new DynamicList(refusedSelectedIfTrueElseGranted(!OTHER_PARTY.equals(party)), getListItems())).otherPartyId("1").otherPartyName("Harry Kane").build()).build());
        otherPartyHearingRecordingReqUi.add(OtherPartyHearingRecordingReqUi.builder().value(OtherPartyHearingRecordingReqUiDetails.builder().hearingRecordingStatus(new DynamicList(refusedSelectedIfTrueElseGranted(!OTHER_PARTY_REPRESENTATIVE.equals(party)), getListItems())).otherPartyId("3").otherPartyName("Harry Kane").build()).build());

        sscsCaseData.getSscsHearingRecordingCaseData().setOtherPartyHearingRecordingReqUi(otherPartyHearingRecordingReqUi);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(1));
        assertThat(response.getWarnings().iterator().next(), is("Are you sure you want to change the request status"));
    }

    private void setReleasedHearingsForParty(PartyItemList party, String otherPartyId) {
        if (party.equals(DWP)) {
            sscsCaseData.getSscsHearingRecordingCaseData().setDwpReleasedHearings(
                    List.of(hearingRecordingRequest(party, otherPartyId)));
        } else {
            sscsCaseData.getSscsHearingRecordingCaseData().setCitizenReleasedHearings(
                    List.of(hearingRecordingRequest(party, otherPartyId)));
        }
    }

    public static void setRefusedHearingsForParty(SscsCaseData sscsCaseData, PartyItemList party, String otherPartyId) {
        sscsCaseData.getSscsHearingRecordingCaseData().setRefusedHearings(List.of(hearingRecordingRequest(party, otherPartyId)));
    }

    private static HearingRecordingRequest hearingRecordingRequest(PartyItemList party, String otherPartyId) {
        return HearingRecordingRequest.builder()
                .value(HearingRecordingRequestDetails.builder()
                        .requestingParty(party.getCode())
                        .otherPartyId(otherPartyId)
                        .sscsHearingRecording(recording("1").getValue())
                        .build())
                .build();
    }

    @NotNull
    private static DynamicList getDynamicList(PartyItemList party, PartyItemList partyItemList, boolean setRefusedForParty) {
        return new DynamicList(refusedSelectedIfTrueElseGranted(party.equals(partyItemList) && setRefusedForParty), getListItems());
    }

    @NotNull
    private static List<DynamicListItem> getListItems() {
        return List.of(REFUSED, GRANTED).stream().map(s -> new DynamicListItem(s.getLabel(), s.getLabel())).collect(Collectors.toList());
    }

    private static DynamicListItem refusedSelectedIfTrueElseGranted(boolean trueIfRefused) {
        RequestStatus status = trueIfRefused ? REFUSED : GRANTED;
        return new DynamicListItem(status.getLabel(), status.getLabel());
    }

    @Test
    @Parameters({"ABOUT_TO_START", "ABOUT_TO_SUBMIT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

    static Hearing getHearing(String hearingId) {
        HearingDetails hearingDetails = HearingDetails.builder()
                .hearingId(hearingId)
                .hearingDate("2021-05-18")
                .time("12:00")
                .venue(Venue.builder().name(format("Venue %s", hearingId)).build())
                .build();
        return Hearing.builder().value(hearingDetails).build();
    }

}
