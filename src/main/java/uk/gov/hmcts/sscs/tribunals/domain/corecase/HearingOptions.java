package uk.gov.hmcts.sscs.tribunals.domain.corecase;

import java.util.Arrays;

public class HearingOptions {

    private ExcludeDates[] excludeDates;

    private String other;

    private String[] arrangements;

    public HearingOptions() {
    }

    public ExcludeDates[] getExcludeDates() {
        return excludeDates;
    }

    public void setExcludeDates(ExcludeDates[] excludeDates) {
        this.excludeDates = excludeDates;
    }

    public String getOther() {
        return other;
    }

    public void setOther(String other) {
        this.other = other;
    }

    public String[] getArrangements() {
        return arrangements;
    }

    public void setArrangements(String[] arrangements) {
        this.arrangements = arrangements;
    }

    @Override
    public String toString() {
        return "HearingOptions{"
                +    "excludeDates=" + Arrays.toString(excludeDates)
                +    ", other='" + other + '\''
                +    ", arrangements=" + Arrays.toString(arrangements)
                +    '}';
    }
}
