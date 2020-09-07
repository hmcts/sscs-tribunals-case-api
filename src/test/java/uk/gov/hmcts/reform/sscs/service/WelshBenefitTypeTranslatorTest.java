package uk.gov.hmcts.reform.sscs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@RunWith(JUnitParamsRunner.class)
public class WelshBenefitTypeTranslatorTest {

    private WelshBenefitTypeTranslator welshBenefitTypeTranslator;

    @Before
    public void setUpTest() {
        welshBenefitTypeTranslator = new WelshBenefitTypeTranslator("welshPipTranslation", "welshUcTranslation", "welshEsaTranslation");
    }

    @Test
    @Parameters({"PIP, welshPipTranslation", "UC, welshUcTranslation", "ESA, welshEsaTranslation"})
    public void shouldTranslateBenefitType(String benefitTypeCode, String translation) {

        BenefitType benefitType = BenefitType.builder()
                .code(benefitTypeCode)
                .build();

        SscsCaseData caseData = buildCaseData();
        caseData.getAppeal().setBenefitType(benefitType);
        String result = welshBenefitTypeTranslator.translate(caseData);

        assertEquals(translation, result);
    }


}