package uk.gov.hmcts.reform.sscs.domain.wrapper;

public class SyaBenefitType {

    private String description;

    private String code;

    public SyaBenefitType() {
        //
    }

    public SyaBenefitType(String description, String code) {
        this.description = description;
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return "SyaBenefitType{"
                + "description='" + description + '\''
                + ", code='" + code + '\''
                + '}';
    }
}
