package uk.gov.hmcts.sscs.domain.corecase;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ExcludeDates that = (ExcludeDates) o;
        return Objects.equals(start, that.start)
                && Objects.equals(end, that.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }
}
