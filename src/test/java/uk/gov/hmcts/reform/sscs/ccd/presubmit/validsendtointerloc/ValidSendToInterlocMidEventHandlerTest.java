package uk.gov.hmcts.reform.sscs.ccd.presubmit.validsendtointerloc;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.SelectWhoReviewsCase.POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.SelectWhoReviewsCase.REVIEW_BY_JUDGE;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.idam.UserRole;


@Slf4j
public class ValidSendToInterlocMidEventHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private ValidSendToInterlocMidEventHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private IdamService idamService;

    @Mock
    private GenerateFile generateFile;


    private SscsCaseData sscsCaseData;

    private final UserDetails userDetails = UserDetails.builder().roles(new ArrayList<>(asList("caseworker-sscs", UserRole.CTSC_CLERK.getValue()))).build();
    private String templateId = "templateId.docx";


    @BeforeEach
    public void setUp() {
        openMocks(this);
        handler = new ValidSendToInterlocMidEventHandler(generateFile, templateId);

        Venue venue = Venue.builder().name("venue name").build();
        HearingDetails hearingDetails = HearingDetails.builder()
                .venue(venue)
                .hearingStatus(HearingStatus.LISTED)
                .start(LocalDateTime.now())
                .build();
        Hearing hearing = Hearing.builder().value(hearingDetails).build();

        sscsCaseData = SscsCaseData.builder()
                .appeal(Appeal.builder().mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build()).build())
                .state(State.HEARING)
                .selectWhoReviewsCase(new DynamicList(new DynamicListItem(POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getId(), POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getLabel()), null))
                .postponementRequest(PostponementRequest.builder()
                        .postponementRequestDetails("Here are some details")
                        .postponementRequestHearingVenue("Venue 1")
                        .postponementPreviewDocument(null)
                        .postponementRequestHearingDateAndTime(LocalDateTime.now().plusDays(1).toString())
                        .build())
                .regionalProcessingCenter(RegionalProcessingCenter.builder()
                        .name("Bradford")
                        .hearingRoute(HearingRoute.GAPS)
                        .build())
                .hearings(List.of(hearing))
                .build();
        sscsCaseData.getSchedulingAndListingFields().setHearingRoute(HearingRoute.LIST_ASSIST);

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getEvent()).thenReturn(EventType.VALID_SEND_TO_INTERLOC);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(caseDetails.getState()).thenReturn(sscsCaseData.getState());
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(userDetails);
    }

    @Test
    void givenANonSendToInterlocEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(MID_EVENT, callback));
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, names = {"VALID_SEND_TO_INTERLOC", "ADMIN_SEND_TO_INTERLOCUTORY_REVIEW_STATE"})
    void givenASendToInterlocEvent_thenReturnTrue(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);

        assertTrue(handler.canHandle(MID_EVENT, callback));
    }

    @ParameterizedTest
    @EnumSource(value = CallbackType.class, names = {"ABOUT_TO_START", "ABOUT_TO_SUBMIT", "SUBMITTED"})
    void givenANonPostponementRequestCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, names = {"VALID_SEND_TO_INTERLOC", "ADMIN_SEND_TO_INTERLOCUTORY_REVIEW_STATE"})
    void givenThereIsNoRequestDetails_thenReturnAnErrorMessage(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        sscsCaseData.getPostponementRequest().setPostponementRequestDetails(null);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getErrors().iterator().next(), is("Please enter request details to generate a postponement request document"));
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, names = {"VALID_SEND_TO_INTERLOC", "ADMIN_SEND_TO_INTERLOCUTORY_REVIEW_STATE"})
    void givenRequestDetails_thenCreatePdfFromItAndStoreItInThePreviewDocuments(EventType eventType) {
        String dmUrl = "http://dm-store/documents/123";
        when(callback.getEvent()).thenReturn(eventType);
        when(generateFile.assemble(any())).thenReturn(dmUrl);
        sscsCaseData.setRegionalProcessingCenter(RegionalProcessingCenter.builder()
                .name("Bradford")
                .hearingRoute(HearingRoute.LIST_ASSIST)
                .build());

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        DocumentLink documentLink = DocumentLink.builder()
                .documentBinaryUrl(dmUrl + "/binary")
                .documentUrl(dmUrl)
                .documentFilename("Postponement Request.pdf")
                .build();
        assertThat(sscsCaseData.getPostponementRequest().getPostponementPreviewDocument(), is(documentLink));
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, names = {"VALID_SEND_TO_INTERLOC", "ADMIN_SEND_TO_INTERLOCUTORY_REVIEW_STATE"})
    void givenNonPostponementRequest_thenDoNothing(EventType eventType) {
        sscsCaseData.setSelectWhoReviewsCase(new DynamicList(new DynamicListItem(REVIEW_BY_JUDGE.getId(), REVIEW_BY_JUDGE.getLabel()), null));

        when(callback.getEvent()).thenReturn(eventType);
        sscsCaseData.getPostponementRequest().setPostponementRequestDetails(null);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
        verifyNoInteractions(generateFile);
    }

    @Test
    void givenGapsPostponementRequestReviewedByTwc_thenThroughError() {
        when(caseDetails.getState()).thenReturn(State.HEARING);
        sscsCaseData.setSelectWhoReviewsCase(new DynamicList(new DynamicListItem(POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getId(), POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getLabel()), null));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors(), is(not(empty())));
        assertThat(response.getErrors().iterator().next(), is(ValidSendToInterlocMidEventHandler.POSTPONEMENTS_NOT_POSSIBLE_GAPS));
    }

    @Test
    void givenListAssitPostponementRequestReviewedByTwc_thenNoError() {
        when(caseDetails.getState()).thenReturn(State.HEARING);
        sscsCaseData.setSelectWhoReviewsCase(new DynamicList(new DynamicListItem(POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getId(), POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getLabel()), null));
        sscsCaseData.setRegionalProcessingCenter(RegionalProcessingCenter.builder()
                .name("Bradford")
                .hearingRoute(HearingRoute.LIST_ASSIST)
                .build());

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors(), is(empty()));
    }

    @Test
    void givenHearingIsSetupCorrectly_thenNoError() {
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.NONE);
        sscsCaseData.setRegionalProcessingCenter(RegionalProcessingCenter.builder()
                .name("Bradford")
                .hearingRoute(HearingRoute.LIST_ASSIST)
                .build());

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors(), is(empty()));
    }

    @Test
    void giveCaseIsGaps_thenReturnGapsError() {
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getErrors().iterator().next(), is("Postponement requests cannot be made for hearings listed in GAPS"));
    }

    @Test
    void givenCaseIsNotSl_thenReturnSlError() {
        sscsCaseData.setRegionalProcessingCenter(RegionalProcessingCenter.builder()
                .name("Bradford")
                .hearingRoute(HearingRoute.LIST_ASSIST)
                .build());
        sscsCaseData.getSchedulingAndListingFields().setHearingRoute(HearingRoute.GAPS);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getErrors().iterator().next(), is("Postponement requests can only be made for list assist cases"));
    }

    @Test
    void giveNoHearingOnCase_thenReturnNoCurrentHearingError() {
        sscsCaseData.setRegionalProcessingCenter(RegionalProcessingCenter.builder()
                .name("Bradford")
                .hearingRoute(HearingRoute.LIST_ASSIST)
                .build());
        sscsCaseData.setHearings(null);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getErrors().iterator().next(), is("There is no current hearing to postpone on the case"));
    }

    @Test
    void givenNoListedHearing_thenReturnNoListHearingError() {
        sscsCaseData.setRegionalProcessingCenter(RegionalProcessingCenter.builder()
                .name("Bradford")
                .hearingRoute(HearingRoute.LIST_ASSIST)
                .build());
        sscsCaseData.getHearings().get(0).getValue().setHearingStatus(HearingStatus.AWAITING_LISTING);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getErrors().iterator().next(), is("There is no listed hearing to postpone on the case"));
    }
}
