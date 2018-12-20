package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static uk.gov.hmcts.reform.sscs.service.AppealNumberGenerator.generateAppealNumber;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;


public class AppealNumberGeneratorTest {

    @Test
    public void shouldCreateRandomAppealnumber() {
        Pattern pattern = Pattern.compile("^[a-zA-Z0-9]{10}$");

        String appealNumber = generateAppealNumber();
        Matcher matcher = pattern.matcher(appealNumber);
        assertTrue(matcher.matches());
    }

    @Test
    public void shouldGenerateRandomAppealNumberOnEachCall() {
        String appealNumber1 = generateAppealNumber();
        String appealNumber2 = generateAppealNumber();

        assertNotEquals(appealNumber1, appealNumber2);
    }
}