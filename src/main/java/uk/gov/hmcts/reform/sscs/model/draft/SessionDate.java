package uk.gov.hmcts.reform.sscs.model.draft;

import lombok.Value;

@Value
public class SessionDate {
    private String day;
    private String month;
    private String year;
}
