package uk.gov.hmcts.reform.sscs.ccd.presubmit.postponementrequest;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;

import java.time.LocalDate;
import java.util.ArrayList;
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

@RunWith(JUnitParamsRunner.class)
public class PostponementRequestAboutToStartHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private PostponementRequestAboutToStartHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private IdamService idamService;

    private SscsCaseData sscsCaseData;

    private final UserDetails userDetails = UserDetails.builder().roles(new ArrayList<>(asList("caseworker-sscs", UserRole.CTSC_CLERK.getValue()))).build();
    private Hearing hearing;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new PostponementRequestAboutToStartHandler();

        DocumentLink documentLink = DocumentLink.builder().documentUrl("url/1234").build();
        AudioVideoEvidenceDetails details = AudioVideoEvidenceDetails.builder().fileName("filename.mp4").documentLink(documentLink).build();

        hearing = getHearing(1);
        List<Hearing> hearings  = List.of(hearing);
        sscsCaseData = SscsCaseData.builder()
                .appeal(Appeal.builder().mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build()).build())
                .state(State.HEARING)
                .hearings(hearings)
                .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getEvent()).thenReturn(EventType.POSTPONEMENT_REQUEST);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(caseDetails.getState()).thenReturn(sscsCaseData.getState());
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(userDetails);
    }

    static Hearing getHearing(int hearingId) {
        return Hearing.builder().value(HearingDetails.builder()
                .hearingDate(LocalDate.now().plusDays(1).toString())
                .hearingId(String.valueOf(hearingId))
                .venue(Venue.builder().name("Venue " + hearingId).build())
                .time("12:00")
                .build()).build();
    }

    @Test
    public void givenANonPostponementRequestEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    public void givenAPostponementRequest_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_SUBMIT", "MID_EVENT", "SUBMITTED"})
    public void givenANonPostponementRequestCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenNoHearings_returnAnError() {
        sscsCaseData.setHearings(null);
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getErrors().iterator().next(), is("There are no hearing to postpone"));
    }

    @Test
    public void givenHearingsInThePast_returnAnError() {
        HearingDetails hearingDetails = hearing.getValue().toBuilder().hearingDate(LocalDate.now().minusDays(1).toString()).build();
        hearing = Hearing.builder().value(hearingDetails).build();
        sscsCaseData.setHearings(List.of(hearing));
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getErrors().iterator().next(), is("There are no hearing to postpone"));
    }

    @Test
    public void givenAPostponementRequest_setupPostponementRequestData() {
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(0));
        assertThat(response.getData().getPostponementRequest().getPostponementRequestHearingVenue(), is(hearing.getValue().getVenue().getName()));
        assertThat(response.getData().getPostponementRequest().getPostponementRequestHearingDateAndTime(), is(hearing.getValue().getHearingDateTime().toString()));
    }


}
