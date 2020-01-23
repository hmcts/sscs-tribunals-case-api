package uk.gov.hmcts.reform.sscs.ccd.presubmit.createbundle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;

@Data
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SscsDocumentBundlingDto {

    private DocumentLink documentLink;

    private String createdDatetime;

    private String documentName;

}
