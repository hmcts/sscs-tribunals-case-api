package uk.gov.hmcts.reform.sscs.service.conversion;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.Test;


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
    public void convert() throws IOException {
        File input = new File(ClassLoader.getSystemResource("flying-pig.jpg").getPath());
        File output = converter.convert(input);

        assertEquals(".pdf", output.getName().substring(output.getName().lastIndexOf(".")));
        try (PDDocument document = PDDocument.load(output)) {
            assertEquals(1, document.getPages().getCount());
        }
    }
}
