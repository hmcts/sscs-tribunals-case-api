package uk.gov.hmcts.reform.sscs.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.model.BankHolidayDivision;

@Component
public class CachedHolidayClient {

    private static final String HOLIDAY_API_URL = "https://www.gov.uk/bank-holidays.json";
    private static final Map<BankHolidayDivision, String> BANK_HOLIDAY_DIVISION = Map.of(
        BankHolidayDivision.UK, "england-and-wales",
        BankHolidayDivision.SCOTLAND, "scotland"
    );

    private final OkHttpClient httpClient;
    private Map<BankHolidayDivision, Set<LocalDate>> cachedHolidays;

    public CachedHolidayClient(final OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public synchronized Set<LocalDate> getHolidays(final BankHolidayDivision bankHolidayDivision) throws IOException {
        if (cachedHolidays == null) {
            cachedHolidays = parseHolidays(fetchHolidaysFromApi());
        }
        return cachedHolidays.get(bankHolidayDivision);
    }

    private JsonNode fetchHolidaysFromApi() throws IOException {
        final Request request = new Request.Builder().url(HOLIDAY_API_URL).build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Response unsuccessful: " + response);
            }
            return new ObjectMapper().readTree(response.body().string());
        }
    }

    private Map<BankHolidayDivision, Set<LocalDate>> parseHolidays(final JsonNode root) {
        final Map<BankHolidayDivision, Set<LocalDate>> holidaysByRegion = new EnumMap<>(BankHolidayDivision.class);
        BANK_HOLIDAY_DIVISION.forEach((region, division) -> holidaysByRegion.put(region, parseDivision(root, division)));
        return holidaysByRegion;
    }

    private Set<LocalDate> parseDivision(final JsonNode root, final String division) {
        final Set<LocalDate> holidays = new HashSet<>();
        for (final JsonNode event : root.path(division).path("events")) {
            holidays.add(LocalDate.parse(event.path("date").asText()));
        }
        return holidays;
    }
}
