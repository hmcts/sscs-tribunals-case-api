package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MessageAuthenticationServiceImplTest {

    private MessageAuthenticationServiceImpl service;

    @Before
    public void setUp() throws Exception {
        service = new MessageAuthenticationServiceImpl("our-big-secret");
    }

    @Test
    public void shouldGenerateMacUsingSecureAlgorithmAndReturnBenefitType() {

        String startEncryptedToken = service.generateToken("3", "002").substring(0, 9);

        assertEquals("M3wwMDJ8M", startEncryptedToken);
    }
}
