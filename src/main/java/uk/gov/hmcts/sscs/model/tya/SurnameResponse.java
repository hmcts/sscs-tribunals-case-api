package uk.gov.hmcts.sscs.model.tya;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class SurnameResponse {

    private String caseId;
    private String appealNumber;
    private String surname;


    public SurnameResponse() {
    }

    public SurnameResponse(String caseId, String appealNumber, String surname) {
        this.caseId = caseId;
        this.appealNumber = appealNumber;
        this.surname = surname;
    }
}
