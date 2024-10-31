package uk.gov.hmcts.reform.sscs.ccd.presubmit.addhearingoutcome;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsIterableContaining.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
import uk.gov.hmcts.reform.sscs.service.HmcHearingsApiService;


@RunWith(JUnitParamsRunner.class)
public class AddHearingOutcomeAboutToStartHandlerTest {
    private AddHearingOutcomeAboutToStartHandler handler;
    private static final String USER_AUTHORISATION = "Bearer token";
    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    @Mock
    private HmcHearingsApiService hmcHearingsApiService;
    private SscsCaseData sscsCaseData;

    @BeforeEach
    void setup() {
        openMocks(this);
        handler = new AddHearingOutcomeAboutToStartHandler(hmcHearingsApiService);
        when(callback.getEvent()).thenReturn(EventType.ADD_HEARING_OUTCOME);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder().build()).hearingOutcomeValue(HearingOutcomeValue.builder().build()).build();
        sscsCaseData.setHearings(buildHearings());
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    void givenAddHearingOutcomeEventShouldHandle() {
        assertTrue(handler.canHandle(CallbackType.ABOUT_TO_START,callback));
    }

    @Test
    void givenCompletedHearingOnCase_ThenPopulateDropdown() {
        when(hmcHearingsApiService.getHearingsRequest(any(),any())).thenReturn(
                HearingsGetResponse.builder().caseHearings(List.of(CaseHearing.builder().hearingId(1L).build())).build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.ABOUT_TO_START,callback,USER_AUTHORISATION);
        assertFalse(response.getData().getHearingOutcomeValue().getCompletedHearings().getListItems().isEmpty());
        assertEquals(1, response.getData().getHearingOutcomeValue().getCompletedHearings().getListItems().size());
    }

    @Test
    void givenMultipleCompletedHearingOnCase_ThenPopulateDropdownInAscendingOrderByDate() {
        when(hmcHearingsApiService.getHearingsRequest(any(),any())).thenReturn(
                HearingsGetResponse.builder().caseHearings(
                        List.of(CaseHearing.builder().hearingId(1L).build(),
                                CaseHearing.builder().hearingId(3L).build())).build());

        List<Hearing> updatedHearings = new ArrayList<>();

        Hearing latestHearing = Hearing.builder().value(
                HearingDetails.builder().hearingId("1").start(LocalDateTime.now().minusHours(1))
                        .end(LocalDateTime.now()).venue(Venue.builder().name("Cardiff").build()).build()).build();

        Hearing earliestHearing = Hearing.builder().value(
                HearingDetails.builder().hearingId("3").start(LocalDateTime.now().minusMonths(1).minusHours(1))
                        .end(LocalDateTime.now().minusMonths(1)).venue(Venue.builder().name("Cardiff").build()).build()).build();

        updatedHearings.add(latestHearing);
        updatedHearings.add(earliestHearing);
        sscsCaseData.setHearings(updatedHearings);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.ABOUT_TO_START,callback,USER_AUTHORISATION);
        List<DynamicListItem> hearings = response.getData().getHearingOutcomeValue().getCompletedHearings().getListItems();
        assertFalse(hearings.isEmpty());
        assertEquals(2, hearings.size());
        assertTrue(hearings.get(0).getCode().equals("3") && hearings.get(1).getCode().equals("1"));
    }

    @Test
    void givenNoCompletedHearingOnCase_ThenReturnError() {
        when(hmcHearingsApiService.getHearingsRequest(any(),any())).thenReturn(
                HearingsGetResponse.builder().caseHearings(List.of()).build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.ABOUT_TO_START,callback,USER_AUTHORISATION);
        assertFalse(response.getErrors().isEmpty());
        assertThat(response.getErrors(), hasItem("There are no completed hearings on the case."));
    }

    @Test
    void givenCallToHmcFails_ThenReturnError() {
        when(hmcHearingsApiService.getHearingsRequest(any(),any())).thenAnswer(i -> {
            throw new Exception("exception");
        });
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.ABOUT_TO_START,callback,USER_AUTHORISATION);
        assertFalse(response.getErrors().isEmpty());
        assertThat(response.getErrors(), hasItem("There was an error while retrieving hearing details; please try again after some time."));
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
