package uk.gov.hmcts.reform.sscs.service.processhearingrecordingrequest;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.*;
import static uk.gov.hmcts.reform.sscs.model.RequestStatus.GRANTED;
import static uk.gov.hmcts.reform.sscs.model.RequestStatus.REFUSED;
import static uk.gov.hmcts.reform.sscs.model.RequestStatus.REQUESTED;

import java.time.LocalDate;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRecordingRequest;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRecordingRequestDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsHearingRecording;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsHearingRecordingCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsHearingRecordingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Venue;
import uk.gov.hmcts.reform.sscs.model.PartyItemList;
import uk.gov.hmcts.reform.sscs.model.RequestStatus;

@RunWith(JUnitParamsRunner.class)
public class ProcessHearingRecordingRequestServiceTest {

    private final ProcessHearingRecordingRequestService service = new ProcessHearingRecordingRequestService();
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
        final Optional<RequestStatus> requestStatus = service.getRequestStatus(party, HEARING, sscsCaseData);
        assertThat(requestStatus, is(Optional.empty()));
    }

    @Test
    @Parameters({"APPELLANT", "DWP", "JOINT_PARTY"})
    public void getRequestStatusWithHearingRecordingRequests_willReturnRequested(PartyItemList party) {
        final SscsHearingRecordingCaseData sscsHearingRecordingCaseData = SscsHearingRecordingCaseData.builder()
                .requestedHearings(newArrayList(getHearingRecordingRequest(party, REQUESTED)))
                .build();
        SscsCaseData sscsCaseData = SscsCaseData.builder().sscsHearingRecordingCaseData(sscsHearingRecordingCaseData).hearings(newArrayList(HEARING)).build();
        final Optional<RequestStatus> requestStatus = service.getRequestStatus(party, HEARING, sscsCaseData);
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
                .jointParty(YES.getValue())
                .sscsHearingRecordingCaseData(sscsHearingRecordingCaseData)
                .hearings(newArrayList(HEARING)).build();
        final Optional<RequestStatus> requestStatus = service.getRequestStatus(party, HEARING, sscsCaseData);
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
                .jointParty(YES.getValue())
                .sscsHearingRecordingCaseData(sscsHearingRecordingCaseData)
                .hearings(newArrayList(HEARING)).build();
        final Optional<RequestStatus> requestStatus = service.getRequestStatus(party, HEARING, sscsCaseData);
        assertThat(requestStatus, is(Optional.of(REFUSED)));
    }

    private HearingRecordingRequest getHearingRecordingRequest(PartyItemList party, RequestStatus status) {
        SscsHearingRecording sscsHearingRecording = SscsHearingRecording.builder().value(SscsHearingRecordingDetails.builder()
                .hearingId(HEARING.getValue().getHearingId())
                .venue(HEARING.getValue().getVenue().getName())
                .build()).build();
        return HearingRecordingRequest.builder().value(HearingRecordingRequestDetails.builder()
                .dateRequested(LocalDate.now().toString())
                .sscsHearingRecordingList(newArrayList(sscsHearingRecording))
                .status(status.name())
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
