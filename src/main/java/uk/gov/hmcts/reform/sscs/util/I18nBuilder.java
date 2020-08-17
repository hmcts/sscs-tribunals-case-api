package uk.gov.hmcts.reform.sscs.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.IOUtils;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class I18nBuilder {

    public Map build() throws IOException {
        InputStream in = null;
        try {
            in = getClass().getResourceAsStream("/json/en.json");
        } finally {
            if (in != null) {
                byte[] byteArray = IOUtils.toByteArray(in);
                HashMap hashObject = new ObjectMapper().readValue(byteArray, HashMap.class);

                safeClose(in);

                return hashObject;
            }
        }
        return null;
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
