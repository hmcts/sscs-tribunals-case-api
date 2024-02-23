package uk.gov.hmcts.reform.sscs.pdf;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.util.Matrix;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.model.InputStreamWrapper;

/**
 * Copied from https://github.com/keefmarshall/pdfpoc
 */
@Service
@Slf4j
public class PdfWatermarker {

    private static final float SCALE_PERCENTAGE = .88f;
    private static final float margin = 45f;

    public byte[] shrinkAndWatermarkPdf(byte[] input, String leftText, String rightText) throws Exception {
        try (PDDocument document = PDDocument.load(input)) {
            document.setAllSecurityToBeRemoved(true);
            int count = 1;
            for (PDPage page : document.getPages()) {
                scaleContent(document, page, SCALE_PERCENTAGE);
                addFooterText(document, page, leftText, rightText + " | Page " + count++);
            }

            PdfACompliance p1a = new PdfACompliance();
            p1a.makeCompliant(document);

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                document.save(baos);
                return baos.toByteArray();
            }

        }
    }

    // TODO amend this to be appropriately styled / designed / spaced - also bear in mind lower margins
    // need to be in place for bulk print support which will define the text positioning.
    private void addFooterText(PDDocument document, PDPage page, String leftText, String rightText)  throws IOException {

        // NB we need to embed the font for PDF/A compliance.
        // Loading it here causes it to be embedded into the resulting PDF doc.
        // Note this TTF file is included in pdfbox 2.x, there don't seem to be any others available
        // - the running code can load form the deployed system but if this is likely to be a Docker image there's
        // no guarantee it will have any TTF fonts present. If we want something else we'll likely have to embed
        // it into the code base, and be really careful about licensing.
        PDFont font;
        /**
         * The input stream here is automatically closed by this try-with-resources statement.
         * If there is an exception closing the input stream, the InputStreamWrapper will
         * log and swallow the close-exception.
         * We only need to wrap with an InputStreamWrapper here because we want to
         * log and swallow any exceptions occurring on the close itself - if we didn't have this requirement we could just
         * assign the InputStream itself in the try-with-resources statement and the input stream would
         * be safely closed (eg.  try (InputStream inputStream = this.getClass().getResourceAsStream(... )) {
         */
        try (InputStreamWrapper fontStreamWrapper = new InputStreamWrapper(log, this.getClass().getResourceAsStream(
            "/org/apache/pdfbox/resources/ttf/LiberationSans-Regular.ttf"))) {

            font = PDType0Font.load(document, fontStreamWrapper.get(), true);

        }

        float fontSize = 12f;

        float rightTextWidth = (font.getStringWidth(rightText) / 1000.0f) * fontSize;

        float pageWidthDifference = (page.getMediaBox().getWidth() - page.getBBox().getWidth()) / 2;
        float xoffset = page.getMediaBox().getWidth() - pageWidthDifference - margin - rightTextWidth;

        try (PDPageContentStream contentStream =
                     new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true)) {

            contentStream.setFont(font, fontSize);
            contentStream.setNonStrokingColor(Color.BLACK);
            contentStream.beginText();
            contentStream.newLineAtOffset(pageWidthDifference + margin, margin);
            contentStream.showText(leftText);
            contentStream.endText();
            contentStream.beginText();
            contentStream.newLineAtOffset(xoffset, margin);
            contentStream.showText(rightText);
            contentStream.endText();
        }

    }

    /**
     * This solution duplicates what the examples do in iText, which is to write a raw scaling command
     * directly into the PDF, and then write a final command to cancel the scaling.
     * However, PDFBox has actual calls to replicate this, you don't have to write raw commands.
     */
    private void scaleContent(PDDocument document, PDPage page, float percentage) throws IOException {
        float offsetX = (page.getCropBox().getWidth() * (1 - percentage)) / 2; // centre horizontally
        float offsetY = (page.getCropBox().getHeight() * (1 - percentage)); // anchor the top, creates space for footer

        // First create a content stream before all others, add a matrix transformation to scale:
        try (PDPageContentStream contentStream =
                     new PDPageContentStream(document, page, PDPageContentStream.AppendMode.PREPEND, false)) {
            contentStream.saveGraphicsState(); // 'q' in PDF commands
            contentStream.transform(new Matrix(percentage, 0, 0, percentage, offsetX, offsetY));
            contentStream.saveGraphicsState();
        }

        // Now add a closing command to remove the scale effect by restoring the graphics states:
        try (PDPageContentStream contentStream =
                     new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, false)) {
            // In raw PDF this equates to: "\nQ\nQ\n" - we saved it twice so we have to restore twice
            contentStream.restoreGraphicsState();
            contentStream.restoreGraphicsState();
        }
    }

}