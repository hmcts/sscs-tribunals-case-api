package uk.gov.hmcts.reform.sscs.service.conversion;

import com.google.common.collect.Lists;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

/**
 * Uses pdfboxx to create a PDF with a single page showing the image.
 * copied from https://github.com/keefmarshall/pdfpoc
 */
@Service
public class ImageConverter implements FileToPdfConverter {
    private static final int MARGIN = 50; // TODO allow this to be configurable

    @Override
    public List<String> accepts() {
        return Lists.newArrayList(
                "image/bmp",
                "image/gif",
                "image/jpeg",
                "image/png",
                "image/svg+xml",
                "image/tiff",
                "image/jpeg"
        );
    }

    @Override
    public File convert(File file) throws IOException {
        // create blank PDF
        try (PDDocument doc = new PDDocument()) {


            // Load image:
            byte[] imageBytes = readImageAsBytesWithCorrectRotation(file);
            PDImageXObject pdImage = PDImageXObject.createFromByteArray(doc, imageBytes, null);

            // need to find the scale, or the image will be too big or too small
            // Q: do we scale up, if the image is too small? Will result in pixellation..
            int imageHeight = pdImage.getHeight();
            int imageWidth = pdImage.getWidth();

            boolean isPdfLandscape = imageHeight < imageWidth;
            float pdfWidth = isPdfLandscape ?  PDRectangle.A4.getHeight() : PDRectangle.A4.getWidth();
            float pdfHeight = isPdfLandscape ? PDRectangle.A4.getWidth() : PDRectangle.A4.getHeight();

            // a valid PDF document requires at least one page
            PDPage page = new PDPage(new PDRectangle(pdfWidth, pdfHeight));

            // To get MacOSX Preview to scale exactly 100% for A4 you need to use rounded dimensions:
            doc.addPage(page);

            float hscale = (page.getCropBox().getWidth() - (MARGIN * 2)) / imageWidth;
            float vscale = (page.getCropBox().getHeight() - (MARGIN * 2)) / imageHeight;
            float scale = Math.min(hscale, vscale);

            // Place image near the top of the page:
            float ypos = page.getCropBox().getHeight() - (imageHeight * scale) - MARGIN;

            // Add image to document
            try (PDPageContentStream contentStream =
                         new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                contentStream.drawImage(pdImage, MARGIN, ypos, imageWidth * scale, imageHeight * scale);
            }
            File outputFile = Files.createTempFile(Paths.get("").toAbsolutePath(), file.getName(), ".pdf").toFile();
            outputFile.deleteOnExit();

            doc.save(outputFile);
            return outputFile;
        }
    }

    private byte[] readImageAsBytesWithCorrectRotation(File file) throws IOException {

        // "Thumbnails" seems like real overkill, but honestly rotating the image correctly
        // is a right pain otherwise. Reading it in at scale 1.0 like this does the trick.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Thumbnails.of(file).scale(1).toOutputStream(baos);
        baos.flush();
        byte[] imageInByte = baos.toByteArray();
        baos.close();

        return imageInByte;
    }

}
