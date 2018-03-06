package uk.gov.hmcts.sscs.config.properties;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class IdamPropertiesTest {

    @Autowired
    private IdamProperties idamProperties;

    @Test
    public void givenAnInstanceOfIdamProperties_shouldContainOauth2Values() {
        IdamProperties.Oauth2 oauth2 = idamProperties.getOauth2();
        assertNotNull(oauth2.getClient());
        assertNotNull(oauth2.getUser());
        assertNotNull(oauth2.getRedirectUrl());
    }
}
