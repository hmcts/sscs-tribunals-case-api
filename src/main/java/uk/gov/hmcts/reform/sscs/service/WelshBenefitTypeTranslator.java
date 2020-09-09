package uk.gov.hmcts.reform.sscs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Component
public class WelshBenefitTypeTranslator {


    private static final String BENEFIT_TYPE_PIP = "PIP";
    private static final String BENEFIT_TYPE_UC = "UC";
    public static final String BENEFIT_TYPE_ESA = "ESA";

    private String welshPipTranslation;
    private String welshUcTranslation;
    private String welshEsaTranslation;

    @Autowired
    public WelshBenefitTypeTranslator(@Value("${welsh.benefittype.pip}") String welshPipTranslation,
                                      @Value("${welsh.benefittype.uc}") String welshUcTranslation,
                                      @Value("${welsh.benefittype.esa}") String welshEsaTranslation) {
        this.welshPipTranslation = welshPipTranslation;
        this.welshUcTranslation = welshUcTranslation;
        this.welshEsaTranslation = welshEsaTranslation;
    }

    public String translate(SscsCaseData sscsCaseData) {
        String benefitType;
        if (sscsCaseData.getAppeal().getBenefitType().getCode().equalsIgnoreCase(BENEFIT_TYPE_PIP)) {
            benefitType = welshPipTranslation;
        } else if (sscsCaseData.getAppeal().getBenefitType().getCode().equalsIgnoreCase(BENEFIT_TYPE_UC)) {
            benefitType = welshUcTranslation;
        } else if (sscsCaseData.getAppeal().getBenefitType().getCode().equalsIgnoreCase(BENEFIT_TYPE_ESA)) {
            benefitType = welshEsaTranslation;
        } else {
            benefitType = sscsCaseData.getAppeal().getBenefitType().getDescription() + " (" + sscsCaseData.getAppeal().getBenefitType().getCode() + ")";
        }
        return benefitType;
    }
}
