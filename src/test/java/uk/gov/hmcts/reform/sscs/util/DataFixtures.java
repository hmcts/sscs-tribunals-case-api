package uk.gov.hmcts.reform.sscs.util;

import uk.gov.hmcts.reform.sscs.domain.wrapper.*;

public class DataFixtures {
    private DataFixtures() {

    }

    public static OnlineHearing someOnlineHearing() {
        return someOnlineHearing(123456789L);
    }

    public static OnlineHearing someOnlineHearing(long caseId) {
        AddressDetails addressDetails = new AddressDetails("line1", "line2", "town", "country", "postcode");
        AppellantDetails appellantDetails = new AppellantDetails(addressDetails, "email", "phone", "mobile");
        return new OnlineHearing("someOnlineHearingId", "someAppellantName", "someCaseReference", caseId, null, new FinalDecision("final decision"), true, appellantDetails, someAppealDetails());
    }

    public static AppealDetails someAppealDetails() {
        return new AppealDetails("12-12-2019", "11-11-2019", "PIP", "hearing");
    }


    public static Statement someStatement() {
        return new Statement("Some Statement body", "someTyaCode");
    }

}
