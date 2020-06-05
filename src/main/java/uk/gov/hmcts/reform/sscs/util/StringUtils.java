package uk.gov.hmcts.reform.sscs.util;

import java.util.List;
import java.util.stream.Collectors;

public final class StringUtils {

    private StringUtils() {
        //
    }

    public static String getGramaticallyJoinedStrings(List<String> strings) {
        StringBuilder result = new StringBuilder();
        if (strings.size() == 1) {
            return strings.get(0);
        } else if (strings.size() > 1) {
            result.append(strings.subList(0, strings.size() - 1)
                .stream().collect(Collectors.joining(", ")));
            result.append(" and ");
            result.append(strings.get(strings.size() - 1));
        }
        return result.toString();
    }
}
