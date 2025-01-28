package uk.gov.hmcts.reform.sscs.service.conversion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

public class WordDocumentConverterTest {

    private OkHttpClient httpClient;

    private WordDocumentConverter converter;

    @Before
    public void setup() {
        httpClient = new OkHttpClient
                .Builder()
                .addInterceptor(chain -> intercept(chain, 200))
                .build();
        converter = new WordDocumentConverter(httpClient, "http://www.example.com", "key");
    }

    protected static Response intercept(Interceptor.Chain chain, int response) throws IOException {
        InputStream file = ClassLoader.getSystemResourceAsStream("wordDocument.doc");

        return new Response.Builder()
                .body(ResponseBody.create(IOUtils.toByteArray(file), MediaType.get("application/pdf")))
                .request(chain.request())
                .message("")
                .code(response)
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

    @Test(expected = IOException.class)
    public void failsConversion() throws IOException {
        httpClient = new OkHttpClient
                .Builder()
                .addInterceptor(chain -> intercept(chain, 500))
                .build();
        converter = new WordDocumentConverter(httpClient, "http://www.example.com", "key");

        File input = new File(ClassLoader.getSystemResource("wordDocument.doc").getPath());

        converter.convert(input);
    }

}
