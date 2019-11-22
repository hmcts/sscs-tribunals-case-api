package uk.gov.hmcts.reform.sscs.ccd.presubmit.directionissued;

import static org.hamcrest.CoreMatchers.is;
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
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.DwpState;
import uk.gov.hmcts.reform.sscs.service.FooterService;


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

    private SscsCaseData sscsCaseData;

    private SscsDocument expectedDocument;

    @Before
    public void setUp() {
        initMocks(this);

        handler = new DirectionIssuedAboutToSubmitHandler(footerService);

        when(callback.getEvent()).thenReturn(EventType.DIRECTION_ISSUED);
        when(footerService.createFooterDocument(any(), any(), any(), any(), any(), any())).thenReturn(SscsDocument.builder().value(SscsDocumentDetails.builder().documentType(DocumentType.DIRECTION_NOTICE.getValue()).bundleAddition("A").documentLink(DocumentLink.builder().documentUrl("footerUrl").build()).build()).build());
        when(footerService.getNextBundleAddition(any())).thenReturn("A");

        SscsDocument document = SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName("myTest.doc").build()).build();
        List<SscsDocument> docs = new ArrayList<>();
        docs.add(document);

        sscsCaseData = SscsCaseData.builder()
                .generateNotice("Yes")
                .signedBy("User")
                .directionType(DirectionType.APPEAL_TO_PROCEED)
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
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(caseDetails.getState()).thenReturn(State.INTERLOCUTORY_REVIEW_STATE);
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
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertNull(response.getData().getPreviewDocument());
        assertNull(response.getData().getSignedBy());
        assertNull(response.getData().getSignedRole());
        assertNull(response.getData().getGenerateNotice());
        assertNull(response.getData().getDateAdded());

        assertEquals(2, response.getData().getSscsDocument().size());
        assertEquals("myTest.doc", response.getData().getSscsDocument().get(1).getValue().getDocumentFileName());
        assertEquals(expectedDocument.getValue().getDocumentType(), response.getData().getSscsDocument().get(0).getValue().getDocumentType());
        verify(footerService).createFooterDocument(eq(expectedDocument.getValue().getDocumentLink()), eq("Direction notice"), eq("A"), any(), any(), eq(DocumentType.DIRECTION_NOTICE));
        assertEquals(AWAITING_ADMIN_ACTION.getId(), response.getData().getInterlocReviewState());
        assertEquals(DirectionType.APPEAL_TO_PROCEED, response.getData().getDirectionType());
        verify(footerService).createFooterDocument(any(), eq("Direction notice"), any(), any(), any(), any());
    }

    @Test
    public void givenManuallyUploadedDirectionDocument_thenOverwriteOriginalWithFooterDocument() {
        sscsCaseData.setPreviewDocument(null);
        sscsCaseData.setSscsDocument(null);

        SscsInterlocDirectionDocument theDocument = SscsInterlocDirectionDocument.builder()
                .documentType(DocumentType.DIRECTION_NOTICE.getValue())
                .documentLink(DocumentLink.builder().documentUrl(DOCUMENT_URL).build())
                .documentDateAdded(LocalDate.now()).build();

        sscsCaseData.setSscsInterlocDirectionDocument(theDocument);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getData().getSscsDocument().size());
        assertEquals("A", response.getData().getSscsDocument().get(0).getValue().getBundleAddition());
        assertEquals("footerUrl", response.getData().getSscsDocument().get(0).getValue().getDocumentLink().getDocumentUrl());
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

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(footerService).createFooterDocument(eq(theDocument.getDocumentLink()), eq("Direction notice"), eq("A"), any(), any(), eq(DocumentType.DIRECTION_NOTICE));
        assertEquals(2, response.getData().getSscsDocument().size());
        assertEquals("A", response.getData().getSscsDocument().get(0).getValue().getBundleAddition());
        assertEquals("footerUrl", response.getData().getSscsDocument().get(0).getValue().getDocumentLink().getDocumentUrl());
    }

    public void willSetTheWithDwpStateToDirectionActionRequired() {
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getDwpState(), is(DwpState.DIRECTION_ACTION_REQUIRED.getId()));
    }

    @Test
    public void givenDirectionTypeIsNull_displayAnError() {
        callback.getCaseDetails().getCaseData().setDirectionType(null);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        assertEquals("Direction Type cannot be empty", response.getErrors().toArray()[0]);
    }

    @Test
    public void givenDirectionTypeOfProvideInformation_setInterlocStateToAwaitingInformationAndDirectionTypeIsNull() {
        callback.getCaseDetails().getCaseData().setDirectionType(DirectionType.PROVIDE_INFORMATION);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(AWAITING_INFORMATION.getId(), response.getData().getInterlocReviewState());
        assertNull(response.getData().getDirectionType());
    }

    @Test
    public void givenDirectionTypeOfAppealToProceedAndCaseIsPreValidInterloc_setInterlocStateToAwaitingAdminActionAndDirectionTypeIsSet() {
        callback.getCaseDetails().getCaseData().setDirectionType(DirectionType.APPEAL_TO_PROCEED);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(AWAITING_ADMIN_ACTION.getId(), response.getData().getInterlocReviewState());
        assertEquals(DirectionType.APPEAL_TO_PROCEED, response.getData().getDirectionType());
        assertThat(response.getData().getDwpState(), is(DwpState.DIRECTION_ACTION_REQUIRED.getId()));
    }

    @Test
    public void givenDirectionTypeOfAppealToProceedAndCaseIsPostValidInterloc_setInterlocStateToAwaitingAdminActionAndDirectionTypeIsNotSet() {
        callback.getCaseDetails().getCaseData().setDirectionType(DirectionType.APPEAL_TO_PROCEED);
        when(caseDetails.getState()).thenReturn(State.WITH_DWP);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(AWAITING_ADMIN_ACTION.getId(), response.getData().getInterlocReviewState());
        assertNull(response.getData().getDirectionType());
        assertThat(response.getData().getDwpState(), is(DwpState.DIRECTION_ACTION_REQUIRED.getId()));
    }

    @Test
    public void givenDirectionTypeOfGrantExtension_setDwpStateAndDirectionTypeIsNotSet() {
        callback.getCaseDetails().getCaseData().setDirectionType(DirectionType.GRANT_EXTENSION);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getInterlocReviewState());
        assertNull(response.getData().getDirectionType());
        assertThat(response.getData().getDwpState(), is(DwpState.DIRECTION_ACTION_REQUIRED.getId()));
    }

    @Test
    public void givenDirectionTypeOfRefuseExtensionAndExtensionNextEventIsSendToListing_setResponseReceivedStateAndInterlocStateToAwaitingAdminAction() {
        callback.getCaseDetails().getCaseData().setDirectionType(DirectionType.REFUSE_EXTENSION);
        callback.getCaseDetails().getCaseData().setExtensionNextEvent(ExtensionNextEvent.SEND_TO_LISTING);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(AWAITING_ADMIN_ACTION.getId(), response.getData().getInterlocReviewState());
        assertNull(response.getData().getDirectionType());
        assertThat(response.getData().getDwpState(), is(DwpState.DIRECTION_ACTION_REQUIRED.getId()));
        assertThat(response.getData().getState(), is(State.RESPONSE_RECEIVED));
    }

    @Test
    public void givenDirectionTypeOfRefuseExtensionAndExtensionNextEventIsSendToValidAppeal_setWithDwpStateAndDoNotSetInterlocState() {
        callback.getCaseDetails().getCaseData().setDirectionType(DirectionType.REFUSE_EXTENSION);
        callback.getCaseDetails().getCaseData().setExtensionNextEvent(ExtensionNextEvent.SEND_TO_VALID_APPEAL);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getInterlocReviewState());
        assertNull(response.getData().getDirectionType());
        assertThat(response.getData().getDwpState(), is(DwpState.DIRECTION_ACTION_REQUIRED.getId()));
        assertThat(response.getData().getState(), is(State.WITH_DWP));
        assertThat(response.getData().getHmctsDwpState(), is("sentToDwp"));
        assertThat(response.getData().getDateSentToDwp(), is(LocalDate.now().toString()));
    }
}
