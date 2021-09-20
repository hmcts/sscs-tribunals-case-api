package uk.gov.hmcts.reform.sscs.ccd.presubmit.validsendtointerloc;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.SelectWhoReviewsCase.POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.SelectWhoReviewsCase.REVIEW_BY_JUDGE;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.FILENAME;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.idam.UserRole;


@RunWith(JUnitParamsRunner.class)
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


    @Before
    public void setUp() {
        openMocks(this);
        handler = new ValidSendToInterlocMidEventHandler(generateFile, templateId);

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
                .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getEvent()).thenReturn(EventType.VALID_SEND_TO_INTERLOC);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(caseDetails.getState()).thenReturn(sscsCaseData.getState());
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(userDetails);
    }

    @Test
    public void givenANonSendToInterlocEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    @Parameters({"VALID_SEND_TO_INTERLOC", "ADMIN_SEND_TO_INTERLOCUTORY_REVIEW_STATE"})
    public void givenASendToInterlocEvent_thenReturnTrue(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);

        assertTrue(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "ABOUT_TO_SUBMIT", "SUBMITTED"})
    public void givenANonPostponementRequestCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    @Parameters({"VALID_SEND_TO_INTERLOC", "ADMIN_SEND_TO_INTERLOCUTORY_REVIEW_STATE"})
    public void givenThereIsNoRequestDetails_thenReturnAnErrorMessage(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        sscsCaseData.getPostponementRequest().setPostponementRequestDetails(null);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getErrors().iterator().next(), is("Please enter request details to generate a postponement request document"));
    }

    @Test
    @Parameters({"VALID_SEND_TO_INTERLOC", "ADMIN_SEND_TO_INTERLOCUTORY_REVIEW_STATE"})
    public void givenRequestDetails_thenCreatePdfFromItAndStoreItInThePreviewDocuments(EventType eventType) {
        String dmUrl = "http://dm-store/documents/123";
        when(callback.getEvent()).thenReturn(eventType);
        when(generateFile.assemble(any())).thenReturn(dmUrl);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        DocumentLink documentLink = DocumentLink.builder()
                .documentBinaryUrl(dmUrl + "/binary")
                .documentUrl(dmUrl)
                .documentFilename(FILENAME)
                .build();
        assertThat(sscsCaseData.getPostponementRequest().getPostponementPreviewDocument(), is(documentLink));
    }

    @Test
    @Parameters({"VALID_SEND_TO_INTERLOC", "ADMIN_SEND_TO_INTERLOCUTORY_REVIEW_STATE"})
    public void givenNonPostponementRequest_thenDoNothing(EventType eventType) {
        sscsCaseData.setSelectWhoReviewsCase(new DynamicList(new DynamicListItem(REVIEW_BY_JUDGE.getId(), REVIEW_BY_JUDGE.getLabel()), null));

        when(callback.getEvent()).thenReturn(eventType);
        sscsCaseData.getPostponementRequest().setPostponementRequestDetails(null);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
        verifyNoInteractions(generateFile);
    }
}
