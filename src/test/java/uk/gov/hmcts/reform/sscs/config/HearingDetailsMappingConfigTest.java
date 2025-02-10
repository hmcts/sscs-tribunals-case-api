package uk.gov.hmcts.reform.sscs.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

public class HearingDetailsMappingConfigTest {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldSetHearingDetailsMappingFlagOnInit(boolean flagVal) {
        HearingsDetailsMappingConfig hearingsDetailsMappingConfig = new HearingsDetailsMappingConfig();
        ReflectionTestUtils.setField(hearingsDetailsMappingConfig, "isDirectionHearingsEnabled", flagVal);
        hearingsDetailsMappingConfig.init();
        assertEquals(flagVal, HearingsDetailsMapping.isDirectionHearingsEnabled());
    }
}
