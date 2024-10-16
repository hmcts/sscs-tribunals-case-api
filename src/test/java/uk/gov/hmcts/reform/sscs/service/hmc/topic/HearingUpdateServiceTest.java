package uk.gov.hmcts.reform.sscs.service.hmc.topic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus.ADJOURNED;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus.LISTED;

import com.google.common.collect.Lists;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.exception.InvalidHearingDataException;
import uk.gov.hmcts.reform.sscs.exception.InvalidMappingException;
import uk.gov.hmcts.reform.sscs.model.VenueDetails;
import uk.gov.hmcts.reform.sscs.model.client.JudicialUserBase;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus;
import uk.gov.hmcts.reform.sscs.model.single.hearing.CaseDetails;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingDaySchedule;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingGetResponse;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingResponse;
import uk.gov.hmcts.reform.sscs.model.single.hearing.RequestDetails;
import uk.gov.hmcts.reform.sscs.service.JudicialRefDataService;
import uk.gov.hmcts.reform.sscs.service.VenueService;

@ExtendWith(MockitoExtension.class)
class HearingUpdateServiceTest {
    public static final LocalDateTime HEARING_START_DATE_TIME = LocalDateTime.now();
    public static final LocalDateTime HEARING_END_DATE_TIME = HEARING_START_DATE_TIME.plusHours(2);
    public static final String HEARING_DATE = HEARING_START_DATE_TIME.toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    public static final String HEARING_TIME = HEARING_START_DATE_TIME.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
    private static final Long HEARING_ID = 789L;
    private static final String EPIMS_ID = "123";
    private static final String NEW_EPIMS_ID = "456";
    private static final String CASE_ID = "777";
    private static final String VENUE_NAME = "VenueName";
    private static final String JUDGE_ID = "1";
    private static final List<String> PANEL_IDS = List.of("2", "3");
    private static final String HEARING_TIME_STR = "10:00:00";

    private VenueDetails venueDetails;
    private SscsCaseData caseData;
    private HearingGetResponse hearingGetResponse;
    public LocalDateTime zoneUtcStartDateTime;
    public LocalDateTime zoneUkStartDateTime;
    public LocalDateTime zoneUtcEndDateTime;
    public LocalDateTime zoneUkEndDateTime;

    @Mock
    private VenueService venueService;

    @Mock
    private JudicialRefDataService judicialRefDataService;

    private HearingUpdateService hearingUpdateService;

    @BeforeEach
    void setUp() {
        hearingUpdateService = new HearingUpdateService(venueService, judicialRefDataService);
        ReflectionTestUtils.setField(hearingUpdateService, "isPostHearingsEnabled", true);

        hearingGetResponse = HearingGetResponse.builder()
                .hearingResponse(HearingResponse.builder().build())
                .requestDetails(RequestDetails.builder()
                        .hearingRequestId(String.valueOf(HEARING_ID))
                        .build())
                .hearingDetails(uk.gov.hmcts.reform.sscs.model.single.hearing.HearingDetails.builder().build())
                .caseDetails(CaseDetails.builder().build())
                .partyDetails(new ArrayList<>())
                .hearingResponse(HearingResponse.builder().build())
                .build();

        caseData = SscsCaseData.builder()
                .ccdCaseId(CASE_ID)
                .build();

        venueDetails = VenueDetails.builder()
                .venueId(NEW_EPIMS_ID)
                .venName(VENUE_NAME)
                .regionalProcessingCentre("regionalProcessingCentre")
                .build();

        zoneUtcStartDateTime = HEARING_START_DATE_TIME.atZone(ZoneId.of("UTC")).toLocalDateTime();
        zoneUkStartDateTime = hearingUpdateService.convertUtcToUk(zoneUtcStartDateTime);
        zoneUtcEndDateTime = HEARING_END_DATE_TIME.atZone(ZoneId.of("UTC")).toLocalDateTime();
        zoneUkEndDateTime = hearingUpdateService.convertUtcToUk(zoneUtcEndDateTime);
    }

