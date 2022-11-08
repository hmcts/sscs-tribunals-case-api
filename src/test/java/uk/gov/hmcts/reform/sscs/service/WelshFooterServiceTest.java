package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import junitparams.JUnitParamsRunner;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.pdf.PdfWatermarker;

@RunWith(JUnitParamsRunner.class)
public class WelshFooterServiceTest {

    @Mock
    private PdfStoreService pdfStoreService;

    @Mock
    private PdfWatermarker pdfWatermarker;

    private WelshFooterService footerService;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Captor
    private ArgumentCaptor<String> stringCaptor;

    @Mock
    private UploadResponse uploadResponse;
    @Mock
    private UploadResponse.Embedded uploadResponseEmbedded;

    private SscsDocument sscsDocument;
    //private List<uk.gov.hmcts.reform.document.domain.Document> uploadedDocuments;
    //private uk.gov.hmcts.reform.document.domain.Document uploadedDocument = new uk.gov.hmcts.reform.document.domain.Document();

    private SscsCaseData sscsCaseData;

    private String fileName = "some-file.pdf";
    private String expectedDocumentUrl = "document-self-href";
    private String expectedBinaryUrl = "document-binary-href";

    @Before
    public void setup() {
        footerService = new WelshFooterService(pdfStoreService, pdfWatermarker);

        sscsDocument = SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName(fileName)
                .documentLink(DocumentLink.builder().documentUrl(expectedDocumentUrl).documentBinaryUrl(expectedBinaryUrl).build()).build()).build();

        SscsWelshDocument document = SscsWelshDocument.builder().value(SscsWelshDocumentDetails.builder().documentFileName("myTest.doc").build()).build();
        List<SscsWelshDocument> docs = new ArrayList<>();
        docs.add(document);

        sscsCaseData = SscsCaseData.builder()
            .documentGeneration(DocumentGeneration.builder()
                .generateNotice(YES)
                .signedBy("User")
                .signedRole("Judge")
                .build())
            .documentStaging(DocumentStaging.builder()
                .dateAdded(LocalDate.now().minusDays(1))
                .previewDocument(DocumentLink.builder()
                    .documentUrl("dm-store/documents/123")
                    .documentBinaryUrl("dm-store/documents/123/binary")
                    .documentFilename("directionIssued.pdf")
                    .build())
                .build())
            .directionTypeDl(new DynamicList(DirectionType.APPEAL_TO_PROCEED.toString()))
            .sscsWelshDocuments(docs)
            .appeal(Appeal.builder()
                .appellant(Appellant.builder()
                    .name(Name.builder().build())
                    .identity(Identity.builder().build())
                    .build())
                .build()).build();
    }

    @Test
    public void givenADocumentWithNoOverrideDateAndFileName_thenAddAFooterWithCorrectValues() throws Exception {
        setupMockCalls();

        FooterDetails footerDetails = footerService.addFooterToExistingToContentAndCreateNewUrl(DocumentLink.builder().documentUrl("MyUrl").build(), sscsCaseData.getSscsWelshDocuments(), DocumentType.DIRECTION_NOTICE, null, LocalDate.of(2020, 02, 28).format(DateTimeFormatter.ofPattern("dd-MM-YYYY")));

        SscsWelshDocumentDetails footerDoc = sscsCaseData.getSscsWelshDocuments().get(0).getValue();
        assertEquals("A", footerDetails.getBundleAddition());
        assertEquals("Addition A - Directions Notice issued on 28-02-2020.pdf", footerDetails.getBundleFileName());
        assertEquals("document-self-href/binary", footerDetails.getUrl().getDocumentBinaryUrl());

    }

    @Test
    public void givenADocumentWithOverrideDateAndFileName_thenAddAFooterWithOverriddenValues() throws Exception {
        setupMockCalls();

        FooterDetails footerDetails = footerService.addFooterToExistingToContentAndCreateNewUrl(DocumentLink.builder().documentUrl("MyUrl").build(), sscsCaseData.getSscsWelshDocuments(), DocumentType.DIRECTION_NOTICE, "overrideFileName", LocalDate.of(2020, 02, 28).format(DateTimeFormatter.ofPattern("dd-MM-YYYY")));

        SscsWelshDocumentDetails footerDoc = sscsCaseData.getSscsWelshDocuments().get(0).getValue();
        assertEquals("A", footerDetails.getBundleAddition());
        assertEquals("overrideFileName", footerDetails.getBundleFileName());
        assertEquals("document-self-href/binary", footerDetails.getUrl().getDocumentBinaryUrl());

    }

    private void setupMockCalls() throws Exception {
        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdf/sample.pdf"));
        when(pdfStoreService.download(any())).thenReturn(pdfBytes);

        when(pdfStoreService.storeDocument(any(), any())).thenReturn(sscsDocument);

        when(pdfWatermarker.shrinkAndWatermarkPdf(any(), stringCaptor.capture(), stringCaptor.capture())).thenReturn(new byte[]{});
    }

}
