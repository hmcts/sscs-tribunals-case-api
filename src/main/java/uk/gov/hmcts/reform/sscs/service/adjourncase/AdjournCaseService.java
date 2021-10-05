package uk.gov.hmcts.reform.sscs.service.adjourncase;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Slf4j
public class AdjournCaseService {

    private AdjournCaseService() {
        // remove default constructor
    }

    public static void clearTransientFields(SscsCaseData sscsCaseData) {
        log.info("Clearing transient adjournment case fields for caseId {}", sscsCaseData.getCcdCaseId());
        sscsCaseData.setAdjournCaseGenerateNotice(null);
        sscsCaseData.setAdjournCaseTypeOfHearing(null);
        sscsCaseData.setAdjournCaseCanCaseBeListedRightAway(null);
        sscsCaseData.setAdjournCaseAreDirectionsBeingMadeToParties(null);
        sscsCaseData.setAdjournCaseDirectionsDueDateDaysOffset(null);
        sscsCaseData.setAdjournCaseDirectionsDueDate(null);
        sscsCaseData.setAdjournCaseTypeOfNextHearing(null);
        sscsCaseData.setAdjournCaseNextHearingVenue(null);
        sscsCaseData.setAdjournCaseNextHearingVenueSelected(null);
        sscsCaseData.setAdjournCasePanelMembersExcluded(null);
        sscsCaseData.setAdjournCaseDisabilityQualifiedPanelMemberName(null);
        sscsCaseData.setAdjournCaseMedicallyQualifiedPanelMemberName(null);
        sscsCaseData.setAdjournCaseOtherPanelMemberName(null);
        sscsCaseData.setAdjournCaseNextHearingListingDurationType(null);
        sscsCaseData.setAdjournCaseNextHearingListingDuration(null);
        sscsCaseData.setAdjournCaseNextHearingListingDurationUnits(null);
        sscsCaseData.setAdjournCaseInterpreterRequired(null);
        sscsCaseData.setAdjournCaseInterpreterLanguage(null);
        sscsCaseData.setAdjournCaseNextHearingDateType(null);
        sscsCaseData.setAdjournCaseNextHearingDateOrPeriod(null);
        sscsCaseData.setAdjournCaseNextHearingDateOrTime(null);
        sscsCaseData.setAdjournCaseNextHearingFirstAvailableDateAfterDate(null);
        sscsCaseData.setAdjournCaseNextHearingFirstAvailableDateAfterPeriod(null);
        sscsCaseData.setAdjournCaseTime(null);
        sscsCaseData.setAdjournCaseReasons(null);
        sscsCaseData.setAdjournCaseAdditionalDirections(null);
    }
}
