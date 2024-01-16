package uk.gov.hmcts.reform.sscs.service.processhearingrecordingrequest;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.*;
import static uk.gov.hmcts.reform.sscs.model.RequestStatus.GRANTED;
import static uk.gov.hmcts.reform.sscs.model.RequestStatus.REFUSED;
import static uk.gov.hmcts.reform.sscs.model.RequestStatus.REQUESTED;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.model.PartyItemList;
import uk.gov.hmcts.reform.sscs.model.RequestStatus;
import uk.gov.hmcts.reform.sscs.service.actionhearingrecordingrequest.ActionHearingRecordingRequestService;

@RunWith(JUnitParamsRunner.class)
public class ActionHearingRecordingRequestServiceTest {

    private final ActionHearingRecordingRequestService service = new ActionHearingRecordingRequestService();
    private static final Hearing HEARING = getHearing();

    @Test
    public void getFormattedHearingInformation_willFormatStringBasedOnVenueNameHearingTimeAndDate() {
        final String formattedHearingInformation = service.getFormattedHearingInformation(HEARING);
        assertThat(formattedHearingInformation, is("Venue 1 12:00:00 18 May 2021"));
    }

    @Test
    @Parameters({"APPELLANT", "DWP", "JOINT_PARTY"})
    public void getRequestStatusWithNoHearingRecordingRequests_willReturnEmpty(PartyItemList party) {
        SscsCaseData sscsCaseData = SscsCaseData.builder().hearings(newArrayList(HEARING)).build();
        final Optional<RequestStatus> requestStatus = service.getRequestStatus(party, null, HEARING, sscsCaseData);
        assertThat(requestStatus, is(Optional.empty()));
    }

    @Test
    @Parameters({"APPELLANT", "DWP", "JOINT_PARTY"})
    public void getRequestStatusWithHearingRecordingRequests_willReturnRequested(PartyItemList party) {
        final SscsHearingRecordingCaseData sscsHearingRecordingCaseData = SscsHearingRecordingCaseData.builder()
                .requestedHearings(newArrayList(getHearingRecordingRequest(party, REQUESTED)))
                .build();
        SscsCaseData sscsCaseData = SscsCaseData.builder().sscsHearingRecordingCaseData(sscsHearingRecordingCaseData).hearings(newArrayList(HEARING)).build();
        final Optional<RequestStatus> requestStatus = service.getRequestStatus(party, null, HEARING, sscsCaseData);
        assertThat(requestStatus, is(Optional.of(REQUESTED)));
    }

    @Test
    @Parameters({"APPELLANT", "DWP", "JOINT_PARTY"})
    public void getRequestStatusWithHearingRecordingGranted_willReturnGranted(PartyItemList party) {
        final SscsHearingRecordingCaseData sscsHearingRecordingCaseData = SscsHearingRecordingCaseData.builder()
                .requestedHearings(newArrayList(getHearingRecordingRequest(party, REQUESTED)))
                .dwpReleasedHearings(newArrayList(getHearingRecordingRequest(PartyItemList.DWP, GRANTED)))
                .citizenReleasedHearings(newArrayList(getHearingRecordingRequest(PartyItemList.JOINT_PARTY, GRANTED), getHearingRecordingRequest(PartyItemList.APPELLANT, GRANTED)))
                .build();

        SscsCaseData sscsCaseData = SscsCaseData.builder()
                .jointParty(JointParty.builder().hasJointParty(YES).build())
                .sscsHearingRecordingCaseData(sscsHearingRecordingCaseData)
                .hearings(newArrayList(HEARING)).build();
        final Optional<RequestStatus> requestStatus = service.getRequestStatus(party, null, HEARING, sscsCaseData);
        assertThat(requestStatus, is(Optional.of(GRANTED)));
    }