    @DisplayName("When caseData with a hearing that matches one from hearingGetResponse is given,"
            + "updateHearing updates the correct hearing")
    @Test
    void testUpdateHearing() throws Exception {
        hearingGetResponse.getHearingResponse().setHearingSessions(List.of(
                        HearingDaySchedule.builder()
                            .hearingStartDateTime(zoneUtcStartDateTime)
                            .hearingEndDateTime(zoneUtcEndDateTime)
                            .hearingVenueEpimsId(NEW_EPIMS_ID)
                            .hearingJudgeId(JUDGE_ID)
                            .panelMemberIds(PANEL_IDS)
                            .build()));

        caseData.setHearings(Lists.newArrayList(
                Hearing.builder()
                        .value(HearingDetails.builder()
                                .epimsId(EPIMS_ID)
                                .hearingId(String.valueOf(HEARING_ID))
                                .build())
                        .build(),
                Hearing.builder()
                        .value(HearingDetails.builder()
                                .epimsId("23453")
                                .hearingId("35533")
                                .build())
                        .build()));

        given(judicialRefDataService.getJudicialUserFromPersonalCode(any())).willReturn(new JudicialUserBase("idamId", "personalCode"));
        when(venueService.getVenueDetailsForActiveVenueByEpimsId(NEW_EPIMS_ID)).thenReturn(venueDetails);

        // when
        hearingUpdateService.updateHearing(hearingGetResponse, caseData);

        // then
        List<Hearing> hearings = caseData.getHearings();
        assertThat(hearings)
            .hasSize(2)
            .extracting(Hearing::getValue)
            .filteredOn("hearingId", String.valueOf(HEARING_ID))
            .hasSize(1)
            .extracting("epimsId","start","end","hearingDate","time")
            .contains(tuple(NEW_EPIMS_ID, zoneUkStartDateTime, zoneUkEndDateTime, HEARING_DATE, HEARING_TIME));

        assertThat(hearings)
            .extracting(Hearing::getValue)
            .filteredOn("hearingId", String.valueOf(HEARING_ID))
            .extracting(HearingDetails::getVenue)
            .extracting("name")
            .containsOnly(VENUE_NAME);

        assertThat(hearings.get(0).getValue().getPanel().getAllPanelMembers().size()).isEqualTo(3);
    }

    @DisplayName("When caseData with no hearing that matches one from hearingGetResponse is given,"
            + "updateHearing adds the correct hearing")
    @Test
    void testUpdateHearingNewHearing() throws Exception {
        hearingGetResponse.getHearingResponse().setHearingSessions(List.of(
                HearingDaySchedule.builder()
                        .hearingStartDateTime(zoneUtcStartDateTime)
                        .hearingEndDateTime(zoneUtcEndDateTime)
                        .hearingVenueEpimsId(NEW_EPIMS_ID)
                        .build()));

        caseData.setHearings(Lists.newArrayList(
                Hearing.builder()
                        .value(HearingDetails.builder()
                                .epimsId("23453")
                                .hearingId("35533")
                                .build())
                        .build()));


        when(venueService.getVenueDetailsForActiveVenueByEpimsId(NEW_EPIMS_ID)).thenReturn(venueDetails);

        // when
        hearingUpdateService.updateHearing(hearingGetResponse, caseData);

        // then
        List<Hearing> hearings = caseData.getHearings();
        assertThat(hearings)
            .hasSize(2)
            .extracting(Hearing::getValue)
            .filteredOn("hearingId", String.valueOf(HEARING_ID))
            .hasSize(1)
            .extracting("epimsId","start","end","hearingDate","time")
            .contains(tuple(NEW_EPIMS_ID, zoneUkStartDateTime, zoneUkEndDateTime, HEARING_DATE, HEARING_TIME));

        assertThat(hearings)
            .extracting(Hearing::getValue)
            .filteredOn("hearingId", String.valueOf(HEARING_ID))
            .extracting(HearingDetails::getVenue)
            .extracting("name")
            .containsOnly(VENUE_NAME);
    }

    @DisplayName("When hearingGetResponse with multiple Hearing Sessions are given,"
            + "updateHearing throws the correct error and message")
    @Test
    void testUpdateHearingMultipleHearingSessions() {
        hearingGetResponse.getHearingResponse().setHearingSessions(List.of(
                HearingDaySchedule.builder().build(),
                HearingDaySchedule.builder().build()));

        assertThatExceptionOfType(InvalidHearingDataException.class)
                .isThrownBy(() -> hearingUpdateService.updateHearing(hearingGetResponse, caseData))
                .withMessageContaining("Invalid HearingDaySchedule, should have 1 session but instead has 2 sessions");
    }

