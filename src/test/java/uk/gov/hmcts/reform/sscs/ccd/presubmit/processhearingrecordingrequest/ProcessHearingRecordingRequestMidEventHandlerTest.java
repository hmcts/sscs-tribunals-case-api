package uk.gov.hmcts.reform.sscs.ccd.presubmit.processhearingrecordingrequest;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.processhearingrecordingrequest.ProcessHearingRecordingRequestAboutToStartHandlerTest.*;
import static uk.gov.hmcts.reform.sscs.model.PartyItemList.*;
import static uk.gov.hmcts.reform.sscs.model.RequestStatus.GRANTED;
import static uk.gov.hmcts.reform.sscs.model.RequestStatus.REFUSED;

import java.util.ArrayList;
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
import uk.gov.hmcts.reform.sscs.service.processhearingrecordingrequest.ProcessHearingRecordingRequestService;

@RunWith(JUnitParamsRunner.class)
public class ProcessHearingRecordingRequestMidEventHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private final ProcessHearingRecordingRequestMidEventHandler handler = new ProcessHearingRecordingRequestMidEventHandler(new ProcessHearingRecordingRequestService());

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

        sscsCaseData = SscsCaseData.builder()
                .appeal(Appeal.builder()
                        .mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build())
                        .rep(Representative.builder()
                                .hasRepresentative(YES.getValue())
                                .organisation("org")
                                .build())
                        .build())
                .jointParty(YES.getValue())
                .hearings(List.of(HEARING, getHearing(2)))
                .sscsHearingRecordingCaseData(SscsHearingRecordingCaseData.builder()
                        .sscsHearingRecordings(List.of(recording(1), recording(2)))
                        .build())
                .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getEvent()).thenReturn(EventType.PROCESS_HEARING_RECORDING_REQUEST);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(userDetails);
    }


    @Test
    @Parameters({"REPRESENTATIVE", "APPELLANT", "DWP", "JOINT_PARTY"})
    public void changingRequestFromGrantedToRefusedReturnsWarning(PartyItemList party) {

        setReleasedHearingsForParty(party);

        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequests(
                List.of(getProcessHearingRecordingRequestDetails(party, true))
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

        setRefusedHearingsForParty(party);

        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequests(
                List.of(getProcessHearingRecordingRequestDetails(party, false))
        );
        final PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(1));
        assertThat(response.getWarnings().iterator().next(), is("Are you sure you want to change the request status"));
    }

    private ProcessHearingRecordingRequest getProcessHearingRecordingRequestDetails(PartyItemList party, boolean setRefusedForParty) {
        return ProcessHearingRecordingRequest.builder()
                .value(ProcessHearingRecordingRequestDetails.builder()
                        .hearingId(HEARING.getValue().getHearingId())
                        .appellant(getDynamicList(party, APPELLANT, setRefusedForParty))
                        .dwp(getDynamicList(party, DWP, setRefusedForParty))
                        .jointParty(getDynamicList(party, JOINT_PARTY, setRefusedForParty))
                        .rep(getDynamicList(party, REPRESENTATIVE, setRefusedForParty))
                        .build())
                .build();
    }

    @Test
    @Parameters({"APPELLANT", "DWP", "JOINT_PARTY", "REPRESENTATIVE"})
    public void changingFromRequestedToRefusedHasNoWarningsOrErrors(PartyItemList party) {
        sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearings(
                List.of(hearingRecordingRequest(party))
        );

        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequests(
                List.of(getProcessHearingRecordingRequestDetails(party, true))
        );
        final PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(0));
    }


    private void setReleasedHearingsForParty(PartyItemList party) {
        if (party.equals(DWP)) {
            sscsCaseData.getSscsHearingRecordingCaseData().setDwpReleasedHearings(
                    List.of(hearingRecordingRequest(party)));
        } else {
            sscsCaseData.getSscsHearingRecordingCaseData().setCitizenReleasedHearings(
                    List.of(hearingRecordingRequest(party)));
        }
    }

    private void setRefusedHearingsForParty(PartyItemList party) {
        sscsCaseData.getSscsHearingRecordingCaseData().setRefusedHearings(List.of(hearingRecordingRequest(party)));
    }

    private HearingRecordingRequest hearingRecordingRequest(PartyItemList party) {
        return HearingRecordingRequest.builder()
                .value(HearingRecordingRequestDetails.builder()
                        .requestingParty(party.getCode())
                        .sscsHearingRecording(recording(1).getValue())
                        .build())
                .build();
    }

    @NotNull
    private DynamicList getDynamicList(PartyItemList party, PartyItemList partyItemList, boolean setRefusedForParty) {
        return new DynamicList(refusedSelectedIfTrueElseGranted(party.equals(partyItemList) && setRefusedForParty), getListItems());
    }

    @NotNull
    private List<DynamicListItem> getListItems() {
        return List.of(REFUSED, GRANTED).stream().map(s -> new DynamicListItem(s.getLabel(), s.getLabel())).collect(Collectors.toList());
    }

    private DynamicListItem refusedSelectedIfTrueElseGranted(boolean trueIfRefused) {
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

}