    @Test
    @Parameters({"APPELLANT", "DWP", "JOINT_PARTY"})
    public void getRequestStatusWithHearingRecordingRefused_willReturnRefused(PartyItemList party) {
        final SscsHearingRecordingCaseData sscsHearingRecordingCaseData = SscsHearingRecordingCaseData.builder()
                .requestedHearings(newArrayList(getHearingRecordingRequest(party, REQUESTED)))
                .refusedHearings(newArrayList(getHearingRecordingRequest(PartyItemList.DWP, GRANTED),
                        getHearingRecordingRequest(PartyItemList.JOINT_PARTY, GRANTED),
                        getHearingRecordingRequest(PartyItemList.APPELLANT, GRANTED)))
                .build();

        SscsCaseData sscsCaseData = SscsCaseData.builder()
                .jointParty(JointParty.builder().hasJointParty(YES).build())
                .sscsHearingRecordingCaseData(sscsHearingRecordingCaseData)
                .hearings(newArrayList(HEARING)).build();
        final Optional<RequestStatus> requestStatus = service.getRequestStatus(party, null, HEARING, sscsCaseData);
        assertThat(requestStatus, is(Optional.of(REFUSED)));
    }

    @Test
    @Parameters({
        "APPELLANT, GRANTED", "APPELLANT, REFUSED", "APPELLANT, REQUESTED",
        "DWP, GRANTED",  "DWP, REFUSED", "DWP, REQUESTED",
        "JOINT_PARTY, GRANTED", "JOINT_PARTY, REFUSED", "JOINT_PARTY, REQUESTED"})
    public void getChangedRequestStatus(PartyItemList party, RequestStatus status) {
        final Optional<RequestStatus> changedRequestStatus = service.getChangedRequestStatus(party, null, processHearingRecordingRequests(status), null);
        assertThat(changedRequestStatus.isPresent(), is(true));
        assertThat(changedRequestStatus.get(), is(status));
    }

    @Test
    public void whenHearingRecordingRequestHasNoHearingRecordings_thenDoNotThrowAnError() {
        final HearingRecordingRequest hearingRecordingRequest = getHearingRecordingRequest(PartyItemList.APPELLANT, REQUESTED);

        hearingRecordingRequest.getValue().setSscsHearingRecording(null);
        final SscsHearingRecordingCaseData sscsHearingRecordingCaseData = SscsHearingRecordingCaseData.builder()
                .requestedHearings(newArrayList(hearingRecordingRequest))
                .build();
        SscsCaseData sscsCaseData = SscsCaseData.builder().sscsHearingRecordingCaseData(sscsHearingRecordingCaseData).hearings(newArrayList(HEARING)).build();
        final Optional<RequestStatus> requestStatus = service.getRequestStatus(PartyItemList.APPELLANT, null, HEARING, sscsCaseData);
        assertThat(requestStatus, is(Optional.empty()));
    }

    private ProcessHearingRecordingRequest processHearingRecordingRequests(RequestStatus status) {
        return ProcessHearingRecordingRequest.builder()
                    .hearingId(HEARING.getValue().getHearingId())
                    .jointParty(dynamicList(status))
                    .dwp(dynamicList(status))
                    .appellant(dynamicList(status))
                    .jointParty(dynamicList(status))
                .build();
    }

    private DynamicList dynamicList(RequestStatus value) {
        return new DynamicList(new DynamicListItem(value.getLabel(), value.getLabel()),
                List.of(GRANTED, REQUESTED, REFUSED).stream()
                        .map(status -> new DynamicListItem(status.getLabel(), status.getLabel()))
                        .collect(Collectors.toList()));
    }

    private HearingRecordingRequest getHearingRecordingRequest(PartyItemList party, RequestStatus status) {
        SscsHearingRecordingDetails sscsHearingRecording = SscsHearingRecordingDetails.builder()
                .hearingId(HEARING.getValue().getHearingId())
                .venue(HEARING.getValue().getVenue().getName())
                .build();
        return HearingRecordingRequest.builder().value(HearingRecordingRequestDetails.builder()
                .dateRequested(LocalDate.now().toString())
                .sscsHearingRecording(sscsHearingRecording)
                .requestingParty(party.getCode()).build()).build();
    }

    private static Hearing getHearing() {
        HearingDetails hearingDetails = HearingDetails.builder()
                .hearingId("1")
                .hearingDate("2021-05-18")
                .time("12:00")
                .venue(Venue.builder().name("Venue 1").build())
                .build();
        return Hearing.builder().value(hearingDetails).build();
    }
}
