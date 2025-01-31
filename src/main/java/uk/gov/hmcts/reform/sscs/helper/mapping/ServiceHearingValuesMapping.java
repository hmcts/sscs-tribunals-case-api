package uk.gov.hmcts.reform.sscs.helper.mapping;

import static uk.gov.hmcts.reform.sscs.helper.mapping.HearingsMapping.getSessionCaseCodeMap;

import java.util.Collections;
import java.util.List;
import javax.validation.Valid;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.exception.ListingException;
import uk.gov.hmcts.reform.sscs.model.service.hearingvalues.Judiciary;
import uk.gov.hmcts.reform.sscs.model.service.hearingvalues.PanelPreference;
import uk.gov.hmcts.reform.sscs.model.service.hearingvalues.ServiceHearingValues;
import uk.gov.hmcts.reform.sscs.service.holder.ReferenceDataServiceHolder;
import uk.gov.hmcts.reform.sscs.utility.HearingChannelUtil;


public final class ServiceHearingValuesMapping {

    public static final String BENEFIT = "Benefit";

    private ServiceHearingValuesMapping() {
    }


    public static ServiceHearingValues mapServiceHearingValues(@Valid SscsCaseData caseData, ReferenceDataServiceHolder refData)
            throws ListingException {

        boolean shouldBeAutoListed = HearingsAutoListMapping.shouldBeAutoListed(caseData, refData);

        return ServiceHearingValues.builder()
                .publicCaseName(HearingsCaseMapping.getPublicCaseName(caseData))
                .caseDeepLink(HearingsCaseMapping.getCaseDeepLink(caseData, refData))
                .caseManagementLocationCode(HearingsCaseMapping.getCaseManagementLocationCode(caseData))
                .caseRestrictedFlag(HearingsCaseMapping.shouldBeSensitiveFlag())
                .caseSlaStartDate(HearingsCaseMapping.getCaseCreated(caseData))
                .hmctsInternalCaseName(HearingsCaseMapping.getInternalCaseName(caseData))
                .autoListFlag(shouldBeAutoListed)
                .hearingType(HearingsDetailsMapping.getHearingType())
                .caseType(BENEFIT)
                .caseCategories(HearingsCaseMapping.buildCaseCategories(caseData, refData))
                .hearingWindow(HearingsWindowMapping.buildHearingWindow(caseData, refData))
                .duration(HearingsDurationMapping.getHearingDuration(caseData, refData))
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
                .judiciary(getJudiciary(caseData, refData))
                .hearingIsLinkedFlag(HearingsDetailsMapping.isCaseLinked(caseData))
                .parties(ServiceHearingPartiesMapping.buildServiceHearingPartiesDetails(caseData, refData))
                .caseFlags(PartyFlagsMapping.getCaseFlags(caseData))
                .hmctsServiceID(refData.getSscsServiceCode())
                .hearingChannels(HearingsChannelMapping.getHearingChannels(caseData))
                .screenFlow(null)
                .vocabulary(null)
                .caseInterpreterRequiredFlag(HearingChannelUtil.isInterpreterRequired(caseData))
                .panelRequirements(HearingsPanelMapping.getPanelRequirements(caseData, refData))
                .build();
    }

    public static Judiciary getJudiciary(@Valid SscsCaseData sscsCaseData, ReferenceDataServiceHolder refData) {
        return Judiciary.builder()
                .roleType(HearingsPanelMapping.getRoleTypes(sscsCaseData.getBenefitCode()))
                .authorisationTypes(HearingsPanelMapping.getAuthorisationTypes())
                .authorisationSubType(HearingsPanelMapping.getAuthorisationSubTypes())
                .judiciarySpecialisms(HearingsPanelMapping.getPanelSpecialisms(sscsCaseData, getSessionCaseCodeMap(sscsCaseData, refData)))
                .judiciaryPreferences(getPanelPreferences())
                .build();
    }

    public static List<PanelPreference> getPanelPreferences() {
        //TODO Need to retrieve PanelPreferences from caseData and/or ReferenceData
        return Collections.emptyList();
    }
}
