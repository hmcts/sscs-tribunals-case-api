package uk.gov.hmcts.reform.sscs.tyanotifications.service.coh;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuestionReferences {
    private String deadlineExpiryDate;

    public QuestionReferences(@JsonProperty(value = "deadline_expiry_date") String deadlineExpiryDate) {
        this.deadlineExpiryDate = deadlineExpiryDate;
    }
}
