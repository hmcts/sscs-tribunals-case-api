package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.DateRange;
import uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.ExcludeDate;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.domain.wrapper.pdf.PdfState;
import uk.gov.hmcts.reform.sscs.pdf.PdfWatermarker;

@RunWith(JUnitParamsRunner.class)
public class FooterServiceTest {

    @Mock
    private PdfStoreService pdfStoreService;

    @Mock
    private PdfWatermarker pdfWatermarker;

    private FooterService footerService;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Captor
    private ArgumentCaptor<String> stringCaptor;

    @Mock
    private UploadResponse uploadResponse;
    @Mock
    private UploadResponse.Embedded uploadResponseEmbedded;
    @Mock
    private List<uk.gov.hmcts.reform.document.domain.Document> uploadedDocuments;
    private uk.gov.hmcts.reform.document.domain.Document uploadedDocument = new uk.gov.hmcts.reform.document.domain.Document();
    private SscsDocument sscsDocument;

    private SscsCaseData sscsCaseData;

    private String fileName = "some-file.pdf";
    private String expectedDocumentUrl = "document-self-href";
    private String expectedBinaryUrl = "document-binary-href";

    @Before
    public void setup() {
        footerService = new FooterService(pdfStoreService, pdfWatermarker);

        sscsDocument = SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName(fileName)
                .documentLink(DocumentLink.builder().documentUrl(expectedDocumentUrl)
                .documentBinaryUrl(expectedBinaryUrl).build()).build()).build();

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
                        .documentUrl("dm-store/documents/123")
                        .documentBinaryUrl("dm-store/documents/123/binary")
                        .documentFilename("directionIssued.pdf")
                        .build())
                .appeal(Appeal.builder()
                        .appellant(Appellant.builder()
                                .name(Name.builder().build())
                                .identity(Identity.builder().build())
                                .build())
                        .build()).build();
    }

    @Test
    public void givenADocument_thenAddAFooter() throws Exception {
        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdf/sample.pdf"));
        when(pdfStoreService.download(any())).thenReturn(pdfBytes);

        when(pdfStoreService.storeDocument(any(), anyString())).thenReturn(sscsDocument);

        when(pdfWatermarker.shrinkAndWatermarkPdf(any(), stringCaptor.capture(), stringCaptor.capture())).thenReturn(new byte[]{});

        String now = LocalDate.now().toString();

        footerService.createFooterAndAddDocToCase(DocumentLink.builder().documentUrl("MyUrl").documentFilename("afilename").build(),
                sscsCaseData, DocumentType.DIRECTION_NOTICE, now, null, null, null);

        assertEquals(2, sscsCaseData.getSscsDocument().size());
        SscsDocumentDetails footerDoc = sscsCaseData.getSscsDocument().get(0).getValue();
        assertEquals(DocumentType.DIRECTION_NOTICE.getValue(), footerDoc.getDocumentType());
        assertEquals("Addition A - Directions Notice issued on " + now + ".pdf", footerDoc.getDocumentFileName());
        assertEquals(now, footerDoc.getDocumentDateAdded());
        assertEquals(expectedDocumentUrl, footerDoc.getDocumentLink().getDocumentUrl());
        verify(pdfStoreService).storeDocument(any(), anyString());
        assertEquals("Directions Notice", stringCaptor.getAllValues().get(0));
        assertEquals("Addition A", stringCaptor.getAllValues().get(1));
    }

    @Test
    public void givenADocumentWithOverridenDateAndFileName_thenAddAFooterWithOverriddenValues() throws Exception {
        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdf/sample.pdf"));
        when(pdfStoreService.download(any())).thenReturn(pdfBytes);

        when(pdfStoreService.storeDocument(any(), anyString())).thenReturn(sscsDocument);

        when(pdfWatermarker.shrinkAndWatermarkPdf(any(), stringCaptor.capture(), stringCaptor.capture())).thenReturn(new byte[]{});

        footerService.createFooterAndAddDocToCase(DocumentLink.builder().documentUrl("MyUrl").documentFilename("afilename").build(),
                sscsCaseData, DocumentType.DIRECTION_NOTICE, LocalDate.now().toString(), LocalDate.now().minusDays(1), "overriden.pdf", null);

        assertEquals(2, sscsCaseData.getSscsDocument().size());
        SscsDocumentDetails footerDoc = sscsCaseData.getSscsDocument().get(0).getValue();
        assertEquals(DocumentType.DIRECTION_NOTICE.getValue(), footerDoc.getDocumentType());
        assertEquals("overriden.pdf", footerDoc.getDocumentFileName());
        assertEquals(LocalDate.now().minusDays(1).toString(), footerDoc.getDocumentDateAdded());
        assertEquals(expectedDocumentUrl, footerDoc.getDocumentLink().getDocumentUrl());
        verify(pdfStoreService).storeDocument(any(), anyString());
        assertEquals("Directions Notice", stringCaptor.getAllValues().get(0));
        assertEquals("Addition A", stringCaptor.getAllValues().get(1));
    }


    @Test
    public void givenADocumentWithTranslationStatus_thenAddTranslationStatusToDocumentDetails() throws Exception {
        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdf/sample.pdf"));
        when(pdfStoreService.download(any())).thenReturn(pdfBytes);

        when(pdfStoreService.storeDocument(any(), anyString())).thenReturn(sscsDocument);

        when(pdfWatermarker.shrinkAndWatermarkPdf(any(), stringCaptor.capture(), stringCaptor.capture())).thenReturn(new byte[]{});

        String now = LocalDate.now().toString();

        footerService.createFooterAndAddDocToCase(DocumentLink.builder().documentUrl("MyUrl").documentFilename("afilename").build(),
                sscsCaseData, DocumentType.DIRECTION_NOTICE, now, null, null, SscsDocumentTranslationStatus.TRANSLATION_REQUIRED);

        assertEquals(2, sscsCaseData.getSscsDocument().size());
        SscsDocumentDetails footerDoc = sscsCaseData.getSscsDocument().get(0).getValue();
        assertEquals(DocumentType.DIRECTION_NOTICE.getValue(), footerDoc.getDocumentType());
        assertEquals("Addition A - Directions Notice issued on " + now + ".pdf", footerDoc.getDocumentFileName());
        assertEquals(SscsDocumentTranslationStatus.TRANSLATION_REQUIRED, footerDoc.getDocumentTranslationStatus());
        assertEquals(now, footerDoc.getDocumentDateAdded());
        assertEquals(expectedDocumentUrl, footerDoc.getDocumentLink().getDocumentUrl());
        verify(pdfStoreService).storeDocument(any(), anyString());
        assertEquals("Directions Notice", stringCaptor.getAllValues().get(0));
        assertEquals("Addition A", stringCaptor.getAllValues().get(1));
    }

    @Test
    public void buildFooterLinkFromLeftAndRightText() throws IOException {
        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdf/sample.pdf"));
        when(pdfStoreService.download(any())).thenReturn(pdfBytes);

        when(pdfStoreService.storeDocument(any(), anyString())).thenReturn(sscsDocument);

        DocumentLink result = footerService.addFooter(DocumentLink.builder().documentUrl("oldLink").documentFilename("afilename").build(), "leftText", "rightText");

        assertEquals(expectedDocumentUrl, result.getDocumentUrl());
    }

    @Test
    @Parameters({"", "A", "B", "C", "D", "X", "Y"})
    public void canWorkOutTheNextAppendixValue(String currentAppendix) {
        List<SscsDocument> sscsDocuments = new ArrayList<>();
        if (!currentAppendix.equals("")) {
            SscsDocument theDocument = SscsDocument.builder().value(SscsDocumentDetails.builder().bundleAddition(currentAppendix).build()).build();
            sscsDocuments.add(theDocument);

            if (currentAppendix.toCharArray()[0] > 'A') {
                SscsDocument document = SscsDocument.builder().value(SscsDocumentDetails.builder().bundleAddition("A").build()).build();
                sscsDocuments.add(document);
            }
            if (currentAppendix.toCharArray()[0] > 'B') {
                SscsDocument document = SscsDocument.builder().value(SscsDocumentDetails.builder().bundleAddition("B").build()).build();
                sscsDocuments.add(document);
            }
            if (currentAppendix.toCharArray()[0] > 'C') {
                SscsDocument document = SscsDocument.builder().value(SscsDocumentDetails.builder().bundleAddition("C").build()).build();
                sscsDocuments.add(document);
            }
        }

        String actual = footerService.getNextBundleAddition(sscsDocuments);

        String expected = currentAppendix.equals("") ? "A" : String.valueOf((char) (currentAppendix.charAt(0) + 1));
        assertEquals(expected, actual);
    }


    @Test
    @Parameters({"Z", "Z1", "Z9", "Z85", "Z100"})
    public void canWorkOutTheNextAppendixValueAfterZ(String currentAppendix) {
        SscsDocument theDocument = SscsDocument.builder().value(SscsDocumentDetails.builder().bundleAddition(currentAppendix).build()).build();
        SscsDocument documentA = SscsDocument.builder().value(SscsDocumentDetails.builder().bundleAddition("A").build()).build();
        SscsDocument documentB = SscsDocument.builder().value(SscsDocumentDetails.builder().bundleAddition("B").build()).build();
        SscsDocument documentC = SscsDocument.builder().value(SscsDocumentDetails.builder().bundleAddition("Y").build()).build();
        List<SscsDocument> sscsDocuments = new ArrayList<>(Arrays.asList(theDocument, documentA, documentB, documentC));

        int index = currentAppendix.length() == 1 ? 0 : (Integer.valueOf(currentAppendix.substring(1)));

        if (index > 0) {
            SscsDocument document = SscsDocument.builder().value(SscsDocumentDetails.builder().bundleAddition("Z").build()).build();
            sscsDocuments.add(document);
        }
        if (index > 8) {
            SscsDocument document = SscsDocument.builder().value(SscsDocumentDetails.builder().bundleAddition("Z7").build()).build();
            sscsDocuments.add(document);
        }
        if (index > 30) {
            SscsDocument document = SscsDocument.builder().value(SscsDocumentDetails.builder().bundleAddition("Z28").build()).build();
            sscsDocuments.add(document);
        }
        if (index > 80) {
            SscsDocument document = SscsDocument.builder().value(SscsDocumentDetails.builder().bundleAddition("Z79").build()).build();
            sscsDocuments.add(document);
        }

        String expected = index == 0 ? "Z1" : "Z" + (index + 1);
        String actual = footerService.getNextBundleAddition(sscsDocuments);
        assertEquals(expected, actual);
    }

    @Test
    @Parameters({"Z!", "Z3$", "ZN"})
    public void nextAppendixCanHandleInvalidDataThatAreNotNumbersAfterZ(String currentAppendix) {
        SscsDocument theDocument = SscsDocument.builder().value(SscsDocumentDetails.builder().bundleAddition(currentAppendix).build()).build();
        String actual = footerService.getNextBundleAddition(Collections.singletonList(theDocument));
        assertEquals("[", actual);
    }

    @Test
    public void buildBundleAdditionFileNameText() {
        String result = footerService.buildBundleAdditionFileName("A", "I am the right text");

        assertEquals("Addition A - I am the right text.pdf", result);
    }

    @Test
    public void isReadablePdfReturnTrueForReadablePdf() throws Exception {
        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdf/sample.pdf"));
        when(pdfStoreService.download(any())).thenReturn(pdfBytes);

        PdfState result = footerService.isReadablePdf("url.pdf");
        assertEquals(PdfState.OK, result);
    }

    @Test
    public void isReadablePdfReturnFalseForGarbledBytes() {
        byte[] pdfBytes = new byte[0];
        when(pdfStoreService.download(any())).thenReturn(pdfBytes);

        PdfState result = footerService.isReadablePdf("url.pdf");
        assertEquals(PdfState.UNREADABLE, result);
    }

    @Test
    public void isReadablePdfReturnFalseForPasswordProtectedPdf() throws Exception {
        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdf/test-protected.pdf"));
        when(pdfStoreService.download(any())).thenReturn(pdfBytes);

        PdfState result = footerService.isReadablePdf("url.pdf");
        assertEquals(PdfState.PASSWORD_ENCRYPTED, result);
    }

    @Test
    public void shouldAddHearingDateToExcludeDateWhichIsNull() {
        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate(LocalDate.now().plusDays(1).toString())
                .time("10:00")
                .build()).build()));
        sscsCaseData.setAppeal(Appeal.builder().hearingOptions(HearingOptions.builder().build()).build());

        footerService.setHearingDateAsExcludeDate(sscsCaseData.getHearings().get(0), sscsCaseData);

        assertEquals(sscsCaseData.getAppeal().getHearingOptions().getExcludeDates().get(0).getValue().getStart(),
                LocalDate.now().plusDays(1).toString());
        assertEquals(sscsCaseData.getAppeal().getHearingOptions().getExcludeDates().get(0).getValue().getEnd(),
                LocalDate.now().plusDays(1).toString());
    }

    @Test
    public void shouldAddHearingDateToExistingExcludeDate() {
        List<ExcludeDate> excludeDate = Arrays.asList(
                ExcludeDate.builder().value(DateRange.builder()
                        .start(LocalDate.now().toString())
                        .end(LocalDate.now().toString())
                        .build()).build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate(LocalDate.now().plusDays(1).toString())
                .time("10:00")
                .build()).build()));
        sscsCaseData.setAppeal(
                Appeal.builder().hearingOptions(
                       HearingOptions.builder().excludeDates(excludeDate).build())
                .build()
        );

        footerService.setHearingDateAsExcludeDate(sscsCaseData.getHearings().get(0), sscsCaseData);

        assertEquals(2, sscsCaseData.getAppeal().getHearingOptions().getExcludeDates().size());
    }

}
