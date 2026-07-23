package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

import com.launchdarkly.sdk.LDContext;
import com.launchdarkly.sdk.server.LDClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.featureflag.FeatureFlag;

@ExtendWith(MockitoExtension.class)
class FeatureToggleServiceTest {

    private FeatureToggleService featureToggleService;

    @Mock
    private LDClient ldClient;

    @BeforeEach
    void setUp() {
        featureToggleService = new FeatureToggleService(ldClient, "some-user-key");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldGetTrueBooleanFeatureFlag(boolean expected) {

        when(ldClient.boolVariation(anyString(), any(LDContext.class), eq(Boolean.FALSE)))
            .thenReturn(expected);

        boolean result = featureToggleService.isEnabled(
            FeatureFlag.SSCS_CHILD_MAINTENANCE_FT,
            "some-user-id",
            "example@email.com");

        assertEquals(expected, result);

    }

    @Test
    void shouldThrowWhenMandatoryFieldMissing() {

        assertThrowsExactly(IllegalArgumentException.class,
            () -> featureToggleService.isEnabled(
                null,
                "some-user-id",
                "example@email.com")
        );
        assertThrowsExactly(IllegalArgumentException.class,
            () -> featureToggleService.isEnabled(
                FeatureFlag.SSCS_CHILD_MAINTENANCE_FT,
                null,
                "example@email.com")
        );

    }
}