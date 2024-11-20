package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import static jakarta.xml.bind.DatatypeConverter.printBase64Binary;
import static java.time.ZoneId.of;
import static java.time.ZonedDateTime.now;
import static java.util.Base64.getEncoder;
import static javax.crypto.Mac.getInstance;
import static org.slf4j.LoggerFactory.getLogger;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.AppConstants;
import uk.gov.hmcts.reform.sscs.tyanotifications.exception.MacException;
import uk.gov.hmcts.reform.sscs.tyanotifications.exception.TokenException;

@Service
public class MessageAuthenticationServiceImpl {
    private static final Logger LOG = getLogger(MessageAuthenticationServiceImpl.class);
    public static final Charset CHARSET = StandardCharsets.UTF_8;

    private Mac mac;
    private String macString;

    public MessageAuthenticationServiceImpl(@Value("${subscription.hmac.secret.text}") String macString) throws InvalidKeyException, NoSuchAlgorithmException {
        this.macString = macString;
        this.mac = initializeMac();
    }

    protected Mac initializeMac() throws NoSuchAlgorithmException, InvalidKeyException {
        try {
            SecretKeySpec key = new SecretKeySpec(macString.getBytes(CHARSET), AppConstants.MAC_ALGO);
            mac = getInstance(AppConstants.MAC_ALGO);
            mac.init(key);
            return mac;
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            LOG.error("Error while initializing MAC Key", new MacException(ex));
            throw ex;
        }
    }

    public String generateToken(String appealNumber, String benefitType) {
        try {
            long timestamp = now(of(AppConstants.ZONE_ID)).toInstant().toEpochMilli() / 1000;
            String originalMessage = "%s|%s|%d".formatted(appealNumber, benefitType, timestamp);
            byte[] digest = mac.doFinal(originalMessage.getBytes(CHARSET));
            String macSubString = printBase64Binary(digest).substring(0, 10);
            String macToken = "%s|%s".formatted(originalMessage, macSubString);
            return getEncoder().withoutPadding().encodeToString(macToken.getBytes(CHARSET));
        } catch (Exception ex) {
            TokenException tokenException = new TokenException(ex);
            LOG.error("Error while generating MAC", tokenException);
            throw tokenException;
        }
    }
}
