package uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;

@Data
@Builder
public class InputScannedDoc {

    private final String type;
    private final String subtype;
    private final DocumentLink url;
    private final String controlNumber;
    private final String fileName;
    private final LocalDateTime scannedDate;
    private final LocalDateTime deliveryDate;

    public InputScannedDoc(
        @JsonProperty("type") String type,
        @JsonProperty("subtype") String subtype,
        @JsonProperty("url") DocumentLink url,
        @JsonProperty("control_number") String controlNumber,
        @JsonProperty("file_name") String fileName,
        @JsonProperty("scanned_date") LocalDateTime scannedDate,
        @JsonProperty("delivery_date") LocalDateTime deliveryDate
    ) {
        this.type = type;
        this.subtype = subtype;
        this.url = url;
        this.controlNumber = controlNumber;
        this.fileName = fileName;
        this.scannedDate = scannedDate;
        this.deliveryDate = deliveryDate;
    }
}
