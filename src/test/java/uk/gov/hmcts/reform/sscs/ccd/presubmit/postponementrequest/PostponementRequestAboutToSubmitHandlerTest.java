package uk.gov.hmcts.reform.sscs.ccd.presubmit.postponementrequest;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.POSTPONEMENT_REQUEST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostponementRequest;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.UploadParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.Venue;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.idam.UserRole;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.service.PostponementRequestService;

@RunWith(JUnitParamsRunner.class)
public class PostponementRequestAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private PostponementRequestAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private IdamService idamService;

    @Mock
    private FooterService footerService;

    private SscsCaseData sscsCaseData;

    private SscsDocument expectedDocument;

    private final UserDetails userDetails = UserDetails.builder().roles(new ArrayList<>(asList("caseworker-sscs", UserRole.CTSC_CLERK.getValue()))).build();

    @Before
    public void setUp() {
        openMocks(this);
        handler = new PostponementRequestAboutToSubmitHandler(new PostponementRequestService(), footerService);

        Hearing hearing = Hearing.builder().value(HearingDetails.builder()
            .hearingDate(LocalDate.now().plusDays(1).toString())
            .start(LocalDateTime.now().plusDays(1))
            .hearingId(String.valueOf(1))
            .venue(Venue.builder().name("Venue 1").build())
            .time("12:00")
            .build()).build();
        List<Hearing> hearings  = List.of(hearing);
        sscsCaseData = SscsCaseData.builder()
                .appeal(Appeal.builder().mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build()).build())
                .state(State.HEARING)
                .hearings(hearings)
                .postponementRequest(PostponementRequest.builder()
                        .postponementRequestDetails("Here are some details")
                        .postponementRequestHearingVenue("Venue 1")
                        .postponementPreviewDocument(DocumentLink.builder()
                                .documentBinaryUrl("http://example.com")
                                .documentFilename("example.pdf")
                                .build())
                        .postponementRequestHearingDateAndTime(LocalDateTime.now().plusDays(1).toString())
                        .build())
                .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getEvent()).thenReturn(EventType.POSTPONEMENT_REQUEST);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(caseDetails.getState()).thenReturn(sscsCaseData.getState());
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(userDetails);
        when(footerService.getNextBundleAddition(any())).thenReturn("A");

        expectedDocument = SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentLink(sscsCaseData.getPostponementRequest().getPostponementPreviewDocument())
                .documentFileName(sscsCaseData.getPostponementRequest().getPostponementPreviewDocument().getDocumentFilename())
                .documentType(POSTPONEMENT_REQUEST.getValue())
                .documentDateAdded(LocalDate.now().format(DateTimeFormatter.ISO_DATE))
                .build()).build();
    }

    @Test
    public void givenANonPostponementRequestEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenAPostponementRequest_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonPostponementRequestCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }


    @Test
    public void givenNoPostponementRequestPreviewDocument_ReturnAnError() {
        sscsCaseData.getPostponementRequest().setPostponementPreviewDocument(null);
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getWarnings().size(), is(0));
        assertThat(response.getErrors().iterator().next(), is("There is no postponement request document"));
    }

    @Test
    public void givenAPostponementRequest_moveDocumentToSscsDocuments() {
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(0));
        assertThat(sscsCaseData.getPostponementRequest().getPostponementPreviewDocument(), is(nullValue()));
        assertThat(sscsCaseData.getPostponementRequest().getPostponementRequestDetails(), is(nullValue()));
        assertThat(sscsCaseData.getPostponementRequest().getPostponementRequestHearingVenue(), is(nullValue()));
        assertThat(sscsCaseData.getPostponementRequest().getPostponementRequestHearingDateAndTime(), is(nullValue()));

        assertThat(sscsCaseData.getSscsDocument().size(), is(1));
        final SscsDocument document = sscsCaseData.getSscsDocument().get(0);
        assertThat(document.getValue().getDocumentType(), is(POSTPONEMENT_REQUEST.getValue()));
        assertThat(document.getValue().getDocumentLink().getDocumentFilename(), is("example.pdf"));
        assertThat(document.getValue().getOriginalPartySender(), is(UploadParty.DWP.getValue()));
        assertThat(sscsCaseData.getInterlocReviewState(), is(InterlocReviewState.REVIEW_BY_TCW));
        assertThat(sscsCaseData.getInterlocReferralReason(), is(InterlocReferralReason.REVIEW_POSTPONEMENT_REQUEST));
        assertThat(sscsCaseData.getPostponementRequest().getUnprocessedPostponementRequest(), is(YES));
    }

    @Test
    public void givenAPostponementRequest_AddGeneratedDocumentToBundle() {
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        List<SscsDocument> sscsDocuments = response.getData().getSscsDocument();
        Assert.assertEquals(sscsDocuments, Collections.singletonList(expectedDocument));
        assertEquals("A", response.getData().getSscsDocument().get(0).getValue().getBundleAddition());
    }
}
