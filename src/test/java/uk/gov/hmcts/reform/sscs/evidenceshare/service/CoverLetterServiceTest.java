package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static uk.gov.hmcts.reform.sscs.evidenceshare.domain.FurtherEvidenceLetterType.APPELLANT_LETTER;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentSelectionDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.docmosis.service.PdfGenerationService;
import uk.gov.hmcts.reform.sscs.evidenceshare.exception.UnableToContactThirdPartyException;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.FurtherEvidencePlaceholderService;
import uk.gov.hmcts.reform.sscs.service.PdfStoreService;

@ExtendWith(MockitoExtension.class)
class CoverLetterServiceTest {

    @Mock
    private FurtherEvidencePlaceholderService furtherEvidencePlaceholderService;
    @Mock
    private PdfGenerationService pdfGenerationService;

    @Mock
    private PdfStoreService pdfStoreService;

    private CoverLetterService coverLetterService;

    @BeforeEach
    void initMocks() {
        coverLetterService = new CoverLetterService(furtherEvidencePlaceholderService, pdfStoreService, pdfGenerationService, 3);
    }

    @ParameterizedTest
    @MethodSource("generateNullScenarios")
    void givenNullArgs_shouldThrowException(byte[] coverLetterContent, List<Pdf> pdfsToBulkPrint) {
        assertThatThrownBy(() -> coverLetterService.appendCoverLetter(coverLetterContent, pdfsToBulkPrint, ""))
            .isInstanceOf(NullPointerException.class);
    }

    private static Object[][] generateNullScenarios() {
        return new Object[][]{
            new Object[]{null, buildPdfListWithOneDoc()},
            new Object[]{new byte[]{'d', 'o', 'c'}, null}
        };
    }

    @Test
    void appendCoverLetter() {
        List<Pdf> pdfsToBulkPrint = buildPdfListWithOneDoc();
        coverLetterService.appendCoverLetter(new byte[]{'l', 'e', 't', 't', 'e', 'r'}, pdfsToBulkPrint,
            "609_97_OriginalSenderCoverLetter");
        assertCoverLetterIsFirstDocInList(pdfsToBulkPrint);
        assertThat(pdfsToBulkPrint.get(1).getName()).isEqualTo("doc");
        assertThat(pdfsToBulkPrint.get(1).getContent()).isEqualTo(new byte[]{'d', 'o', 'c'});
    }

    @Test
    void generateCoverLetter() {
        SscsCaseData caseData = SscsCaseData.builder().build();

        given(furtherEvidencePlaceholderService
            .populatePlaceholders(caseData, APPELLANT_LETTER, null))
            .willReturn(singletonMap("someKey", "someValue"));

        given(pdfGenerationService.generatePdf(any(DocumentHolder.class)))
            .willReturn(new byte[]{'l', 'e', 't', 't', 'e', 'r'});

        coverLetterService.generateCoverLetter(caseData, APPELLANT_LETTER, "testName.doc", "testDocName", null);

        then(furtherEvidencePlaceholderService).should(times(1))
                                               .populatePlaceholders(eq(caseData), eq(APPELLANT_LETTER), eq(null));

        assertArgumentsForPdfGeneration();
    }

