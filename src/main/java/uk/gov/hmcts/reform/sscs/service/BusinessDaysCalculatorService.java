package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.model.BankHolidayDivision.SCOTLAND;
import static uk.gov.hmcts.reform.sscs.model.BankHolidayDivision.UK;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Set;
import net.objectlab.kit.datecalc.common.DefaultHolidayCalendar;
import net.objectlab.kit.datecalc.jdk8.LocalDateKitCalculatorsFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.client.CachedHolidayClient;
import uk.gov.hmcts.reform.sscs.model.BankHolidayDivision;

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

    public LocalDate getNextBusinessDay(final LocalDate date, final BankHolidayDivision bankHolidayDivision) throws IOException {
        initialiseHolidays();
        return LocalDateKitCalculatorsFactory.forwardCalculator(bankHolidayDivision.name())
            .setStartDate(date.plusDays(1))
            .getCurrentBusinessDate();
    }

    private void initialiseHolidays() throws IOException {
        registerDivisionHolidays(UK);
        registerDivisionHolidays(SCOTLAND);
    }

    private void registerDivisionHolidays(final BankHolidayDivision bankHolidayDivision) throws IOException {
        final Set<LocalDate> holidays = cachedHolidayClient.getHolidays(bankHolidayDivision);
        final DefaultHolidayCalendar<LocalDate> calendar = new DefaultHolidayCalendar<>();
        calendar.setHolidays(holidays);
        LocalDateKitCalculatorsFactory.getDefaultInstance().registerHolidays(bankHolidayDivision.name(), calendar);
    }

    private ZonedDateTime calculateBusinessDay(LocalDate startDate, int numberOfBusinessDays, ZonedDateTime startDateTime) {
        return ZonedDateTime.of(
            calculateBusinessDay(startDate, numberOfBusinessDays),
            startDateTime.toLocalTime(),
            startDateTime.getZone()
        );
    }

    private LocalDate calculateBusinessDay(LocalDate startDate, int numberOfBusinessDays) {
        return LocalDateKitCalculatorsFactory.forwardCalculator(UK.name())
            .setStartDate(startDate)
            .moveByBusinessDays(numberOfBusinessDays)
            .getCurrentBusinessDate();
    }
}
