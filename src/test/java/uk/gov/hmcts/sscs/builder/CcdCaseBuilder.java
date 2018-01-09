package uk.gov.hmcts.sscs.builder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import uk.gov.hmcts.sscs.domain.corecase.*;

public class CcdCaseBuilder {

    public static Appeal appeal() {
        return new Appeal(Benefit.UNIVERSAL_CREDIT, "Birmingham2 SSO",
                convertJsonStringIntoDate("2", "9", "2017"),  LocalDate.now(), "Yes", "12345");
    }

    public static Appellant appellant() {
        return new Appellant(new Name("Dr", "Kenny", "Rodgers"),
                address(false), "01234 123456", "m@test.com", "JT0123456H", "AdminGroup");
    }

    public static Appointee appointee() {
        return new Appointee(new Name("Mrs", "Benny", "Dodgers"),
                address(false), "01234 765432", "appointee@test.com");
    }

    public static Representative representative() {
        return new Representative(new Name("Mr", "Benny", "Dodgers"),
                address(false), "01234 765432", "appointee@test.com", "Monsters Inc.");
    }

    public static Address address(boolean withGoogleMaps) {
        String mapsUrl = withGoogleMaps ? "https://googleURL.com" : null;
        return new Address("My Road", "Village", "Bedrock", "Bedford", "BF12 1HF", mapsUrl);
    }

    public static Hearing hearing() {
        return new Hearing(TribunalType.PAPER, true, true, false, false,
        "Additional info", new ExcludeDates[]{new ExcludeDates("November 5th", "November 12th")});
    }

    public static CcdCase ccdCase() {
        return new CcdCase(appeal(), appellant(), appointee(), representative(),
                new ArrayList<Hearing>() {
                {
                    add(hearing());
                }
            });
    }

    public static LocalDate convertJsonStringIntoDate(String day, String month, String year) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d M yyyy");
        StringBuilder date = new StringBuilder(day).append(" " + month).append(" " + year);
        return LocalDate.parse(date.toString(), formatter);
    }
}
