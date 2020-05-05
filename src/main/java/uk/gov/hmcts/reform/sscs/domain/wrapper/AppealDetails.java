package uk.gov.hmcts.reform.sscs.domain.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import java.util.Objects;

@JsonInclude(value = JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppealDetails {
    private final String submittedDate;
    private final String mrnDate;
    private final String benefitType;

    public AppealDetails(String submittedDate, String mrnDate, String benefitType) {
        this.submittedDate = submittedDate;
        this.mrnDate = mrnDate;
        this.benefitType = benefitType;
    }

    @ApiModelProperty(example = "some date format to do", required = true)
    @JsonProperty(value = "submitted_date")
    public String getSubmittedDate() {
        return submittedDate;
    }

    @ApiModelProperty(example = "some date format to do", required = true)
    @JsonProperty(value = "mrn_date")
    public String getMrnDate() {
        return mrnDate;
    }

    @ApiModelProperty(example = "PIP", required = true)
    @JsonProperty(value = "benefit_type")
    public String getBenefitType() {
        return benefitType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AppealDetails that = (AppealDetails) o;
        return Objects.equals(submittedDate, that.submittedDate)
                && Objects.equals(mrnDate, that.mrnDate)
                && Objects.equals(benefitType, that.benefitType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(submittedDate, mrnDate, benefitType);
    }

    @Override
    public String toString() {
        return "AppealDetails{"
                + "submittedDate='" + submittedDate + '\''
                + ", mrnDate='" + mrnDate + '\''
                + ", benefitType='" + benefitType + '\''
                + '}';
    }
}
