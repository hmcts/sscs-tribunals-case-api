package uk.gov.hmcts.reform.sscs.ccd.presubmit.managedocuments;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DocumentTabChoice.INTERNAL;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DocumentTabChoice.REGULAR;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.managedocuments.UploadDocumentAboutToSubmitHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentTabChoice;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicMixedChoiceList;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.InternalCaseDocumentData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.service.FooterService;

class UploadDocumentAboutToSubmitHandlerTest {

    private UploadDocumentAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    private SscsCaseData sscsCaseData;
    @Mock
    private FooterService footerService;

    @BeforeEach
    void setUp() {
        openMocks(this);
        handler = new UploadDocumentAboutToSubmitHandler(footerService, true);
        sscsCaseData = SscsCaseData.builder().internalCaseDocumentData(InternalCaseDocumentData.builder().build()).build();
        sscsCaseData.getInternalCaseDocumentData().setUploadRemoveDocumentType("not null");
        sscsCaseData.getInternalCaseDocumentData().setUploadRemoveOrMoveDocument("not null");
        sscsCaseData.getInternalCaseDocumentData().setMoveDocumentToDocumentsTabDL(new DynamicMixedChoiceList(Collections.emptyList(), Collections.emptyList()));
        sscsCaseData.getInternalCaseDocumentData().setMoveDocumentToInternalDocumentsTabDL(new DynamicMixedChoiceList(Collections.emptyList(), Collections.emptyList()));
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(callback.getEvent()).thenReturn(EventType.UPLOAD_DOCUMENT);
    }

    @Test
    void canHandleWithCorrectEventAndCallbackType() {
        assertTrue(handler.canHandle(CallbackType.ABOUT_TO_SUBMIT, callback));
    }

    @Test
    void canHandleThrowsNullCallback() {
        assertThrows(NullPointerException.class, () -> handler.canHandle(CallbackType.ABOUT_TO_SUBMIT, null));
    }

    @Test
    void canHandleThrowsNullCallbackType() {
        assertThrows(NullPointerException.class, () -> handler.canHandle(null, callback));
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, mode = EnumSource.Mode.EXCLUDE, names = {"UPLOAD_DOCUMENT"})
    void cannotHandleWithIncorrectEventAndCorrectCallbackType(EventType event) {
        when(callback.getEvent()).thenReturn(event);
        assertFalse(handler.canHandle(CallbackType.ABOUT_TO_SUBMIT, callback));
    }

    @ParameterizedTest
    @EnumSource(value = CallbackType.class, mode = EnumSource.Mode.EXCLUDE, names = {"ABOUT_TO_SUBMIT"})
    void cannotHandleWithCorrectEventAndIncorrectCallbackType(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    void handleThrowsIfCannotHandle() {
        assertThrows(IllegalStateException.class, () -> handler.handle(CallbackType.ABOUT_TO_START, callback, ""), "Cannot handle callback");
    }

    @Test
    void doesNothingIfInternalDocumentFlagOff() {
        handler = new UploadDocumentAboutToSubmitHandler(footerService, false);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.ABOUT_TO_SUBMIT, callback, "");
        assertNotNull(response);
        assertTrue(response.getErrors().isEmpty());
        assertNotNull(sscsCaseData.getInternalCaseDocumentData().getUploadRemoveDocumentType());
        assertNotNull(sscsCaseData.getInternalCaseDocumentData().getUploadRemoveOrMoveDocument());
        assertNotNull(sscsCaseData.getInternalCaseDocumentData().getMoveDocumentToDocumentsTabDL());
        assertNotNull(sscsCaseData.getInternalCaseDocumentData().getMoveDocumentToInternalDocumentsTabDL());
    }

