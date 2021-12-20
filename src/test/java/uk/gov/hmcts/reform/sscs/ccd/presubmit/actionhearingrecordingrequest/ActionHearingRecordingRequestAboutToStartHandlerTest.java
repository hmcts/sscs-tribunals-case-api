package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionhearingrecordingrequest;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.idam.UserRole;
import uk.gov.hmcts.reform.sscs.service.actionhearingrecordingrequest.ActionHearingRecordingRequestService;

@RunWith(JUnitParamsRunner.class)
public class ActionHearingRecordingRequestAboutToStartHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    static final Hearing HEARING = getHearing("1");

    private final ActionHearingRecordingRequestAboutToStartHandler handler = new ActionHearingRecordingRequestAboutToStartHandler(new ActionHearingRecordingRequestService());

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private IdamService idamService;

    private SscsCaseData sscsCaseData;

    private final UserDetails userDetails = UserDetails.builder().roles(new ArrayList<>(asList("caseworker-sscs", UserRole.CTSC_CLERK.getValue()))).build();

    @Before
    public void setUp() {
        openMocks(this);

        sscsCaseData = SscsCaseData.builder()
                .appeal(Appeal.builder().mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build()).build())
                .hearings(List.of(HEARING))

                .sscsHearingRecordingCaseData(SscsHearingRecordingCaseData.builder()
                        .sscsHearingRecordings(List.of(recording("1")))
                        .build())
                .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getEvent()).thenReturn(EventType.ACTION_HEARING_RECORDING_REQUEST);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(userDetails);
    }

    public static SscsHearingRecording recording(String hearingId) {
        final Hearing hearing = getHearing(hearingId);
        return SscsHearingRecording.builder()
                .value(SscsHearingRecordingDetails.builder()
                        .hearingId(hearing.getValue().getHearingId())
                        .venue(hearing.getValue().getVenue().getName())
                        .hearingDate(hearing.getValue().getHearingDate())
                        .hearingType("Adjourned")
                        .uploadDate(LocalDate.now().toString())
                        .recordings(List.of(HearingRecordingDetails.builder()
                                .value(DocumentLink.builder().documentBinaryUrl(format("https://example/%s", hearingId)).build())
                                .build()))
                        .build())
                .build();
    }

    @Test
    public void givenThereAreNoHearingRecordings_ReturnError() {
        sscsCaseData.getSscsHearingRecordingCaseData().setSscsHearingRecordings(null);
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());
        assertEquals("No hearing recordings on this case", response.getErrors().toArray()[0]);
    }

    @Test
    public void givenThereAreHearingsWithNoHearingRecordings_ReturnError() {
        sscsCaseData.getSscsHearingRecordingCaseData().setSscsHearingRecordings(null);
        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2021-01-01").time("12:00").hearingId("an_id1").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());
        assertEquals("No hearing recordings on this case", response.getErrors().toArray()[0]);
    }

    @Test
    public void givenThereAreHearingRecordingsForAHearing_thenAddToDynamicList() {
        SscsHearingRecording recording1 = getHearingRecording("an_id1");
        sscsCaseData.getSscsHearingRecordingCaseData().setSscsHearingRecordings(singletonList(recording1));
        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2021-01-01").time("12:00").hearingId("an_id1").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
        assertEquals(1, response.getData().getSscsHearingRecordingCaseData().getSelectHearingDetails().getListItems().size());
        assertEquals("Venue Name 12:00:00 01 Jan 2021", response.getData().getSscsHearingRecordingCaseData().getSelectHearingDetails().getListItems().get(0).getLabel());
        assertEquals("an_id1", response.getData().getSscsHearingRecordingCaseData().getSelectHearingDetails().getListItems().get(0).getCode());
    }

    @Test
    public void givenThereAreMultipleHearingWithRecordings_thenAddToDynamicList() {
        SscsHearingRecording recording1 = getHearingRecording("an_id1");
        SscsHearingRecording recording2 = getHearingRecording("an_id2");
        sscsCaseData.getSscsHearingRecordingCaseData().setSscsHearingRecordings(Arrays.asList(recording1, recording2));

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2021-01-01").time("12:00").hearingId("an_id1").venue(Venue.builder().name("Venue Name").build()).build()).build(),
                Hearing.builder().value(HearingDetails.builder()
                        .hearingDate("2021-02-01").time("13:00").hearingId("an_id2").venue(Venue.builder().name("Venue Name2").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
        assertEquals(2, response.getData().getSscsHearingRecordingCaseData().getSelectHearingDetails().getListItems().size());
        assertEquals("Venue Name 12:00:00 01 Jan 2021", response.getData().getSscsHearingRecordingCaseData().getSelectHearingDetails().getListItems().get(0).getLabel());
        assertEquals("an_id1", response.getData().getSscsHearingRecordingCaseData().getSelectHearingDetails().getListItems().get(0).getCode());
        assertEquals("Venue Name2 13:00:00 01 Feb 2021", response.getData().getSscsHearingRecordingCaseData().getSelectHearingDetails().getListItems().get(1).getLabel());
        assertEquals("an_id2", response.getData().getSscsHearingRecordingCaseData().getSelectHearingDetails().getListItems().get(1).getCode());
    }

    @Test
    @Parameters({"ABOUT_TO_SUBMIT", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

    public static SscsHearingRecording getHearingRecording(String id) {
        return SscsHearingRecording.builder()
                .value(SscsHearingRecordingDetails.builder().hearingId(id).build())
                .build();
    }

    static Hearing getHearing(String hearingId) {
        HearingDetails hearingDetails = HearingDetails.builder()
                .hearingId(String.valueOf(hearingId))
                .hearingDate("2021-05-18")
                .time("12:00")
                .venue(Venue.builder().name(format("Venue %s", hearingId)).build())
                .build();
        return Hearing.builder().value(hearingDetails).build();
    }
}
