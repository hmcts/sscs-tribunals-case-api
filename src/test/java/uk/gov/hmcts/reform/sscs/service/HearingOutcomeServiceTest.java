package uk.gov.hmcts.reform.sscs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Venue;
import uk.gov.hmcts.reform.sscs.model.VenueDetails;
import uk.gov.hmcts.reform.sscs.model.multi.hearing.CaseHearing;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingDaySchedule;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;
import uk.gov.hmcts.reform.sscs.service.hmc.topic.HearingUpdateService;

@ExtendWith(MockitoExtension.class)
public class HearingOutcomeServiceTest {

    @Mock
    private VenueService venueService;
    @Mock
    private  HearingUpdateService hearingUpdateService;
    private HearingOutcomeService hearingOutcomeService;
    public static final String EPIMS_ID = "12";
    private static final String VENUE_NAME = "VenueName";
    private static final String RPC = "regionalProcessingCentre";
    public static final LocalDateTime HEARING_START_DATE_TIME = LocalDateTime.now();
    public static final LocalDateTime HEARING_END_DATE_TIME = HEARING_START_DATE_TIME.plusHours(2);

    @Before
    public void setup() {
        openMocks(this);
        hearingOutcomeService = new HearingOutcomeService(hearingUpdateService, venueService);
    }

    @Test
    public void mapCaseHearingToHearing_ShouldReturnCorrectHearingObject() {
        VenueDetails venueDetails = VenueDetails.builder()
                .venueId(EPIMS_ID)
                .venName(VENUE_NAME)
                .regionalProcessingCentre(RPC)
                .build();
        CaseHearing caseHearing = CaseHearing.builder()
                .hearingId(1L)
                .hearingChannels(List.of(HearingChannel.FACE_TO_FACE))
                .hearingDaySchedule(List.of(HearingDaySchedule.builder()
                        .hearingVenueEpimsId(EPIMS_ID)
                        .hearingStartDateTime(HEARING_START_DATE_TIME)
                        .hearingEndDateTime(HEARING_END_DATE_TIME)
                        .build()))
                .build();
        when(venueService.getVenueDetailsForActiveVenueByEpimsId(any())).thenReturn(venueDetails);
        when(hearingUpdateService.convertUtcToUk(HEARING_START_DATE_TIME)).thenReturn(HEARING_START_DATE_TIME);
        when(hearingUpdateService.convertUtcToUk(HEARING_END_DATE_TIME)).thenReturn(HEARING_END_DATE_TIME);
        Hearing result = hearingOutcomeService.mapCaseHearingToHearing(caseHearing);
        assertEquals("1", result.getValue().getHearingId());
        assertEquals(VENUE_NAME, result.getValue().getVenue().getName());
        assertEquals(HEARING_START_DATE_TIME, result.getValue().getStart());
        assertEquals(HEARING_END_DATE_TIME, result.getValue().getEnd());

    }

    @Test
    public void mapCaseHearingToHearing_ShouldHandleNullFields() {
        CaseHearing caseHearing = CaseHearing.builder()
                .hearingId(1L)
                .hearingDaySchedule(List.of(HearingDaySchedule.builder().build()))
                .build();
        Hearing result = hearingOutcomeService.mapCaseHearingToHearing(caseHearing);
        assertEquals("1", result.getValue().getHearingId());
        assertEquals(null, result.getValue().getVenue().getName());
        assertEquals(null, result.getValue().getStart());
        assertEquals(null, result.getValue().getEnd());
    }

    @Test
    public void setHearingOutcomeCompletedHearings_ShouldReturnCorrectDynamicList() {
        List<Hearing> hearings = List.of(Hearing.builder()
                .value(HearingDetails.builder()
                        .hearingId("1")
                        .start(HEARING_START_DATE_TIME)
                        .end(HEARING_END_DATE_TIME)
                        .venue(Venue.builder().name(VENUE_NAME).build())
                        .build())
                .build());
        DynamicList result = hearingOutcomeService.setHearingOutcomeCompletedHearings(hearings);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm", Locale.ENGLISH);
        String hearingLabel = HEARING_START_DATE_TIME.format(formatter)
                + "-" + HEARING_END_DATE_TIME.toLocalTime()
                + ", " + VENUE_NAME;
        assertEquals(1, result.getListItems().size());
        assertEquals("1", result.getListItems().get(0).getCode());
        assertEquals(hearingLabel, result.getListItems().get(0).getLabel());
    }

    @Test
    public void emptyVenueInHearingOutcomeCompletedHearings_ShouldReturnCorrectDynamicList() {
        List<Hearing> hearings = List.of(Hearing.builder()
                        .value(HearingDetails.builder()
                                .hearingId("1")
                                .start(HEARING_START_DATE_TIME)
                                .end(HEARING_END_DATE_TIME)
                                .venue(Venue.builder().name(VENUE_NAME).build())
                                .build())
                        .build(),
                Hearing.builder()
                        .value(HearingDetails.builder()
                                .hearingId("2")
                                .start(HEARING_START_DATE_TIME)
                                .end(HEARING_END_DATE_TIME)
                                .venue(Venue.builder().build())
                                .build())
                        .build());
        DynamicList result = hearingOutcomeService.setHearingOutcomeCompletedHearings(hearings);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm", Locale.ENGLISH);
        final String hearingLabel = HEARING_START_DATE_TIME.format(formatter)
                + "-" + HEARING_END_DATE_TIME.toLocalTime()
                + ", " + VENUE_NAME;
        final String hearingLabelNoVenue = HEARING_START_DATE_TIME.format(formatter)
                + "-" + HEARING_END_DATE_TIME.toLocalTime();
        assertEquals(2, result.getListItems().size());
        assertEquals("1", result.getListItems().get(0).getCode());
        assertEquals("2", result.getListItems().get(1).getCode());
        assertEquals(hearingLabel, result.getListItems().get(0).getLabel());
        assertEquals(hearingLabelNoVenue, result.getListItems().get(1).getLabel());
    }

}
