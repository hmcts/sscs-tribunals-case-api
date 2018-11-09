package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;


public class AppealNumberGeneratorTest {

    @Test
    public void shouldCreateRandomAppealnumber() {

        Pattern pattern = Pattern.compile("^[a-zA-Z0-9]{10}$");

        String appealNumber = new AppealNumberGenerator().generateAppealNumber();
        Matcher matcher = pattern.matcher(appealNumber);
        assertTrue(matcher.matches());

    }

    @Test
    public void shouldGenerateRandomAppealNumberOnEachCall() {

        String appealNumber1 = new AppealNumberGenerator().generateAppealNumber();
        String appealNumber2 = new AppealNumberGenerator().generateAppealNumber();

        assertNotEquals(appealNumber1, appealNumber2);
    }
}