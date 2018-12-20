package uk.gov.hmcts.reform.sscs.service;

import java.security.SecureRandom;
import org.apache.commons.text.CharacterPredicates;
import org.apache.commons.text.RandomStringGenerator;

public class AppealNumberGenerator {
    private static final int LENGTH = 10;
    private static final char MINIMUM_CODE_POINT = '0';
    private static final char MAXIMUM_CODE_POINT = 'z';

    private AppealNumberGenerator() {
        //Empty
    }

    public static String generateAppealNumber() {
        SecureRandom random = new SecureRandom();
        RandomStringGenerator generator = new RandomStringGenerator.Builder()
                .withinRange(MINIMUM_CODE_POINT, MAXIMUM_CODE_POINT)
                .filteredBy(CharacterPredicates.DIGITS, CharacterPredicates.LETTERS).usingRandom(random::nextInt)
                .build();
        return generator.generate(LENGTH);
    }

}
