package uk.gov.hmcts.reform.sscs.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.helper.mapping.HearingsDetailsMapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
