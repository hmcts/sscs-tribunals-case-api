package uk.gov.hmcts.sscs.tribunals.domain.corecase;

public class AppealReasons {

    private String reason;

    private String description;

    public AppealReasons() {
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "AppealReasons{"
                +   "reason='" + reason + '\''
                +  ", description='" + description + '\''
                +  '}';
    }
}