    @DisplayName("When a invalid Epims ID is given, "
            + "updateHearing throws the correct error and message")
    @Test
    void testUpdateHearingVenueNull() {
        hearingGetResponse.getHearingResponse().setHearingSessions(List.of(
                HearingDaySchedule.builder()
                        .hearingStartDateTime(zoneUtcStartDateTime)
                        .hearingEndDateTime(zoneUtcEndDateTime)
                        .hearingVenueEpimsId(NEW_EPIMS_ID)
                        .build()));

        caseData.setHearings(Lists.newArrayList(
                Hearing.builder()
                        .value(HearingDetails.builder()
                                .epimsId("23453")
                                .hearingId("35533")
                                .build())
                        .build()));


        when(venueService.getVenueDetailsForActiveVenueByEpimsId(NEW_EPIMS_ID)).thenReturn(null);

        assertThatExceptionOfType(InvalidMappingException.class)
                .isThrownBy(() -> hearingUpdateService.updateHearing(hearingGetResponse, caseData))
                .withMessageContaining("Invalid epims Id %s, unable to find active venue with that id, regarding Case Id %s", NEW_EPIMS_ID, CASE_ID);
    }

    @DisplayName("When a valid HmcStatus and casedata with a matching hearing is given, setHearingStatus updates the hearing with the correct status")
    @ParameterizedTest
    @EnumSource(
        value = HmcStatus.class,
        mode = EnumSource.Mode.EXCLUDE,
        names = {"UPDATE_REQUESTED", "UPDATE_SUBMITTED", "CANCELLATION_REQUESTED", "CANCELLATION_SUBMITTED", "NOT_FOUND"})
    void testSetHearingStatus(HmcStatus value) {

        caseData.setHearings(Lists.newArrayList(
            Hearing.builder()
                .value(HearingDetails.builder()
                    .hearingStatus(HearingStatus.EXCEPTION)
                    .hearingId(String.valueOf(HEARING_ID))
                    .build())
                .build()));

        hearingUpdateService.setHearingStatus(String.valueOf(HEARING_ID), caseData, value);

        assertThat(caseData.getHearings().get(0).getValue().getHearingStatus()).isEqualTo(value.getHearingStatus());
    }

    @DisplayName("When a HmcStatus with a null HearingStatus is given, setHearingStatus does not update a hearing")
    @ParameterizedTest
    @EnumSource(
        value = HmcStatus.class,
        mode = EnumSource.Mode.INCLUDE,
        names = {"UPDATE_REQUESTED", "UPDATE_SUBMITTED"})
    void testSetHearingStatusNullHearingStatus(HmcStatus value) {

        caseData.setHearings(Lists.newArrayList(
            Hearing.builder()
                .value(HearingDetails.builder()
                    .hearingStatus(HearingStatus.EXCEPTION)
                    .hearingId(String.valueOf(HEARING_ID))
                    .build())
                .build()));

        hearingUpdateService.setHearingStatus(String.valueOf(HEARING_ID), caseData, value);

        assertThat(caseData.getHearings().get(0).getValue().getHearingStatus()).isEqualTo(HearingStatus.EXCEPTION);
    }

    @DisplayName("When caseData with a empty Hearings is given, setHearingStatus returns a empty Hearings")
    @Test
    void testSetHearingStatusNoHearing() {

        caseData.setHearings(List.of());

        hearingUpdateService.setHearingStatus(String.valueOf(HEARING_ID), caseData, LISTED);

        assertThat(caseData.getHearings()).isEmpty();
    }

    @DisplayName("When HmcStatus is listed, the caseData DwpState should be set as hearingDataIssued")
    @Test
    void testSetDwpState() {
        DwpState resolvedState = hearingUpdateService.resolveDwpState(LISTED);

        assertThat(resolvedState).isEqualTo(DwpState.HEARING_DATE_ISSUED);
    }

    @DisplayName("When HmcStatus is null, the caseData DwpState will not be updated and remain null")
    @Test
    void testSetDwpStateWhenNullHmcStatus() {
        DwpState resolvedState = hearingUpdateService.resolveDwpState(null);

        assertThat(resolvedState).isNull();
    }

