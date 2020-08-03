package uk.gov.hmcts.reform.sscs.ccd.presubmit.directionissued;

import static java.util.Collections.emptySet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.AWAITING_ADMIN_ACTION;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.AWAITING_INFORMATION;

import com.google.common.collect.ImmutableSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.service.ServiceRequestExecutor;

@RunWith(JUnitParamsRunner.class)
public class DirectionIssuedAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String DOCUMENT_URL = "dm-store/documents/123";
    private static final String DOCUMENT_URL2 = "dm-store/documents/456";

    @Mock
    private FooterService footerService;

    private DirectionIssuedAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private CaseDetails<SscsCaseData> caseDetailsBefore;

    @Mock
    private ServiceRequestExecutor serviceRequestExecutor;

    private SscsCaseData sscsCaseData;

    private SscsDocument expectedDocument;

    @Mock
    private PreSubmitCallbackResponse<SscsCaseData> response;

    @Before
    public void setUp() {
        initMocks(this);

        handler = new DirectionIssuedAboutToSubmitHandler(footerService, serviceRequestExecutor, "https://sscs-bulk-scan.net", "/validate");

        when(callback.getEvent()).thenReturn(EventType.DIRECTION_ISSUED);

        SscsDocument document = SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName("myTest.doc").build()).build();
        List<SscsDocument> docs = new ArrayList<>();
        docs.add(document);

        sscsCaseData = SscsCaseData.builder()
                .generateNotice("Yes")
                .signedBy("User")
                .directionTypeDl(new DynamicList(DirectionType.APPEAL_TO_PROCEED.toString()))
                .signedRole("Judge")
                .dateAdded(LocalDate.now().minusDays(1))
                .sscsDocument(docs)
                .previewDocument(DocumentLink.builder()
                        .documentUrl(DOCUMENT_URL)
                        .documentBinaryUrl(DOCUMENT_URL + "/binary")
                        .documentFilename("directionIssued.pdf")
                        .build())
                .appeal(Appeal.builder()
                        .appellant(Appellant.builder()
                                .name(Name.builder().build())
                                .identity(Identity.builder().build())
                                .build())
                        .build()).build();

        expectedDocument = SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                .documentFileName(sscsCaseData.getPreviewDocument().getDocumentFilename())
                .documentLink(sscsCaseData.getPreviewDocument())
                .documentDateAdded(LocalDate.now().minusDays(1).toString())
                .documentType(DocumentType.DIRECTION_NOTICE.getValue())
                .build()).build();


        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(caseDetails.getState()).thenReturn(State.INTERLOCUTORY_REVIEW_STATE);
        when(caseDetailsBefore.getState()).thenReturn(State.INTERLOCUTORY_REVIEW_STATE);
        when(serviceRequestExecutor.post(eq(callback), eq("https://sscs-bulk-scan.net/validate"))).thenReturn(response);
        when(response.getErrors()).thenReturn(emptySet());
    }

    @Test
    @Parameters({"APPEAL_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    public void givenANonHandleEvidenceEvent_thenReturnFalse(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertFalse(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenGenerateNoticeIsYes_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void willCopyThePreviewFileToTheInterlocDirectionDocumentAndAddFooter() {
        when(caseDetails.getState()).thenReturn(State.READY_TO_LIST);
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertNull(response.getData().getPreviewDocument());
        assertNull(response.getData().getSignedBy());
        assertNull(response.getData().getSignedRole());
        assertNull(response.getData().getGenerateNotice());
        assertNull(response.getData().getDateAdded());

        verify(footerService).createFooterAndAddDocToCase(eq(expectedDocument.getValue().getDocumentLink()), any(), eq(DocumentType.DIRECTION_NOTICE), any(), any(), eq(null));
        assertNull(response.getData().getInterlocReviewState());
        assertEquals(DirectionType.APPEAL_TO_PROCEED.toString(), response.getData().getDirectionTypeDl().getValue().getCode());
    }

    @Test
    public void givenDirectionNoticeAlreadyExistsAndThenManuallyUploadANewNotice_thenIssueTheNewDocumentWithFooter() {
        sscsCaseData.setPreviewDocument(null);

        List<SscsDocument> sscsDocuments = new ArrayList<>();
        SscsDocument document1 = SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentType(DocumentType.DIRECTION_NOTICE.getValue())
                .documentLink(DocumentLink.builder().documentUrl(DOCUMENT_URL).build()).build())
                .build();

        SscsInterlocDirectionDocument theDocument = SscsInterlocDirectionDocument.builder()
                .documentType(DocumentType.DIRECTION_NOTICE.getValue())
                .documentLink(DocumentLink.builder().documentUrl(DOCUMENT_URL2).build())
                .documentDateAdded(LocalDate.now()).build();

        sscsCaseData.setSscsInterlocDirectionDocument(theDocument);

        sscsDocuments.add(document1);
        sscsCaseData.setSscsDocument(sscsDocuments);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(footerService).createFooterAndAddDocToCase(eq(theDocument.getDocumentLink()), any(), eq(DocumentType.DIRECTION_NOTICE), any(), eq(theDocument.getDocumentDateAdded()), eq(null));
    }

    public void willSetTheWithDwpStateToDirectionActionRequired() {
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getDwpState(), is(DwpState.DIRECTION_ACTION_REQUIRED.getId()));
    }

    @Test
    public void givenDirectionTypeIsNull_displayAnError() {
        callback.getCaseDetails().getCaseData().setDirectionTypeDl(null);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        assertEquals("Direction Type cannot be empty", response.getErrors().toArray()[0]);
    }

    @Test
    public void givenDirectionTypeOfProvideInformation_setInterlocStateToAwaitingInformationAndDirectionTypeIsNull() {
        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.PROVIDE_INFORMATION.toString()));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(AWAITING_INFORMATION.getId(), response.getData().getInterlocReviewState());
        assertNull(response.getData().getDirectionTypeDl());
    }

    @Test
    public void givenDirectionTypeOfAppealToProceedAndCaseIsPreValidInterloc_setInterlocStateToAwaitingAdminActionAndDirectionTypeIsSet() {
        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.APPEAL_TO_PROCEED.toString()));
        when(caseDetails.getState()).thenReturn(State.INTERLOCUTORY_REVIEW_STATE);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(AWAITING_ADMIN_ACTION.getId(), response.getData().getInterlocReviewState());
        assertEquals(DirectionType.APPEAL_TO_PROCEED.toString(), response.getData().getDirectionTypeDl().getValue().getCode());
    }

    @Test
    public void givenDirectionTypeOfAppealToProceedAndCaseIsPreValidInterloc_willReturnValidationErrorsFromExternalService() {
        String errorMessage = "There was an error in the external service";
        when(response.getErrors()).thenReturn(ImmutableSet.of(errorMessage));
        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.APPEAL_TO_PROCEED.toString()));
        when(caseDetails.getState()).thenReturn(State.INTERLOCUTORY_REVIEW_STATE);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertEquals(errorMessage, response.getErrors().iterator().next());
    }

    @Test
    public void givenDirectionTypeOfAppealToProceedAndCaseIsPostValidInterloc_setInterlocStateToAwaitingAdminActionAndDirectionTypeIsSet() {
        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.APPEAL_TO_PROCEED.toString()));
        when(caseDetailsBefore.getState()).thenReturn(State.WITH_DWP);
        when(caseDetails.getState()).thenReturn(State.WITH_DWP);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getInterlocReviewState());
        assertNull(response.getData().getDateSentToDwp());
        assertNull(response.getData().getDirectionTypeDl());
        assertThat(response.getData().getDwpState(), is(DwpState.DIRECTION_ACTION_REQUIRED.getId()));
    }

    @Test
    public void givenDirectionTypeOfGrantExtension_setDwpStateAndDirectionTypeIsNotSet() {
        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.GRANT_EXTENSION.toString()));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getInterlocReviewState());
        assertNull(response.getData().getDirectionTypeDl());
        assertThat(response.getData().getDwpState(), is(DwpState.DIRECTION_ACTION_REQUIRED.getId()));
    }

    @Test
    public void givenDirectionTypeOfRefuseExtensionAndExtensionNextEventIsSendToListing_setResponseReceivedStateAndInterlocStateToAwaitingAdminAction() {
        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.REFUSE_EXTENSION.toString()));

        callback.getCaseDetails().getCaseData().setExtensionNextEventDl(new DynamicList(ExtensionNextEvent.SEND_TO_LISTING.toString()));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(AWAITING_ADMIN_ACTION.getId(), response.getData().getInterlocReviewState());
        assertNull(response.getData().getDirectionTypeDl());
        assertThat(response.getData().getDwpState(), is(DwpState.DIRECTION_ACTION_REQUIRED.getId()));
        assertThat(response.getData().getState(), is(State.RESPONSE_RECEIVED));
        assertThat(response.getData().getHmctsDwpState(), is("sentToDwp"));
        assertThat(response.getData().getDateSentToDwp(), is(LocalDate.now().toString()));
    }

    @Test
    public void givenDirectionTypeOfRefuseExtensionAndExtensionNextEventIsSendToValidAppeal_setWithDwpStateAndDoNotSetInterlocState() {
        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.REFUSE_EXTENSION.toString()));
        callback.getCaseDetails().getCaseData().setExtensionNextEventDl(new DynamicList(ExtensionNextEvent.SEND_TO_VALID_APPEAL.toString()));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getInterlocReviewState());
        assertNull(response.getData().getDirectionTypeDl());
        assertThat(response.getData().getDwpState(), is(DwpState.DIRECTION_ACTION_REQUIRED.getId()));
        assertThat(response.getData().getState(), is(State.WITH_DWP));
        assertThat(response.getData().getHmctsDwpState(), is("sentToDwp"));
        assertThat(response.getData().getDateSentToDwp(), is(LocalDate.now().toString()));
    }

}
