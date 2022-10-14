package uk.gov.hmcts.reform.sscs.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;

@SpringBootTest
@RunWith(JUnitParamsRunner.class)
@TestPropertySource(locations = "classpath:config/application_it.properties")
public class DocumentConfigurationTest {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private DocumentConfiguration config;

    public static Object[][] documentParameters() {
        return new Object[][] {
                {LanguagePreference.ENGLISH, EventType.DECISION_ISSUED, Optional.empty(), "TB-SCS-GNO-ENG-draft-decision-notice.docx"},
                {LanguagePreference.ENGLISH, EventType.DIRECTION_ISSUED, Optional.empty(), "TB-SCS-GNO-ENG-draft-decision-notice.docx"},
                {LanguagePreference.ENGLISH, EventType.ISSUE_FINAL_DECISION, Optional.empty(), "TB-SCS-GNO-ENG-00642.docx"},
                {LanguagePreference.WELSH, EventType.DECISION_ISSUED, Optional.empty(), "TB-SCS-GNO-WEL-00473.docx"},
                {LanguagePreference.WELSH, EventType.DIRECTION_ISSUED, Optional.empty(), "TB-SCS-GNO-WEL-00473.docx"},
                {LanguagePreference.WELSH, EventType.ISSUE_FINAL_DECISION, Optional.empty(), "TB-SCS-GNO-ENG-00642.docx"}
        };
    }

    public static Object[][] evidenceParameters() {
        return new Object[][] {
                {LanguagePreference.ENGLISH, "template","TB-SCS-GNO-ENG-00012.docx"},
                {LanguagePreference.ENGLISH, "hmctsImgVal","\"[userImage:hmcts.png]\""},
                {LanguagePreference.WELSH, "template","TB-SCS-GNO-WEL-00479.docx"},
                {LanguagePreference.WELSH, "hmctsImgVal","\"[userImage:welshhmcts.png]\""},
        };
    }

    public static Object[][] coverPageParameters() {
        return new Object[][] {
                {LanguagePreference.ENGLISH, "SSCS-cover-page.docx"},
                {LanguagePreference.WELSH, "TB-SCS-LET-WEL-00486.docx"},
        };
    }

    @Test
    @Parameters(method = "documentParameters")
    public void testDocumnetConfig(LanguagePreference languagePreference, EventType eventType, Optional<String> benefitType, String documentName) {
        if (benefitType.isPresent()) {
            assertThat(config.getBenefitSpecificDocuments().get(benefitType.get()).get(languagePreference).get(eventType)).isEqualTo(documentName);
        } else {
            assertThat(config.getDocuments().get(languagePreference).get(eventType)).isEqualTo(documentName);
        }
    }

    @Test
    @Parameters(method = "evidenceParameters")
    public void testEvidenceConfig(LanguagePreference languagePreference, String type, String documentName) {
        assertThat(config.getEvidence().get(languagePreference).get(type)).isEqualTo(documentName);
    }

    @Test
    @Parameters(method = "coverPageParameters")
    public void testCoverPage(LanguagePreference languagePreference, String documentName) {
        assertThat(config.getCover().get(languagePreference)).isEqualTo(documentName);
    }


}