    @Test
    void wipesSpecificFieldsIfFlagOn() {
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.ABOUT_TO_SUBMIT, callback, "");
        assertNotNull(response);
        assertTrue(response.getErrors().isEmpty());
        assertNull(sscsCaseData.getInternalCaseDocumentData().getUploadRemoveDocumentType());
        assertNull(sscsCaseData.getInternalCaseDocumentData().getUploadRemoveOrMoveDocument());
        assertNull(sscsCaseData.getInternalCaseDocumentData().getMoveDocumentToDocumentsTabDL());
        assertNull(sscsCaseData.getInternalCaseDocumentData().getMoveDocumentToInternalDocumentsTabDL());
    }

    @Test
    void setsInternalDocsBundleFieldsIfFlagOn() {
        SscsDocument sscsDocument = SscsDocument.builder().value(SscsDocumentDetails.builder()
            .bundleAddition("A").documentFileName("file").build()).build();
        sscsCaseData.getInternalCaseDocumentData().setSscsInternalDocument(List.of(sscsDocument));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.ABOUT_TO_SUBMIT, callback, "");
        assertNotNull(response);
        assertTrue(response.getErrors().isEmpty());
        assertNull(sscsCaseData.getInternalCaseDocumentData().getUploadRemoveDocumentType());
        assertNull(sscsCaseData.getInternalCaseDocumentData().getUploadRemoveOrMoveDocument());
        assertNull(sscsCaseData.getInternalCaseDocumentData().getMoveDocumentToDocumentsTabDL());
        assertNull(sscsCaseData.getInternalCaseDocumentData().getMoveDocumentToInternalDocumentsTabDL());
        assertNull(sscsCaseData.getInternalCaseDocumentData().getSscsInternalDocument().getFirst().getValue().getBundleAddition());
        assertNull(sscsCaseData.getInternalCaseDocumentData().getSscsInternalDocument().getFirst().getValue().getEvidenceIssued());
    }

    @ParameterizedTest
    @EnumSource(value = DocumentTabChoice.class, names = {"INTERNAL", "REGULAR"})
    void ifMoveAndDlNoChoiceThenReturnError(DocumentTabChoice documentTabChoice) {
        sscsCaseData.getInternalCaseDocumentData().setUploadRemoveOrMoveDocument("move");
        sscsCaseData.getInternalCaseDocumentData().setMoveDocumentTo(documentTabChoice);
        setDynamicList(sscsCaseData, documentTabChoice.equals(INTERNAL), new DynamicMixedChoiceList(Collections.emptyList(), Collections.emptyList()));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.ABOUT_TO_SUBMIT, callback, "");
        assertNotNull(response);
        assertEquals(1, response.getErrors().size());
        assertTrue(response.getErrors().contains("Please select at least one document to move"));
    }

    @ParameterizedTest
    @EnumSource(value = DocumentTabChoice.class, names = {"INTERNAL", "REGULAR"})
    void ifMoveAndDlOptionsDoNotExistOnCaseThenReturnErrors(DocumentTabChoice documentTabChoice) {
        sscsCaseData.getInternalCaseDocumentData().setUploadRemoveOrMoveDocument("move");
        sscsCaseData.getInternalCaseDocumentData().setMoveDocumentTo(documentTabChoice);
        DynamicListItem dynamicListItem = new DynamicListItem("code", "label");
        DynamicListItem dynamicListItem2 = new DynamicListItem("code2", "label2");
        DynamicMixedChoiceList dynamicMixedChoiceList = new DynamicMixedChoiceList(List.of(dynamicListItem, dynamicListItem2),
            List.of(dynamicListItem, dynamicListItem2));
        setDynamicList(sscsCaseData, documentTabChoice.equals(INTERNAL), dynamicMixedChoiceList);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.ABOUT_TO_SUBMIT, callback, "");
        assertNotNull(response);
        assertEquals(2, response.getErrors().size());
        assertTrue(response.getErrors().contains("Document label could not be found on the case."));
        assertTrue(response.getErrors().contains("Document label2 could not be found on the case."));
    }

    @Test
    void movesToInternalDocumentTabSuccessfully() {
        sscsCaseData.getInternalCaseDocumentData().setUploadRemoveOrMoveDocument("move");
        DynamicListItem dynamicListItem = new DynamicListItem("id_1", "label");
        DynamicListItem dynamicListItem2 = new DynamicListItem("id_2", "label2");
        DynamicListItem dynamicListItem3 = new DynamicListItem("id_3", "label3");
        SscsDocument sscsDocument1 = generateDocumentFromId("1", DocumentType.APPELLANT_EVIDENCE);
        SscsDocument sscsDocument2 = generateDocumentFromId("2", DocumentType.DWP_EVIDENCE);
        SscsDocument sscsDocument3 = generateDocumentFromId("3", DocumentType.SET_ASIDE_APPLICATION);
        sscsCaseData.setSscsDocument(List.of(sscsDocument1, sscsDocument2, sscsDocument3));
        sscsCaseData.getInternalCaseDocumentData().setMoveDocumentTo(INTERNAL);
        DynamicMixedChoiceList dynamicMixedChoiceList = new DynamicMixedChoiceList(List.of(dynamicListItem, dynamicListItem2),
            List.of(dynamicListItem, dynamicListItem2, dynamicListItem3));
        setDynamicList(sscsCaseData, true, dynamicMixedChoiceList);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.ABOUT_TO_SUBMIT, callback, "");
        assertNotNull(response);
        assertTrue(response.getErrors().isEmpty());
        assertEquals(2, sscsCaseData.getInternalCaseDocumentData().getSscsInternalDocument().size());
        assertEquals(1, sscsCaseData.getSscsDocument().size());
        SscsDocumentDetails internalDoc1 = sscsCaseData.getInternalCaseDocumentData().getSscsInternalDocument().get(0).getValue();
        SscsDocumentDetails internalDoc2 = sscsCaseData.getInternalCaseDocumentData().getSscsInternalDocument().get(1).getValue();
        SscsDocumentDetails doc1 = sscsCaseData.getSscsDocument().getFirst().getValue();
        assertEquals("some-slug/id_2", internalDoc1.getDocumentLink().getDocumentUrl());
        assertEquals("some-slug/id_1", internalDoc2.getDocumentLink().getDocumentUrl());
        assertEquals("some-slug/id_3", doc1.getDocumentLink().getDocumentUrl());
    }

    @Test
    void movesToDocumentTabSuccessfullyWithoutIssued() {
        sscsCaseData.getInternalCaseDocumentData().setUploadRemoveOrMoveDocument("move");
        DynamicListItem dynamicListItem = new DynamicListItem("id_1", "label");
        DynamicListItem dynamicListItem2 = new DynamicListItem("id_2", "label2");
        DynamicListItem dynamicListItem3 = new DynamicListItem("id_3", "label3");
        SscsDocument sscsDocument1 = generateDocumentFromId("1", DocumentType.APPELLANT_EVIDENCE);
        SscsDocument sscsDocument2 = generateDocumentFromId("2", DocumentType.DWP_EVIDENCE);
        SscsDocument sscsDocument3 = generateDocumentFromId("3", DocumentType.SET_ASIDE_APPLICATION);
        sscsCaseData.getInternalCaseDocumentData().setSscsInternalDocument(List.of(sscsDocument1, sscsDocument2, sscsDocument3));
        sscsCaseData.getInternalCaseDocumentData().setMoveDocumentTo(REGULAR);
        DynamicMixedChoiceList dynamicMixedChoiceList = new DynamicMixedChoiceList(List.of(dynamicListItem, dynamicListItem2),
            List.of(dynamicListItem, dynamicListItem2, dynamicListItem3));
        setDynamicList(sscsCaseData, false, dynamicMixedChoiceList);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.ABOUT_TO_SUBMIT, callback, "");
        assertNotNull(response);
        assertTrue(response.getErrors().isEmpty());
        assertEquals(1, sscsCaseData.getInternalCaseDocumentData().getSscsInternalDocument().size());
        assertEquals(2, sscsCaseData.getSscsDocument().size());
        SscsDocumentDetails doc1 = sscsCaseData.getSscsDocument().get(0).getValue();
        SscsDocumentDetails doc2 = sscsCaseData.getSscsDocument().get(1).getValue();
        SscsDocumentDetails internalDoc1 = sscsCaseData.getInternalCaseDocumentData().getSscsInternalDocument().getFirst().getValue();
        assertEquals("some-slug/id_2", doc1.getDocumentLink().getDocumentUrl());
        assertEquals("some-slug/id_1", doc2.getDocumentLink().getDocumentUrl());
        assertEquals("some-slug/id_3", internalDoc1.getDocumentLink().getDocumentUrl());
    }

    @Test
    void movesToDocumentTabErrorsInvalidDocumentTypesWithIssued() {
        sscsCaseData.getInternalCaseDocumentData().setUploadRemoveOrMoveDocument("move");
        sscsCaseData.getInternalCaseDocumentData().setShouldBeIssued(YesNo.YES);
        DynamicListItem dynamicListItem = new DynamicListItem("id_1", "label");
        DynamicListItem dynamicListItem2 = new DynamicListItem("id_2", "label2");
        DynamicListItem dynamicListItem3 = new DynamicListItem("id_3", "label3");
        SscsDocument sscsDocument1 = SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentType("invalid")
                .documentLink(DocumentLink.builder().documentUrl("some-slug/id_1").documentFilename("test1.pdf").build())
                .build())
            .build();
        SscsDocument sscsDocument2 = generateDocumentFromId("2", DocumentType.DWP_EVIDENCE);
        SscsDocument sscsDocument3 = SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentType("invalid")
                .documentFileName("someOtherFileName.pdf")
                .documentLink(DocumentLink.builder().documentUrl("some-slug/id_3").documentFilename("test3.pdf").build())
                .build())
            .build();
        sscsCaseData.getInternalCaseDocumentData().setSscsInternalDocument(List.of(sscsDocument1, sscsDocument2, sscsDocument3));
        sscsCaseData.getInternalCaseDocumentData().setMoveDocumentTo(REGULAR);
        DynamicMixedChoiceList dynamicMixedChoiceList = new DynamicMixedChoiceList(List.of(dynamicListItem, dynamicListItem2, dynamicListItem3),
            List.of(dynamicListItem, dynamicListItem2, dynamicListItem3));
        setDynamicList(sscsCaseData, false, dynamicMixedChoiceList);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.ABOUT_TO_SUBMIT, callback, "");
        assertNotNull(response);
        assertEquals(2, response.getErrors().size());
        assertTrue(response.getErrors().contains("Document needs a valid Document Type to be moved: test1.pdf"));
        assertTrue(response.getErrors().contains("Document needs a valid Document Type to be moved: someOtherFileName.pdf"));
    }

    @Test
    void movesToDocumentTabSuccessfullyWithIssued() {
        sscsCaseData.getInternalCaseDocumentData().setUploadRemoveOrMoveDocument("move");
        sscsCaseData.getInternalCaseDocumentData().setShouldBeIssued(YesNo.YES);
        mockedFooterServiceMethod();
        DynamicListItem dynamicListItem = new DynamicListItem("id_1", "label");
        DynamicListItem dynamicListItem2 = new DynamicListItem("id_2", "label2");
        DynamicListItem dynamicListItem3 = new DynamicListItem("id_3", "label3");
        SscsDocument sscsDocument1 = generateDocumentFromId("1", DocumentType.APPELLANT_EVIDENCE);
        SscsDocument sscsDocument2 = generateDocumentFromId("2", DocumentType.DWP_EVIDENCE);
        SscsDocument sscsDocument3 = generateDocumentFromId("3", DocumentType.SET_ASIDE_APPLICATION);
        sscsCaseData.getInternalCaseDocumentData().setSscsInternalDocument(List.of(sscsDocument1, sscsDocument2, sscsDocument3));
        sscsCaseData.getInternalCaseDocumentData().setMoveDocumentTo(REGULAR);
        DynamicMixedChoiceList dynamicMixedChoiceList = new DynamicMixedChoiceList(List.of(dynamicListItem, dynamicListItem2, dynamicListItem3),
            List.of(dynamicListItem, dynamicListItem2, dynamicListItem3));
        setDynamicList(sscsCaseData, false, dynamicMixedChoiceList);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.ABOUT_TO_SUBMIT, callback, "");
        assertNotNull(response);
        assertTrue(response.getErrors().isEmpty());
        assertNull(sscsCaseData.getInternalCaseDocumentData().getSscsInternalDocument());
        assertEquals(3, sscsCaseData.getSscsDocument().size());
        SscsDocumentDetails doc1 = sscsCaseData.getSscsDocument().get(0).getValue();
        SscsDocumentDetails doc2 = sscsCaseData.getSscsDocument().get(1).getValue();
        SscsDocumentDetails doc3 = sscsCaseData.getSscsDocument().get(2).getValue();
        assertEquals("some-slug/id_1", doc1.getDocumentLink().getDocumentUrl());
        assertEquals("some-slug/id_2", doc2.getDocumentLink().getDocumentUrl());
        assertEquals("some-slug/id_3", doc3.getDocumentLink().getDocumentUrl());
        assertEquals("Addition A - fileName.pdf", doc2.getDocumentFileName());
        verify(footerService).createFooterAndAddDocToCase(eq(sscsDocument1.getValue().getDocumentLink()), eq(sscsCaseData), eq(DocumentType.APPELLANT_EVIDENCE), any(), eq(null), eq(null), eq(null), eq(null), eq(true));
        verify(footerService).createFooterAndAddDocToCase(eq(sscsDocument2.getValue().getDocumentLink()), eq(sscsCaseData), eq(DocumentType.DWP_EVIDENCE), any(), eq(null), eq(null), eq(null), eq(null), eq(true));
        verify(footerService).createFooterAndAddDocToCase(eq(sscsDocument3.getValue().getDocumentLink()), eq(sscsCaseData), eq(DocumentType.SET_ASIDE_APPLICATION), any(), eq(null), eq(null), eq(null), eq(null), eq(true));
    }

    private SscsDocument generateDocumentFromId(String id, DocumentType documentType) {
        return SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentType(documentType.getValue())
                .documentLink(DocumentLink.builder().documentUrl("some-slug/id_" + id).documentFilename("test" + id + ".pdf").build())
                .build())
            .build();
    }

    private void mockedFooterServiceMethod() {
        doAnswer(invocation -> {
            DocumentLink url = invocation.getArgument(0);
            SscsCaseData caseData = invocation.getArgument(1);
            DocumentType documentType = invocation.getArgument(2);
            LocalDate dateAdded = invocation.getArgument(4);
            SscsDocumentTranslationStatus documentTranslationStatus = invocation.getArgument(6);
            SscsDocument sscsDocument = SscsDocument.builder().value(SscsDocumentDetails.builder()
                    .documentFileName("Addition " + "A" + " - " + "fileName" + ".pdf")
                    .documentLink(url)
                    .bundleAddition("A")
                    .documentDateAdded(Optional.ofNullable(dateAdded).orElse(LocalDate.now()).format(DateTimeFormatter.ISO_DATE))
                    .documentType(documentType.getValue())
                    .documentTranslationStatus(documentTranslationStatus)
                    .originalPartySender(null)
                    .build())
                .build();
            List<SscsDocument> documents = Optional.ofNullable(caseData.getSscsDocument()).orElse(new ArrayList<>());
            documents.add(sscsDocument);
            caseData.setSscsDocument(documents);
            return null;
        }).when(footerService).createFooterAndAddDocToCase(any(), any(), any(), any(), any(), any(), any(), any(), eq(true));
    }

    private void setDynamicList(SscsCaseData sscsCaseData, boolean isInternal, DynamicMixedChoiceList dynamicMixedChoiceList) {
        if (isInternal) {
            sscsCaseData.getInternalCaseDocumentData().setMoveDocumentToInternalDocumentsTabDL(dynamicMixedChoiceList);
        } else {
            sscsCaseData.getInternalCaseDocumentData().setMoveDocumentToDocumentsTabDL(dynamicMixedChoiceList);
        }
    }

}
