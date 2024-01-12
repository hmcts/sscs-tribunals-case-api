package uk.gov.hmcts.reform.sscs.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Test;

public class PdfWatermarkerTest {

    final String page1 = "Appellant evidence Addition  A | Page 1\n";
    final String page2 = "Appellant evidence Addition  A | Page 2\n";

    @Test
    public void shrinkAndWatermarkAPdfWithOnePage() throws Exception {
        PdfWatermarker pw = new PdfWatermarker();
        PDDocument document = new PDDocument();
        document.addPage(new PDPage(PDRectangle.A4));
        byte[] blankPdf = getBytes(document);
        byte[] outputBytes = pw.shrinkAndWatermarkPdf(blankPdf,
                "Appellant evidence","Addition  A");
        try (PDDocument doc = PDDocument.load(outputBytes)) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(page1).isEqualToNormalizingNewlines(text);
        }
    }

    @Test
    public void shrinkAndWatermarkAPdfWithTwoPages() throws Exception {
        PdfWatermarker pw = new PdfWatermarker();
        PDDocument document = new PDDocument();
        document.addPage(new PDPage(PDRectangle.A4));
        document.addPage(new PDPage(PDRectangle.A4));
        byte[] blankPdf = getBytes(document);
        byte[] outputBytes = pw.shrinkAndWatermarkPdf(blankPdf,
                "Appellant evidence", "Addition  A");
        try (PDDocument doc = PDDocument.load(outputBytes)) {
            String text = new PDFTextStripper().getText(doc);
            assertEquals(2, doc.getNumberOfPages());
            assertThat(page1 + page2).isEqualToNormalizingNewlines(text);
        }
    }

    @Test
    public void shrinkAndWatermarkAPdfWithTwoDifferentSizePages() throws Exception {
        PdfWatermarker pw = new PdfWatermarker();
        PDDocument document = new PDDocument();
        document.addPage(new PDPage(PDRectangle.LETTER));
        document.addPage(new PDPage(PDRectangle.A4));
        byte[] blankPdf = getBytes(document);
        byte[] outputBytes = pw.shrinkAndWatermarkPdf(blankPdf,
                "Appellant evidence", "Addition  A");
        try (PDDocument doc = PDDocument.load(outputBytes)) {
            String text = new PDFTextStripper().getText(doc);
            assertEquals(2, doc.getNumberOfPages());
            assertThat(page1 + page2).isEqualToNormalizingNewlines(text);
        }
    }

    @Test
    public void shrinkAndWatermarkPdfWithOwnerPassword() throws IOException {
        URL resource = getClass().getClassLoader().getResource("pdf/test-owner-password.pdf");
        PdfWatermarker pw = new PdfWatermarker();
        byte[] file = FileUtils.readFileToByteArray(new File(Objects.requireNonNull(resource).getPath()));
        assertDoesNotThrow(() -> pw.shrinkAndWatermarkPdf(file, "test", "test"));
    }

    private byte[] getBytes(PDDocument doc) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            doc.save(baos);
            doc.close();
            return baos.toByteArray();
        }

    }
}
