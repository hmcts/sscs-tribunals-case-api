package uk.gov.hmcts.reform.sscs.service.pdf.util;

import static java.time.LocalDate.parse;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.hmcts.reform.sscs.service.pdf.util.PdfDateUtil.reformatDate;
import static uk.gov.hmcts.reform.sscs.service.pdf.util.PdfDateUtil.reformatDateTimeToDate;

import org.junit.Test;

public class PdfDateUtilTest {
    @Test
    public void canHandleBlankDate() {
        String reformatDate = reformatDate("");

        assertThat(reformatDate, is(""));
    }

    @Test
    public void formatsStringDate() {
        String reformatDate = reformatDate("2001-06-24");

        assertThat(reformatDate, is("24 June 2001"));
    }

    @Test
    public void formatsLocalDate() {
        String reformatDate = reformatDate(parse("2001-06-24"));

        assertThat(reformatDate, is("24 June 2001"));
    }

    @Test
    public void formatsDateTime() {
        String reformatDate = reformatDateTimeToDate("2007-12-03T10:15:30Z");

        assertThat(reformatDate, is("03 December 2007"));
    }

    @Test
    public void formatsDateTimeWithOffset() {
        String reformatDate = reformatDateTimeToDate("2019-05-10T15:24:21+01:00");

        assertThat(reformatDate, is("10 May 2019"));
    }
}
