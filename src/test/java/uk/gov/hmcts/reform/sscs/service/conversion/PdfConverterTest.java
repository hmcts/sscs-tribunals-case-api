package uk.gov.hmcts.reform.sscs.service.conversion;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;
import org.junit.Test;

public class PdfConverterTest {
    private final PdfConverter converter = new PdfConverter();

    @Test
    public void accepts() {
        List<String> result = converter.accepts();

        assertEquals("application/pdf", result.get(0));
    }

    @Test
    public void convert() {
        File file = new File("/tmp");

        assertEquals(file, converter.convert(file));
    }
}
