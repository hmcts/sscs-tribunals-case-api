package uk.gov.hmcts.reform.sscs.service.conversion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static uk.gov.hmcts.reform.sscs.service.conversion.WordDocumentConverterTest.intercept;

import java.io.File;
import java.io.IOException;
import okhttp3.OkHttpClient;
import org.apache.commons.io.FilenameUtils;
import org.junit.Before;
import org.junit.Test;


public class RtfConverterTest {
    private RtfConverter rtfConverter;

    private OkHttpClient httpClient;

    @Before
    public void setup() {
        httpClient = new OkHttpClient
                .Builder()
                .addInterceptor(chain -> intercept(chain, 200))
                .build();
        rtfConverter = new RtfConverter(httpClient, "http://www.example.com", "key");
    }

    @Test
    public void accepts() {
        assertEquals("text/rtf", rtfConverter.accepts().get(0));
        assertEquals("application/rtf", rtfConverter.accepts().get(1));
    }

    @Test
    public void convertRtfToPdf() throws IOException {
        File input = new File(ClassLoader.getSystemResource("Evidence.rtf").getPath());
        File output = rtfConverter.convert(input);

        output.createNewFile();

        assertNotEquals(input.getName(), output.getName());
        assertEquals("pdf", FilenameUtils.getExtension(output.getName()));
    }

}
