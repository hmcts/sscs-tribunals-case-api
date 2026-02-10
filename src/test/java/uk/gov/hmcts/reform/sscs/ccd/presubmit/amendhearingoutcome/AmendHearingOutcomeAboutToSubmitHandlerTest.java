package uk.gov.hmcts.reform.sscs.ccd.presubmit.amendhearingoutcome;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.time.LocalDateTime;
import java.util.List;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.amendhearingoutcome.AmendHearingOutcomeAboutToSubmitHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Venue;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;

@RunWith(JUnitParamsRunner.class)
public class AmendHearingOutcomeAboutToSubmitHandlerTest {

    private AmendHearingOutcomeAboutToSubmitHandler handler;
    private static final String USER_AUTHORISATION = "Bearer token";

    private static final DynamicList completedHearings = new DynamicList(new DynamicListItem("2", "test2"),
            List.of(new DynamicListItem("1", "test1"), new DynamicListItem("2", "test2")));

    private static final HearingOutcome hearingOutcome1 = HearingOutcome.builder()
            .value(
                    HearingOutcomeDetails.builder()
                            .completedHearingId("1")
                            .didPoAttendHearing(YesNo.NO)
                            .hearingOutcomeId("1")
                            .completedHearings(completedHearings)
                            .venue(Venue.builder().name("venue 1 name").build())
                            .hearingStartDateTime(LocalDateTime.of(2024,1,30,10,00))
                            .hearingEndDateTime(LocalDateTime.of(2024,1,30,13,00))
                            .epimsId("654321")
                            .hearingChannelId(HearingChannel.FACE_TO_FACE)
                            .build())
            .build();

    private static final HearingOutcome hearingOutcome2 = HearingOutcome.builder()
            .value(
                    HearingOutcomeDetails.builder()
                            .completedHearingId("2")
                            .didPoAttendHearing(YesNo.YES)
                            .hearingOutcomeId("2")
                            .completedHearings(completedHearings)
                            .venue(Venue.builder().name("venue 2 name").build())
                            .hearingStartDateTime(LocalDateTime.of(2024,6,30,10,00))
                            .hearingEndDateTime(LocalDateTime.of(2024,6,30,13,00))
                            .epimsId("123456")
                            .hearingChannelId(HearingChannel.FACE_TO_FACE)
                            .build()
            ).build();

    private static final Hearing hearing1 = Hearing.builder()
            .value(HearingDetails.builder()
                    .hearingId("1")
                    .venue(Venue.builder().name("venue 1 name").build())
                    .start(LocalDateTime.of(2024,1,30,10,00))
                    .end(LocalDateTime.of(2024,1,30,13,00))
                    .epimsId("654321")
                    .hearingChannel(HearingChannel.FACE_TO_FACE)
                    .build())
            .build();

    private static final Hearing hearing2 = Hearing.builder()
            .value(HearingDetails.builder()
                    .hearingId("2")
                    .venue(Venue.builder().name("venue 2 name").build())
                    .start(LocalDateTime.of(2024,6,30,10,00))
                    .end(LocalDateTime.of(2024,6,30,13,00))
                    .epimsId("123456")
                    .hearingChannel(HearingChannel.FACE_TO_FACE)
                    .build())
            .build();


    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new AmendHearingOutcomeAboutToSubmitHandler();

        when(callback.getEvent()).thenReturn(EventType.AMEND_HEARING_OUTCOME);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdDd").appeal(Appeal.builder().build()).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenAValidAboutToSubmitEvent_thenReturnTrue() {
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isTrue();
    }

    @Test
    public void givenAnInvalidAboutToSubmitEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @Test
    public void givenAnEmptyOutcomeList_thenSetHearingOutcomesToNull() {
        sscsCaseData.setHearingOutcomes(List.of());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getHearingOutcomes()).isEqualTo(null);
    }

    @Test
    public void givenDifferentHearingForOutcomeSelected_thenAlterHearingOutcome() {
        sscsCaseData.setCompletedHearingsList(List.of(hearing1, hearing2));
        sscsCaseData.setHearingOutcomes(List.of(hearingOutcome1));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT,callback,USER_AUTHORISATION);

        assertThat(response.getData().getHearingOutcomes().get(0).getValue().getCompletedHearingId()).isEqualTo("2");
        assertThat(response.getData().getHearingOutcomes().get(0).getValue().getDidPoAttendHearing()).isEqualTo(YesNo.NO);
        assertThat(response.getData().getHearingOutcomes().get(0).getValue().getHearingOutcomeId()).isEqualTo("1");
        assertThat(response.getData().getHearingOutcomes().get(0).getValue().getHearingStartDateTime()).isEqualTo(LocalDateTime.of(2024,6,30,10,00));
        assertThat(response.getData().getHearingOutcomes().get(0).getValue().getHearingEndDateTime()).isEqualTo(LocalDateTime.of(2024,6,30,13,00));
        assertThat(response.getData().getHearingOutcomes().get(0).getValue().getVenue().getName()).isEqualTo("venue 2 name");
        assertThat(response.getData().getHearingOutcomes().get(0).getValue().getEpimsId()).isEqualTo("123456");
        assertThat(response.getData().getHearingOutcomes().get(0).getValue().getHearingChannelId()).isEqualTo(HearingChannel.FACE_TO_FACE);
        assertThat(response.getData().getCompletedHearingsList()).isNull();
    }

    @Test
    public void givenHearingOutcomeSelectedMoreThanOnce_thenThrowError() {
        sscsCaseData.setHearingOutcomes(List.of(hearingOutcome1, hearingOutcome2));

        sscsCaseData.setCompletedHearingsList(List.of(hearing1, hearing2));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT,callback,USER_AUTHORISATION);

        assertThat(response.getErrors())
                .contains("This hearing already has an outcome recorded: test2");
    }
}
