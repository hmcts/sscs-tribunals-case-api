package uk.gov.hmcts.reform.sscs.bulkscan.common;

import static com.google.common.io.Resources.getResource;

import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import org.apache.commons.codec.Charsets;

public class TestHelper {
    public static final String TEST_SERVICE_AUTH_TOKEN = "testServiceAuth";
    public static final String TEST_USER_AUTH_TOKEN = "testUserAuth";
    public static final String TEST_USER_ID = "1234";

    private TestHelper() {
        //Utility class
    }

    public static String exceptionRecord(String fileName) throws IOException {
        URL url = getResource(fileName);
        return Resources.toString(url, Charsets.toCharset("UTF-8"));
    }
}
