package uk.gov.hmcts.reform.sscs.ccd.presubmit.processhearingrecordingrequest;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
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
import uk.gov.hmcts.reform.sscs.service.processhearingrecordingrequest.ProcessHearingRecordingRequestService;

@RunWith(JUnitParamsRunner.class)
public class ProcessHearingRecordingRequestAboutToStartHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    static final Hearing HEARING = getHearing(1);

    private final ProcessHearingRecordingRequestAboutToStartHandler handler = new ProcessHearingRecordingRequestAboutToStartHandler(new ProcessHearingRecordingRequestService());

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
                        .sscsHearingRecordings(List.of(recording(1)))
                        .build())
                .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getEvent()).thenReturn(EventType.PROCESS_HEARING_RECORDING_REQUEST);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(userDetails);
    }

    static SscsHearingRecording recording(int hearingId) {
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
    public void givenHearingRecordings_showProcessHearingRecordingRequests() {
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        SscsCaseData responseData = response.getData();
        assertThat(responseData.getSscsHearingRecordingCaseData().getProcessHearingRecordingRequests().size(), is(1));
        final ProcessHearingRecordingRequestDetails processHearingRecordingRequest = responseData.getSscsHearingRecordingCaseData().getProcessHearingRecordingRequests().get(0).getValue();
        assertThat(processHearingRecordingRequest.getHearingId(), is(HEARING.getValue().getHearingId()));
        assertThat(processHearingRecordingRequest.getHearingTitle(), is("Hearing 1"));
        assertThat(processHearingRecordingRequest.getHearingInformation(), is("Venue 1 12:00:00 18 May 2021"));
        assertThat(processHearingRecordingRequest.getRecordings().size(), is(1));
        assertThat(processHearingRecordingRequest.getAppellant().getValue().getCode(), is(""));
        assertThat(processHearingRecordingRequest.getAppellant().getListItems().stream().map(DynamicListItem::getCode).collect(Collectors.toList()), is(List.of("Granted", "Refused")));
        assertThat(processHearingRecordingRequest.getDwp().getValue().getCode(), is(""));
        assertThat(processHearingRecordingRequest.getJointParty().getValue().getCode(), is(""));
        assertThat(processHearingRecordingRequest.getRep().getValue().getCode(), is(""));
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

    static Hearing getHearing(int hearingId) {
        HearingDetails hearingDetails = HearingDetails.builder()
                .hearingId(String.valueOf(hearingId))
                .hearingDate("2021-05-18")
                .time("12:00")
                .venue(Venue.builder().name(format("Venue %s", hearingId)).build())
                .build();
        return Hearing.builder().value(hearingDetails).build();
    }
}
