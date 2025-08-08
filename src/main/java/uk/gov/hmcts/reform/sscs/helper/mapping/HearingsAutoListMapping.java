package uk.gov.hmcts.reform.sscs.helper.mapping;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberType.REGIONAL_MEDICAL_MEMBER;
import static uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberType.TRIBUNAL_MEMBER_FINANCIALLY_QUALIFIED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberType.TRIBUNAL_MEMBER_MEDICAL;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.helper.mapping.HearingsCaseMapping.shouldBeAdditionalSecurityFlag;
import static uk.gov.hmcts.reform.sscs.helper.service.HearingsServiceHelper.checkBenefitIssueCode;
import static uk.gov.hmcts.reform.sscs.utility.HearingChannelUtil.isInterpreterRequired;

import jakarta.validation.Valid;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.OverrideFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.exception.ListingException;
import uk.gov.hmcts.reform.sscs.reference.data.model.DefaultPanelComposition;
import uk.gov.hmcts.reform.sscs.reference.data.service.PanelCompositionService;
import uk.gov.hmcts.reform.sscs.utility.HearingChannelUtil;

@Slf4j
@Service
public final class HearingsAutoListMapping {

    private final PanelCompositionService panelCompositionService;

    HearingsAutoListMapping(PanelCompositionService panelCompositionService) {
        this.panelCompositionService = panelCompositionService;
    }

    public boolean shouldBeAutoListed(@Valid SscsCaseData caseData) throws ListingException {
        OverrideFields overrideFields = OverridesMapping.getOverrideFields(caseData);
        if (nonNull(overrideFields.getAutoList())) {
            return isYes(overrideFields.getAutoList());
        }
        if (caseData.isIbcCase()) {
            return false;
        }
        return !(HearingsDetailsMapping.isCaseUrgent(caseData)
                || hasOrgRepresentative(caseData)
                || shouldBeAdditionalSecurityFlag(caseData)
                || isInterpreterRequired(caseData)
                || HearingsDetailsMapping.isCaseLinked(caseData)
                || isPaperCase(caseData)
                || hasMqpmOrFqpm(caseData)
                || isThereOtherComments(caseData)
                || doesNotHaveDwpResponseDate(caseData)
            );
    }

    public static boolean hasOrgRepresentative(@Valid SscsCaseData caseData) {
        return isRepresentativeOrg(caseData.getAppeal().getRep())
                || hasOrgOtherParties(caseData.getOtherParties());
    }

    public static boolean hasOrgOtherParties(Collection<CcdValue<OtherParty>> otherParties) {
        return Optional.ofNullable(otherParties)
                .orElse(Collections.emptyList())
                .stream()
                .map(CcdValue::getValue)
                .map(OtherParty::getRep)
                .anyMatch(HearingsAutoListMapping::isRepresentativeOrg);
    }

    public static boolean isRepresentativeOrg(Representative rep) {
        return nonNull(rep)
                && isYes(rep.getHasRepresentative()) && isNotBlank(rep.getOrganisation());
    }

    public static boolean isPaperCase(@Valid SscsCaseData caseData) {
        return HearingChannelUtil.isPaperCase(caseData);
    }

    public static boolean isThereOtherComments(@Valid SscsCaseData caseData) {
        return isNotBlank(HearingsDetailsMapping.getListingComments(caseData));
    }

    public static boolean doesNotHaveDwpResponseDate(@Valid SscsCaseData caseData) {
        return isBlank(caseData.getDwpResponseDate());
    }

    public boolean hasMqpmOrFqpm(@Valid SscsCaseData caseData) throws ListingException {
        DefaultPanelComposition panelCategory = panelCompositionService.getDefaultPanelComposition(caseData);
        checkBenefitIssueCode(panelCompositionService
                .isBenefitIssueCodeValid(caseData.getBenefitCode(), caseData.getIssueCode()));

        return nonNull(panelCategory) && panelCategory.containsAnyPanelMembers(
                List.of(TRIBUNAL_MEMBER_MEDICAL, REGIONAL_MEDICAL_MEMBER, TRIBUNAL_MEMBER_FINANCIALLY_QUALIFIED)
        );
    }
}