    @DisplayName("When a HmcStatus with a listing and the hearing has a valid start date, setWorkBasketFields updates setHearingDate to the correct date")
    @Test
    void testSetWorkBasketFields() {
        caseData.setHearings(Lists.newArrayList(
            Hearing.builder()
                .value(HearingDetails.builder()
                    .hearingId(String.valueOf(HEARING_ID))
                    .start(zoneUtcStartDateTime)
                    .time(HEARING_TIME_STR)
                    .epimsId(EPIMS_ID)
                    .build())
                .build()));

        hearingUpdateService.setWorkBasketFields(String.valueOf(HEARING_ID), caseData, LISTED);

        assertThat(caseData.getWorkBasketFields().getHearingDate()).isEqualTo(zoneUtcStartDateTime.toLocalDate());
        assertThat(caseData.getWorkBasketFields().getHearingEpimsId()).isEqualTo(EPIMS_ID);
    }

    @DisplayName("When a HmcStatus with a listing and the hearing has a valid epims id, setWorkBasketFields updates getHearingEpimsId to the correct epims id")
    @Test
    void testSetWorkBasketFieldsEpims() {
        caseData.setHearings(Lists.newArrayList(
            Hearing.builder()
                .value(HearingDetails.builder()
                    .hearingId(String.valueOf(HEARING_ID))
                    .epimsId(EPIMS_ID)
                    .start(zoneUtcStartDateTime)
                    .time(HEARING_TIME_STR)
                    .build())
                .build()));

        hearingUpdateService.setWorkBasketFields(String.valueOf(HEARING_ID), caseData, LISTED);

        assertThat(caseData.getWorkBasketFields().getHearingEpimsId()).isEqualTo(EPIMS_ID);
    }


    @DisplayName("When a HmcStatus with a listing and the hearing has a valid start date, setWorkBasketFields updates setHearingDate to the correct date")
    @Test
    void testSetWorkBasketFieldsStartDate() {
        caseData.setHearings(Lists.newArrayList(
            Hearing.builder()
                .value(HearingDetails.builder()
                    .hearingId(String.valueOf(HEARING_ID))
                    .start(zoneUtcStartDateTime)
                    .time(HEARING_TIME_STR)
                    .build())
                .build()));

        hearingUpdateService.setWorkBasketFields(String.valueOf(HEARING_ID), caseData, LISTED);

        assertThat(caseData.getWorkBasketFields().getHearingDate()).isEqualTo(zoneUtcStartDateTime.toLocalDate());
        assertThat(caseData.getWorkBasketFields().getHearingEpimsId()).isNull();
    }

    @DisplayName("When a HmcStatus with no listing is given, setWorkBasketFields updates getHearingEpimsId to null")
    @Test
    void testSetWorkBasketFieldsInvalidStatus() {
        caseData.setHearings(Lists.newArrayList(
            Hearing.builder()
                .value(HearingDetails.builder()
                    .hearingId(String.valueOf(HEARING_ID))
                    .epimsId(EPIMS_ID)
                    .build())
                .build()));

        hearingUpdateService.setWorkBasketFields(String.valueOf(HEARING_ID), caseData, ADJOURNED);

        assertThat(caseData.getWorkBasketFields().getHearingDate()).isNull();
        assertThat(caseData.getWorkBasketFields().getHearingEpimsId()).isNull();
    }

    @DisplayName("When a HmcStatus with a listing and the hearing has a valid start date, setWorkBasketFields updates hearing date issued to the correct date")
    @Test
    void testSetWorkBasketFieldsForHearingDateIssued() {
        caseData.setHearings(Lists.newArrayList(
            Hearing.builder()
                .value(HearingDetails.builder()
                           .hearingId(String.valueOf(HEARING_ID))
                           .start(zoneUtcStartDateTime)
                           .time(HEARING_TIME_STR)
                           .epimsId(EPIMS_ID)
                           .build())
                .build()));

        hearingUpdateService.setWorkBasketFields(String.valueOf(HEARING_ID), caseData, LISTED);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String expectedHearingDateIssued = zoneUtcStartDateTime.format(formatter);

        assertThat(caseData.getWorkBasketFields().getHearingDateIssued()).isEqualTo(expectedHearingDateIssued);
    }

