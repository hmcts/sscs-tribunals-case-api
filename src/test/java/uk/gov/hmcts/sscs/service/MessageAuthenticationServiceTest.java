package uk.gov.hmcts.sscs.service;

import static org.junit.Assert.assertEquals;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MessageAuthenticationServiceTest {

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
        String encryptedToken = service.generateToken("dfdsf435345");
        String decryptedToken = service.decryptMacToken(encryptedToken);

        String[] split = decryptedToken.split("\\|");
        assertEquals("dfdsf435345", split[0]);
    }

    @Test
    public void shouldHandleInvalidSubscriptionTokenException() {
        String encryptedToken = service.generateToken("dfdsf435345");

        exception.expect(uk.gov.hmcts.sscs.exception.InvalidSubscriptionTokenException.class);
        exception.expectMessage("Error while decrypting HMAC token " + encryptedToken + "sdft5e");

        service.decryptMacToken(encryptedToken + "sdft5e");
    }
}
