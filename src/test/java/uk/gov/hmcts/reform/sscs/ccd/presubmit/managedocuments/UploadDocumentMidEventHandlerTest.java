package uk.gov.hmcts.reform.sscs.ccd.presubmit.managedocuments;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DocumentTabChoice.INTERNAL;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DocumentTabChoice.REGULAR;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.InternalCaseDocumentData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;

class UploadDocumentMidEventHandlerTest {

    private UploadDocumentMidEventHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    private SscsCaseData sscsCaseData;
    private SscsCaseData sscsCaseDataBefore;
    @Mock
    private CaseDetails<SscsCaseData> caseDetailsBefore;

    @BeforeEach
    void setUp() {
        openMocks(this);
        handler = new UploadDocumentMidEventHandler(true);
        sscsCaseData = SscsCaseData.builder().internalCaseDocumentData(InternalCaseDocumentData.builder().build()).build();
        sscsCaseDataBefore = SscsCaseData.builder().internalCaseDocumentData(InternalCaseDocumentData.builder().build()).build();
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));
        when(caseDetailsBefore.getCaseData()).thenReturn(sscsCaseDataBefore);
        when(callback.getEvent()).thenReturn(EventType.UPLOAD_DOCUMENT);
    }

    @Test
    void canHandleWithCorrectEventAndCallbackType() {
        assertTrue(handler.canHandle(CallbackType.MID_EVENT, callback));
    }

    @Test
    void canHandleThrowsNullCallback() {
        assertThrows(NullPointerException.class, () -> handler.canHandle(CallbackType.MID_EVENT, null));
    }

    @Test
    void canHandleThrowsNullCallbackType() {
        assertThrows(NullPointerException.class, () -> handler.canHandle(null, callback));
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, mode = EnumSource.Mode.EXCLUDE, names = {"UPLOAD_DOCUMENT"})
    void cannotHandleWithIncorrectEventAndCorrectCallbackType(EventType event) {
        when(callback.getEvent()).thenReturn(event);
        assertFalse(handler.canHandle(CallbackType.MID_EVENT, callback));
    }

    @ParameterizedTest
    @EnumSource(value = CallbackType.class, mode = EnumSource.Mode.EXCLUDE, names = {"MID_EVENT"})
    void cannotHandleWithCorrectEventAndIncorrectCallbackType(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    void handleThrowsIfCannotHandle() {
        assertThrows(IllegalStateException.class, () -> handler.handle(CallbackType.ABOUT_TO_SUBMIT, callback, ""), "Cannot handle callback");
    }

    @Test
    void getDocumentIdFromUrl() {
        SscsDocument sscsDocument = SscsDocument.builder().value(SscsDocumentDetails.builder().documentLink(DocumentLink.builder().documentUrl("http://localhost:8080/documents/123jgsafiuhafskn123").build()).build()).build();
        assertEquals("123jgsafiuhafskn123", UploadDocumentMidEventHandler.getDocumentIdFromUrl(sscsDocument));
    }

    @Test
    void getDocumentIdFromUrlWithInvalidUrl() {
        SscsDocument sscsDocument = SscsDocument.builder().build();
        assertEquals("", UploadDocumentMidEventHandler.getDocumentIdFromUrl(sscsDocument));
    }

    @Test
    void doesNothingIfInternalDocumentFlagOff() {
        handler = new UploadDocumentMidEventHandler(false);
        when(callback.getPageId()).thenReturn("moveDocumentTo");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.MID_EVENT, callback, "");

        assertNotNull(response);
        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    void doesNothingIfInternalDocumentFlagOnPageIdNotMoveDocumentTo() {
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.MID_EVENT, callback, "");
        assertNotNull(response);
        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    void givesErrorIfMoveToInternalAndDocumentsEmptyFlagOn() {
        when(callback.getPageId()).thenReturn("moveDocumentTo");
        sscsCaseData.getInternalCaseDocumentData().setMoveDocumentTo(INTERNAL);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.MID_EVENT, callback, "");
        assertNotNull(response);
        assertTrue(response.getErrors().contains("No documents available to move"));
    }

    @Test
    void givesErrorIfMoveToDocumentsAndInternalDocumentsEmptyFlagOn() {
        when(callback.getPageId()).thenReturn("moveDocumentTo");
        sscsCaseData.getInternalCaseDocumentData().setMoveDocumentTo(REGULAR);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.MID_EVENT, callback, "");
        assertNotNull(response);
        assertTrue(response.getErrors().contains("No Tribunal Internal documents available to move"));
    }

    @Test
    void populatesCorrectDlIfMoveToDocumentsAndInternalDocumentsNotEmptyFlagOn() {
        when(callback.getPageId()).thenReturn("moveDocumentTo");
        sscsCaseData.getInternalCaseDocumentData().setMoveDocumentTo(REGULAR);
        SscsDocument document1 = generateDocumentFromId("1", DocumentType.ADJOURNMENT_NOTICE, false);
        SscsDocument document2 = generateDocumentFromId("2", DocumentType.AUDIO_DOCUMENT, false);
        SscsDocument document3 = generateDocumentFromId("3", DocumentType.VIDEO_DOCUMENT, false);
        SscsDocument document5 = generateDocumentFromId("4", DocumentType.AT38, true);
        SscsDocument document4 = generateDocumentFromId("5", DocumentType.AUDIO_VIDEO_EVIDENCE_DIRECTION_NOTICE, false);
        sscsCaseDataBefore.getInternalCaseDocumentData().setSscsInternalDocument(List.of(document1, document2, document3, document4, document5));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.MID_EVENT, callback, "");
        assertNotNull(response);
        assertTrue(response.getErrors().isEmpty());
        InternalCaseDocumentData internalCaseDocumentData = response.getData().getInternalCaseDocumentData();
        assertNull(internalCaseDocumentData.getMoveDocumentToInternalDocumentsTabDL());
        assertTrue(internalCaseDocumentData.getMoveDocumentToDocumentsTabDL().getValue().isEmpty());
        assertEquals(2, internalCaseDocumentData.getMoveDocumentToDocumentsTabDL().getListItems().size());
        assertTrue(internalCaseDocumentData.getMoveDocumentToDocumentsTabDL().getListItems().contains(new DynamicListItem("id_1", "test1.pdf")));
        assertTrue(internalCaseDocumentData.getMoveDocumentToDocumentsTabDL().getListItems().contains(new DynamicListItem("id_4", "testSetFileName4")));
    }

    @Test
    void populatesCorrectDlIfMoveToInternalDocumentsAndDocumentsNotEmptyFlagOn() {
        when(callback.getPageId()).thenReturn("moveDocumentTo");
        sscsCaseData.getInternalCaseDocumentData().setMoveDocumentTo(INTERNAL);
        SscsDocument document1 = generateDocumentFromId("1", DocumentType.ADJOURNMENT_NOTICE, true);
        SscsDocument document2 = generateDocumentFromId("2", DocumentType.AUDIO_DOCUMENT, false);
        SscsDocument document3 = generateDocumentFromId("3", DocumentType.VIDEO_DOCUMENT, true);
        SscsDocument document5 = generateDocumentFromId("4", DocumentType.AT38, false);
        SscsDocument document4 = generateDocumentFromId("5", DocumentType.AUDIO_VIDEO_EVIDENCE_DIRECTION_NOTICE, false);
        sscsCaseDataBefore.setSscsDocument(List.of(document1, document2, document3, document4, document5));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.MID_EVENT, callback, "");
        assertNotNull(response);
        assertTrue(response.getErrors().isEmpty());
        InternalCaseDocumentData internalCaseDocumentData = response.getData().getInternalCaseDocumentData();
        assertNull(internalCaseDocumentData.getMoveDocumentToDocumentsTabDL());
        assertTrue(internalCaseDocumentData.getMoveDocumentToInternalDocumentsTabDL().getValue().isEmpty());
        assertEquals(2, internalCaseDocumentData.getMoveDocumentToInternalDocumentsTabDL().getListItems().size());
        assertTrue(internalCaseDocumentData.getMoveDocumentToInternalDocumentsTabDL().getListItems().contains(new DynamicListItem("id_1", "testSetFileName1")));
        assertTrue(internalCaseDocumentData.getMoveDocumentToInternalDocumentsTabDL().getListItems().contains(new DynamicListItem("id_4", "test4.pdf")));
    }

    private SscsDocument generateDocumentFromId(String id, DocumentType documentType, boolean hasFileName) {
        return SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentType(documentType.getValue())
                .documentFileName(hasFileName ? "testSetFileName" + id : null)
                .documentLink(DocumentLink.builder().documentUrl("some-slug/id_" + id).documentFilename("test" + id + ".pdf").build())
                .build())
            .build();
    }
}