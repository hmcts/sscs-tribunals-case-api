package uk.gov.hmcts.reform.sscs.model.docassembly;

import lombok.Builder;
import lombok.Getter;
import uk.gov.hmcts.reform.docassembly.domain.FormPayload;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;

@Builder
@Getter
public class GenerateFileParams {
    private String userAuthentication;
    private String templateId;
    private FormPayload formPayload;
    @Builder.Default private OutputType outputType = OutputType.PDF;
    private String renditionOutputLocation;
}
