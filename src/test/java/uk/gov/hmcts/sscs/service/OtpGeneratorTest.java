package uk.gov.hmcts.sscs.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class OtpGeneratorTest {
    private OtpGenerator otpGenerator = new OtpGenerator();

    @Test
    public void shouldGenerate6DigitOtp() throws Exception {
        String key = "AAAAAAAAAAAAAAAA";
        String otp = otpGenerator.issueOneTimePassword(key);
        assertEquals(6,otp.length());
    }
}
