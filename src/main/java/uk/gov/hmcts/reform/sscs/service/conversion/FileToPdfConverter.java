package uk.gov.hmcts.reform.sscs.service.conversion;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface FileToPdfConverter {

    List<String> accepts();

    File convert(File file) throws IOException;
}
