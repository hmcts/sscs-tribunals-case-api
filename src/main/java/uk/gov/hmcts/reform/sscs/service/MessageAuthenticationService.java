package uk.gov.hmcts.reform.sscs.service;

import static java.lang.String.format;
import static java.time.ZoneId.of;
import static java.time.ZonedDateTime.now;
import static java.util.Base64.getDecoder;
import static java.util.Base64.getEncoder;
import static javax.crypto.Mac.getInstance;
import static javax.xml.bind.DatatypeConverter.printBase64Binary;
import static org.slf4j.LoggerFactory.getLogger;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.exception.InvalidSubscriptionTokenException;
import uk.gov.hmcts.reform.sscs.exception.MacException;
import uk.gov.hmcts.reform.sscs.exception.TokenException;

@Service
public class MessageAuthenticationService {
    private static final Logger LOG = getLogger(MessageAuthenticationService.class);

    public static final Charset CHARSET = StandardCharsets.UTF_8;
    public static final String ZONE_ID = "Europe/London";
    public static final String MAC_ALGO = "HmacSHA256";
    public static final String ERROR_MESSAGE = "Error while decrypting HMAC token ";
    public static final String SUBSCRIPTION_ID = "subscriptionId";
    public static final String APPEAL_ID = "appealId";
    public static final String DECRYPTED_TOKEN = "decryptedToken";
    public static final String BENEFIT_TYPE = "benefitType";

    private Mac mac;
    private String macString;

    @Autowired
    public MessageAuthenticationService(@Value("${subscriptions.mac.secret}") String macString) throws InvalidKeyException, NoSuchAlgorithmException {
        this.macString = macString;
        this.mac = initializeMac();
    }

    protected Mac initializeMac() throws NoSuchAlgorithmException, InvalidKeyException {
        try {
            SecretKeySpec key = new SecretKeySpec(macString.getBytes(CHARSET), MAC_ALGO);
            mac = getInstance(MAC_ALGO);
            mac.init(key);
            return mac;
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            LOG.error("Error while initializing MAC Key", new MacException(ex));
            throw ex;
        }
    }

    public String generateToken(String appealNumber, String benefitType)  {
        try {
            long timestamp = now(of(ZONE_ID)).toInstant().toEpochMilli() / 1000;
            String originalMessage = format("%s|%s|%d", appealNumber, benefitType, timestamp);
            byte[] digest = mac.doFinal(originalMessage.getBytes(CHARSET));
            String macSubString =  printBase64Binary(digest).substring(0,10);
            String macToken = format("%s|%s", originalMessage, macSubString);
            return getEncoder().withoutPadding().encodeToString(macToken.getBytes(CHARSET));
        } catch (Exception ex) {
            TokenException tokenException = new TokenException(ex);
            LOG.error("Error while generating MAC", tokenException);
            throw tokenException;
        }
    }

    public String validateMacTokenAndReturnBenefitType(String encryptedToken) {
        try {
            validateMacToken(encryptedToken);
            return getBenefitType(encryptedToken);
        } catch (Exception ex) {
            throw logInvalidSubscriptionTokenException(ex, encryptedToken);
        }
    }

    public Map<String, Object> decryptMacToken(String encryptedToken) {
        try {
            validateMacToken(encryptedToken);
            return getSubscriptions(encryptedToken);
        } catch (Exception ex) {
            throw logInvalidSubscriptionTokenException(ex, encryptedToken);
        }
    }

    private String validateMacToken(String encryptedToken) throws InvalidKeyException,
            NoSuchAlgorithmException {
        String decrypted = decryptToken(encryptedToken);
        String[] parts = tokenParts(decrypted);
        String originalMessage = parts[0] + "|" + parts[1] + "|" + parts[2];
        mac = initializeMac();
        mac.update(originalMessage.getBytes(CHARSET));
        byte[] digest = mac.doFinal();
        String macSubString = printBase64Binary(digest).substring(0, 10);
        String macToken = format("%s|%s", originalMessage, macSubString);
        if (!decrypted.equals(macToken)) {
            throw logInvalidSubscriptionTokenException(new Exception(ERROR_MESSAGE + encryptedToken), encryptedToken);
        }

        return macToken;
    }

    private InvalidSubscriptionTokenException logInvalidSubscriptionTokenException(Exception ex, String encryptedToken) {
        InvalidSubscriptionTokenException invalidSubscriptionTokenException = new InvalidSubscriptionTokenException(ex);
        LOG.error(ERROR_MESSAGE + encryptedToken, invalidSubscriptionTokenException);
        return invalidSubscriptionTokenException;
    }

    private String getBenefitType(String encryptedToken) {
        String decrypted = decryptToken(encryptedToken);
        return tokenParts(decrypted)[1];
    }

    private String[] tokenParts(String decryptedToken) {
        return decryptedToken.split("\\|");
    }

    public String decryptToken(String encryptedToken) {
        return new String(getDecoder().decode(encryptedToken), CHARSET);
    }

    private Map<String, Object> getSubscriptions(String encryptedToken) {
        String decrypted = decryptToken(encryptedToken);
        String[] parts = tokenParts(decrypted);
        Map<String,Object> tokenDetails = new HashMap<>();
        tokenDetails.put(SUBSCRIPTION_ID, SUBSCRIPTION_ID);  // Dummy subscription id for TYA frontend
        tokenDetails.put(APPEAL_ID, parts[0]);
        tokenDetails.put(DECRYPTED_TOKEN, decrypted);
        tokenDetails.put(BENEFIT_TYPE, parts[1]);
        return tokenDetails;
    }

}
