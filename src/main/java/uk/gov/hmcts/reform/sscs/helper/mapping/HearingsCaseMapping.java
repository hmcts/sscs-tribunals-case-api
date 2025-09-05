package uk.gov.hmcts.reform.sscs.helper.mapping;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.helper.service.HearingsServiceHelper.checkBenefitIssueCode;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.CaseCategoryType.CASE_SUBTYPE;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.CaseCategoryType.CASE_TYPE;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getCategorySubTypeValue;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getCategoryTypeValue;
import static uk.gov.hmcts.reform.sscs.utility.HearingChannelUtil.isInterpreterRequired;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.exception.ListingException;
import uk.gov.hmcts.reform.sscs.model.HearingWrapper;
import uk.gov.hmcts.reform.sscs.model.single.hearing.CaseCategory;
import uk.gov.hmcts.reform.sscs.model.single.hearing.CaseDetails;
import uk.gov.hmcts.reform.sscs.reference.data.service.PanelCompositionService;
import uk.gov.hmcts.reform.sscs.service.holder.ReferenceDataServiceHolder;

@RestController
@Slf4j
public final class HearingsCaseMapping {

    public static final String CASE_DETAILS_URL = "%s/cases/case-details/%s";

    private final PanelCompositionService panelCompositionService;

    HearingsCaseMapping(PanelCompositionService panelCompositionService) {
        this.panelCompositionService = panelCompositionService;
    }

    public CaseDetails buildHearingCaseDetails(HearingWrapper wrapper, ReferenceDataServiceHolder refData)
        throws ListingException {

        SscsCaseData caseData = wrapper.getCaseData();
        return CaseDetails.builder()
                .hmctsServiceCode(getServiceCode(refData))
                .caseId(getCaseID(caseData))
                .caseDeepLink(getCaseDeepLink(wrapper.getCaseData(), refData))
                .hmctsInternalCaseName(getInternalCaseName(caseData))
                .publicCaseName(getPublicCaseName(caseData))
                .caseAdditionalSecurityFlag(shouldBeAdditionalSecurityFlag(caseData))
                .caseInterpreterRequiredFlag(isInterpreterRequired(caseData))
                .caseCategories(buildCaseCategories(caseData))
                .caseManagementLocationCode(getCaseManagementLocationCode(caseData))
                .caseRestrictedFlag(shouldBeSensitiveFlag())
                .caseSlaStartDate(getCaseCreated(caseData))
                .build();
    }

    public static String getServiceCode(ReferenceDataServiceHolder refData) {
        return refData.getSscsServiceCode();
    }

    public static String getCaseID(SscsCaseData caseData) {
        return caseData.getCcdCaseId();
    }

    public static String getCaseDeepLink(SscsCaseData caseData, ReferenceDataServiceHolder refData) {
        return String.format(CASE_DETAILS_URL, refData.getExUiUrl(), getCaseID(caseData));
    }

    public static String getInternalCaseName(SscsCaseData caseData) {
        return caseData.getCaseAccessManagementFields().getCaseNameHmctsInternal();
    }

    public static String getPublicCaseName(SscsCaseData caseData) {
        return caseData.getCaseAccessManagementFields().getCaseNamePublic();
    }

    public static boolean shouldBeAdditionalSecurityFlag(SscsCaseData caseData) {
        return isYes(caseData.getDwpUcb())
                || shouldBeAdditionalSecurityOtherParties(caseData.getOtherParties());
    }

    public static boolean shouldBeAdditionalSecurityOtherParties(List<CcdValue<OtherParty>> otherParties) {
        return nonNull(otherParties) && otherParties.stream()
                .map(CcdValue::getValue)
                .anyMatch(o -> isYes(o.getUnacceptableCustomerBehaviour()));
    }

    public List<CaseCategory> buildCaseCategories(SscsCaseData caseData)
        throws ListingException {
        checkBenefitIssueCode(panelCompositionService.isBenefitIssueCodeValid(caseData.getBenefitCode(), caseData.getIssueCode()));
        List<CaseCategory> categories = new ArrayList<>(getCaseTypes(caseData));
        categories.addAll(getCaseSubTypes(caseData));
        return categories;
    }

    public static List<CaseCategory> getCaseTypes(SscsCaseData caseData) {
        List<CaseCategory> categories = new ArrayList<>();
        categories.add(CaseCategory.builder()
                .categoryType(CASE_TYPE)
                .categoryValue(getCategoryTypeValue(caseData))
                .build());
        return categories;
    }

    public static List<CaseCategory> getCaseSubTypes(SscsCaseData caseData) {
        List<CaseCategory> categories = new ArrayList<>();
        categories.add(CaseCategory.builder()
                .categoryType(CASE_SUBTYPE)
                .categoryParent(getCategoryTypeValue(caseData))
                .categoryValue(getCategorySubTypeValue(caseData))
                .build());
        return categories;
    }

    public static String getCaseManagementLocationCode(SscsCaseData caseData) {
        if (isNull(caseData.getCaseManagementLocation())) {
            return null;
        }

        return caseData.getCaseManagementLocation().getBaseLocation();
    }

    public static boolean shouldBeSensitiveFlag() {
        // TODO Future Work
        return false;
    }

    public static String getCaseCreated(SscsCaseData caseData) {
        return caseData.getCaseCreated();
    }

    public static List<String> getReasonsForLink(SscsCaseData caseData) {
        return new ArrayList<>();
    }

}


