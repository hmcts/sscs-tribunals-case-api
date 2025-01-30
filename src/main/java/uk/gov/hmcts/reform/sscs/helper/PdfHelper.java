package uk.gov.hmcts.reform.sscs.helper;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.util.Matrix;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PdfHelper {

    private static final float TOLERANCE_FACTOR = 0.01f;
    private static final BigDecimal SCALE_UP = BigDecimal.ONE.negate();
    private static final BigDecimal NO_CHANGE = BigDecimal.ZERO;

    public Optional<PDDocument> scaleToA4(PDDocument document) throws Exception {
        return scaleToPageSize(document, PDRectangle.A4);
    }

    public Optional<PDDocument> scaleToPageSize(PDDocument document, PDRectangle size) throws Exception {

        boolean isWithinPageSize = isDocumentWithinSizeTolerance(document, size);

        if (isWithinPageSize) {
            log.info("PDF is correct size");
            return Optional.empty();
        }
        for (PDPage page : document.getPages()) {
            BigDecimal scalingFactor = scalingFactor(page, size);
            if (!scalingFactor.equals(NO_CHANGE)) {
                scalePageToSize(page, size);

                if (!scalingFactor.equals(SCALE_UP)) {
                    scaleContent(document, page, scalingFactor.floatValue());
                }
            }
        }
        return Optional.of(document);
    }

    private boolean isPageCorrectSize(PDPage page, PDRectangle size) {
        float pageHeight = page.getCropBox().getHeight();
        float pageWidth = page.getCropBox().getWidth();

        float upperLimitHeight = size.getHeight() * (1 + TOLERANCE_FACTOR);
        float lowerLimitHeight = size.getHeight() * (1 - TOLERANCE_FACTOR);

        float upperLimitWidth = size.getWidth() * (1 + TOLERANCE_FACTOR);
        float lowerLimitWidth = size.getWidth() * (1 - TOLERANCE_FACTOR);

        log.debug("Pdf height {}, upper limit {}, lower limit {}", pageHeight, upperLimitHeight, lowerLimitHeight);
        log.debug("Pdf width {}, limit {}, lower limit {}", pageWidth, upperLimitWidth, lowerLimitWidth);

        if (pageHeight > pageWidth) {
            return (pageHeight <= upperLimitHeight)
                    && (pageHeight >= lowerLimitHeight)
                    && (pageWidth <= upperLimitWidth)
                    && (pageWidth >= lowerLimitWidth);
        }
        return (pageWidth <= upperLimitHeight)
                && (pageWidth >= lowerLimitHeight)
                && (pageHeight <= upperLimitWidth)
                && (pageHeight >= lowerLimitWidth);

    }

    protected boolean isDocumentWithinSizeTolerance(PDDocument document, PDRectangle size) {
        for (PDPage page : document.getPages()) {
            boolean isCorrectSize = isPageCorrectSize(page, size);
            if (!isCorrectSize) {
                return false;
            }
        }
        return true;
    }

    protected BigDecimal scalingFactor(PDPage page, PDRectangle size) {
        if (isPageCorrectSize(page, size)) {
            return NO_CHANGE;
        }

        final float pageHeight = page.getMediaBox().getHeight();
        final float pageWidth = page.getMediaBox().getWidth();
        float sizeHeight = size.getHeight();
        float sizeWidth = size.getWidth();

        if (pageWidth > pageHeight) {
            sizeHeight = size.getWidth();
            sizeWidth = size.getHeight();
        }

        log.debug("A4 height limit = " + sizeHeight);
        log.debug("A4 width limit = " + sizeWidth);

        log.debug("Page Height = " + pageHeight);
        log.debug("Page Width = " + pageWidth);

        float heightOverage = pageHeight - sizeHeight;
        log.debug("height overage = " + heightOverage);
        float widthOverage = pageWidth - sizeWidth;
        log.debug("width overage = " + widthOverage);

        BigDecimal maxHeightScaling = maxScaleFactor(heightOverage, pageHeight);
        log.debug("max height scaling = " + maxHeightScaling);
        BigDecimal maxWidthScaling = maxScaleFactor(widthOverage, pageWidth);
        log.debug("max width scaling = " + maxWidthScaling);

        if (maxHeightScaling.compareTo(NO_CHANGE) < 0
                && maxWidthScaling.compareTo(NO_CHANGE) < 0) {
            return SCALE_UP;
        }

        BigDecimal scalingFactor = maxHeightScaling.compareTo(maxWidthScaling) > 0 ? maxHeightScaling : maxWidthScaling;

        return BigDecimal.ONE.subtract(scalingFactor).setScale(4, RoundingMode.HALF_EVEN);
    }

    private BigDecimal maxScaleFactor(float overage, float pageAxisDimension) {
        BigDecimal maxScaleFactor = NO_CHANGE;
        if (overage > 0) {
            BigDecimal scaleFactor = BigDecimal.valueOf(overage / pageAxisDimension);
            maxScaleFactor = scaleFactor.compareTo(maxScaleFactor) > 0 ? scaleFactor : maxScaleFactor;
        } else if (overage < 0) {
            maxScaleFactor = SCALE_UP;
        }
        return maxScaleFactor;
    }

    protected void scalePageToSize(PDPage page, PDRectangle size) {
        PDRectangle newSize = page.getMediaBox().getHeight() > page.getMediaBox().getWidth() ? size : new PDRectangle(size.getWidth(), size.getHeight());
        page.setMediaBox(newSize);
        page.setCropBox(newSize);
        page.setBleedBox(newSize);
        page.setTrimBox(newSize);
        page.setArtBox(newSize);
    }

    private void scaleContent(PDDocument document, PDPage page, float percentage) throws IOException {

        // First create a content stream before all others, add a matrix transformation to scale:
        try (PDPageContentStream contentStream =
                     new PDPageContentStream(document, page, PDPageContentStream.AppendMode.PREPEND, false)) {

            contentStream.saveGraphicsState(); // 'q' in PDF commands
            contentStream.transform(new Matrix(percentage, 0, 0, percentage, 0, 0));
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
