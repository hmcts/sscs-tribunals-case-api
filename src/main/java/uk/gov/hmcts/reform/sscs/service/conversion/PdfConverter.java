package uk.gov.hmcts.reform.sscs.service.conversion;

import com.google.common.collect.Lists;
import java.io.File;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * No-op. This converter does nothing as the document is already a PDF.
 */
@Service
public class PdfConverter implements FileToPdfConverter {
    @Override
    public List<String> accepts() {
        return Lists.newArrayList("application/pdf");
    }

    @Override
    public File convert(File file) {
        return file;
    }
}
