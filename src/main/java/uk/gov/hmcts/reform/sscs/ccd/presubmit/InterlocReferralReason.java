package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Arrays;

public enum InterlocReferralReason {

    TIME_EXTENSION("timeExtension","Time extension"),
    PHME_REQUEST("phmeRequest", "PHME request"),
    OVER_200_PAGES("over200Pages", "Over 200 pages"),
    OVER_13_MONTHS("over13months", "Over 13 months"),
    OTHER("other", "Other"),
    NO_RESPONSE_TO_DIRECTION("noResponseToDirection", "No response to a direction"),
    NO_MRN("noMrn", "No MRN"),
    NONE("none", "N/A"),
    LISTING_DIRECTIONS("listingDirections", "Listing directions"),
    OVER_13_MONTHS_AND_GROUNDS_MISSING("over13MonthsAndGroundsMissing", "Grounds for appeal missing and over 13 months"),
    COMPLEX_CASE("complexCase", "Complex Case"),
    ADVICE_ON_HOW_TO_PROCEED("adviceOnHowToProceed", "Advice on how to proceed");

    private final String id;
    private final String label;

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    InterlocReferralReason(String id, String label) {
        this.id = id;
        this.label = label;
    }

    @JsonCreator
    public static String findLabelById(String id) {
        return Arrays.stream(InterlocReferralReason.values()).filter(pt -> pt.id.equals(id)).findFirst().get().getLabel();
    }
}