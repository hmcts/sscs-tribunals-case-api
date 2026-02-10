package uk.gov.hmcts.reform.sscs.ccd.presubmit.amendhearingoutcome;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import junitparams.JUnitParamsRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.amendhearingoutcome.AmendHearingOutcomeAboutToStartHandler;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcomeDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcomeValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Venue;
import uk.gov.hmcts.reform.sscs.model.multi.hearing.CaseHearing;
import uk.gov.hmcts.reform.sscs.model.multi.hearing.HearingsGetResponse;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingDaySchedule;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;
import uk.gov.hmcts.reform.sscs.service.HearingOutcomeService;
import uk.gov.hmcts.reform.sscs.service.HmcHearingApiService;


@RunWith(JUnitParamsRunner.class)
public class AmendHearingOutcomeAboutToStartHandlerTest {
    private AmendHearingOutcomeAboutToStartHandler handler;
    private static final String USER_AUTHORISATION = "Bearer token";
    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    @Mock
    private HmcHearingApiService hmcHearingApiService;
    @Mock
    private HearingOutcomeService hearingOutcomeService;
    private SscsCaseData sscsCaseData;
    public static final String VENUE_NAME = "venueName";


    @BeforeEach
    void setup() {
        openMocks(this);
        handler = new AmendHearingOutcomeAboutToStartHandler(hmcHearingApiService, hearingOutcomeService);
        when(callback.getEvent()).thenReturn(EventType.AMEND_HEARING_OUTCOME);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder().build())
                .hearingOutcomeValue(HearingOutcomeValue.builder().build())
                .hearingOutcomes(Collections.singletonList((HearingOutcome.builder()
                        .value(HearingOutcomeDetails.builder().build()).build())))
                .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    void givenAmendHearingOutcomeEventShouldHandle() {
        assertThat(handler.canHandle(CallbackType.ABOUT_TO_START,callback)).isTrue();
    }

    @Test
    void givenHearingOutcomeOnCase_ThenDontReturnError() {
        when(hmcHearingApiService.getHearingsRequest(any(),any())).thenReturn(
                HearingsGetResponse.builder().caseHearings(List.of(CaseHearing.builder()
                                .hearingId(1L)
                                .hearingChannels(List.of(HearingChannel.FACE_TO_FACE))
                                .hearingDaySchedule(List.of(HearingDaySchedule.builder()
                                        .hearingStartDateTime(LocalDateTime.now())
                                        .hearingEndDateTime(LocalDateTime.now().plusHours(2))
                                        .build()))
                                .build()))
                        .build());
        when(hearingOutcomeService.mapCaseHearingToHearing(any())).thenReturn(
                Hearing.builder().value(HearingDetails.builder()
                        .start(LocalDateTime.now()).venue(Venue.builder().name(VENUE_NAME).build()).build()).build()
        );
        when(hearingOutcomeService.setHearingOutcomeCompletedHearings(any())).thenReturn(
                new DynamicList(new DynamicListItem("1", "test"), List.of(new DynamicListItem("1", "test")))
        );
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.ABOUT_TO_START,callback,USER_AUTHORISATION);
        assertThat(response.getData().getHearingOutcomes()).isNotEmpty();
        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void givenNoHearingOutcomesOnCase_ThenReturnError() {
        sscsCaseData.setHearingOutcomes(null);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.ABOUT_TO_START,callback,USER_AUTHORISATION);
        assertThat(response.getErrors()).isNotEmpty();
        assertThat(response.getErrors()).contains("There are no hearing outcomes recorded on the case. Please add a hearing outcome using 'Add a Hearing Outcome' event");
    }

}
