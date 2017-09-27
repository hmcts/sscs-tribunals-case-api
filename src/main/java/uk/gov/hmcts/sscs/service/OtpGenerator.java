package uk.gov.hmcts.sscs.service;

import static com.google.common.io.BaseEncoding.base32;

import com.google.common.primitives.Longs;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

@Service
public class OtpGenerator implements TotpAuthenticator {
    /**
     * Recommended time step by RFC 6238.
     */
    static final int TIME_STEP = 30000;

    private final Clock clock;

    public OtpGenerator() {
        this(Clock.systemDefaultZone());
    }

    public OtpGenerator(Clock clock) {
        this.clock = clock;
    }

    @Override
    public String issueOneTimePassword(String base32Key)
            throws InvalidKeyException, NoSuchAlgorithmException {
        return issueOneTimePassword(base32Key, clock.millis());
    }

    String issueOneTimePassword(String base32Key, long millis)
            throws NoSuchAlgorithmException,InvalidKeyException {
        byte[] key = base32().decode(base32Key);
        byte[] hash = calculateHash(millis / TIME_STEP, key);
        long truncatedHash = truncateHash(hash);
        return String.format("%06d", truncatedHash);
    }

    @Override
    public boolean isOneTimePasswordValid(String base32Key, String token)
            throws InvalidKeyException, NoSuchAlgorithmException {

        long time = clock.millis();

        if (issueOneTimePassword(base32Key, time).equals(token)) {
            return true;
        }

        //RFC 6238 Suggests accepting at least one previous window, which should suffice.
        return issueOneTimePassword(base32Key, time - TIME_STEP).equals(token);
    }

    private byte[] calculateHash(long value, byte[] key)
            throws InvalidKeyException, NoSuchAlgorithmException {
        SecretKeySpec signKey = new SecretKeySpec(key, "HmacSHA1");
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(signKey);
        return mac.doFinal(toBytes(value));
    }

    private byte[] toBytes(long value) {
        return Longs.toByteArray(value);
    }

    private long truncateHash(byte[] hash) {
        int offset = hash[20 - 1] & 0xF;

        long truncatedHash = 0;
        for (int i = 0; i < 4; ++i) {
            truncatedHash <<= 8;
            truncatedHash |= (hash[offset + i] & 0xFF);
        }

        truncatedHash &= 0x7FFFFFFF;
        truncatedHash %= 1000000;
        return truncatedHash;
    }
}
