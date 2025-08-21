package uk.gov.hmcts.reform.sscs.helper.mapping;

import jakarta.validation.Valid;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.exception.ListingException;
import uk.gov.hmcts.reform.sscs.model.service.hearingvalues.Judiciary;
import uk.gov.hmcts.reform.sscs.model.service.hearingvalues.PanelPreference;
import uk.gov.hmcts.reform.sscs.model.service.hearingvalues.ServiceHearingValues;
import uk.gov.hmcts.reform.sscs.reference.data.service.PanelCompositionService;
import uk.gov.hmcts.reform.sscs.service.holder.ReferenceDataServiceHolder;
import uk.gov.hmcts.reform.sscs.utility.HearingChannelUtil;

@Component
@Slf4j
public final class ServiceHearingValuesMapping {

    public static final String BENEFIT = "Benefit";

    private final HearingsPanelMapping hearingsPanelMapping;

    private final HearingsAutoListMapping hearingsAutoListMapping;

    private final PanelCompositionService panelCompositionService;

    private final HearingsCaseMapping hearingsCaseMapping;

    ServiceHearingValuesMapping(HearingsPanelMapping hearingsPanelMapping, HearingsAutoListMapping hearingsAutoListMapping, PanelCompositionService panelCompositionService, HearingsCaseMapping hearingsCaseMapping) {
        this.hearingsPanelMapping = hearingsPanelMapping;
        this.hearingsAutoListMapping = hearingsAutoListMapping;
        this.panelCompositionService = panelCompositionService;
        this.hearingsCaseMapping = hearingsCaseMapping;
    }


    public ServiceHearingValues mapServiceHearingValues(@Valid SscsCaseData caseData, ReferenceDataServiceHolder refData)
            throws ListingException {

        boolean shouldBeAutoListed = hearingsAutoListMapping.shouldBeAutoListed(caseData);
        int hearingDuration = 0;
        try {
            hearingDuration = HearingsDurationMapping.getHearingDuration(caseData, refData);
        } catch (ListingException e) {
            log.error("Error getting hearing duration for case ID {}: {}", caseData.getCcdCaseId(), e.getMessage());
        }
        return ServiceHearingValues.builder()
                .publicCaseName(HearingsCaseMapping.getPublicCaseName(caseData))
                .caseDeepLink(HearingsCaseMapping.getCaseDeepLink(caseData, refData))
                .caseManagementLocationCode(HearingsCaseMapping.getCaseManagementLocationCode(caseData))
                .caseRestrictedFlag(HearingsCaseMapping.shouldBeSensitiveFlag())
                .caseSlaStartDate(HearingsCaseMapping.getCaseCreated(caseData))
                .hmctsInternalCaseName(HearingsCaseMapping.getInternalCaseName(caseData))
                .autoListFlag(shouldBeAutoListed)
                .hearingType(HearingsDetailsMapping.getHearingType(caseData))
                .caseType(BENEFIT)
                .caseCategories(hearingsCaseMapping.buildCaseCategories(caseData))
                .hearingWindow(HearingsWindowMapping.buildHearingWindow(caseData, refData))
                .duration(hearingDuration)
                .hearingPriorityType(HearingsDetailsMapping.getHearingPriority(caseData))
                .numberOfPhysicalAttendees(HearingsNumberAttendeesMapping.getNumberOfPhysicalAttendees(caseData))
                .hearingInWelshFlag(HearingsDetailsMapping.shouldBeHearingsInWelshFlag())
                .hearingLocations(HearingsLocationMapping.getHearingLocations(caseData, refData))
                .caseAdditionalSecurityFlag(HearingsCaseMapping.shouldBeAdditionalSecurityFlag(caseData))
                .facilitiesRequired(HearingsDetailsMapping.getFacilitiesRequired())
                .listingComments(HearingsDetailsMapping.getListingComments(caseData))
                .hearingRequester(HearingsDetailsMapping.getHearingRequester())
                .privateHearingRequiredFlag(HearingsDetailsMapping.isPrivateHearingRequired())
                .leadJudgeContractType(HearingsDetailsMapping.getLeadJudgeContractType())
                .judiciary(getJudiciary(caseData))
                .hearingIsLinkedFlag(HearingsDetailsMapping.isCaseLinked(caseData))
                .parties(ServiceHearingPartiesMapping.buildServiceHearingPartiesDetails(caseData, refData))
                .caseFlags(PartyFlagsMapping.getCaseFlags(caseData))
                .hmctsServiceID(refData.getSscsServiceCode())
                .hearingChannels(HearingsChannelMapping.getHearingChannels(caseData))
                .caseInterpreterRequiredFlag(HearingChannelUtil.isInterpreterRequired(caseData))
                .panelRequirements(hearingsPanelMapping.getPanelRequirements(caseData))
                .build();
    }

    public Judiciary getJudiciary(@Valid SscsCaseData sscsCaseData) {
        return Judiciary.builder()
                .roleType(panelCompositionService.getRoleTypes(sscsCaseData))
                .authorisationTypes(HearingsPanelMapping.getAuthorisationTypes())
                .authorisationSubType(HearingsPanelMapping.getAuthorisationSubTypes())
                .judiciarySpecialisms(hearingsPanelMapping.getPanelSpecialisms(sscsCaseData))
                .judiciaryPreferences(getPanelPreferences())
                .build();
    }

    public static List<PanelPreference> getPanelPreferences() {
        //TODO Need to retrieve PanelPreferences from caseData and/or ReferenceData
        return Collections.emptyList();
    }
}
