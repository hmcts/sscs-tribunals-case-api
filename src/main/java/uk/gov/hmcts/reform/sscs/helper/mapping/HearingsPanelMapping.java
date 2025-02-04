package uk.gov.hmcts.reform.sscs.helper.mapping;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.CHILD_SUPPORT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberMedicallyQualified.getPanelMemberMedicallyQualified;
import static uk.gov.hmcts.reform.sscs.helper.mapping.HearingsMapping.getSessionCaseCodeMap;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.sscs.ccd.domain.CollectionItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelMember;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberExclusions;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberMedicallyQualified;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.model.client.JudicialUserBase;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.BenefitRoleRelationType;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.RequirementType;
import uk.gov.hmcts.reform.sscs.model.single.hearing.MemberType;
import uk.gov.hmcts.reform.sscs.model.single.hearing.PanelPreference;
import uk.gov.hmcts.reform.sscs.model.single.hearing.PanelRequirements;
import uk.gov.hmcts.reform.sscs.reference.data.model.SessionCategoryMap;
import uk.gov.hmcts.reform.sscs.service.holder.ReferenceDataServiceHolder;

@Slf4j
public final class HearingsPanelMapping {

    private HearingsPanelMapping() {

    }

    public static PanelRequirements getPanelRequirements(SscsCaseData caseData,
                                                         ReferenceDataServiceHolder refData) {
        return PanelRequirements.builder()
                .roleTypes(getRoleTypes(caseData.getBenefitCode()))
                .authorisationTypes(getAuthorisationTypes())
                .authorisationSubTypes(getAuthorisationSubTypes())
                .panelPreferences(getPanelPreferences(caseData))
                .panelSpecialisms(getPanelSpecialisms(caseData, getSessionCaseCodeMap(caseData, refData)))
                .build();
    }

    public static List<String> getRoleTypes(String benefitCode) {
        return BenefitRoleRelationType.findRoleTypesByBenefitCode(benefitCode);
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
