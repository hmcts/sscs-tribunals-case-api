package uk.gov.hmcts.reform.sscs.service.adjourncase;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.HEARING;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.time.LocalDate;
import java.util.List;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseDaysOffset;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateOrPeriod;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateType;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDurationType;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDurationUnits;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingPeriod;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingVenue;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCasePanelMembersExcluded;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseTypeOfHearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.Adjournment;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CollectionItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

public class AdjournCaseServiceTest {

    SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
            .appeal(Appeal.builder().build())
            .state(HEARING)
        .adjournment(Adjournment.builder()
            .generateNotice(YES)
            .typeOfHearing(AdjournCaseTypeOfHearing.VIDEO)
            .canCaseBeListedRightAway(YES)
            .areDirectionsBeingMadeToParties(NO)
            .directionsDueDateDaysOffset(AdjournCaseDaysOffset.FOURTEEN_DAYS)
            .directionsDueDate(LocalDate.now().plusMonths(1))
            .typeOfNextHearing(AdjournCaseTypeOfHearing.FACE_TO_FACE)
            .nextHearingVenue(AdjournCaseNextHearingVenue.SOMEWHERE_ELSE)
            .nextHearingVenueSelected(new DynamicList(new DynamicListItem("",""), List.of(new DynamicListItem("", ""))))
            .panelMembersExcluded(AdjournCasePanelMembersExcluded.NO)
            .nextHearingListingDurationType(AdjournCaseNextHearingDurationType.STANDARD)
            .nextHearingListingDuration(1)
            .nextHearingListingDurationUnits(AdjournCaseNextHearingDurationUnits.SESSIONS)
            .interpreterRequired(NO)
            .interpreterLanguage(new DynamicList("Spanish"))
            .nextHearingDateType(AdjournCaseNextHearingDateType.FIRST_AVAILABLE_DATE_AFTER)
            .nextHearingDateOrPeriod(AdjournCaseNextHearingDateOrPeriod.PROVIDE_PERIOD)
            .nextHearingFirstAvailableDateAfterDate(null)
            .nextHearingFirstAvailableDateAfterPeriod(AdjournCaseNextHearingPeriod.NINETY_DAYS)
            .nextHearingDateOrTime("")
            .reasons(List.of(new CollectionItem<>(null, "")))
            .additionalDirections(List.of(new CollectionItem<>(null, "")))
            .previewDocument(DocumentLink.builder().build())
            .generatedDate(LocalDate.now())
            .adjournmentInProgress(YES)
            .build())
        .build();

    @DisplayName("All adjournment fields are cleared")
    @Test
    public void willRemoveTransientAdjournCaseData_andSetAdjournmentInProgressToNoWhenFeatureFlagIsEnabled() {
        SscsUtil.clearAdjournmentTransientFields(sscsCaseData);
        assertThat(sscsCaseData.getAdjournment()).hasAllNullFieldsOrProperties();
    }
}