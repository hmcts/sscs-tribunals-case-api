package uk.gov.hmcts.reform.sscs.service.adjourncase;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.HEARING;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;


public class AdjournCaseServiceTest {

    SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
            .appeal(Appeal.builder().build())
            .state(HEARING)
            .adjournCaseGenerateNotice("")
            .adjournCaseTypeOfHearing("")
            .adjournCaseCanCaseBeListedRightAway("")
            .adjournCaseAreDirectionsBeingMadeToParties("")
            .adjournCaseDirectionsDueDateDaysOffset("")
            .adjournCaseDirectionsDueDate("")
            .adjournCaseTypeOfNextHearing("")
            .adjournCaseNextHearingVenue("")
            .adjournCaseNextHearingVenueSelected(new DynamicList(new DynamicListItem("",""), Arrays.asList(new DynamicListItem("", ""))))
            .adjournCasePanelMembersExcluded("")
            .adjournCaseDisabilityQualifiedPanelMemberName("")
            .adjournCaseMedicallyQualifiedPanelMemberName("")
            .adjournCaseOtherPanelMemberName("")
            .adjournCaseNextHearingListingDurationType("")
            .adjournCaseNextHearingListingDuration("")
            .adjournCaseNextHearingListingDurationUnits("")
            .adjournCaseInterpreterRequired("")
            .adjournCaseInterpreterLanguage("")
            .adjournCaseNextHearingDateType("")
            .adjournCaseNextHearingDateOrPeriod("")
            .adjournCaseNextHearingDateOrTime("")
            .adjournCaseNextHearingFirstAvailableDateAfterDate("")
            .adjournCaseNextHearingFirstAvailableDateAfterPeriod("")
            .adjournCaseReasons(List.of(new CollectionItem<>(null, "")))
            .adjournCaseAdditionalDirections(List.of(new CollectionItem<>(null, "")))
            .isAdjournmentInProgress(YesNo.YES)
            .build();

    @Test
    public void willRemoveTransientAdjournCaseData() {

        AdjournCaseService.clearTransientFields(sscsCaseData);

        verifyTemporaryAdjournCaseFieldsAreCleared(sscsCaseData);
    }

    private void verifyTemporaryAdjournCaseFieldsAreCleared(SscsCaseData sscsCaseData) {
        assertThat(sscsCaseData.getAdjournCaseDirectionsDueDate()).isNull();
        assertThat(sscsCaseData.getAdjournCaseGenerateNotice()).isNull();
        assertThat(sscsCaseData.getAdjournCaseTypeOfHearing()).isNull();
        assertThat(sscsCaseData.getAdjournCaseCanCaseBeListedRightAway()).isNull();
        assertThat(sscsCaseData.getAdjournCaseAreDirectionsBeingMadeToParties()).isNull();
        assertThat(sscsCaseData.getAdjournCaseDirectionsDueDateDaysOffset()).isNull();
        assertThat(sscsCaseData.getAdjournCaseDirectionsDueDate()).isNull();
        assertThat(sscsCaseData.getAdjournCaseTypeOfNextHearing()).isNull();
        assertThat(sscsCaseData.getAdjournCaseNextHearingVenue()).isNull();
        assertThat(sscsCaseData.getAdjournCaseNextHearingVenueSelected()).isNull();
        assertThat(sscsCaseData.getAdjournCasePanelMembersExcluded()).isNull();
        assertThat(sscsCaseData.getAdjournCaseDisabilityQualifiedPanelMemberName()).isNull();
        assertThat(sscsCaseData.getAdjournCaseMedicallyQualifiedPanelMemberName()).isNull();
        assertThat(sscsCaseData.getAdjournCaseOtherPanelMemberName()).isNull();
        assertThat(sscsCaseData.getAdjournCaseNextHearingListingDurationType()).isNull();
        assertThat(sscsCaseData.getAdjournCaseNextHearingListingDuration()).isNull();
        assertThat(sscsCaseData.getAdjournCaseNextHearingListingDurationUnits()).isNull();
        assertThat(sscsCaseData.getAdjournCaseInterpreterRequired()).isNull();
        assertThat(sscsCaseData.getAdjournCaseInterpreterLanguage()).isNull();
        assertThat(sscsCaseData.getAdjournCaseNextHearingDateType()).isNull();
        assertThat(sscsCaseData.getAdjournCaseNextHearingDateOrPeriod()).isNull();
        assertThat(sscsCaseData.getAdjournCaseNextHearingDateOrTime()).isNull();
        assertThat(sscsCaseData.getAdjournCaseNextHearingFirstAvailableDateAfterDate()).isNull();
        assertThat(sscsCaseData.getAdjournCaseNextHearingFirstAvailableDateAfterPeriod()).isNull();
        assertThat(sscsCaseData.getAdjournCaseTime()).isNull();
        assertThat(sscsCaseData.getAdjournCaseReasons()).isNull();
        assertThat(sscsCaseData.getAdjournCaseAdditionalDirections()).isNull();
        assertThat(sscsCaseData.getIsAdjournmentInProgress()).isNull();
    }
}
