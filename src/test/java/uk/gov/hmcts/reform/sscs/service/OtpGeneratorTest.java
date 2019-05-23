package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class OtpGeneratorTest {
    private final OtpGenerator otpGenerator = new OtpGenerator();

    @Test
    public void shouldGenerate6DigitOtp() throws Exception {
        String key = "KWCNVXMFJ6PIZIDX";
        String otp = otpGenerator.issueOneTimePassword(key);
        assertEquals(6,otp.length());
    }
}
