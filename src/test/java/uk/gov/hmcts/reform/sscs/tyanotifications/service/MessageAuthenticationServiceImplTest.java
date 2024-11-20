package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MessageAuthenticationServiceImplTest {

    private MessageAuthenticationServiceImpl service;

    @BeforeEach
    public void setUp() throws Exception {
        service = new MessageAuthenticationServiceImpl("our-big-secret");
    }

    @Test
    public void shouldGenerateMacUsingSecureAlgorithmAndReturnBenefitType() {

        String startEncryptedToken = service.generateToken("3", "002").substring(0, 9);

        assertEquals("M3wwMDJ8M", startEncryptedToken);
    }
}
