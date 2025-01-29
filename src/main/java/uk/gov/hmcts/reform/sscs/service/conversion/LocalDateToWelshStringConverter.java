package uk.gov.hmcts.reform.sscs.service.conversion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public final class LocalDateToWelshStringConverter {

    private LocalDateToWelshStringConverter() {

    }

    static Map<Integer,String> months = new HashMap<>();

    static {
        months.put(1,"Ionawr");
        months.put(2,"Chwefror");
        months.put(3,"Mawrth");
        months.put(4,"Ebrill");
        months.put(5,"Mai");
        months.put(6,"Mehefin");
        months.put(7,"Gorffennaf");
        months.put(8,"Awst");
        months.put(9,"Medi");
        months.put(10,"Hydref");
        months.put(11,"Tachwedd");
        months.put(12,"Rhagfyr");
    }

    public static String convert(LocalDate dateToConvert) {
        return Optional.ofNullable(dateToConvert).map(date -> {
            int day = dateToConvert.getDayOfMonth();
            int year = dateToConvert.getYear();
            int month = dateToConvert.getMonth().getValue();
            return String.join(" ", Integer.toString(day),
                    months.get(month),
                    Integer.toString(year));
        }).orElse(null);
    }

    public static String convert(String localDateFormat) {
        return convert(LocalDate.parse(localDateFormat));
    }

    public static String convertDateTime(String localDateTimeFormat) {
        return convert(LocalDateTime.parse(localDateTimeFormat).toLocalDate());
    }
}
