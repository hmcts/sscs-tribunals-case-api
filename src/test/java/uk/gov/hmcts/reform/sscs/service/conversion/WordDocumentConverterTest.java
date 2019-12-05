package uk.gov.hmcts.reform.sscs.service.conversion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import okhttp3.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

public class WordDocumentConverterTest {

    private OkHttpClient httpClient;

    private Call call;

    private Response response;

    private ResponseBody responseBody;

    private WordDocumentConverter converter;

    @Before
    public void setup() {
        httpClient = new OkHttpClient
                .Builder()
                .addInterceptor(WordDocumentConverterTest::intercept)
                .build();
        converter = new WordDocumentConverter(httpClient, "http://www.example.com", "key");
    }

    private static Response intercept(Interceptor.Chain chain) throws IOException {
        InputStream file = ClassLoader.getSystemResourceAsStream("wordDocument.doc");

        return new Response.Builder()
                .body(ResponseBody.create(IOUtils.toByteArray(Objects.requireNonNull(file)),
                    MediaType.get("application/pdf")))
                .request(chain.request())
                .message("")
                .code(200)
                .protocol(Protocol.HTTP_2)
                .build();
    }

    @Test
    public void accepts() {
        assertEquals("text/plain", converter.accepts().get(0));
        assertEquals("application/msword", converter.accepts().get(1));
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document", converter.accepts().get(2));
        assertEquals("application/x-tika-ooxml", converter.accepts().get(3));
        assertEquals("application/x-tika-msoffice", converter.accepts().get(4));
    }

    @Test
    public void convert() throws IOException {
        File input = new File(ClassLoader.getSystemResource("wordDocument.doc").getPath());

        File output = converter.convert(input);

        assertNotEquals(input.getName(), output.getName());
        assertEquals("pdf", FilenameUtils.getExtension(output.getName()));
    }

}
