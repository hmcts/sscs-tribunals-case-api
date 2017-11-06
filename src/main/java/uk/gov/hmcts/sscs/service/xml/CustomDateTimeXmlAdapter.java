package uk.gov.hmcts.sscs.service.xml;

import com.migesok.jaxb.adapter.javatime.TemporalAccessorXmlAdapter;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * {@code XmlAdapter} mapping JSR-310 {@code ZonedDateTime} to ISO-8601 string
 * <p>
 * String format details: dd/MM/yyyy
 *
 * @see javax.xml.bind.annotation.adapters.XmlAdapter
 * @see java.time.ZonedDateTime
 */
public class CustomDateTimeXmlAdapter extends TemporalAccessorXmlAdapter<ZonedDateTime> {
    public CustomDateTimeXmlAdapter() {
        super(DateTimeFormatter.ofPattern("dd/MM/yyyy"), ZonedDateTime::from);
    }
}
