package uk.gov.hmcts.sscs.tribunals.domain.corecase;

public class ExcludeDates {

    private String start;

    private String end;

    public ExcludeDates() {
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    @Override
    public String toString() {
        return "ExcludeDates{"
                +    "start='" + start + '\''
                +    ", end='" + end + '\''
                +    '}';
    }
}
