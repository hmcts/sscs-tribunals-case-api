package uk.gov.hmcts.reform.sscs.ccd.presubmit.postponementrequest;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingStatus.AWAITING_LISTING;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingStatus.LISTED;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.AudioVideoEvidenceDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.Venue;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.idam.UserRole;

@ExtendWith(MockitoExtension.class)
public class PostponementRequestAboutToStartHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    @InjectMocks
    private PostponementRequestAboutToStartHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private IdamService idamService;

    private SscsCaseData sscsCaseData;

    private final UserDetails userDetails = UserDetails.builder()
        .roles(new ArrayList<>(asList("caseworker-sscs", UserRole.CTSC_CLERK.getValue())))
        .build();
    private Hearing hearing;

    @BeforeEach
    public void setUp() {
        DocumentLink documentLink = DocumentLink.builder().documentUrl("url/1234").build();
        AudioVideoEvidenceDetails details = AudioVideoEvidenceDetails.builder().fileName("filename.mp4").documentLink(documentLink).build();

        ReflectionTestUtils.setField(handler, "isScheduleListingEnabled", true);

        hearing =  Hearing.builder().value(HearingDetails.builder()
            .hearingDate(LocalDate.now().plusDays(1).toString())
            .start(LocalDateTime.now().plusDays(1))
            .hearingId(String.valueOf(1))
            .venue(Venue.builder().name("Venue 1").build())
            .hearingStatus(LISTED)
            .build()).build();
        sscsCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .mrnDetails(MrnDetails.builder()
                    .dwpIssuingOffice("3")
                    .build())
                .build())
            .schedulingAndListingFields(SchedulingAndListingFields.builder()
                .hearingRoute(HearingRoute.LIST_ASSIST)
                .build())
            .state(State.HEARING)
            .hearings(new ArrayList<>(List.of(hearing)))
            .build();
    }

    @Test
    public void givenAPostponementRequest_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.POSTPONEMENT_REQUEST);
        assertThat(handler.canHandle(ABOUT_TO_START, callback)).isTrue();
    }

    @Test
    public void givenANonPostponementRequestEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertThat(handler.canHandle(ABOUT_TO_START, callback)).isFalse();
    }

    @Test
    public void givenAPostponementRequest_ScheduleListingNotEnabled_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.POSTPONEMENT_REQUEST);
        ReflectionTestUtils.setField(handler, "isScheduleListingEnabled", false);
        assertThat(handler.canHandle(ABOUT_TO_START, callback)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(
        value = CallbackType.class,
        names = {"ABOUT_TO_SUBMIT", "MID_EVENT", "SUBMITTED"})
    public void givenANonPostponementRequestCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertThat(handler.canHandle(callbackType, callback)).isFalse();
    }

    @Test
    public void givenNoHearings_returnAnError() {
        sscsCaseData.setHearings(null);

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(response.getErrors())
            .hasSize(1)
            .containsExactlyInAnyOrder("There is not a hearing to postpone");
    }

    @Test
    public void givenHearingNotListed_returnAnError() {
        sscsCaseData.getHearings().get(0).getValue().setHearingStatus(AWAITING_LISTING);

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(response.getErrors())
            .hasSize(1)
            .containsExactlyInAnyOrder("There not a listed hearing to postpone");
    }

    @Test
    public void givenAPostponementRequestFromListAssist_setupPostponementRequestData() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getWarnings()).isEmpty();
        assertThat(response.getData().getPostponementRequest().getPostponementRequestHearingVenue())
            .isEqualTo(hearing.getValue().getVenue().getName());
        assertThat(response.getData().getPostponementRequest().getPostponementRequestHearingDateAndTime())
            .isEqualTo(hearing.getValue().getStart().toString());
    }

    @Test
    public void givenAPostponementRequestFromGaps_returnAnError() {
        sscsCaseData.getSchedulingAndListingFields().setHearingRoute(HearingRoute.GAPS);

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(response.getErrors())
            .hasSize(1)
            .containsExactlyInAnyOrder("Postponement requests can only be made for list assist cases");
    }

    @Test
    public void givenAPostponementRequestFromListAssist_shouldNotReturnAnError() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

}