    @DisplayName("When a hearing with a valid start is given, getHearingDate returns the correct date")
    @Test
    void testGetHearingDate() {
        caseData.setHearings(Lists.newArrayList(
            Hearing.builder()
                .value(HearingDetails.builder()
                    .hearingId(String.valueOf(HEARING_ID))
                    .start(zoneUtcStartDateTime)
                    .build())
                .build()));

        LocalDate result = hearingUpdateService.getHearingDate(String.valueOf(HEARING_ID), caseData);

        assertThat(result).isEqualTo(zoneUtcStartDateTime.toLocalDate());
    }

    @DisplayName("When caseData with no hearing is given, getHearingDate returns null")
    @Test
    void testGetHearingDateNoHearing() {
        caseData.setHearings(List.of());

        LocalDate result = hearingUpdateService.getHearingDate(String.valueOf(HEARING_ID), caseData);

        assertThat(result).isNull();
    }

    @DisplayName("When caseData with a hearing but the start is null, getHearingDate returns null")
    @Test
    void testGetHearingDateNullStart() {
        caseData.setHearings(Lists.newArrayList(
            Hearing.builder()
                .value(HearingDetails.builder()
                    .hearingId(String.valueOf(HEARING_ID))
                    .build())
                .build()));

        LocalDate result = hearingUpdateService.getHearingDate(String.valueOf(HEARING_ID), caseData);

        assertThat(result).isNull();
    }

    @DisplayName("When a hearing with a valid epims Id is given, getHearingEpimsId returns the epims Id")
    @Test
    void testGetHearingEpims() {
        caseData.setHearings(Lists.newArrayList(
            Hearing.builder()
                .value(HearingDetails.builder()
                    .hearingId(String.valueOf(HEARING_ID))
                    .epimsId(EPIMS_ID)
                    .build())
                .build()));

        String result = hearingUpdateService.getHearingEpimsId(String.valueOf(HEARING_ID), caseData);

        assertThat(result).isEqualTo(EPIMS_ID);
    }

    @DisplayName("When caseData with no hearing is given, getHearingEpimsId returns null")
    @Test
    void testGetHearingEpimsIdNoHearing() {
        caseData.setHearings(List.of());

        String result = hearingUpdateService.getHearingEpimsId(String.valueOf(HEARING_ID), caseData);

        assertThat(result).isNull();
    }

    @DisplayName("When caseData with a hearing but the epimds Id is null, getHearingEpimsId returns null")
    @Test
    void testGetHearingEpimsNullStart() {
        caseData.setHearings(Lists.newArrayList(
            Hearing.builder()
                .value(HearingDetails.builder()
                    .hearingId(String.valueOf(HEARING_ID))
                    .build())
                .build()));

        String result = hearingUpdateService.getHearingEpimsId(String.valueOf(HEARING_ID), caseData);

        assertThat(result).isNull();
    }

    @DisplayName("When a HmcStatus with LISTED or UPDATE_SUBMITTED given, isCaseListed returns true")
    @ParameterizedTest
    @EnumSource(
        value = HmcStatus.class,
        mode = EnumSource.Mode.INCLUDE,
        names = {"LISTED"})
    void testIsCaseListed(HmcStatus value) {
        boolean result = hearingUpdateService.isCaseListed(value);

        assertThat(result).isTrue();
    }

    @DisplayName("When a HmcStatus not LISTED or UPDATE_SUBMITTED given, isCaseListed returns false")
    @ParameterizedTest
    @EnumSource(
        value = HmcStatus.class,
        mode = EnumSource.Mode.EXCLUDE,
        names = {"LISTED"})
    @NullSource
    void testIsCaseListedInvalid(HmcStatus value) {
        boolean result = hearingUpdateService.isCaseListed(value);

        assertThat(result).isFalse();
    }

    @DisplayName("LA LocalDateTime properly converted from UTC to UK value")
    @Test
    void testUtcToUkDateTimeConvert() {
        var utcDateTime = ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime();
        var ukDateTime = ZonedDateTime.now(ZoneId.of("Europe/London")).toLocalDateTime();

        assertThat(ukDateTime.getHour()).isEqualTo(hearingUpdateService.convertUtcToUk(utcDateTime).getHour());
    }
}
