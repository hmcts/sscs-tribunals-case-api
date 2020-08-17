package uk.gov.hmcts.reform.sscs.thirdparty.pdfservice;

import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.IOUtils;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ResourceManager {

    public byte[] getResource(String file) throws IOException {
        InputStream in = null;
        byte[] byteArray = null;
        try {
            in = getClass().getResourceAsStream(file);
        } finally {
            if (in != null) {
                byteArray = IOUtils.toByteArray(in);

                safeClose(in);
            }
        }
        return byteArray;
    }

    public static void safeClose(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
    }

}
