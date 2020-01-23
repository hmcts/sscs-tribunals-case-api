package uk.gov.hmcts.reform.sscs.ccd.presubmit.createbundle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseData;

@Data
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SscsBundlingDto2 {

    private String bundleConfiguration;

    private List<SscsDocumentBundlingDto> sscsDocument;

}
