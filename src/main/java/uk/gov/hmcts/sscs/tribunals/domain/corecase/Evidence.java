package uk.gov.hmcts.sscs.tribunals.domain.corecase;

public class Evidence {

    private String description;

    private String date;

    public Evidence() {
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "Evidence{"
                +       "description='" + description + '\''
                +       ", date='" + date + '\''
                +       '}';
    }
}
