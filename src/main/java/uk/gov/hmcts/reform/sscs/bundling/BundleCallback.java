package uk.gov.hmcts.reform.sscs.bundling;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseData;

@Setter
@Getter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BundleCallback<T extends CaseData> extends Callback<T> {

    @JsonProperty("caseTypeId")
    private String caseTypeId;

    @JsonProperty("jurisdictionId")
    private String jurisdictionId;

    public BundleCallback(Callback<T> callback) {
        super(callback.getCaseDetails(), callback.getCaseDetailsBefore(), callback.getEvent(), callback.isIgnoreWarnings());
        setCaseTypeId(callback.getCaseDetails().getCaseTypeId());
        setJurisdictionId(callback.getCaseDetails().getJurisdiction());
    }

}
