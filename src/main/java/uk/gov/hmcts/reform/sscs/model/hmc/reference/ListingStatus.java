package uk.gov.hmcts.reform.sscs.model.hmc.reference;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ListingStatus {
    DRAFT("Draft"),
    PROVISIONAL("Provisional"),
    FIXED("Fixed"),
    CNCL("Cancel");

    private final String label;
}
