package uk.gov.hmcts.reform.sscs.helper.mapping;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.CHILD_SUPPORT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberMedicallyQualified.getPanelMemberMedicallyQualified;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.helper.mapping.HearingsMapping.getSessionCaseCodeMap;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.CollectionItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelMember;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberComposition;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberExclusions;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberMedicallyQualified;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.model.client.JudicialUserBase;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.BenefitRoleRelationType;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.RequirementType;
import uk.gov.hmcts.reform.sscs.model.single.hearing.MemberType;
import uk.gov.hmcts.reform.sscs.model.single.hearing.PanelPreference;
import uk.gov.hmcts.reform.sscs.model.single.hearing.PanelRequirements;
import uk.gov.hmcts.reform.sscs.reference.data.model.JudicialMemberType;
import uk.gov.hmcts.reform.sscs.reference.data.model.PanelCategoryMap;
import uk.gov.hmcts.reform.sscs.reference.data.model.SessionCategoryMap;
import uk.gov.hmcts.reform.sscs.reference.data.service.PanelCategoryMapService;
import uk.gov.hmcts.reform.sscs.service.holder.ReferenceDataServiceHolder;

@Slf4j
@Component
public final class HearingsPanelMapping {

    private final PanelCategoryMapService panelCategoryMapService;

    @Value("${feature.default-panel-comp.enabled}")
    private boolean defaultPanelCompEnabled;

    HearingsPanelMapping(PanelCategoryMapService panelCategoryMapService) {

        this.panelCategoryMapService = panelCategoryMapService;
    }

    public PanelRequirements getPanelRequirements(SscsCaseData caseData,
                                                         ReferenceDataServiceHolder refData) {
        return PanelRequirements.builder()
                .roleTypes(getRoleTypes(caseData))
                .authorisationTypes(getAuthorisationTypes())
                .authorisationSubTypes(getAuthorisationSubTypes())
                .panelPreferences(getPanelPreferences(caseData))
                .panelSpecialisms(getPanelSpecialisms(caseData, getSessionCaseCodeMap(caseData, refData)))
                .build();
    }

    public List<String> getRoleTypes(SscsCaseData caseData) {
        if (!defaultPanelCompEnabled) {
            return BenefitRoleRelationType.findRoleTypesByBenefitCode(caseData.getBenefitCode());
        }
        if (caseData.getPanelMemberComposition() != null) {
            return mapPanelMemberCompositionToRoleTypes(caseData.getPanelMemberComposition());
        }
        String benefitIssueCode = caseData.getBenefitCode() + caseData.getIssueCode();
        String specialismCount = caseData.getSscsIndustrialInjuriesData().getPanelDoctorSpecialism() != null
                ? caseData.getSscsIndustrialInjuriesData().getSecondPanelDoctorSpecialism() != null
                ? "2" : "1" : null;
        String isFqpm = isYes(caseData.getIsFqpmRequired()) ? "true" : null;
        PanelCategoryMap panelComp = panelCategoryMapService.getPanelCategoryMap(benefitIssueCode, specialismCount, isFqpm);

        log.info("Panel Category Map for Case {}: {}", caseData.getCcdCaseId(), panelComp);
        if (panelComp != null) {
            setPanelMemberComposition(caseData, panelComp.getJohTiers());
            return panelComp.getJohTiers();
        } else {
            return Collections.emptyList();
        }
    }

    public static List<String> mapPanelMemberCompositionToRoleTypes(PanelMemberComposition panelMemberComposition) {
        ArrayList<String> roleTypes = new ArrayList<>();
        if (nonNull(panelMemberComposition.getPanelCompositionJudge())) {
            roleTypes.add(panelMemberComposition.getPanelCompositionJudge().getHmcReference());
        }
        if (nonNull(panelMemberComposition.getPanelCompositionMemberMedical1())) {
            roleTypes.add(panelMemberComposition.getPanelCompositionMemberMedical1().getReference());
        }
        if (nonNull(panelMemberComposition.getPanelCompositionMemberMedical2())) {
            roleTypes.add(panelMemberComposition.getPanelCompositionMemberMedical2().getReference());
        }
        if (nonNull(panelMemberComposition.getPanelCompositionDisabilityAndFqMember())) {
            roleTypes.addAll(panelMemberComposition.getPanelCompositionDisabilityAndFqMember().stream().map(PanelMemberType::getReference).toList());
        }
        return roleTypes;
    }

