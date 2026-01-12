package uk.gov.hmcts.reform.sscs.functional.featureflag;

import com.launchdarkly.sdk.server.LDClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.FeatureToggleService;
import uk.gov.hmcts.reform.sscs.featureflag.FeatureFlag;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/application_functional.properties")
@SpringBootTest
public class FeatureToggleServiceTest {

    @Autowired
    private FeatureToggleService featureToggleService;

    @Autowired
    private LDClient ldClient;

    @Test
    public void shouldGetTrueBooleanFeatureFlag() {

        boolean result = featureToggleService.isEnabled(
                FeatureFlag.SSCS_CHILD_MAINTENANCE_FT,
                "some-user-id",
                "example@email.com");

        assertTrue(result);

    }

    @Test
    public void shouldGetFalseForAnotherUserId() {

        boolean result = featureToggleService.isEnabled(
                FeatureFlag.SSCS_CHILD_MAINTENANCE_FT,
                "another-user-id",
                "example@email.com");

        assertFalse(result);
    }

}
