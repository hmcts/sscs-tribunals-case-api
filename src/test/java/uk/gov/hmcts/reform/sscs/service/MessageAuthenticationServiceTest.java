package uk.gov.hmcts.reform.sscs.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.exception.InvalidSubscriptionTokenException;


@RunWith(MockitoJUnitRunner.class)
public class MessageAuthenticationServiceTest {

    public static final String APPEAL_NUMBER = "dfdsf435345";
    public static final String SUBSCRIPTION_ID = "subscriptionId";
    public static final String BENEFIT_TYPE_VALUE = "002";

    private static final String macString = "our-big-secret";

    private MessageAuthenticationService service;

    @Before
    public void setUp() throws NoSuchAlgorithmException, InvalidKeyException {
        service = new MessageAuthenticationService(macString);
    }

    @Test
    public void shouldGenerateMacToken() {
        String encryptedToken = service.generateToken(APPEAL_NUMBER, BENEFIT_TYPE_VALUE);
        String decryptedToken = service.decryptToken(encryptedToken);

        String[] split = decryptedToken.split("\\|");
        assertEquals(APPEAL_NUMBER, split[0]);
        assertEquals(BENEFIT_TYPE_VALUE, split[1]);
    }

    @Test
    public void validateMacTokenAndReturnBenefitType() {
        String encryptedToken = service.generateToken(APPEAL_NUMBER, BENEFIT_TYPE_VALUE);

        assertEquals(BENEFIT_TYPE_VALUE, service.validateMacTokenAndReturnBenefitType(encryptedToken));
    }

    @Test
    public void shouldHandleInvalidSubscriptionTokenException() {
        String encryptedToken = service.generateToken(APPEAL_NUMBER, BENEFIT_TYPE_VALUE);

        Exception exception = assertThrows(InvalidSubscriptionTokenException.class, () ->
            service.validateMacTokenAndReturnBenefitType(encryptedToken + "sdft5e"));

        assertThat(exception.getMessage())
            .contains("Error while decrypting HMAC token " + encryptedToken + "sdft5e");
    }

    @Test
    public void validateMacTokenAndReturnAppealDetailsIfTokenIsValid() {
        String encryptedToken = service.generateToken(APPEAL_NUMBER, BENEFIT_TYPE_VALUE);

        Map<String, Object> tokenDetailsMap = service.decryptMacToken(encryptedToken);

        assertThat(tokenDetailsMap)
            .containsValue(APPEAL_NUMBER)
            .containsValue(SUBSCRIPTION_ID)
            .containsValue(BENEFIT_TYPE_VALUE);
    }

    @Test
    public void shouldHandleInvalidMacTokenException() {
        String encryptedToken = service.generateToken(APPEAL_NUMBER, BENEFIT_TYPE_VALUE);

        Exception exception = assertThrows(InvalidSubscriptionTokenException.class, () ->
                service.decryptMacToken(encryptedToken + "sdft5e")
        );

        assertThat(exception.getMessage())
            .contains("Error while decrypting HMAC token " + encryptedToken + "sdft5e");
    }
}
