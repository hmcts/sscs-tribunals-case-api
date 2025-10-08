package uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScannedData {

    private Map<String, Object> ocrCaseData;

    private List<InputScannedDoc> records;

    private String openingDate;

}
