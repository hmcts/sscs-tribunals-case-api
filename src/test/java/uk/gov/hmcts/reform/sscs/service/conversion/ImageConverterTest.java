package uk.gov.hmcts.reform.sscs.service.conversion;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnitParamsRunner.class)
public class ImageConverterTest {
    private final ImageConverter converter = new ImageConverter();

    @Test
    public void accepts() {
        assertEquals("image/bmp", converter.accepts().get(0));
        assertEquals("image/gif", converter.accepts().get(1));
        assertEquals("image/jpeg", converter.accepts().get(2));
        assertEquals("image/png", converter.accepts().get(3));
        assertEquals("image/svg+xml", converter.accepts().get(4));
        assertEquals("image/tiff", converter.accepts().get(5));
        assertEquals("image/jpeg", converter.accepts().get(6));
    }

    @Test
    @Parameters({"flying-pig.jpg"})
    public void convertImageWithALargerHeightThanWidthToPortraitPdf(String filename) throws IOException {
        File input = new File(ClassLoader.getSystemResource(filename).getPath());
        File output = converter.convert(input);

        assertEquals(".pdf", output.getName().substring(output.getName().lastIndexOf(".")));
        try (PDDocument document = Loader.loadPDF(output)) {
            assertEquals(1, document.getPages().getCount());
            assertEquals(PDRectangle.A4.getWidth(), document.getPage(0).getMediaBox().getWidth(), 0);
            assertEquals(PDRectangle.A4.getHeight(), document.getPage(0).getMediaBox().getHeight(), 0);
        }
    }

    @Test
    public void convertImageWithASmallerHeightThanWidthToLandscapePdf() throws IOException {
        File input = new File(ClassLoader.getSystemResource("Soviet_BMP-1_IFV.bmp").getPath());
        File output = converter.convert(input);

        assertEquals(".pdf", output.getName().substring(output.getName().lastIndexOf(".")));
        try (PDDocument document = Loader.loadPDF(output)) {
            assertEquals(1, document.getPages().getCount());
            assertEquals(PDRectangle.A4.getHeight(), document.getPage(0).getMediaBox().getWidth(), 0);
            assertEquals(PDRectangle.A4.getWidth(), document.getPage(0).getMediaBox().getHeight(), 0);
        }
    }
}
