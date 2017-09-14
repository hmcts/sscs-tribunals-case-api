package uk.gov.hmcts.sscs.tribunals.domain.corecase;

import java.time.ZonedDateTime;

public class Hearing {

    private ZonedDateTime time;

    private String judge;

    private String venue;

    private ZonedDateTime date;

    public Hearing() {
    }

    public ZonedDateTime getTime() {
        return time;
    }

    public void setTime(ZonedDateTime time) {
        this.time = time;
    }

    public String getJudge() {
        return judge;
    }

    public void setJudge(String judge) {
        this.judge = judge;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public ZonedDateTime getDate() {
        return date;
    }

    public void setDate(ZonedDateTime date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "Hearing{"
                +     "time='" + time + '\''
                +     ", judge='" + judge + '\''
                +    ", venue='" + venue + '\''
                +   ", date='" + date + '\''
                +   '}';
    }
}
