package uk.gov.hmcts.reform.sscs.util;

import java.util.List;
import uk.gov.hmcts.reform.sscs.domain.wrapper.*;

public class DataFixtures {
    private DataFixtures() {

    }

    public static OnlineHearing someOnlineHearing() {
        return someOnlineHearing(123456789L);
    }

    public static OnlineHearing someOnlineHearing(long caseId) {
        AddressDetails addressDetails = new AddressDetails("line1", "line2", "town", "country", "postcode");
        UserDetails userDetails = new UserDetails(UserType.APPELLANT.getType(), "First Last", addressDetails, "email", "phone", "mobile", List.of());
        return new OnlineHearing("someOnlineHearingId", "someAppellantName", "someCaseReference", caseId, null, new FinalDecision("final decision"), true, userDetails, someAppealDetails());
    }

    public static AppealDetails someAppealDetails() {
        return new AppealDetails("12-12-2019", "11-11-2019", "PIP", "hearing");
    }


    public static Statement someStatement() {
        return new Statement("Some Statement body", "someTyaCode");
    }

}