    public static void setPanelMemberComposition(SscsCaseData caseData, List<String> johTiers) {
        PanelMemberComposition panelMemberComposition = new PanelMemberComposition();
        panelMemberComposition.setPanelCompositionDisabilityAndFqMember(new ArrayList<>());
        for (String johTier : johTiers) {
            switch (johTier) {
                case "50":
                    panelMemberComposition.getPanelCompositionDisabilityAndFqMember().add(PanelMemberType.TRIBUNALS_MEMBER_FINANCIALLY_QUALIFIED);
                    break;
                case "58":
                    if (panelMemberComposition.getPanelCompositionMemberMedical1() != null) {
                        panelMemberComposition.setPanelCompositionMemberMedical2(PanelMemberType.TRIBUNALS_MEMBER_MEDICAL);
                    } else {
                        panelMemberComposition.setPanelCompositionMemberMedical1(PanelMemberType.TRIBUNALS_MEMBER_MEDICAL);
                    }
                    break;
                case "85":
                    panelMemberComposition.setPanelCompositionJudge(JudicialMemberType.TRIBUNAL_JUDGE);
                    break;
                case "74":
                    panelMemberComposition.setPanelCompositionJudge(JudicialMemberType.REGIONAL_TRIBUNAL_JUDGE);
                    break;
                case "69":
                    if (panelMemberComposition.getPanelCompositionMemberMedical1() != null) {
                        panelMemberComposition.setPanelCompositionMemberMedical2(PanelMemberType.REGIONAL_MEDICAL_MEMBER);
                    } else {
                        panelMemberComposition.setPanelCompositionMemberMedical1(PanelMemberType.REGIONAL_MEDICAL_MEMBER);
                    }
                    break;
                case "44":
                    panelMemberComposition.getPanelCompositionDisabilityAndFqMember().add(PanelMemberType.TRIBUNALS_MEMBER_DISABILITY);
                    break;
                default:
            }

        }
        caseData.setPanelMemberComposition(panelMemberComposition);
    }

    public static List<String> getAuthorisationTypes() {
        //TODO Need to retrieve AuthorisationTypes from caseData and/or ReferenceData
        return Collections.emptyList();
    }

    public static List<String> getAuthorisationSubTypes() {
        //TODO Need to retrieve AuthorisationSubTypes from caseData and/or ReferenceData
        return Collections.emptyList();
    }

    public static List<PanelPreference> getPanelPreferences(SscsCaseData caseData) {
        List<PanelPreference> panelMemberPreferences = new ArrayList<>();
        PanelMemberExclusions panelMembers = caseData.getSchedulingAndListingFields().getPanelMemberExclusions();

        if (nonNull(panelMembers)) {
            panelMemberPreferences.addAll(getSlPanelPreferences(panelMembers));
        }

        return panelMemberPreferences.stream().toList();
    }

    private static List<PanelPreference> getSlPanelPreferences(PanelMemberExclusions panelMembers) {
        var excluded = getExcludedPanelMembers(panelMembers);
        var reserved = getReservedPreferences(panelMembers);
        var preferences = new ArrayList<>(excluded);
        preferences.addAll(reserved);

        return preferences;
    }

    private static List<PanelPreference> getReservedPreferences(PanelMemberExclusions panelMembers) {
        List<CollectionItem<JudicialUserBase>> reservedPanelMembers = panelMembers.getReservedPanelMembers();
        return getMemberPreferences(reservedPanelMembers, RequirementType.MUST_INCLUDE);
    }

    private static List<PanelPreference> getExcludedPanelMembers(PanelMemberExclusions panelMembers) {
        List<CollectionItem<JudicialUserBase>> excludedPanelMembers = panelMembers.getExcludedPanelMembers();
        return getMemberPreferences(excludedPanelMembers, RequirementType.EXCLUDE);
    }

    private static List<PanelPreference> getMemberPreferences(List<CollectionItem<JudicialUserBase>> panelMembers,
                                                              RequirementType requirementType) {
        if (nonNull(panelMembers)) {
            return panelMembers.stream()
                    .filter(panelMember -> nonNull(panelMember.getValue().getPersonalCode()))
                    .map(paneMember -> getPanelPreference(
                            paneMember.getValue().getPersonalCode(),
                            requirementType
                    ))
                    .toList();
        }
        return List.of();
    }

    private static PanelPreference getPanelPreference(String memberID, RequirementType requirementType) {
        return PanelPreference.builder()
                .memberID(memberID)
                .memberType(MemberType.JOH)
                .requirementType(requirementType)
                .build();
    }

    public static List<String> getPanelSpecialisms(@Valid SscsCaseData caseData, SessionCategoryMap sessionCategoryMap) {
        List<String> panelSpecialisms = new ArrayList<>();

        if (isNull(sessionCategoryMap)) {
            return panelSpecialisms;
        }
        // if benefit is child support specialism should be empty
        if (isNotBlank(caseData.getBenefitCode()) && caseData.getBenefitCode().equals(CHILD_SUPPORT.getBenefitCode())) {
            return panelSpecialisms;
        }

        String doctorSpecialism = caseData.getSscsIndustrialInjuriesData().getPanelDoctorSpecialism();
        String doctorSpecialismSecond = caseData.getSscsIndustrialInjuriesData().getSecondPanelDoctorSpecialism();
        panelSpecialisms = sessionCategoryMap.getCategory().getPanelMembers().stream()
                .map(panelMember -> getPanelMemberSpecialism(panelMember, doctorSpecialism, doctorSpecialismSecond))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return panelSpecialisms;
    }

    public static String getPanelMemberSpecialism(PanelMember panelMember,
                                                  String doctorSpecialism, String doctorSpecialismSecond) {
        switch (panelMember) {
            case FQPM:
                return null;
            case MQPM1:
                return getReference(doctorSpecialism);
            case MQPM2:
                return getReference(doctorSpecialismSecond);
            default:
                return panelMember.getReference();
        }
    }

    public static String getReference(String panelMemberSubtypeCcdRef) {
        PanelMemberMedicallyQualified subType = getPanelMemberMedicallyQualified(panelMemberSubtypeCcdRef);
        return nonNull(subType) ? subType.getHmcReference() : null;
    }
}
