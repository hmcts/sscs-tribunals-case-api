package uk.gov.hmcts.reform.sscs.evidenceshare.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import junitparams.JUnitParamsRunner;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.sscs.service.AirLookupService;

@RunWith(JUnitParamsRunner.class)
@SpringBootTest
@TestPropertySource(locations = "classpath:config/application_it.properties")
public class EvidenceShareConfigTest {

    @MockBean
    protected AirLookupService airLookupService;
    //end of rules needed for junitParamsRunner

    @Autowired
    private EvidenceShareConfig evidenceShareConfig;

    @Test
    public void submitTypeContainsPaperAndAllowedBenefitTypesContainsPip() {
        assertNotNull(evidenceShareConfig, "evidenceShareConfig must be autowired");
        assertThat(evidenceShareConfig.getSubmitTypes()).contains("PAPER");
    }

}
