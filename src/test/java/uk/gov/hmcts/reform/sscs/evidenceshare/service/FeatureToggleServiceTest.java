package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

import com.launchdarkly.sdk.LDContext;
import com.launchdarkly.sdk.server.LDClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.featureflag.FeatureFlag;

@ExtendWith(MockitoExtension.class)
class FeatureToggleServiceTest {

    private FeatureToggleService featureToggleService;

    @Mock
    private LDClient ldClient;

    private final String ldUserKey = "some-user-key";

    @BeforeEach
    void setUp() {
        featureToggleService = new FeatureToggleService(ldClient, ldUserKey);
    }

    @Test
    void shouldGetTrueBooleanFeatureFlag() {

        when(ldClient.boolVariation(anyString(), any(LDContext.class), eq(Boolean.FALSE)))
            .thenReturn(true);

        boolean result = featureToggleService.getBooleanValue(
            FeatureFlag.SSCS_CHILD_MAINTENANCE_FT,
            "some-user-id",
            "example@email.com");

        assertTrue(result);

    }

    @Test
    void shouldGetFalseBooleanFeatureFlag() {

        when(ldClient.boolVariation(anyString(), any(LDContext.class), eq(Boolean.FALSE)))
            .thenReturn(false);

        boolean result = featureToggleService.getBooleanValue(
            FeatureFlag.SSCS_CHILD_MAINTENANCE_FT,
            "some-user-id",
            "example@email.com");

        assertFalse(result);

    }

    @Test
    void shouldThrowWhenMandatoryFieldMissing() {

        assertThrowsExactly(NullPointerException.class,
            () -> featureToggleService.getBooleanValue(
                null,
                "some-user-id",
                "example@email.com")
        );
        assertThrowsExactly(NullPointerException.class,
            () -> featureToggleService.getBooleanValue(
                FeatureFlag.SSCS_CHILD_MAINTENANCE_FT,
                null,
                "example@email.com")
        );

    }
}