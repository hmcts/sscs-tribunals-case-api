package uk.gov.hmcts.reform.sscs.ccd.presubmit.addhearingoutcome;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.time.LocalDateTime;
import java.util.List;
import junitparams.JUnitParamsRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.addhearingoutcome.AddHearingOutcomeAboutToStartHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcomeValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Venue;
import uk.gov.hmcts.reform.sscs.model.VenueDetails;
import uk.gov.hmcts.reform.sscs.model.multi.hearing.CaseHearing;
import uk.gov.hmcts.reform.sscs.model.multi.hearing.HearingsGetResponse;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingDaySchedule;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;
import uk.gov.hmcts.reform.sscs.service.HearingOutcomeService;
import uk.gov.hmcts.reform.sscs.service.HmcHearingApiService;
import uk.gov.hmcts.reform.sscs.service.VenueService;
import uk.gov.hmcts.reform.sscs.service.hmc.topic.HearingUpdateService;


@RunWith(JUnitParamsRunner.class)
public class AddHearingOutcomeAboutToStartHandlerTest {
    private AddHearingOutcomeAboutToStartHandler handler;
    private static final String USER_AUTHORISATION = "Bearer token";
    public static final LocalDateTime HEARING_START_DATE_TIME = LocalDateTime.now();
    public static final LocalDateTime HEARING_END_DATE_TIME = HEARING_START_DATE_TIME.plusHours(2);
    public static final String EPIMS_ID_1 = "12";
    public static final String EPIMS_ID_2 = "34";

    public static final String VENUE_NAME = "venueName";
    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    @Mock
    private HmcHearingApiService hmcHearingApiService;
    @Mock
    private HearingUpdateService hearingUpdateService;
    @Mock
    private VenueService venueService;
    @Mock
    private HearingOutcomeService hearingOutcomeService;
    private SscsCaseData sscsCaseData;

    @BeforeEach
    void setup() {
        openMocks(this);
        handler = new AddHearingOutcomeAboutToStartHandler(hmcHearingApiService, hearingUpdateService, venueService, hearingOutcomeService);
        when(callback.getEvent()).thenReturn(EventType.ADD_HEARING_OUTCOME);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder().build()).hearingOutcomeValue(HearingOutcomeValue.builder().build()).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    void givenAddHearingOutcomeEventShouldHandle() {
        assertThat(handler.canHandle(CallbackType.ABOUT_TO_START,callback)).isTrue();
    }

