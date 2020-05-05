package uk.gov.hmcts.reform.sscs.util;

public class DecodeJsonUtil {
    private DecodeJsonUtil() {

    }

    public static String decodeStringWithWhitespace(String value) {
        if (value != null) {
            return value.replaceAll("\\\\n", "\n");
        }
        return null;
    }
}
