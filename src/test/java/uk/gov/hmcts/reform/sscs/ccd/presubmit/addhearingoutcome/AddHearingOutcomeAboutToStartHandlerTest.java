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
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcomeValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Venue;
import uk.gov.hmcts.reform.sscs.model.multi.hearing.CaseHearing;
import uk.gov.hmcts.reform.sscs.model.multi.hearing.HearingsGetResponse;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingDaySchedule;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;
import uk.gov.hmcts.reform.sscs.service.HmcHearingsApiService;
import uk.gov.hmcts.reform.sscs.service.hmc.topic.HearingUpdateService;


@RunWith(JUnitParamsRunner.class)
public class AddHearingOutcomeAboutToStartHandlerTest {
    private AddHearingOutcomeAboutToStartHandler handler;
    private static final String USER_AUTHORISATION = "Bearer token";
    public static final LocalDateTime HEARING_START_DATE_TIME = LocalDateTime.now();
    public static final LocalDateTime HEARING_END_DATE_TIME = HEARING_START_DATE_TIME.plusHours(2);
    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    @Mock
    private HmcHearingsApiService hmcHearingsApiService;
    @Mock
    private HearingUpdateService hearingUpdateService;
    private SscsCaseData sscsCaseData;

    @BeforeEach
    void setup() {
        openMocks(this);
        handler = new AddHearingOutcomeAboutToStartHandler(hmcHearingsApiService, hearingUpdateService);
        when(callback.getEvent()).thenReturn(EventType.ADD_HEARING_OUTCOME);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder().build()).hearingOutcomeValue(HearingOutcomeValue.builder().build()).build();
        sscsCaseData.setHearings(buildHearings());
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    void givenAddHearingOutcomeEventShouldHandle() {
        assertThat(handler.canHandle(CallbackType.ABOUT_TO_START,callback)).isTrue();
    }

    @Test
    void givenCompletedHearingOnCase_ThenPopulateDropdown() {
        when(hearingUpdateService.convertUtcToUk(HEARING_START_DATE_TIME))
            .thenReturn(HEARING_START_DATE_TIME);
        when(hearingUpdateService.convertUtcToUk(HEARING_END_DATE_TIME))
            .thenReturn(HEARING_END_DATE_TIME);
        when(hmcHearingsApiService.getHearingsRequest(any(),any())).thenReturn(
            HearingsGetResponse.builder().caseHearings(List.of(CaseHearing.builder()
                    .hearingId(1L)
                    .hearingChannels(List.of(HearingChannel.FACE_TO_FACE))
                    .hearingDaySchedule(List.of(HearingDaySchedule.builder()
                        .hearingVenueEpimsId("123456")
                        .hearingStartDateTime(HEARING_START_DATE_TIME)
                        .hearingEndDateTime(HEARING_END_DATE_TIME)
                        .build()))
                    .build()))
                .build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.ABOUT_TO_START,callback,USER_AUTHORISATION);
        assertThat(response.getData().getHearingOutcomeValue().getCompletedHearings().getListItems()).isNotEmpty();
        assertThat(response.getData().getHearingOutcomeValue().getCompletedHearings().getListItems().size()).isEqualTo(1);
    }

    @Test
    void givenMultipleCompletedHearingOnCase_ThenPopulateDropdownInDescendingOrderByDate() {
        when(hearingUpdateService.convertUtcToUk(HEARING_START_DATE_TIME))
            .thenReturn(HEARING_START_DATE_TIME);
        when(hearingUpdateService.convertUtcToUk(HEARING_END_DATE_TIME))
            .thenReturn(HEARING_END_DATE_TIME);
        when(hearingUpdateService.convertUtcToUk(HEARING_START_DATE_TIME.minusMonths(1)))
            .thenReturn(HEARING_START_DATE_TIME.minusMonths(1));
        when(hearingUpdateService.convertUtcToUk(HEARING_END_DATE_TIME.minusMonths(1)))
            .thenReturn(HEARING_END_DATE_TIME.minusMonths(1));
        when(hmcHearingsApiService.getHearingsRequest(any(),any())).thenReturn(
                HearingsGetResponse.builder().caseHearings(
                        List.of(CaseHearing.builder()
                                .hearingId(1L)
                                .hearingChannels(List.of(HearingChannel.FACE_TO_FACE))
                                .hearingDaySchedule(List.of(HearingDaySchedule.builder()
                                    .hearingVenueEpimsId("12")
                                    .hearingStartDateTime(HEARING_START_DATE_TIME.minusMonths(1))
                                    .hearingEndDateTime(HEARING_END_DATE_TIME.minusMonths(1))
                                    .build()))
                                .build(),
                            CaseHearing.builder()
                                .hearingId(2L)
                                .hearingChannels(List.of(HearingChannel.FACE_TO_FACE))
                                .hearingDaySchedule(List.of(HearingDaySchedule.builder()
                                    .hearingVenueEpimsId("21")
                                    .hearingStartDateTime(HEARING_START_DATE_TIME)
                                    .hearingEndDateTime(HEARING_END_DATE_TIME)
                                    .build()))
                                .build()))
                    .build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.ABOUT_TO_START,callback,USER_AUTHORISATION);
        List<DynamicListItem> hearings = response.getData().getHearingOutcomeValue().getCompletedHearings().getListItems();
        assertThat(hearings).isNotEmpty();
        assertThat(hearings.size()).isEqualTo(2);
        assertThat(hearings.get(0).getCode()).isEqualTo("2");
        assertThat(hearings.get(1).getCode()).isEqualTo("1");
    }

    @Test
    void givenNoCompletedHearingOnCase_ThenReturnError() {
        when(hmcHearingsApiService.getHearingsRequest(any(),any())).thenReturn(
                HearingsGetResponse.builder().caseHearings(List.of()).build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.ABOUT_TO_START,callback,USER_AUTHORISATION);
        assertThat(response.getErrors()).isNotEmpty();
        assertThat(response.getErrors()).contains("There are no completed hearings on the case.");
    }

    @Test
    void givenCallToHmcFails_ThenReturnError() {
        when(hmcHearingsApiService.getHearingsRequest(any(),any())).thenAnswer(i -> {
            throw new Exception("exception");
        });
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.ABOUT_TO_START,callback,USER_AUTHORISATION);
        assertThat(response.getErrors()).isNotEmpty();
        assertThat(response.getErrors()).contains("There was an error while retrieving hearing details; please try again after some time.");
    }

    private List<Hearing> buildHearings() {
        return List.of(
                Hearing.builder().value(
                        HearingDetails.builder().hearingId("1").start(LocalDateTime.now().minusHours(2))
                                .end(LocalDateTime.now()).venue(Venue.builder().name("Cardiff").build()).build()).build(),
                Hearing.builder().value(
                        HearingDetails.builder().hearingId("2").start(LocalDateTime.now().minusDays(1).minusHours(2))
                                .end(LocalDateTime.now().minusDays(1)).venue(Venue.builder().name("Newport").build()).build()).build()
        );
    }

}
