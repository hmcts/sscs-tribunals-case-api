package uk.gov.hmcts.reform.sscs.functional.mya;

import java.security.SecureRandom;

public class CcdIdGenerator {
    private SecureRandom random = new SecureRandom();

    public String generateUid() {
        String currentTime10OfSeconds = String.valueOf(System.currentTimeMillis()).substring(0, 11);
        StringBuilder builder = new StringBuilder(currentTime10OfSeconds);
        for (int i = 0; i < 4; i++) {
            int digit = random.nextInt(10);
            builder.append(digit);
        }
        // Do the Luhn algorithm to generate the check digit.
        int checkDigit = checkSum(builder.toString(), true);
        builder.append(checkDigit);

        return builder.toString();
    }

    private int checkSum(String numberString, boolean noCheckDigit) {
        int sum = 0;
        int checkDigit = 0;

        if (!noCheckDigit) {
            numberString = numberString.substring(0, numberString.length() - 1);
        }

        boolean isDouble = true;
        for (int i = numberString.length() - 1; i >= 0; i--) {
            int k = Integer.parseInt(String.valueOf(numberString.charAt(i)));
            sum += sumToSingleDigit((k * (isDouble ? 2 : 1)));
            isDouble = !isDouble;
        }

        if ((sum % 10) > 0) {
            checkDigit = (10 - (sum % 10));
        }

        return checkDigit;
    }

    private int sumToSingleDigit(int k) {
        if (k < 10) {
            return k;
        }
        return sumToSingleDigit(k / 10) + (k % 10);
    }

    public static void main(String[] args) {
        System.out.println(new CcdIdGenerator().generateUid());
    }
}
