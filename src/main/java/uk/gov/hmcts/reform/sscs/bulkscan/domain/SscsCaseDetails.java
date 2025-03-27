package uk.gov.hmcts.reform.sscs.bulkscan.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SscsCaseDetails {

    @JsonProperty("case_data")
    private SscsCaseData caseData;

    @JsonProperty("id")
    private String caseId;

    private String state;
}
