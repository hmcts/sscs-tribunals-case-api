package uk.gov.hmcts.reform.sscs.service;

import static java.time.ZonedDateTime.of;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import net.objectlab.kit.datecalc.common.DateCalculator;
import net.objectlab.kit.datecalc.jdk8.LocalDateKitCalculatorsFactory;

public class BusinessDaysCalculatorService {

    private BusinessDaysCalculatorService() {
        //
    }

    public static ZonedDateTime getBusinessDay(
            ZonedDateTime startDateTime, int numberOfBusinessDays) {
        LocalDate startDate = startDateTime.toLocalDate();
        DateCalculator<LocalDate> dateCalculator =
                LocalDateKitCalculatorsFactory.forwardCalculator("UK");
        dateCalculator.setStartDate(startDate);
        LocalDate decisionDate =
                dateCalculator.moveByBusinessDays(numberOfBusinessDays).getCurrentBusinessDate();
        return of(decisionDate, startDateTime.toLocalTime(), startDateTime.getZone());
    }
}
