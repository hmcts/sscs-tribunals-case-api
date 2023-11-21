package uk.gov.hmcts.reform.sscs.bundling;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseData;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BundleCallback<T extends CaseData> extends Callback {

    @JsonProperty("caseTypeId")
    private String caseTypeId;

    @JsonProperty("jurisdictionId")
    private String jurisdictionId;

    public BundleCallback(Callback callback) {
        super(callback.getCaseDetails(), callback.getCaseDetailsBefore(), callback.getEvent(), callback.isIgnoreWarnings());
        setCaseTypeId(callback.getCaseDetails().getCaseTypeId());
        setJurisdictionId(callback.getCaseDetails().getJurisdiction());
    }

    public String getCaseTypeId() {
        return caseTypeId;
    }

    public void setCaseTypeId(String caseTypeId) {
        this.caseTypeId = caseTypeId;
    }

    public String getJurisdictionId() {
        return jurisdictionId;
    }

    public void setJurisdictionId(String jurisdictionId) {
        this.jurisdictionId = jurisdictionId;
    }

}
