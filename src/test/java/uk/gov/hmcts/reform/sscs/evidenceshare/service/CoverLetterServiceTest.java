package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.times;
import static uk.gov.hmcts.reform.sscs.evidenceshare.domain.FurtherEvidenceLetterType.APPELLANT_LETTER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.docmosis.service.PdfGenerationService;
import uk.gov.hmcts.reform.sscs.evidenceshare.exception.UnableToContactThirdPartyException;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.FurtherEvidencePlaceholderService;
import uk.gov.hmcts.reform.sscs.service.PdfStoreService;

@ExtendWith(MockitoExtension.class)
public class CoverLetterServiceTest {
    @Mock
    private FurtherEvidencePlaceholderService furtherEvidencePlaceholderService;
    @Mock
    private PdfGenerationService pdfGenerationService;

    @Mock
    private PdfStoreService pdfStoreService;

    private CoverLetterService coverLetterService;

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.openMocks(this);
        coverLetterService = new CoverLetterService(furtherEvidencePlaceholderService, pdfStoreService, pdfGenerationService, 3);
    }

    @ParameterizedTest
    @MethodSource("generateNullScenarios")
    public void givenNullArgs_shouldThrowException(byte[] coverLetterContent, List<Pdf> pdfsToBulkPrint) {
        try {
            coverLetterService.appendCoverLetter(coverLetterContent, pdfsToBulkPrint, "");
            fail();
        } catch (NullPointerException e) {
            assertNotNull(e);
        }
    }

    private static Object[] generateNullScenarios() {
        return new Object[]{
            new Object[]{null, buildPdfListWithOneDoc()},
            new Object[]{new byte[]{'d', 'o', 'c'}, null}
        };
    }

    @ParameterizedTest
    public void appendCoverLetter() {
        List<Pdf> pdfsToBulkPrint = buildPdfListWithOneDoc();
        coverLetterService.appendCoverLetter(new byte[]{'l', 'e', 't', 't', 'e', 'r'}, pdfsToBulkPrint, "609_97_OriginalSenderCoverLetter");
        assertCoverLetterIsFirstDocInList(pdfsToBulkPrint);
        assertEquals("doc", pdfsToBulkPrint.get(1).getName());
        assertEquals(Arrays.toString(new byte[]{'d', 'o', 'c'}), Arrays.toString(pdfsToBulkPrint.get(1).getContent()));
    }

    @ParameterizedTest
    public void generateCoverLetter() {
        SscsCaseData caseData = SscsCaseData.builder().build();

        given(furtherEvidencePlaceholderService
            .populatePlaceholders(eq(caseData), eq(APPELLANT_LETTER), eq(null)))
            .willReturn(Collections.singletonMap("someKey", "someValue"));

        given(pdfGenerationService.generatePdf(any(DocumentHolder.class)))
            .willReturn(new byte[]{'l', 'e', 't', 't', 'e', 'r'});

        coverLetterService.generateCoverLetter(caseData, APPELLANT_LETTER, "testName.doc", "testDocName", null);

        then(furtherEvidencePlaceholderService).should(times(1))
            .populatePlaceholders(eq(caseData), eq(APPELLANT_LETTER), eq(null));

        assertArgumentsForPdfGeneration();
    }

    @ParameterizedTest
    public void generateCoverLetterHandleError() {
        assertThrows(UnableToContactThirdPartyException.class, () -> {
            SscsCaseData caseData = SscsCaseData.builder().build();

            given(furtherEvidencePlaceholderService
                .populatePlaceholders(eq(caseData), eq(APPELLANT_LETTER), eq(null)))
                .willReturn(Collections.singletonMap("someKey", "someValue"));

            when(pdfGenerationService.generatePdf(any(DocumentHolder.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.valueOf(400)));

            coverLetterService.generateCoverLetter(caseData, APPELLANT_LETTER, "testName.doc", "testDocName", null);
        });
    }

    @ParameterizedTest
    public void givenDocumentLink_returnExpectedDocuments() {
        String documentFilename1 = "filename1";
        String documentFilename2 = "filename2";

        DynamicListItem item1 = new DynamicListItem(documentFilename1, null);
        DynamicListItem item2 = new DynamicListItem(documentFilename2, null);
        DynamicList list1 = new DynamicList(item1, null);
        DynamicList list2 = new DynamicList(item2, null);

        DocumentSelectionDetails documentSelectionDetails1 = new DocumentSelectionDetails(list1);
        DocumentSelectionDetails documentSelectionDetails2 = new DocumentSelectionDetails(list2);

        DwpDocument dwpDocument = getDwpDocument(documentFilename1);
        SscsDocument sscsDocument = getSscsDocument(documentFilename2);

        List<CcdValue<DocumentSelectionDetails>> documentSelection = List.of(
            new CcdValue<>(documentSelectionDetails1),
            new CcdValue<>(documentSelectionDetails2)
        );

        SscsCaseData caseData = SscsCaseData.builder()
            .documentSelection(documentSelection)
            .dwpDocuments(List.of(dwpDocument))
            .sscsDocument(List.of(sscsDocument))
            .build();

        List<Pdf> pdfs = coverLetterService.getSelectedDocuments(caseData);

        assertNotNull(pdfs);
        assertEquals(2, pdfs.size());
    }

    @ParameterizedTest
    public void givenEditedDocs_returnEditedPdfs() {
        String uneditedSscsFilename = "sscs";
        String editedSscsFilename = "sscs_edited";
        String uneditedDwpFilename = "dwp";
        String editedDwpFilename = "dwp_edited";


        DynamicListItem item1 = new DynamicListItem(editedSscsFilename, null);
        DynamicListItem item2 = new DynamicListItem(editedDwpFilename, null);
        DynamicList list1 = new DynamicList(item1, null);
        DynamicList list2 = new DynamicList(item2, null);

        DocumentSelectionDetails documentSelectionDetails1 = new DocumentSelectionDetails(list1);
        DocumentSelectionDetails documentSelectionDetails2 = new DocumentSelectionDetails(list2);

        SscsDocument sscsDocument = getSscsDocumentEdited(uneditedSscsFilename, editedSscsFilename);
        DwpDocument dwpDocument = getDwpDocumentEdited(uneditedDwpFilename, editedDwpFilename);

        List<CcdValue<DocumentSelectionDetails>> documentSelection = List.of(
            new CcdValue<>(documentSelectionDetails1),
            new CcdValue<>(documentSelectionDetails2)
        );

        SscsCaseData caseData = SscsCaseData.builder()
            .documentSelection(documentSelection)
            .sscsDocument(List.of(sscsDocument))
            .dwpDocuments(List.of(dwpDocument))
            .build();

        List<Pdf> pdfs = coverLetterService.getSelectedDocuments(caseData);
        assertNotNull(pdfs);
        assertNotNull(pdfs.stream().filter(pdf -> editedSscsFilename.equals(pdf.getName())).findAny().get());
        assertNotNull(pdfs.stream().filter(pdf -> editedDwpFilename.equals(pdf.getName())).findAny().get());
    }


    @ParameterizedTest
    public void givenNoDocumentExist_returnEmptyList() {
        String documentFilename1 = "filename1";

        DynamicListItem item1 = new DynamicListItem(documentFilename1, null);
        DynamicList list1 = new DynamicList(item1, null);

        DocumentSelectionDetails documentSelectionDetails1 = new DocumentSelectionDetails(list1);

        SscsCaseData caseData = SscsCaseData.builder()
            .documentSelection(List.of(new CcdValue<>(documentSelectionDetails1)))
            .build();

        List<Pdf> pdfs = coverLetterService.getSelectedDocuments(caseData);

        assertNotNull(pdfs);
        assertTrue(pdfs.isEmpty());
    }

    private static DwpDocument getDwpDocument(String documentFilename) {
        DwpDocumentDetails details = DwpDocumentDetails
            .builder()
            .documentLink(new DocumentLink("url1", "url2", documentFilename, "hash"))
            .documentFileName(documentFilename)
            .build();

        DwpDocument document = DwpDocument.builder()
            .value(details)
            .build();
        return document;
    }

    private static DwpDocument getDwpDocumentEdited(String documentFilename, String editedDocumentFileName) {
        DwpDocumentDetails details = DwpDocumentDetails
            .builder()
            .documentLink(new DocumentLink("url1", "url2", documentFilename, "hash"))
            .documentFileName(documentFilename)
            .editedDocumentLink(new DocumentLink("url2", "url2", editedDocumentFileName, "hash"))
            .build();

        DwpDocument document = DwpDocument.builder()
            .value(details)
            .build();
        return document;
    }

    private static SscsDocument getSscsDocument(String documentFilename) {
        SscsDocumentDetails details = SscsDocumentDetails
            .builder()
            .documentLink(new DocumentLink("url1", "url2", documentFilename, "hash"))
            .documentFileName(documentFilename)
            .build();

        SscsDocument document = SscsDocument.builder()
            .value(details)
            .build();
        return document;
    }

    private static SscsDocument getSscsDocumentEdited(String documentFilename, String editedDocumentFileName) {
        SscsDocumentDetails details = SscsDocumentDetails
            .builder()
            .documentLink(new DocumentLink("url1", "url2", documentFilename, "hash"))
            .documentFileName(documentFilename)
            .editedDocumentLink(new DocumentLink("url2", "url2", editedDocumentFileName, "hash"))
            .build();

        SscsDocument document = SscsDocument.builder()
            .value(details)
            .build();
        return document;
    }


    private void assertArgumentsForPdfGeneration() {
        ArgumentCaptor<DocumentHolder> argumentCaptor = ArgumentCaptor.forClass(DocumentHolder.class);
        then(pdfGenerationService).should(times(1)).generatePdf(argumentCaptor.capture());
        DocumentHolder documentHolder = argumentCaptor.getValue();
        assertEquals("testName.doc", documentHolder.getTemplate().getTemplateName());
        assertEquals(Collections.singletonMap("someKey", "someValue").toString(),
            documentHolder.getPlaceholders().toString());
        assertTrue(documentHolder.isPdfArchiveMode());
    }

    private void assertCoverLetterIsFirstDocInList(List<Pdf> pdfsToBulkPrint) {
        assertEquals(2, pdfsToBulkPrint.size());
        assertEquals("609_97_OriginalSenderCoverLetter", pdfsToBulkPrint.get(0).getName());
        assertEquals(Arrays.toString(new byte[]{'l', 'e', 't', 't', 'e', 'r'}),
            Arrays.toString(pdfsToBulkPrint.get(0).getContent()));
    }

    private static List<Pdf> buildPdfListWithOneDoc() {
        List<Pdf> docList = new ArrayList<>(1);
        docList.add(buildPdf());
        return docList;
    }

    private static Pdf buildPdf() {
        byte[] content = new byte[]{'d', 'o', 'c'};
        return new Pdf(content, "doc");
    }
}
