package uk.gov.hmcts.sscs.tribunals.domain.corecase;

import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = {"start", "end"})
public class ExcludeDates {

    private String start;

    private String end;

    public ExcludeDates(String start, String end) {
        this.start = start;
        this.end = end;
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
