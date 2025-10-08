package uk.gov.hmcts.reform.sscs.helper;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.Test;

public class PdfHelperTest {

    PdfHelper pdfHelper = new PdfHelper();

    @Test
    public void returnFalseWhenPdfHasPageGreaterThanAllowedSizePortrait() throws IOException {

        PDRectangle pageSize = PDRectangle.A4;
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(new PDRectangle(pageSize.getWidth() + 100, pageSize.getHeight() + 100)));

            boolean result = pdfHelper.isDocumentWithinSizeTolerance(document, pageSize);
            assertFalse(result);
        }
    }

    @Test
    public void returnFalseWhenPdfHasPageGreaterThanAllowedSizeLandscape() throws IOException {
        PDRectangle pageSize = PDRectangle.A4;
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(new PDRectangle(pageSize.getHeight() + 100, pageSize.getWidth() + 100)));

            boolean result = pdfHelper.isDocumentWithinSizeTolerance(document, pageSize);
            assertFalse(result);
        }
    }

    @Test
    public void returnFalseWhenPdfHasPageLowerThanAllowedSizePortrait() throws IOException {

        PDRectangle pageSize = PDRectangle.A4;
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(new PDRectangle(pageSize.getWidth() - 100, pageSize.getHeight() - 100)));

            boolean result = pdfHelper.isDocumentWithinSizeTolerance(document, pageSize);
            assertFalse(result);
        }
    }

    @Test
    public void returnFalseWhenPdfHasPageLowerThanAllowedSizeLandscape() throws IOException {
        PDRectangle pageSize = PDRectangle.A4;
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(new PDRectangle(pageSize.getHeight() - 100, pageSize.getWidth() - 100)));

            boolean result = pdfHelper.isDocumentWithinSizeTolerance(document, pageSize);
            assertFalse(result);
        }
    }

    @Test
    public void returnTrueWhenPdfHasNoPagesGreaterThanAllowedSizePortrait() throws IOException {
        PDRectangle pageSize = PDRectangle.A4;
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(new PDRectangle(pageSize.getWidth() + 5, pageSize.getHeight())));

            boolean result = pdfHelper.isDocumentWithinSizeTolerance(document, pageSize);
            assertTrue(result);
        }
    }

    @Test
    public void returnTrueWhenPdfHasNoPagesGreaterThanAllowedSizeLandscape() throws IOException {
        PDRectangle pageSize = PDRectangle.A4;
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(new PDRectangle(pageSize.getHeight() + 5, pageSize.getWidth())));

            boolean result = pdfHelper.isDocumentWithinSizeTolerance(document, pageSize);
            assertTrue(result);
        }
    }

    @Test
    public void returnTrueWhenPdfHasNoPagesLowerThanAllowedSizePortrait() throws IOException {
        PDRectangle pageSize = PDRectangle.A4;
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(new PDRectangle(pageSize.getWidth() - 5, pageSize.getHeight())));

            boolean result = pdfHelper.isDocumentWithinSizeTolerance(document, pageSize);
            assertTrue(result);
        }
    }

    @Test
    public void returnTrueWhenPdfHasNoPagesLowerThanAllowedSizeLandscape() throws IOException {
        PDRectangle pageSize = PDRectangle.A4;
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(new PDRectangle(pageSize.getHeight() - 5, pageSize.getWidth())));

            boolean result = pdfHelper.isDocumentWithinSizeTolerance(document, pageSize);
            assertTrue(result);
        }
    }

    @Test
    public void returnCorrectMaxScalingFactorForOversizedPortraitWidth() throws IOException {
        PDRectangle pageSize = PDRectangle.A4;
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(new PDRectangle(pageSize.getWidth() + 100, pageSize.getHeight() + 50)));

            BigDecimal result = pdfHelper.scalingFactor(document.getPage(0), pageSize);
            assertEquals(new BigDecimal("0.8562").setScale(4, RoundingMode.HALF_EVEN), result);
        }
    }

    @Test
    public void returnCorrectMaxScalingFactorForUndersizedPortraitWidth() throws IOException {
        PDRectangle pageSize = PDRectangle.A4;
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(new PDRectangle(pageSize.getWidth() - 100, pageSize.getHeight())));

            BigDecimal result = pdfHelper.scalingFactor(document.getPage(0), pageSize);
            assertEquals(BigDecimal.ONE.setScale(2), result.setScale(2));
        }
    }

    @Test
    public void returnCorrectMaxScalingFactorForUndersizedPortraitHeight() throws IOException {
        PDRectangle pageSize = PDRectangle.A4;
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(new PDRectangle(pageSize.getWidth(), pageSize.getHeight() - 100)));

            BigDecimal result = pdfHelper.scalingFactor(document.getPage(0), pageSize);
            assertEquals(BigDecimal.ONE.setScale(2), result.setScale(2));
        }
    }

    @Test
    public void returnCorrectMaxScalingFactorForOversizedPortraitWidthExtraLarge() throws IOException {
        PDRectangle pageSize = PDRectangle.A4;
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(new PDRectangle(PDRectangle.A1.getWidth(), PDRectangle.A1.getHeight())));

            BigDecimal result = pdfHelper.scalingFactor(document.getPage(0), pageSize);
            assertEquals(new BigDecimal("0.3532").setScale(4, RoundingMode.HALF_EVEN), result);
        }
    }

    @Test
    public void returnCorrectMaxScalingFactorForOversizedPortraitHeight() throws IOException {
        PDRectangle pageSize = PDRectangle.A4;
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(new PDRectangle(pageSize.getWidth() + 50, pageSize.getHeight() + 100)));

            BigDecimal result = pdfHelper.scalingFactor(document.getPage(0), pageSize);
            assertEquals(new BigDecimal("0.8938").setScale(4, RoundingMode.HALF_EVEN), result);
        }
    }

    @Test
    public void scaleDownLandscapeDocumentCorrectly() throws Exception {

        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("A3 Landscape.pdf"));

        PDRectangle pageSize = PDRectangle.A4;
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {

            Optional<PDDocument> result = pdfHelper.scaleToPageSize(document, pageSize);

            assertTrue(result.isPresent());

            try (PDDocument newDocument = result.get()) {
                PDRectangle mediaBox = newDocument.getPage(0).getMediaBox();

                assertTrue(pageSize.getHeight() >= mediaBox.getHeight());
                assertTrue(pageSize.getWidth() >= mediaBox.getWidth());
            }
        }
    }

    @Test
    public void scaleDownPortraitDocumentCorrectly() throws Exception {

        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("another-test.pdf"));

        PDRectangle pageSize = PDRectangle.A4;
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {

            Optional<PDDocument> result = pdfHelper.scaleToPageSize(document, pageSize);

            assertTrue(result.isPresent());
            try (PDDocument newDocument = result.get()) {
                PDRectangle mediaBox = newDocument.getPage(0).getMediaBox();

                assertTrue(pageSize.getHeight() >= mediaBox.getHeight());
                assertTrue(pageSize.getWidth() >= mediaBox.getWidth());
            }
        }
    }

    @Test
    public void scalePageUpCorrectlyPortrait() throws Exception {

        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("another-test.pdf"));

        PDRectangle pageSize = PDRectangle.A2;
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {

            Optional<PDDocument> result = pdfHelper.scaleToPageSize(document, pageSize);

            assertTrue(result.isPresent());
            try (PDDocument newDocument = result.get()) {
                PDRectangle mediaBox = newDocument.getPage(0).getMediaBox();

                assertEquals(pageSize.getHeight(), mediaBox.getHeight(), 0.0);
                assertEquals(pageSize.getWidth(), mediaBox.getWidth(), 0.0);
            }
        }
    }

    @Test
    public void scalePageUpCorrectlyLandscape() throws Exception {

        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("A3 Landscape.pdf"));

        PDRectangle pageSize = PDRectangle.A2;
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {

            Optional<PDDocument> result = pdfHelper.scaleToPageSize(document, pageSize);

            assertTrue(result.isPresent());
            try (PDDocument newDocument = result.get()) {
                PDRectangle mediaBox = newDocument.getPage(0).getMediaBox();

                assertEquals(0.0, pageSize.getHeight(), mediaBox.getHeight());
                assertEquals(0.0, pageSize.getWidth(), mediaBox.getWidth());
            }
        }
    }

    @Test
    public void doesResizeDownDocumentWhichDoesNeedResizing() throws Exception {

        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("another-test.pdf"));

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDRectangle pageSize = PDRectangle.A4;
            Optional<PDDocument> result = pdfHelper.scaleToPageSize(document, pageSize);

            assertTrue(result.isPresent());
            try (PDDocument newDocument = result.get()) {
                PDRectangle mediaBox = newDocument.getPage(0).getMediaBox();

                assertTrue(pageSize.getHeight() >= mediaBox.getHeight());
                assertTrue(pageSize.getWidth() >= mediaBox.getWidth());
            }
        }
    }

    @Test
    public void doesResizeUpDocumentWhichDoesNeedResizing() throws Exception {

        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("another-test.pdf"));

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDRectangle pageSize = PDRectangle.A2;
            Optional<PDDocument> result = pdfHelper.scaleToPageSize(document, pageSize);

            assertTrue(result.isPresent());
            try (PDDocument newDocument = result.get()) {
                PDRectangle mediaBox = newDocument.getPage(0).getMediaBox();

                assertTrue(pageSize.getHeight() <= mediaBox.getHeight());
                assertTrue(pageSize.getWidth() <= mediaBox.getWidth());
            }
        }
    }

    @Test
    public void doesNotResizeDocumentWhichDoesNotNeedResizing() throws Exception {
        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("A3 Portrait.pdf"));

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {

            Optional<PDDocument> result = pdfHelper.scaleToPageSize(document, PDRectangle.A3);

            assertTrue(result.isEmpty());
        }
    }

    @Test
    public void doesResizeA4DocumentWhichDoesNeedResizing() throws Exception {

        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("another-test.pdf"));

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {

            Optional<PDDocument> result = pdfHelper.scaleToA4(document);

            assertTrue(result.isPresent());
            try (PDDocument newDocument = result.get()) {
                PDRectangle mediaBox = newDocument.getPage(0).getMediaBox();
                PDRectangle pageSize = PDRectangle.A4;
                assertTrue(pageSize.getHeight() >= mediaBox.getHeight());
                assertTrue(pageSize.getWidth() >= mediaBox.getWidth());
            }
        }
    }

    @Test
    public void allPagesWillBeScaledToA4() throws Exception {
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(new PDRectangle(PDRectangle.A1.getWidth(), PDRectangle.A1.getHeight())));
            document.addPage(new PDPage(new PDRectangle(PDRectangle.A2.getWidth(), PDRectangle.A2.getHeight())));
            document.addPage(new PDPage(new PDRectangle(PDRectangle.A3.getWidth(), PDRectangle.A3.getHeight())));
            document.addPage(new PDPage(new PDRectangle(PDRectangle.A4.getWidth(), PDRectangle.A4.getHeight())));
            document.addPage(new PDPage(new PDRectangle(PDRectangle.A5.getWidth(), PDRectangle.A5.getHeight())));
            document.addPage(new PDPage(new PDRectangle(PDRectangle.A6.getWidth(), PDRectangle.A5.getHeight())));

            Optional<PDDocument> result = pdfHelper.scaleToA4(document);
            assertThat(result.isPresent(), is(true));
            try (PDDocument newDocument = result.get()) {
                assertThat(newDocument.getNumberOfPages(), is(6));
                for (PDPage page: newDocument.getPages()) {
                    PDRectangle mediaBox = page.getMediaBox();
                    assertThat(mediaBox.getHeight(), is(PDRectangle.A4.getHeight()));
                    assertThat(mediaBox.getWidth(), is(PDRectangle.A4.getWidth()));
                }
            }
        }
    }

    @Test
    public void resizesDocumentWithMultiPages() throws Exception {

        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("MultiPage.pdf"));

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {

            Optional<PDDocument> result = pdfHelper.scaleToA4(document);

            assertTrue(result.isPresent());
            try (PDDocument newDocument = result.get()) {
                assertThat(newDocument.getNumberOfPages(), is(3));
                for (PDPage page: newDocument.getPages()) {
                    PDRectangle mediaBox = page.getMediaBox();
                    if (mediaBox.getWidth() > mediaBox.getHeight()) {
                        assertThat(mediaBox.getHeight(), is(PDRectangle.A4.getWidth()));
                        assertThat(mediaBox.getWidth(), is(PDRectangle.A4.getHeight()));
                    } else {
                        assertThat(mediaBox.getHeight(), is(PDRectangle.A4.getHeight()));
                        assertThat(mediaBox.getWidth(), is(PDRectangle.A4.getWidth()));
                    }
                }
            }
        }
    }
}
