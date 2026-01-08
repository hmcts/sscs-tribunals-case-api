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
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@ExtendWith(MockitoExtension.class)
class FeatureToggleServiceTest {

    private static final String SOME_USER_KEY = "some-user-key";
    private static final String MAIL = "example@email.com";
    private static final String SOME_USER_ID = "some-user-id";
    private FeatureToggleService featureToggleService;

    @Mock
    private LDClient ldClient;

    @Mock
    private IdamService idamService;

    @BeforeEach
    void setUp() {
        featureToggleService = new FeatureToggleService(ldClient, idamService, SOME_USER_KEY);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldGetTrueBooleanFeatureFlag(boolean expected) {

        when(ldClient.boolVariation(anyString(), any(LDContext.class), eq(Boolean.FALSE))).thenReturn(expected);

        boolean result = featureToggleService.isEnabled(FeatureFlag.SSCS_CHILD_MAINTENANCE_FT, SOME_USER_ID, MAIL);

        assertEquals(expected, result);

    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldGetBooleanFeatureFlagWithoutUserDetails(boolean expected) {

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().userId(SOME_USER_ID).email(MAIL).build());
        when(ldClient.boolVariation(anyString(), any(LDContext.class), eq(Boolean.FALSE))).thenReturn(expected);

        boolean result = featureToggleService.isEnabled(FeatureFlag.SSCS_CHILD_MAINTENANCE_FT);

        assertEquals(expected, result);

    }

    @Test
    void shouldThrowWhenMandatoryFieldMissing() {

        assertThrowsExactly(IllegalArgumentException.class,
            () -> featureToggleService.isEnabled(null, SOME_USER_ID, MAIL));
        assertThrowsExactly(IllegalArgumentException.class,
            () -> featureToggleService.isEnabled(FeatureFlag.SSCS_CHILD_MAINTENANCE_FT, null, MAIL));

    }
}