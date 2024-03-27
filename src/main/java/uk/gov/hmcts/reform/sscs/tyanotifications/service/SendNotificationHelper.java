package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import static uk.gov.hmcts.reform.sscs.tyanotifications.config.AppConstants.REP_SALUTATION;

import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;

public class SendNotificationHelper {

    private SendNotificationHelper() {
        // Hiding utility class constructor
    }

    public static String getRepSalutation(Representative rep, boolean ignoreOrg) {
        Name name = rep.getName();
        if (null == name
            || null == name.getFirstName()
            || "".equalsIgnoreCase(name.getFirstName())
            || "undefined".equalsIgnoreCase(name.getFirstName())
            || null == name.getLastName()
            || "".equalsIgnoreCase(name.getLastName())
            || "undefined".equalsIgnoreCase(name.getLastName())
        ) {
            return !ignoreOrg && null != rep.getOrganisation() && !"".equals(rep.getOrganisation()) ? rep.getOrganisation() : REP_SALUTATION;
        } else {
            return name.getFullNameNoTitle();
        }
    }
}
