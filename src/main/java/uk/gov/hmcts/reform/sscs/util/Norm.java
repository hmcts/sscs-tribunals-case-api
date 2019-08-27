package uk.gov.hmcts.reform.sscs.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Norm {

    private static final Pattern PIP_NUMBER = Pattern.compile("DWP PIP \\(\\s*(\\d+)\\s*\\)");

    private Norm() {
        // Void
    }

    public static String dwpIssuingOffice(String value) {
        if (value != null) {
            Matcher m = PIP_NUMBER.matcher(value);
            if (m.find()) {
                return m.replaceFirst("DWP PIP ($1)");
            }
        }
        return value;
    }

}
