package uk.gov.hmcts.reform.sscs.helper.mapping;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.CollectionUtils.addIgnoreNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.CHILD_SUPPORT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberMedicallyQualified.getPanelMemberMedicallyQualified;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.CollectionItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberComposition;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberExclusions;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberMedicallyQualified;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.model.client.JudicialUserBase;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.RequirementType;
import uk.gov.hmcts.reform.sscs.model.single.hearing.MemberType;
import uk.gov.hmcts.reform.sscs.model.single.hearing.PanelPreference;
import uk.gov.hmcts.reform.sscs.model.single.hearing.PanelRequirements;
import uk.gov.hmcts.reform.sscs.reference.data.service.PanelCompositionService;

@Slf4j
@Component
public final class HearingsPanelMapping {

    private final PanelCompositionService panelCompositionService;

    HearingsPanelMapping(PanelCompositionService panelCompositionService) {
        this.panelCompositionService = panelCompositionService;
    }

    public PanelRequirements getPanelRequirements(SscsCaseData caseData) {
        return PanelRequirements.builder()
                .roleTypes(panelCompositionService.getRoleTypes(caseData))
                .authorisationTypes(getAuthorisationTypes())
                .authorisationSubTypes(getAuthorisationSubTypes())
                .panelPreferences(getPanelPreferences(caseData))
                .panelSpecialisms(getPanelSpecialisms(caseData))
                .build();
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

    public List<String> getPanelSpecialisms(@Valid SscsCaseData caseData) {
        List<String> panelSpecialisms = new ArrayList<>();

        if (CHILD_SUPPORT.getBenefitCode().equals(caseData.getBenefitCode())) {
            return panelSpecialisms;
        }

        PanelMemberComposition panelComposition = caseData.getPanelMemberComposition();
        if (isNull(panelComposition) || panelComposition.isEmpty()) {
            panelComposition = new PanelMemberComposition(
                    panelCompositionService.getDefaultPanelComposition(caseData).getJohTiers()
            );
        }

        if (nonNull(panelComposition.getPanelCompositionMemberMedical1())) {
            addIgnoreNull(panelSpecialisms,
                    getReference(caseData.getSscsIndustrialInjuriesData().getPanelDoctorSpecialism()));
        }
        if (nonNull(panelComposition.getPanelCompositionMemberMedical2())) {
            addIgnoreNull(panelSpecialisms,
                    getReference(caseData.getSscsIndustrialInjuriesData().getSecondPanelDoctorSpecialism()));
        }
        return panelSpecialisms;
    }


    public static String getReference(String panelMemberSubtypeCcdRef) {
        PanelMemberMedicallyQualified subType = getPanelMemberMedicallyQualified(panelMemberSubtypeCcdRef);
        return nonNull(subType) ? subType.getHmcReference() : null;
    }
}
