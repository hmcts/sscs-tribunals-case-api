package uk.gov.hmcts.reform.sscs.service;

import static java.time.ZonedDateTime.of;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.objectlab.kit.datecalc.common.DateCalculator;
import net.objectlab.kit.datecalc.common.DefaultHolidayCalendar;
import net.objectlab.kit.datecalc.jdk8.LocalDateKitCalculatorsFactory;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BusinessDaysCalculatorService {
    private final OkHttpClient httpClient;

    @Autowired
    public BusinessDaysCalculatorService(OkHttpClient httpClient) throws IOException {
        this.httpClient = httpClient;
        Set<LocalDate> holidays = fetchUkBankHolidaysFromGovUkApi();
        initialiseHolidays(holidays);
    }

    public ZonedDateTime getBusinessDay(
            ZonedDateTime startDateTime, int numberOfBusinessDays) {
        LocalDate startDate = startDateTime.toLocalDate();
        DateCalculator<LocalDate> dateCalculator =
                LocalDateKitCalculatorsFactory.forwardCalculator("UK");
        dateCalculator.setStartDate(startDate);
        LocalDate decisionDate =
                dateCalculator.moveByBusinessDays(numberOfBusinessDays).getCurrentBusinessDate();
        return of(decisionDate, startDateTime.toLocalTime(), startDateTime.getZone());
    }

    public LocalDate getBusinessDay(
        LocalDate date, int numberOfBusinessDays) {
        DateCalculator<LocalDate> dateCalculator =
            LocalDateKitCalculatorsFactory.forwardCalculator("UK");
        dateCalculator.setStartDate(date);
        return dateCalculator.moveByBusinessDays(numberOfBusinessDays).getCurrentBusinessDate();
    }

    private Set<LocalDate> fetchUkBankHolidaysFromGovUkApi() throws IOException {
        String url = "https://www.gov.uk/bank-holidays.json";
        Request request = new Request.Builder()
            .url(url)
            .build();

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

    private void initialiseHolidays(Set<LocalDate> holidays) {
        DefaultHolidayCalendar<LocalDate> ukCalendar = new DefaultHolidayCalendar<>();
        ukCalendar.setHolidays(holidays);
        LocalDateKitCalculatorsFactory.getDefaultInstance()
            .registerHolidays("UK", ukCalendar);
    }
}