    @Test
    void generateCoverLetterHandleError() {
        SscsCaseData caseData = SscsCaseData.builder().build();

        given(furtherEvidencePlaceholderService
            .populatePlaceholders(caseData, APPELLANT_LETTER, null))
            .willReturn(singletonMap("someKey", "someValue"));

        when(pdfGenerationService.generatePdf(any(DocumentHolder.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.valueOf(400)));

        assertThatThrownBy(
            () -> coverLetterService.generateCoverLetter(caseData, APPELLANT_LETTER, "testName.doc", "testDocName", null))
            .isInstanceOf(UnableToContactThirdPartyException.class);
    }

    @Test
    void givenDocumentLink_returnExpectedDocuments() {
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

        assertThat(pdfs).isNotNull().hasSize(2);
    }

    @Test
    void givenEditedDocs_returnEditedPdfs() {
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
        assertThat(pdfs).isNotNull();
        assertThat(pdfs.stream().filter(pdf -> editedSscsFilename.equals(pdf.getName())).findAny()).isPresent();
        assertThat(pdfs.stream().filter(pdf -> editedDwpFilename.equals(pdf.getName())).findAny()).isPresent();
    }


    @Test
    void givenNullDocumentSelection_returnEmptyList() {
        final SscsCaseData caseData = SscsCaseData.builder().documentSelection(null).build();

        final List<Pdf> pdfs = coverLetterService.getSelectedDocuments(caseData);

        assertThat(pdfs).isEmpty();
        then(pdfStoreService).should(never()).download(any());
    }

    @Test
    void givenNoDocumentExist_returnEmptyList() {
        String documentFilename1 = "filename1";

        DynamicListItem item1 = new DynamicListItem(documentFilename1, null);
        DynamicList list1 = new DynamicList(item1, null);

        DocumentSelectionDetails documentSelectionDetails1 = new DocumentSelectionDetails(list1);

        SscsCaseData caseData = SscsCaseData.builder()
                                            .documentSelection(List.of(new CcdValue<>(documentSelectionDetails1)))
                                            .build();

        List<Pdf> pdfs = coverLetterService.getSelectedDocuments(caseData);

        assertThat(pdfs).isNotNull().isEmpty();
    }

    private static DwpDocument getDwpDocument(String documentFilename) {
        DwpDocumentDetails details = DwpDocumentDetails
            .builder()
            .documentLink(new DocumentLink("url1", "url2", documentFilename, "hash"))
            .documentFileName(documentFilename)
            .build();

        return DwpDocument.builder()
                                          .value(details)
                                          .build();
    }

    private static DwpDocument getDwpDocumentEdited(String documentFilename, String editedDocumentFileName) {
        DwpDocumentDetails details = DwpDocumentDetails
            .builder()
            .documentLink(new DocumentLink("url1", "url2", documentFilename, "hash"))
            .documentFileName(documentFilename)
            .editedDocumentLink(new DocumentLink("url2", "url2", editedDocumentFileName, "hash"))
            .build();

        return DwpDocument.builder()
                                          .value(details)
                                          .build();
    }

    private static SscsDocument getSscsDocument(String documentFilename) {
        SscsDocumentDetails details = SscsDocumentDetails
            .builder()
            .documentLink(new DocumentLink("url1", "url2", documentFilename, "hash"))
            .documentFileName(documentFilename)
            .build();

        return SscsDocument.builder()
                                            .value(details)
                                            .build();
    }

    private static SscsDocument getSscsDocumentEdited(String documentFilename, String editedDocumentFileName) {
        SscsDocumentDetails details = SscsDocumentDetails
            .builder()
            .documentLink(new DocumentLink("url1", "url2", documentFilename, "hash"))
            .documentFileName(documentFilename)
            .editedDocumentLink(new DocumentLink("url2", "url2", editedDocumentFileName, "hash"))
            .build();

        return SscsDocument.builder()
                                            .value(details)
                                            .build();
    }


    private void assertArgumentsForPdfGeneration() {
        ArgumentCaptor<DocumentHolder> argumentCaptor = ArgumentCaptor.forClass(DocumentHolder.class);
        then(pdfGenerationService).should(times(1)).generatePdf(argumentCaptor.capture());
        DocumentHolder documentHolder = argumentCaptor.getValue();
        assertThat(documentHolder.getTemplate().getTemplateName()).isEqualTo("testName.doc");
        assertThat(documentHolder.getPlaceholders().toString())
            .hasToString(singletonMap("someKey", "someValue").toString());
        assertThat(documentHolder.isPdfArchiveMode()).isTrue();
    }

    private void assertCoverLetterIsFirstDocInList(List<Pdf> pdfsToBulkPrint) {
        assertThat(pdfsToBulkPrint).hasSize(2);
        assertThat(pdfsToBulkPrint.getFirst().getName()).isEqualTo("609_97_OriginalSenderCoverLetter");
        assertThat(pdfsToBulkPrint.getFirst().getContent()).isEqualTo(new byte[]{'l', 'e', 't', 't', 'e', 'r'});
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