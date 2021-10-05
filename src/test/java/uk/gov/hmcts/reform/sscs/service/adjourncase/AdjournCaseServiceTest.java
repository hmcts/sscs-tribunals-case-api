package uk.gov.hmcts.reform.sscs.service.adjourncase;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
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
            .build();

    @Test
    public void willRemoveTransientAdjournCaseData() {

        AdjournCaseService.clearTransientFields(sscsCaseData);

        verifyTemporaryAdjournCaseFieldsAreCleared(sscsCaseData);
    }

    private void verifyTemporaryAdjournCaseFieldsAreCleared(SscsCaseData sscsCaseData) {
        assertThat(sscsCaseData.getAdjournCaseDirectionsDueDate(), is(nullValue()));
        assertThat(sscsCaseData.getAdjournCaseGenerateNotice(), is(nullValue()));
        assertThat(sscsCaseData.getAdjournCaseTypeOfHearing(), is(nullValue()));
        assertThat(sscsCaseData.getAdjournCaseCanCaseBeListedRightAway(), is(nullValue()));
        assertThat(sscsCaseData.getAdjournCaseAreDirectionsBeingMadeToParties(), is(nullValue()));
        assertThat(sscsCaseData.getAdjournCaseDirectionsDueDateDaysOffset(), is(nullValue()));
        assertThat(sscsCaseData.getAdjournCaseDirectionsDueDate(), is(nullValue()));
        assertThat(sscsCaseData.getAdjournCaseTypeOfNextHearing(), is(nullValue()));
        assertThat(sscsCaseData.getAdjournCaseNextHearingVenue(), is(nullValue()));
        assertThat(sscsCaseData.getAdjournCaseNextHearingVenueSelected(), is(nullValue()));
        assertThat(sscsCaseData.getAdjournCasePanelMembersExcluded(), is(nullValue()));
        assertThat(sscsCaseData.getAdjournCaseDisabilityQualifiedPanelMemberName(), is(nullValue()));
        assertThat(sscsCaseData.getAdjournCaseMedicallyQualifiedPanelMemberName(), is(nullValue()));
        assertThat(sscsCaseData.getAdjournCaseOtherPanelMemberName(), is(nullValue()));
        assertThat(sscsCaseData.getAdjournCaseNextHearingListingDurationType(), is(nullValue()));
        assertThat(sscsCaseData.getAdjournCaseNextHearingListingDuration(), is(nullValue()));
        assertThat(sscsCaseData.getAdjournCaseNextHearingListingDurationUnits(), is(nullValue()));
        assertThat(sscsCaseData.getAdjournCaseInterpreterRequired(), is(nullValue()));
        assertThat(sscsCaseData.getAdjournCaseInterpreterLanguage(), is(nullValue()));
        assertThat(sscsCaseData.getAdjournCaseNextHearingDateType(), is(nullValue()));
        assertThat(sscsCaseData.getAdjournCaseNextHearingDateOrPeriod(), is(nullValue()));
        assertThat(sscsCaseData.getAdjournCaseNextHearingDateOrTime(), is(nullValue()));
        assertThat(sscsCaseData.getAdjournCaseNextHearingFirstAvailableDateAfterDate(), is(nullValue()));
        assertThat(sscsCaseData.getAdjournCaseNextHearingFirstAvailableDateAfterPeriod(), is(nullValue()));
        assertThat(sscsCaseData.getAdjournCaseTime(), is(nullValue()));
        assertThat(sscsCaseData.getAdjournCaseReasons(), is(nullValue()));
        assertThat(sscsCaseData.getAdjournCaseAdditionalDirections(), is(nullValue()));
    }
}
