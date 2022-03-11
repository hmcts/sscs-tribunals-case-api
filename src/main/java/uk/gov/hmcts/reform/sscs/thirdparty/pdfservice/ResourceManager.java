package uk.gov.hmcts.reform.sscs.thirdparty.pdfservice;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.model.InputStreamWrapper;

@Service
@Slf4j
public class ResourceManager {

    public byte[] getResource(String file) throws IOException {
        /**
         * The input stream obtained here is wrapped inside an InputStreamWrapper AutoCloseable
         * and so is automatically closed by this try-with-resource statement.
         * Any exceptions occurring on actually closing the stream are logged and swallowed by InputStreamWrapper,
         * whereas any exceptions occurring prior to a close attempt will cause a call to close and
         * the original exception will propagate
         */
        try (InputStreamWrapper inputStreamWrapper = new InputStreamWrapper(log, getClass().getResourceAsStream(file))) {
            return inputStreamWrapper.get() != null ?  inputStreamWrapper.get().readAllBytes() : null;
        }
    }
}
