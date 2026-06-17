package uk.gov.hmcts.reform.sscs.helper;

import static java.math.RoundingMode.HALF_EVEN;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.within;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.evidenceshare.exception.BulkPrintException;
import uk.gov.hmcts.reform.sscs.evidenceshare.exception.PdfException;

class PdfHelperTest {

    private final PdfHelper pdfHelper = new PdfHelper();

    @Test
    void returnFalseWhenPdfHasPageGreaterThanAllowedSizePortrait() throws IOException {

        PDRectangle pageSize = PDRectangle.A4;
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(new PDRectangle(pageSize.getWidth() + 100, pageSize.getHeight() + 100)));

            boolean result = pdfHelper.isDocumentWithinSizeTolerance(document, pageSize);
            assertThat(result).isFalse();
        }
    }

    @Test
    void returnFalseWhenPdfHasPageGreaterThanAllowedSizeLandscape() throws IOException {
        PDRectangle pageSize = PDRectangle.A4;
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(new PDRectangle(pageSize.getHeight() + 100, pageSize.getWidth() + 100)));

            boolean result = pdfHelper.isDocumentWithinSizeTolerance(document, pageSize);
            assertThat(result).isFalse();
        }
    }

    @Test
    void returnFalseWhenPdfHasPageLowerThanAllowedSizePortrait() throws IOException {

        PDRectangle pageSize = PDRectangle.A4;
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(new PDRectangle(pageSize.getWidth() - 100, pageSize.getHeight() - 100)));

            boolean result = pdfHelper.isDocumentWithinSizeTolerance(document, pageSize);
            assertThat(result).isFalse();
        }
    }

    @Test
    void returnFalseWhenPdfHasPageLowerThanAllowedSizeLandscape() throws IOException {
        PDRectangle pageSize = PDRectangle.A4;
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(new PDRectangle(pageSize.getHeight() - 100, pageSize.getWidth() - 100)));

            boolean result = pdfHelper.isDocumentWithinSizeTolerance(document, pageSize);
            assertThat(result).isFalse();
        }
    }

    @Test
    void returnTrueWhenPdfHasNoPagesGreaterThanAllowedSizePortrait() throws IOException {
        PDRectangle pageSize = PDRectangle.A4;
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(new PDRectangle(pageSize.getWidth() + 5, pageSize.getHeight())));

            boolean result = pdfHelper.isDocumentWithinSizeTolerance(document, pageSize);
            assertThat(result).isTrue();
        }
    }

    @Test
    void returnTrueWhenPdfHasNoPagesGreaterThanAllowedSizeLandscape() throws IOException {
        PDRectangle pageSize = PDRectangle.A4;
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(new PDRectangle(pageSize.getHeight() + 5, pageSize.getWidth())));

            boolean result = pdfHelper.isDocumentWithinSizeTolerance(document, pageSize);
            assertThat(result).isTrue();
        }
    }

    @Test
    void returnTrueWhenPdfHasNoPagesLowerThanAllowedSizePortrait() throws IOException {
        PDRectangle pageSize = PDRectangle.A4;
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(new PDRectangle(pageSize.getWidth() - 5, pageSize.getHeight())));

            boolean result = pdfHelper.isDocumentWithinSizeTolerance(document, pageSize);
            assertThat(result).isTrue();
        }
    }

    @Test
    void returnTrueWhenPdfHasNoPagesLowerThanAllowedSizeLandscape() throws IOException {
        PDRectangle pageSize = PDRectangle.A4;
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(new PDRectangle(pageSize.getHeight() - 5, pageSize.getWidth())));

            boolean result = pdfHelper.isDocumentWithinSizeTolerance(document, pageSize);
            assertThat(result).isTrue();
        }
    }

    @Test
    void returnCorrectMaxScalingFactorForOversizedPortraitWidth() throws IOException {
        PDRectangle pageSize = PDRectangle.A4;
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(new PDRectangle(pageSize.getWidth() + 100, pageSize.getHeight() + 50)));

            BigDecimal result = pdfHelper.scalingFactor(document.getPage(0), pageSize);
            assertThat(result).isEqualTo(new BigDecimal("0.8562").setScale(4, HALF_EVEN));
        }
    }

    @Test
    void returnCorrectMaxScalingFactorForUndersizedPortraitWidth() throws IOException {
        PDRectangle pageSize = PDRectangle.A4;
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(new PDRectangle(pageSize.getWidth() - 100, pageSize.getHeight())));

            BigDecimal result = pdfHelper.scalingFactor(document.getPage(0), pageSize);
            assertThat(result.setScale(2, HALF_EVEN)).isEqualTo(BigDecimal.ONE.setScale(2, HALF_EVEN));
        }
    }

    @Test
    void returnCorrectMaxScalingFactorForUndersizedPortraitHeight() throws IOException {
        PDRectangle pageSize = PDRectangle.A4;
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(new PDRectangle(pageSize.getWidth(), pageSize.getHeight() - 100)));

            BigDecimal result = pdfHelper.scalingFactor(document.getPage(0), pageSize);
            assertThat(result.setScale(2, HALF_EVEN)).isEqualTo(BigDecimal.ONE.setScale(2, HALF_EVEN));
        }
    }

    @Test
    void returnCorrectMaxScalingFactorForOversizedPortraitWidthExtraLarge() throws IOException {
        PDRectangle pageSize = PDRectangle.A4;
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(new PDRectangle(PDRectangle.A1.getWidth(), PDRectangle.A1.getHeight())));

            BigDecimal result = pdfHelper.scalingFactor(document.getPage(0), pageSize);
            assertThat(result).isEqualTo(new BigDecimal("0.3532").setScale(4, HALF_EVEN));
        }
    }

    @Test
    void returnCorrectMaxScalingFactorForOversizedPortraitHeight() throws IOException {
        PDRectangle pageSize = PDRectangle.A4;
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(new PDRectangle(pageSize.getWidth() + 50, pageSize.getHeight() + 100)));

            BigDecimal result = pdfHelper.scalingFactor(document.getPage(0), pageSize);
            assertThat(result).isEqualTo(new BigDecimal("0.8938").setScale(4, HALF_EVEN));
        }
    }

    @Test
    void scaleDownLandscapeDocumentCorrectly() throws Exception {

        byte[] pdfBytes = IOUtils.toByteArray(
            requireNonNull(getClass().getClassLoader().getResourceAsStream("A3 Landscape.pdf")));

        PDRectangle pageSize = PDRectangle.A4;
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {

            Optional<PDDocument> result = pdfHelper.scaleToPageSize(document, pageSize);

            assertThat(result).isPresent();

            try (PDDocument newDocument = result.get()) {
                PDRectangle mediaBox = newDocument.getPage(0).getMediaBox();

                assertThat(mediaBox.getHeight()).isLessThanOrEqualTo(pageSize.getHeight());
                assertThat(mediaBox.getWidth()).isLessThanOrEqualTo(pageSize.getWidth());
            }
        }
    }

    @Test
    void scaleDownPortraitDocumentCorrectly() throws Exception {

        byte[] pdfBytes = IOUtils.toByteArray(requireNonNull(getClass().getClassLoader().getResourceAsStream("another-test.pdf")));

        PDRectangle pageSize = PDRectangle.A4;
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {

            Optional<PDDocument> result = pdfHelper.scaleToPageSize(document, pageSize);

            assertThat(result).isPresent();
            try (PDDocument newDocument = result.get()) {
                PDRectangle mediaBox = newDocument.getPage(0).getMediaBox();

                assertThat(mediaBox.getHeight()).isLessThanOrEqualTo(pageSize.getHeight());
                assertThat(mediaBox.getWidth()).isLessThanOrEqualTo(pageSize.getWidth());
            }
        }
    }

    @Test
    void scalePageUpCorrectlyPortrait() throws Exception {

        byte[] pdfBytes = IOUtils.toByteArray(requireNonNull(getClass().getClassLoader().getResourceAsStream("another-test.pdf")));

        PDRectangle pageSize = PDRectangle.A2;
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {

            Optional<PDDocument> result = pdfHelper.scaleToPageSize(document, pageSize);

            assertThat(result).isPresent();
            try (PDDocument newDocument = result.get()) {
                PDRectangle mediaBox = newDocument.getPage(0).getMediaBox();

                assertThat(mediaBox.getHeight()).isEqualTo(pageSize.getHeight(), within(0.0f));
                assertThat(mediaBox.getWidth()).isEqualTo(pageSize.getWidth(), within(0.0f));
            }
        }
    }

    @Test
    void scalePageUpCorrectlyLandscape() throws Exception {

        byte[] pdfBytes = IOUtils.toByteArray(requireNonNull(getClass().getClassLoader().getResourceAsStream("A3 Landscape.pdf")));

        PDRectangle pageSize = PDRectangle.A2;
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {

            Optional<PDDocument> result = pdfHelper.scaleToPageSize(document, pageSize);

            assertThat(result).isPresent();
            try (PDDocument newDocument = result.get()) {
                PDRectangle mediaBox = newDocument.getPage(0).getMediaBox();

                assertThat(mediaBox.getHeight()).isEqualTo(pageSize.getHeight(), within(0.0f));
                assertThat(mediaBox.getWidth()).isEqualTo(pageSize.getWidth(), within(0.0f));
            }
        }
    }

    @Test
    void doesResizeDownDocumentWhichDoesNeedResizing() throws Exception {

        byte[] pdfBytes = IOUtils.toByteArray(requireNonNull(getClass().getClassLoader().getResourceAsStream("another-test.pdf")));

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDRectangle pageSize = PDRectangle.A4;
            Optional<PDDocument> result = pdfHelper.scaleToPageSize(document, pageSize);

            assertThat(result).isPresent();
            try (PDDocument newDocument = result.get()) {
                PDRectangle mediaBox = newDocument.getPage(0).getMediaBox();

                assertThat(mediaBox.getHeight()).isLessThanOrEqualTo(pageSize.getHeight());
                assertThat(mediaBox.getWidth()).isLessThanOrEqualTo(pageSize.getWidth());
            }
        }
    }

    @Test
    void doesResizeUpDocumentWhichDoesNeedResizing() throws Exception {

        byte[] pdfBytes = IOUtils.toByteArray(requireNonNull(getClass().getClassLoader().getResourceAsStream("another-test.pdf")));

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDRectangle pageSize = PDRectangle.A2;
            Optional<PDDocument> result = pdfHelper.scaleToPageSize(document, pageSize);

            assertThat(result).isPresent();
            try (PDDocument newDocument = result.get()) {
                PDRectangle mediaBox = newDocument.getPage(0).getMediaBox();

                assertThat(mediaBox.getHeight()).isGreaterThanOrEqualTo(pageSize.getHeight());
                assertThat(mediaBox.getWidth()).isGreaterThanOrEqualTo(pageSize.getWidth());
            }
        }
    }

    @Test
    void doesNotResizeDocumentWhichDoesNotNeedResizing() throws Exception {
        byte[] pdfBytes = IOUtils.toByteArray(requireNonNull(getClass().getClassLoader().getResourceAsStream("A3 Portrait.pdf")));

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {

            Optional<PDDocument> result = pdfHelper.scaleToPageSize(document, PDRectangle.A3);

            assertThat(result).isEmpty();
        }
    }

    @Test
    void doesResizeA4DocumentWhichDoesNeedResizing() throws Exception {

        byte[] pdfBytes = IOUtils.toByteArray(requireNonNull(getClass().getClassLoader().getResourceAsStream("another-test.pdf")));

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {

            Optional<PDDocument> result = pdfHelper.scaleToA4(document);

            assertThat(result).isPresent();
            try (PDDocument newDocument = result.get()) {
                PDRectangle mediaBox = newDocument.getPage(0).getMediaBox();
                PDRectangle pageSize = PDRectangle.A4;
                assertThat(mediaBox.getHeight()).isLessThanOrEqualTo(pageSize.getHeight());
                assertThat(mediaBox.getWidth()).isLessThanOrEqualTo(pageSize.getWidth());
            }
        }
    }

    @Test
    void allPagesWillBeScaledToA4() throws Exception {
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(new PDRectangle(PDRectangle.A1.getWidth(), PDRectangle.A1.getHeight())));
            document.addPage(new PDPage(new PDRectangle(PDRectangle.A2.getWidth(), PDRectangle.A2.getHeight())));
            document.addPage(new PDPage(new PDRectangle(PDRectangle.A3.getWidth(), PDRectangle.A3.getHeight())));
            document.addPage(new PDPage(new PDRectangle(PDRectangle.A4.getWidth(), PDRectangle.A4.getHeight())));
            document.addPage(new PDPage(new PDRectangle(PDRectangle.A5.getWidth(), PDRectangle.A5.getHeight())));
            document.addPage(new PDPage(new PDRectangle(PDRectangle.A6.getWidth(), PDRectangle.A5.getHeight())));

            Optional<PDDocument> result = pdfHelper.scaleToA4(document);
            assertThat(result).isPresent();
            try (PDDocument newDocument = result.get()) {
                assertThat(newDocument.getNumberOfPages()).isEqualTo(6);
                for (PDPage page : newDocument.getPages()) {
                    PDRectangle mediaBox = page.getMediaBox();
                    assertThat(mediaBox.getHeight()).isEqualTo(PDRectangle.A4.getHeight());
                    assertThat(mediaBox.getWidth()).isEqualTo(PDRectangle.A4.getWidth());
                }
            }
        }
    }

    @Test
    void resizesDocumentWithMultiPages() throws Exception {

        byte[] pdfBytes = IOUtils.toByteArray(requireNonNull(getClass().getClassLoader().getResourceAsStream("MultiPage.pdf")));

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {

            Optional<PDDocument> result = pdfHelper.scaleToA4(document);

            assertThat(result).isPresent();
            try (PDDocument newDocument = result.get()) {
                assertThat(newDocument.getNumberOfPages()).isEqualTo(3);
                for (PDPage page : newDocument.getPages()) {
                    PDRectangle mediaBox = page.getMediaBox();
                    if (mediaBox.getWidth() > mediaBox.getHeight()) {
                        assertThat(mediaBox.getHeight()).isEqualTo(PDRectangle.A4.getWidth());
                        assertThat(mediaBox.getWidth()).isEqualTo(PDRectangle.A4.getHeight());
                    } else {
                        assertThat(mediaBox.getHeight()).isEqualTo(PDRectangle.A4.getHeight());
                        assertThat(mediaBox.getWidth()).isEqualTo(PDRectangle.A4.getWidth());
                    }
                }
            }
        }
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    void returnsZeroWhenPdfsIsNullOrEmpty(List<Pdf> pdfs) throws PdfException {
        assertThat(PdfHelper.getPhysicalPageCount(pdfs)).isZero();
    }

    @ParameterizedTest
    @CsvSource({
        "1, 1, single-page.pdf",
        "2, 1, two-page.pdf",
        "3, 2, three-page.pdf"
    })
    void returnsCorrectSheetCountForDocuments(int pageCount, int expectedSheetCount, String filename) throws IOException, PdfException {
        final byte[] pdfBytes = createPdfWithPages(pageCount);
        final List<Pdf> pdfs = List.of(new Pdf(pdfBytes, filename));

        assertThat(PdfHelper.getPhysicalPageCount(pdfs)).isEqualTo(expectedSheetCount);
    }

    @Test
    void returnsSumOfSheetCountsAcrossMultiplePdfs() throws IOException, PdfException {
        final byte[] threePagePdf = createPdfWithPages(3);
        final byte[] fourPagePdf = createPdfWithPages(4);
        final List<Pdf> pdfs = List.of(
            new Pdf(threePagePdf, "three-page.pdf"),
            new Pdf(fourPagePdf, "four-page.pdf"));

        assertThat(PdfHelper.getPhysicalPageCount(pdfs)).isEqualTo(4);
    }

    @Test
    void throwsBulkPrintExceptionWhenPdfCannotBeLoaded() {
        final List<Pdf> pdfs = List.of(new Pdf("not a pdf".getBytes(StandardCharsets.UTF_8), "invalid.pdf"));

        assertThatThrownBy(() -> PdfHelper.getPhysicalPageCount(pdfs))
            .isInstanceOf(PdfException.class)
            .hasMessageContaining("invalid.pdf");
    }

    @Test
    void isDocumentWithinSizeTolerance_returnsTrueForEmptyDocument() throws IOException {
        try (PDDocument emptyDoc = new PDDocument()) {
            assertThat(pdfHelper.isDocumentWithinSizeTolerance(emptyDoc, PDRectangle.A4)).isTrue();
        }
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    void throwsBulkPrintExceptionWhenDocumentListIsNullOrEmpty(final List<byte[]> documents) {
        assertThatThrownBy(() -> PdfHelper.buildBundledLetter(documents))
            .isInstanceOf(BulkPrintException.class)
            .hasMessage("Failed to merge documents: document list is empty");
    }

    @Test
    void buildBundledLetter_returnsSingleDocumentUnchangedWhenListHasOneElement() throws IOException {
        final byte[] pdf = createPdfWithPages(1);

        assertThat(PdfHelper.buildBundledLetter(List.of(pdf))).isEqualTo(pdf);
    }

    @Test
    void buildBundledLetter_mergesMultipleDocuments() throws IOException {
        final byte[] first = createPdfWithPages(2);
        final byte[] second = createPdfWithPages(2);

        final byte[] result = PdfHelper.buildBundledLetter(List.of(first, second));

        try (PDDocument merged = Loader.loadPDF(result)) {
            assertThat(merged.getNumberOfPages()).isEqualTo(4);
        }
    }

    @Test
    void buildBundledLetter_addsBlankPageBeforeMergingWhenCurrentPageCountIsOdd() throws IOException {
        final byte[] singlePage = createPdfWithPages(1);

        final byte[] result = PdfHelper.buildBundledLetter(List.of(singlePage, singlePage));

        try (PDDocument merged = Loader.loadPDF(result)) {
            assertThat(merged.getNumberOfPages()).isEqualTo(3); // 1 + 1 blank + 1
        }
    }

    @Test
    void buildBundledLetter_skipsNullDocumentsInList() throws IOException {
        final byte[] validPdf = createPdfWithPages(2);
        final List<byte[]> docs = new ArrayList<>();
        docs.add(validPdf);
        docs.add(null);

        final byte[] result = PdfHelper.buildBundledLetter(docs);

        try (PDDocument merged = Loader.loadPDF(result)) {
            assertThat(merged.getNumberOfPages()).isEqualTo(2);
        }
    }

    @Test
    void buildBundledLetter_throwsBulkPrintExceptionForInvalidPdfBytes() {
        final byte[] invalidPdf = "not a pdf".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> PdfHelper.buildBundledLetter(List.of(invalidPdf, invalidPdf)))
            .isInstanceOf(BulkPrintException.class)
            .hasMessageContaining("Failed to merge documents with exception");
    }

    @Test
    void buildBundledLetterFromPdfs_returnsSingleDocumentUnchanged() throws IOException {
        final byte[] pdfBytes = createPdfWithPages(1);

        assertThat(PdfHelper.buildBundledLetterFromPdfs(List.of(new Pdf(pdfBytes, "test.pdf")))).isEqualTo(pdfBytes);
    }

    @Test
    void buildBundledLetterFromPdfs_mergesMultiplePdfs() throws IOException {
        final byte[] first = createPdfWithPages(2);
        final byte[] second = createPdfWithPages(2);

        final byte[] result = PdfHelper.buildBundledLetterFromPdfs(
            List.of(new Pdf(first, "first.pdf"), new Pdf(second, "second.pdf")));

        try (PDDocument merged = Loader.loadPDF(result)) {
            assertThat(merged.getNumberOfPages()).isEqualTo(4);
        }
    }

    private byte[] createPdfWithPages(int numberOfPages) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            IntStream.range(0, numberOfPages).forEach(i -> document.addPage(new PDPage()));
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }
}