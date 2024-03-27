package uk.gov.hmcts.reform.sscs.evidenceshare.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import junitparams.JUnitParamsRunner;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import uk.gov.hmcts.reform.sscs.service.AirLookupService;

@RunWith(JUnitParamsRunner.class)
@SpringBootTest
@TestPropertySource(locations = "classpath:config/application_it.properties")
public class EvidenceShareConfigTest {

    @MockBean
    protected AirLookupService airLookupService;
    // Below rules are needed to use the junitParamsRunner together with SpringRunner
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();
    //end of rules needed for junitParamsRunner

    @Autowired
    private EvidenceShareConfig evidenceShareConfig;

    @Test
    public void submitTypeContainsPaperAndAllowedBenefitTypesContainsPip() {
        assertNotNull("evidenceShareConfig must be autowired", evidenceShareConfig);
        assertThat(evidenceShareConfig.getSubmitTypes()).contains("PAPER");
    }

}
