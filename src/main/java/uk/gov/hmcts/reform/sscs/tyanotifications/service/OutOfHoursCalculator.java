package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Random;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OutOfHoursCalculator {

    private static final ZoneId UK_TIME_ZONE = ZoneId.of("Europe/London");
    private final DateTimeProvider dateTimeProvider;
    private final int startTime;
    private final int endTime;
    private Random rand = SecureRandom.getInstanceStrong();

    public OutOfHoursCalculator(
        DateTimeProvider dateTimeProvider,
        @Value("${outOfHours.startHour}") int startHour,
        @Value("${outOfHours.endHour}") int endHour) throws NoSuchAlgorithmException {
        this.dateTimeProvider = dateTimeProvider;
        this.startTime = startHour;
        this.endTime = endHour;
    }

    public boolean isItOutOfHours() {
        ZonedDateTime now = dateTimeProvider.now();
        int currentHour = now.withZoneSameInstant(UK_TIME_ZONE).getHour();

        return currentHour < startTime || currentHour >= endTime;
    }

    public ZonedDateTime getStartOfNextInHoursPeriod() {
        ZonedDateTime now = dateTimeProvider.now();
        ZonedDateTime nowInUk = now.withZoneSameInstant(UK_TIME_ZONE);

        ZonedDateTime startDay = (nowInUk.getHour() >= startTime) ? nowInUk.plusDays(1) : nowInUk;

        // SSCS-7472 Hack to stop all notifications getting scheduled as soon as 'out of hours' finishes, causing the reminder database to bombard our service and resulting in notifications going missing
        int randomMinute = rand.nextInt(59);

        return startDay.withHour(startTime).withMinute(randomMinute).withSecond(0).withNano(0).withZoneSameInstant(now.getZone());
    }
}
