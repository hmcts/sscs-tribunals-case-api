package uk.gov.hmcts.reform.sscs.evidenceshare.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;
import uk.gov.hmcts.reform.sscs.service.AirLookupService;

@SpringBootTest
@TestPropertySource(locations = "classpath:config/application_it.properties")
public class DocmosisTemplateConfigTest {
    //end of rules needed for junitParamsRunner

    @Autowired
    private DocmosisTemplateConfig docmosisTemplateConfig;

    @MockBean
    protected AirLookupService airLookupService;

    @Test
    public void docmosisTemplate() {
        assertThat("TB-SCS-GNO-ENG-00010.doc").isEqualTo(
            docmosisTemplateConfig.getTemplate()
                .get(LanguagePreference.ENGLISH).get(DocumentType.DL6.getValue()).get("name"));
        assertThat("TB-SCS-GNO-WEL-00469.docx").isEqualTo(
            docmosisTemplateConfig.getTemplate().get(LanguagePreference.WELSH).get("d609-97").get(
                "name"));

    }
}
