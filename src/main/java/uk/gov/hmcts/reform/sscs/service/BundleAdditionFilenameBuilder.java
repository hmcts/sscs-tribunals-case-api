package uk.gov.hmcts.reform.sscs.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;

@Component
public class BundleAdditionFilenameBuilder {

    public String build(DocumentType documentType, String bundleAddition, String scannedDate) {
        return this.build(documentType, bundleAddition, scannedDate, null);
    }

    public String build(DocumentType documentType, String bundleAddition, String scannedDate, DateTimeFormatter dateTimeFormatter) {
        String bundleText = "";
        if (bundleAddition != null) {
            bundleText = "Addition " + bundleAddition + " - ";
        }
        if (dateTimeFormatter != null) {
            scannedDate = scannedDate != null ? LocalDate.parse(scannedDate, DateTimeFormatter.ISO_LOCAL_DATE).format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) : LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        } else {
            scannedDate = scannedDate != null ? LocalDateTime.parse(scannedDate).format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) : LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        }
        String label = documentType.getLabel() != null ? documentType.getLabel() : documentType.getValue();

        return bundleText + label + " received on " + scannedDate;
    }
}
