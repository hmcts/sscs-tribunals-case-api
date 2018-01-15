package uk.gov.hmcts.sscs.service;

import org.apache.commons.text.CharacterPredicates;
import org.apache.commons.text.RandomStringGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class AppealNumberGenerator {

    private static final int LENGTH = 10;
    public static final char MINIMUM_CODE_POINT = '0';
    public static final char MAXIMUM_CODE_POINT = 'z';

    public String generate() {
        SecureRandom random = new SecureRandom();
        RandomStringGenerator generator = new RandomStringGenerator.Builder()
                .withinRange(MINIMUM_CODE_POINT, MAXIMUM_CODE_POINT)
                .filteredBy(CharacterPredicates.DIGITS,CharacterPredicates.LETTERS)
                .usingRandom(random::nextInt)
                .build();
        return generator.generate(LENGTH);
    }

}
