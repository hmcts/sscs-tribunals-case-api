package uk.gov.hmcts.reform.sscs;

import lombok.Data;

@Data
public class CsvData {
    private String reference;
    private String timestamp;
    private String event;

}
