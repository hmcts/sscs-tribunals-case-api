package uk.gov.hmcts.reform.sscs.thirdparty.pdfservice;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;
import org.slf4j.Logger;

/**
 * Wraps an InputStream as an AutoCloseable who's close method logs and swallows the exception instead of propagating it.
 */
public class InputStreamWrapper implements AutoCloseable, Supplier<InputStream> {

    private Logger log;
    private InputStream inputStream;

    public InputStreamWrapper(Logger log, InputStream inputStream) {
        this.log = log;
        this.inputStream = inputStream;
    }

    @Override
    public void close() {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
    }

    @Override
    public InputStream get() {
        return inputStream;
    }
}
