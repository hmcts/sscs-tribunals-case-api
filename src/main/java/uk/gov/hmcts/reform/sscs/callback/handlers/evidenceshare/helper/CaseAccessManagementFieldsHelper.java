package uk.gov.hmcts.reform.sscs.callback.handlers.evidenceshare.helper;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.getBenefitOptionalByCode;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.FormType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsType;

@Slf4j
public class CaseAccessManagementFieldsHelper {

    private static final String HMRC_OGD_TYPE = "HMRC";
    private static final String DWP_OGD_TYPE = "DWP";

    private CaseAccessManagementFieldsHelper() {
        // no-op
    }

    public static void setCaseAccessManagementFields(SscsCaseData sscsCaseData) {
        setCategories(sscsCaseData);
        setCaseNames(sscsCaseData);
        setOgdType(sscsCaseData);
    }

    public static void setCategories(SscsCaseData sscsCaseData) {
        Appeal appeal = sscsCaseData.getAppeal();

        if (nonNull(appeal)
            && nonNull(appeal.getBenefitType())
            && isNotBlank(appeal.getBenefitType().getCode())) {

            getBenefitOptionalByCode(appeal.getBenefitType().getCode())
                .ifPresent(
                    benefit -> sscsCaseData
                        .getCaseAccessManagementFields()
                        .setCategories(benefit)
                );
        }
    }

    public static void setCaseNames(SscsCaseData sscsCaseData) {
        if (hasAppellantName(sscsCaseData.getAppeal())) {
            sscsCaseData
                .getCaseAccessManagementFields()
                .setCaseNames(sscsCaseData
                    .getAppeal()
                    .getAppellant()
                    .getName()
                    .getFullNameNoTitle()
                );
        }
    }

    public static void setOgdType(SscsCaseData sscsCaseData) {
        if (nonNull(sscsCaseData.getAppeal())
            && nonNull(sscsCaseData.getAppeal().getBenefitType())) {
            sscsCaseData
                .getCaseAccessManagementFields()
                .setOgdType(isHmrcBenefit(sscsCaseData)
                    ? HMRC_OGD_TYPE
                    : DWP_OGD_TYPE);
        }
        log.info("OgdType of {} was added for: {}",
            sscsCaseData.getCaseAccessManagementFields().getOgdType(),
            sscsCaseData.getCcdCaseId());
    }

    public static boolean hasAppellantName(Appeal appeal) {
        return nonNull(appeal)
            && nonNull(appeal.getAppellant())
            && nonNull(appeal.getAppellant().getName())
            && nonNull(appeal.getAppellant().getName().getFirstName())
            && nonNull(appeal.getAppellant().getName().getLastName());
    }

    public static boolean isHmrcBenefit(SscsCaseData sscsCaseData) {
        return getBenefitOptionalByCode(sscsCaseData.getAppeal().getBenefitType().getCode())
            .map(benefit -> SscsType.SSCS5.equals(benefit.getSscsType()))
            .orElseGet(() -> FormType.SSCS5.equals(sscsCaseData.getFormType()));
    }
}
