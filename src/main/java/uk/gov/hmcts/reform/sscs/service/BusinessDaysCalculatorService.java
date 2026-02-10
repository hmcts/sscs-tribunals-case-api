package uk.gov.hmcts.reform.sscs.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.objectlab.kit.datecalc.common.DefaultHolidayCalendar;
import net.objectlab.kit.datecalc.jdk8.LocalDateKitCalculatorsFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BusinessDaysCalculatorService {

    private final CachedHolidayClient cachedHolidayClient;

    @Autowired
    public BusinessDaysCalculatorService(CachedHolidayClient cachedHolidayClient) {
        this.cachedHolidayClient = cachedHolidayClient;
    }

    public ZonedDateTime getBusinessDay(ZonedDateTime startDateTime, int numberOfBusinessDays) throws IOException {
        initialiseHolidays();
        LocalDate startDate = startDateTime.toLocalDate();
        return calculateBusinessDay(startDate, numberOfBusinessDays, startDateTime);
    }

    public LocalDate getBusinessDay(LocalDate date, int numberOfBusinessDays) throws IOException {
        initialiseHolidays();
        return calculateBusinessDay(date, numberOfBusinessDays);
    }

    private void initialiseHolidays() throws IOException {
        Set<LocalDate> holidays = cachedHolidayClient.getHolidays();
        DefaultHolidayCalendar<LocalDate> ukCalendar = new DefaultHolidayCalendar<>();
        ukCalendar.setHolidays(holidays);
        LocalDateKitCalculatorsFactory.getDefaultInstance().registerHolidays("UK", ukCalendar);
    }

    private ZonedDateTime calculateBusinessDay(LocalDate startDate, int numberOfBusinessDays, ZonedDateTime startDateTime) {
        return ZonedDateTime.of(
            calculateBusinessDay(startDate, numberOfBusinessDays),
            startDateTime.toLocalTime(),
            startDateTime.getZone()
        );
    }

    private LocalDate calculateBusinessDay(LocalDate startDate, int numberOfBusinessDays) {
        return LocalDateKitCalculatorsFactory.forwardCalculator("UK")
            .setStartDate(startDate)
            .moveByBusinessDays(numberOfBusinessDays)
            .getCurrentBusinessDate();
    }
}
