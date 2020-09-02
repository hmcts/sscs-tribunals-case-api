package uk.gov.hmcts.reform.sscs.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;

@Component
public class BundleAdditionFilenameBuilder {

    public  String build(DocumentType documentType, String bundleAddition, String scannedDate) {
        String bundleText = "";
        if (bundleAddition != null) {
            bundleText = "Addition " + bundleAddition + " - ";
        }
        scannedDate = scannedDate != null ? LocalDateTime.parse(scannedDate).format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) : LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        String label = documentType.getLabel() != null ? documentType.getLabel() : documentType.getValue();

        return bundleText + label + " received on " + scannedDate;
    }
}
