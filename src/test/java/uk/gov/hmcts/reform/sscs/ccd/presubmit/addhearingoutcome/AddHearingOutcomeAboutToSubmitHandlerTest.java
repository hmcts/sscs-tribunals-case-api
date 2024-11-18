package uk.gov.hmcts.reform.sscs.ccd.presubmit.addhearingoutcome;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcomeDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcomeValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Venue;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;

@RunWith(JUnitParamsRunner.class)
public class AddHearingOutcomeAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private Venue venue1 = Venue.builder().name("venue 1 name").build();
    private Venue venue2 = Venue.builder().name("venue 2 name").build();

    private String epims1 = "123456";
    private String epims2 = "234567";
    private String hearingOutcomeId1 = "2208";
    private String hearingOutcomeId2 = "0509";
    private LocalDateTime start1 = LocalDateTime.of(2024,6,30,10,00);
    private LocalDateTime end1 = LocalDateTime.of(2024,6,30,13,00);
    private LocalDateTime start2 = LocalDateTime.of(2024,9,16,14,00);
    private LocalDateTime end2 = LocalDateTime.of(2024,9,16,16,30);

    @InjectMocks
    AddHearingOutcomeAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    @BeforeEach
    public void setUp() {
        openMocks(this);

        when(callback.getEvent()).thenReturn(EventType.ADD_HEARING_OUTCOME);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        List<Hearing> hearings = new ArrayList<>();
        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
                .hearingId("1")
                .epimsId(epims1)
                .venue(venue1)
                .hearingChannel(HearingChannel.FACE_TO_FACE)
                .start(start1)
                .end(end1)
                .build()).build();
        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
                .hearingId("2")
                .epimsId(epims2)
                .venue(venue2)
                .hearingChannel(HearingChannel.FACE_TO_FACE)
                .start(start2)
                .end(end2)
                .build()).build();
        hearings.add(hearing1);
        hearings.add(hearing2);

        List<DynamicListItem> completedHearingsListOptions = new ArrayList<>();
        completedHearingsListOptions.add(new DynamicListItem("1", "Hearing 1 Date and Time start and End time, venue_name1"));
        completedHearingsListOptions.add(new DynamicListItem("2", "Hearing 2 Date and Time start and End time, venue_name2"));
        DynamicList completedHearings = new DynamicList(new DynamicListItem("1", "Hearing 1 Date and Time start and End time, venue_name1"), completedHearingsListOptions);

        sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("ccdId")
                .appeal(Appeal.builder().hearingOptions(HearingOptions.builder().build()).build())
                .hearings(hearings)
                .hearingOutcomeValue(HearingOutcomeValue.builder()
                        .hearingOutcomeId(hearingOutcomeId1)
                        .completedHearings(completedHearings)
                        .didPoAttendHearing(YES).build())
                .build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenAValidAboutToSubmitEvent_thenReturnTrue() {
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isTrue();
    }

    @Test
    public void givenAddHearingOutcomeWithNoHearingOutcomes_thenAddToHearingOutcome() {
        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getHearingOutcomes().size()).isEqualTo(1);

        HearingOutcomeDetails hearingOutcomeDetails = response.getData().getHearingOutcomes().get(0).getValue();
        assertThat(hearingOutcomeDetails.getCompletedHearingId()).isEqualTo("1");
        assertThat(hearingOutcomeDetails.getHearingStartDateTime()).isEqualTo(start1);
        assertThat(hearingOutcomeDetails.getHearingEndDateTime()).isEqualTo(end1);
        assertThat(hearingOutcomeDetails.getHearingOutcomeId()).isEqualTo(hearingOutcomeId1);
        assertThat(hearingOutcomeDetails.getDidPoAttendHearing()).isEqualTo(YES);
        assertThat(hearingOutcomeDetails.getHearingChannelId()).isEqualTo(HearingChannel.FACE_TO_FACE);
        assertThat(hearingOutcomeDetails.getEpimsId()).isEqualTo(epims1);
    }

    @Test
    public void givenAddHearingOutcomeWithExistingHearingOutcomes_thenAddToHearingOutcomeInDateOrder() {
        sscsCaseData.setHearingOutcomes(new ArrayList<>());
        sscsCaseData.getHearingOutcomes().add(HearingOutcome.builder()
                .value(HearingOutcomeDetails.builder()
                        .completedHearingId("2")
                        .hearingStartDateTime(start2)
                        .hearingEndDateTime(end2)
                        .hearingOutcomeId(hearingOutcomeId2)
                        .didPoAttendHearing(YES)
                        .hearingChannelId(HearingChannel.FACE_TO_FACE)
                        .venue(venue2)
                        .epimsId(epims2)
                        .build())
                .build());

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getHearingOutcomes().size()).isEqualTo(2);

        HearingOutcomeDetails hearingOutcomeDetails1 = response.getData().getHearingOutcomes().get(0).getValue();
        assertThat(hearingOutcomeDetails1.getCompletedHearingId()).isEqualTo("1");
        assertThat(hearingOutcomeDetails1.getHearingStartDateTime()).isEqualTo(start1);
        assertThat(hearingOutcomeDetails1.getHearingEndDateTime()).isEqualTo(end1);
        assertThat(hearingOutcomeDetails1.getHearingOutcomeId()).isEqualTo(hearingOutcomeId1);
        assertThat(hearingOutcomeDetails1.getDidPoAttendHearing()).isEqualTo(YES);
        assertThat(hearingOutcomeDetails1.getHearingChannelId()).isEqualTo(HearingChannel.FACE_TO_FACE);
        assertThat(hearingOutcomeDetails1.getEpimsId()).isEqualTo(epims1);

        HearingOutcomeDetails hearingOutcomeDetails2 = response.getData().getHearingOutcomes().get(1).getValue();
        assertThat(hearingOutcomeDetails2.getCompletedHearingId()).isEqualTo("2");
        assertThat(hearingOutcomeDetails2.getHearingStartDateTime()).isEqualTo(start2);
        assertThat(hearingOutcomeDetails2.getHearingEndDateTime()).isEqualTo(end2);
        assertThat(hearingOutcomeDetails2.getHearingOutcomeId()).isEqualTo(hearingOutcomeId2);
        assertThat(hearingOutcomeDetails2.getDidPoAttendHearing()).isEqualTo(YES);
        assertThat(hearingOutcomeDetails2.getHearingChannelId()).isEqualTo(HearingChannel.FACE_TO_FACE);
        assertThat(hearingOutcomeDetails2.getEpimsId()).isEqualTo(epims2);
    }

    @Test
    public void givenAddHearingOutcome_thenClearHearingOutcomeValues() {
        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getHearingOutcomeValue().getHearingOutcomeId()).isNull();
        assertThat(response.getData().getHearingOutcomeValue().getCompletedHearings()).isNull();
        assertThat(response.getData().getHearingOutcomeValue().getDidPoAttendHearing()).isNull();
    }

    @Test
    public void givenIncorrectHearingId_thenGiveError() {
        sscsCaseData.setHearingOutcomes(new ArrayList<>());
        sscsCaseData.getHearingOutcomes().add(HearingOutcome.builder().value(
                HearingOutcomeDetails.builder().completedHearingId("1").build()).build());

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors())
                .hasSize(1)
                .contains("A hearing outcome already exists for this hearing date. Please select a different hearing date");
    }
}