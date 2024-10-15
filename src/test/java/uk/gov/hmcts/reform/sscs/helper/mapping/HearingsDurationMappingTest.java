package uk.gov.hmcts.reform.sscs.helper.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.reference.data.service.HearingDurationsService;
import uk.gov.hmcts.reform.sscs.service.holder.ReferenceDataServiceHolder;

class HearingsDurationMappingTest extends HearingsMappingBase {

    @Mock
    private HearingDurationsService hearingDurations;

    @Mock
    private ReferenceDataServiceHolder refData;

    @DisplayName("When an invalid adjournCaseDuration and adjournCaseDurationUnits is given and overrideDuration "
        + "is not present then override the duration of hearing")
    @ParameterizedTest
    @CsvSource(value = {
        "null,75",
        "0,75",
        "-1, 75"
    }, nullValues = {"null"})
    void getHearingDurationWillNotReturnOverrideDurationWhenPresent(Integer overrideDuration, int expectedResult) {
        given(refData.getHearingDurations()).willReturn(hearingDurations);
        given(hearingDurations.getHearingDurationBenefitIssueCodes(caseData)).willReturn(expectedResult);

        caseData.getAppeal().getHearingOptions().setWantsToAttend("Yes");
        caseData.getSchedulingAndListingFields().getOverrideFields().setDuration(overrideDuration);

        int result = HearingsDurationMapping.getHearingDuration(caseData, refData);

        assertThat(result).isEqualTo(expectedResult);
    }

    @DisplayName("When an invalid adjournCaseDuration and adjournCaseDurationUnits is given and overrideDuration "
        + "is present then override the duration of hearing")
    @Test
    void getHearingDurationWillReturnOverrideDurationWhenPresent() {
        caseData.getAppeal().getHearingOptions().setWantsToAttend("Yes");
        caseData.getSchedulingAndListingFields().getOverrideFields().setDuration(DURATION_FACE_TO_FACE);

        int result = HearingsDurationMapping.getHearingDuration(caseData, refData);

        assertThat(result).isEqualTo(DURATION_FACE_TO_FACE);
    }
}
