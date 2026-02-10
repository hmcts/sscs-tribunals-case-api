package uk.gov.hmcts.reform.sscs.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CachedHolidayClient {

    private final OkHttpClient httpClient;
    private Set<LocalDate> cachedHolidays;
    private static final String HOLIDAY_API_URL = "https://www.gov.uk/bank-holidays.json";

    public CachedHolidayClient(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public synchronized Set<LocalDate> getHolidays() throws IOException {
        if (cachedHolidays == null) {
            cachedHolidays = fetchHolidaysFromApi();
        }
        return cachedHolidays;
    }

    private Set<LocalDate> fetchHolidaysFromApi() throws IOException {
        Request request = new Request.Builder().url(HOLIDAY_API_URL).build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Response unsuccessful: " + response);
            }

            Set<LocalDate> holidays = new HashSet<>();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.body().string());

            JsonNode events = root.path("england-and-wales").path("events");
            for (JsonNode event : events) {
                String dateStr = event.path("date").asText();
                holidays.add(LocalDate.parse(dateStr));
            }

            return holidays;
        }
    }
}
