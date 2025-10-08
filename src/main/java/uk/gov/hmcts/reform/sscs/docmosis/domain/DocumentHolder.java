package uk.gov.hmcts.reform.sscs.docmosis.domain;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DocumentHolder {

    private final Template template;
    private final Map<String, Object> placeholders;
    private boolean pdfArchiveMode;


}

