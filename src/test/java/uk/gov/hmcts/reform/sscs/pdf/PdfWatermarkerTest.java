package uk.gov.hmcts.reform.sscs.pdf;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Test;

public class PdfWatermarkerTest {

    @Test
    public void shrinkAndWatermarkAPdfWithOnePage() throws Exception {
        PdfWatermarker pw = new PdfWatermarker();
        PDDocument document = new PDDocument();
        document.addPage(new PDPage(PDRectangle.A4));
        byte[] blankPdf = getBytes(document);
        byte[] outputBytes = pw.shrinkAndWatermarkPdf(blankPdf,
                "Appellant Further Evidence | Appendix  A");
        try (PDDocument doc = PDDocument.load(outputBytes)) {
            String text = new PDFTextStripper().getText(doc);
            assertEquals("Appellant Further Evidence | Appendix  A        1\n", text);
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
                "Appellant Further Evidence | Appendix  A");
        try (PDDocument doc = PDDocument.load(outputBytes)) {
            String text = new PDFTextStripper().getText(doc);
            assertEquals(2, doc.getNumberOfPages());
            assertEquals("Appellant Further Evidence | Appendix  A        1\n"
                    + "Appellant Further Evidence | Appendix  A        2\n", text);
        }
    }

    private byte[] getBytes(PDDocument doc) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            doc.save(baos);
            doc.close();
            return baos.toByteArray();
        }

    }
}