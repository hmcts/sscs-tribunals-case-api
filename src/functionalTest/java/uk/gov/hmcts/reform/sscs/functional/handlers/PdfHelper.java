package uk.gov.hmcts.reform.sscs.functional.handlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

public class PdfHelper {

    private PdfHelper() {
        // noop
    }

    public static byte[] getPdf(String text) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (PDDocument doc = new PDDocument()) {
                PDPage pdPage = new PDPage();
                try (PDPageContentStream contentStream = new PDPageContentStream(doc, pdPage)) {
                    contentStream.beginText();
                    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN), 12);
                    contentStream.showText(text);
                    contentStream.newLineAtOffset(2, 10);
                    contentStream.endText();
                }
                doc.addPage(pdPage);
                doc.save(baos);
            }
            return baos.toByteArray();
        }
    }
}
