package uk.gov.hmcts.reform.sscs.service;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class MessageAuthenticationServiceTest {

    public static final String SUBSCRIPTION_ID = "subscriptionId";
    public static final String APPEAL_ID = "appealId";
    public static final String BENEFIT_TYPE = "benefitType";
    public static final String APPEAL_NUMER = "dfdsf435345";
    public static final String BENEFIT_TYPE_VALUE = "002";

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private String macString = "our-big-secret";

    private MessageAuthenticationService service;

    @Before
    public void setUp() throws NoSuchAlgorithmException, InvalidKeyException {
        service = new MessageAuthenticationService(macString);
    }

    @Test
    public void shouldGenerateMacToken() {
        String encryptedToken = service.generateToken(APPEAL_NUMER, BENEFIT_TYPE_VALUE);
        String decryptedToken = service.decryptToken(encryptedToken);

        String[] split = decryptedToken.split("\\|");
        assertEquals(APPEAL_NUMER, split[0]);
        assertEquals(BENEFIT_TYPE_VALUE, split[1]);
    }

    @Test
    public void validateMacTokenAndReturnBenefitType() {
        String encryptedToken = service.generateToken(APPEAL_NUMER, BENEFIT_TYPE_VALUE);

        assertEquals(BENEFIT_TYPE_VALUE, service.validateMacTokenAndReturnBenefitType(encryptedToken));
    }

    @Test
    public void shouldHandleInvalidSubscriptionTokenException() {
        String encryptedToken = service.generateToken(APPEAL_NUMER, BENEFIT_TYPE_VALUE);

        exception.expect(uk.gov.hmcts.reform.sscs.exception.InvalidSubscriptionTokenException.class);
        exception.expectMessage("Error while decrypting HMAC token " + encryptedToken + "sdft5e");

        service.validateMacTokenAndReturnBenefitType(encryptedToken + "sdft5e");
    }

    @Test
    public void validateMacTokenAndReturnAppealDetailsIfTokenIsvalid() {

        String encryptedToken = service.generateToken(APPEAL_NUMER, BENEFIT_TYPE_VALUE);

        Map<String, Object> tokenDetailsMap = service.decryptMacToken(encryptedToken);

        assertThat(tokenDetailsMap.get(APPEAL_ID), equalTo(APPEAL_NUMER));
        assertThat(tokenDetailsMap.get(SUBSCRIPTION_ID), equalTo(SUBSCRIPTION_ID));
        assertThat(tokenDetailsMap.get(BENEFIT_TYPE), equalTo(BENEFIT_TYPE_VALUE));

    }

    @Test
    public void shouldHandleInvalidMacTokenException() {
        String encryptedToken = service.generateToken(APPEAL_NUMER, BENEFIT_TYPE_VALUE);

        exception.expect(uk.gov.hmcts.reform.sscs.exception.InvalidSubscriptionTokenException.class);
        exception.expectMessage("Error while decrypting HMAC token " + encryptedToken + "sdft5e");

        service.decryptMacToken(encryptedToken + "sdft5e");
    }
}
