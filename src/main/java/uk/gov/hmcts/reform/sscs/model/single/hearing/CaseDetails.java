package uk.gov.hmcts.reform.sscs.model.single.hearing;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CaseDetails {

    private String hmctsServiceCode;

    @JsonProperty("caseRef")
    private String caseId;

    private String externalCaseReference;

    private String caseDeepLink;

    private String hmctsInternalCaseName;

    private String publicCaseName;

    private boolean caseAdditionalSecurityFlag;

    private boolean caseInterpreterRequiredFlag;

    private List<CaseCategory> caseCategories;

    private String caseManagementLocationCode;

    @JsonProperty("caserestrictedFlag")
    private boolean caseRestrictedFlag;

    @JsonProperty("caseSLAStartDate")
    private String caseSlaStartDate;

}
