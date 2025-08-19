package helper;

import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.lang3.RandomStringUtils;

public final class NinoGenerator {
    private static final String FIRST_CHARS = "ABCEGHJKLMNPRSTWXYZ";
    private static final String SUFFIX_CHARS = "ABCD";

    private NinoGenerator() {
        // Utility classes should not have a public or default constructor
    }

    public static String getRandomNino() {
        char first = randomChar(FIRST_CHARS);
        char second = randomChar(FIRST_CHARS);
        String digits = RandomStringUtils.secure().next(6, false, true);
        char suffix = randomChar(SUFFIX_CHARS);

        return "" + first + second + digits + suffix;
    }

    private static char randomChar(String pool) {
        return pool.charAt(ThreadLocalRandom.current().nextInt(pool.length()));
    }

}
