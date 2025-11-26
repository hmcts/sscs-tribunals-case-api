package uk.gov.hmcts.reform.sscs.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateUtilities {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private DateUtilities() {
    }

    public static String today() {
        return LocalDate.now().format(FORMATTER);
    }
}
