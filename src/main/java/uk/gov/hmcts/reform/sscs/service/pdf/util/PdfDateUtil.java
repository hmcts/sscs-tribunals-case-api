package uk.gov.hmcts.reform.sscs.service.pdf.util;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class PdfDateUtil {

    private static final String PDF_DATE_FORMAT = "dd MMMM yyyy";

    private PdfDateUtil() {

    }

    public static String reformatDateTimeToDate(String dateString) {
        if (isNotBlank(dateString)) {
            return reformatDate(dateString.substring(0, 10));
        }
        return dateString;
    }

    public static String reformatDate(String dateString) {
        if (isNotBlank(dateString)) {
            return reformatDate(LocalDate.parse(dateString));
        }
        return dateString;
    }

    public static String reformatDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern(PDF_DATE_FORMAT));
    }
}
