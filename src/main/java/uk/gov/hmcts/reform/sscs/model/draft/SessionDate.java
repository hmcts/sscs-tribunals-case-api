package uk.gov.hmcts.reform.sscs.model.draft;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Value;
import uk.gov.hmcts.reform.sscs.ccd.domain.DateRange;

@Value
public class SessionDate {
    private String day;
    private String month;
    private String year;

    @JsonCreator
    public SessionDate(String day, String month, String year) {
        this.day = day;
        this.month = month;
        this.year = year;
    }

    public SessionDate(DateRange dateRange) {
        this.day = Integer.toString(Integer.parseInt(dateRange.getStart().substring(8,10)));
        this.month = Integer.toString(Integer.parseInt(dateRange.getStart().substring(5,7)));
        this.year = Integer.toString(Integer.parseInt(dateRange.getStart().substring(0,4)));
    }
}