    @Test
    void givenCompletedHearingOnCase_ThenPopulateDropdown() {
        when(venueService.getVenueDetailsForActiveVenueByEpimsId(EPIMS_ID_1)).thenReturn(VenueDetails.builder()
            .epimsId(EPIMS_ID_1)
            .venName("venueName")
            .build());
        when(hearingUpdateService.convertUtcToUk(HEARING_START_DATE_TIME)).thenReturn(HEARING_START_DATE_TIME);
        when(hearingUpdateService.convertUtcToUk(HEARING_END_DATE_TIME)).thenReturn(HEARING_END_DATE_TIME);
        when(hmcHearingApiService.getHearingsRequest(any(),any())).thenReturn(
            HearingsGetResponse.builder().caseHearings(List.of(CaseHearing.builder()
                    .hearingId(1L)
                    .hearingChannels(List.of(HearingChannel.FACE_TO_FACE))
                    .hearingDaySchedule(List.of(HearingDaySchedule.builder()
                        .hearingVenueEpimsId(EPIMS_ID_1)
                        .hearingStartDateTime(HEARING_START_DATE_TIME)
                        .hearingEndDateTime(HEARING_END_DATE_TIME)
                        .build()))
                    .build()))
                .build());
        when(hearingOutcomeService.mapCaseHearingToHearing(any())).thenReturn(
                Hearing.builder().value(HearingDetails.builder()
                        .start(HEARING_START_DATE_TIME).venue(Venue.builder().name(VENUE_NAME).build()).build()).build()
        );
        when(hearingOutcomeService.setHearingOutcomeCompletedHearings(any())).thenReturn(
                new DynamicList(new DynamicListItem("1", "test"), List.of(new DynamicListItem("1", "test")))
        );
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.ABOUT_TO_START,callback,USER_AUTHORISATION);
        assertThat(response.getData().getHearingOutcomeValue().getCompletedHearings().getListItems()).isNotEmpty();
        assertThat(response.getData().getHearingOutcomeValue().getCompletedHearings().getListItems().size()).isEqualTo(1);
    }

    @Test
    void givenMultipleCompletedHearingOnCase_ThenPopulateDropdownInDescendingOrderByDate() {
        when(venueService.getVenueDetailsForActiveVenueByEpimsId(EPIMS_ID_1)).thenReturn(VenueDetails.builder()
            .epimsId(EPIMS_ID_1)
            .venName("firstVenueName")
            .build());
        when(venueService.getVenueDetailsForActiveVenueByEpimsId(EPIMS_ID_2)).thenReturn(VenueDetails.builder()
            .epimsId(EPIMS_ID_2)
            .venName("secondVenueName")
            .build());
        when(hearingUpdateService.convertUtcToUk(HEARING_START_DATE_TIME)).thenReturn(HEARING_START_DATE_TIME);
        when(hearingUpdateService.convertUtcToUk(HEARING_END_DATE_TIME)).thenReturn(HEARING_END_DATE_TIME);
        when(hearingUpdateService.convertUtcToUk(HEARING_START_DATE_TIME.minusMonths(1)))
            .thenReturn(HEARING_START_DATE_TIME.minusMonths(1));
        when(hearingUpdateService.convertUtcToUk(HEARING_END_DATE_TIME.minusMonths(1)))
            .thenReturn(HEARING_END_DATE_TIME.minusMonths(1));
        when(hmcHearingApiService.getHearingsRequest(any(),any())).thenReturn(
                HearingsGetResponse.builder().caseHearings(
                        List.of(CaseHearing.builder()
                                .hearingId(1L)
                                .hearingChannels(List.of(HearingChannel.FACE_TO_FACE))
                                .hearingDaySchedule(List.of(HearingDaySchedule.builder()
                                    .hearingVenueEpimsId(EPIMS_ID_1)
                                    .hearingStartDateTime(HEARING_START_DATE_TIME.minusMonths(1))
                                    .hearingEndDateTime(HEARING_END_DATE_TIME.minusMonths(1))
                                    .build()))
                                .build(),
                            CaseHearing.builder()
                                .hearingId(2L)
                                .hearingChannels(List.of(HearingChannel.FACE_TO_FACE))
                                .hearingDaySchedule(List.of(HearingDaySchedule.builder()
                                    .hearingVenueEpimsId(EPIMS_ID_2)
                                    .hearingStartDateTime(HEARING_START_DATE_TIME)
                                    .hearingEndDateTime(HEARING_END_DATE_TIME)
                                    .build()))
                                .build()))
                    .build());
        when(hearingOutcomeService.mapCaseHearingToHearing(any())).thenReturn(
                Hearing.builder().value(HearingDetails.builder()
                        .start(HEARING_START_DATE_TIME).venue(Venue.builder().name(VENUE_NAME).build()).build()).build()
        );
        when(hearingOutcomeService.setHearingOutcomeCompletedHearings(any())).thenReturn(
                new DynamicList(new DynamicListItem("1", "test"),
                        List.of(new DynamicListItem("1", "test"), new DynamicListItem("2", "Test")))
        );


        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.ABOUT_TO_START,callback,USER_AUTHORISATION);
        List<DynamicListItem> hearings = response.getData().getHearingOutcomeValue().getCompletedHearings().getListItems();
        assertThat(hearings).isNotEmpty();
        assertThat(hearings.size()).isEqualTo(2);
        assertThat(hearings.get(0).getCode()).isEqualTo("1");
        assertThat(hearings.get(1).getCode()).isEqualTo("2");
    }

    @Test
    void givenOneHearingHasIncorrectVenue_ThenShowInCompletedHearings() {
        CaseHearing caseHearing1 = CaseHearing.builder()
                .hearingId(1L)
                .hearingChannels(List.of(HearingChannel.FACE_TO_FACE))
                .hearingDaySchedule(List.of(HearingDaySchedule.builder()
                        .hearingVenueEpimsId(EPIMS_ID_1)
                        .hearingStartDateTime(HEARING_START_DATE_TIME.minusMonths(1))
                        .hearingEndDateTime(HEARING_END_DATE_TIME.minusMonths(1))
                        .build()))
                .build();

        CaseHearing caseHearing2 = CaseHearing.builder()
                .hearingId(2L)
                .hearingChannels(List.of(HearingChannel.FACE_TO_FACE))
                .hearingDaySchedule(List.of(HearingDaySchedule.builder()
                        .hearingVenueEpimsId(EPIMS_ID_2)
                        .hearingStartDateTime(HEARING_START_DATE_TIME)
                        .hearingEndDateTime(HEARING_END_DATE_TIME)
                        .build()))
                .build();

        when(hmcHearingApiService.getHearingsRequest(any(),any())).thenReturn(
                HearingsGetResponse.builder().caseHearings(
                        List.of(caseHearing1,caseHearing2)).build());
        when(hearingOutcomeService.mapCaseHearingToHearing(caseHearing1)).thenReturn(
                Hearing.builder().value(HearingDetails.builder()
                        .start(HEARING_START_DATE_TIME).venue(Venue.builder().name(VENUE_NAME).build()).build()).build()
        );
        when(hearingOutcomeService.mapCaseHearingToHearing(caseHearing2)).thenReturn(
                Hearing.builder().value(HearingDetails.builder()
                        .start(HEARING_START_DATE_TIME).venue(Venue.builder().build()).build()).build()
        );

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.ABOUT_TO_START,callback,USER_AUTHORISATION);
        List<Hearing> hearings = response.getData().getCompletedHearingsList();
        assertThat(hearings).isNotEmpty();
        assertThat(hearings.size()).isEqualTo(2);
    }

    @Test
    void givenNoCompletedHearingOnCase_ThenReturnError() {
        when(hmcHearingApiService.getHearingsRequest(any(),any())).thenReturn(
                HearingsGetResponse.builder().caseHearings(List.of()).build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.ABOUT_TO_START,callback,USER_AUTHORISATION);
        assertThat(response.getErrors()).isNotEmpty();
        assertThat(response.getErrors()).contains("There are no completed hearings on the case.");
    }

    @Test
    void givenCallToHmcFails_ThenReturnError() {
        when(hmcHearingApiService.getHearingsRequest(any(),any())).thenAnswer(i -> {
            throw new Exception("exception");
        });
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.ABOUT_TO_START,callback,USER_AUTHORISATION);
        assertThat(response.getErrors()).isNotEmpty();
        assertThat(response.getErrors()).contains("There was an error while retrieving hearing details; please try again after some time.");
    }
}
