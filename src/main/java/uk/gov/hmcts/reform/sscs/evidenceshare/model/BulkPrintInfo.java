package uk.gov.hmcts.reform.sscs.evidenceshare.model;

import java.util.Optional;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BulkPrintInfo {
    UUID uuid;
    boolean allowedTypeForBulkPrint;
    String desc;

    public Optional<UUID> getUuid() {
        return Optional.ofNullable(uuid);
    }
}
