package uk.gov.hmcts.reform.sscs.helper.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDurationType;
import uk.gov.hmcts.reform.sscs.ccd.domain.OverrideFields;
import uk.gov.hmcts.reform.sscs.exception.ListingException;
import uk.gov.hmcts.reform.sscs.reference.data.service.HearingDurationsService;
import uk.gov.hmcts.reform.sscs.service.holder.ReferenceDataServiceHolder;

@ExtendWith(MockitoExtension.class)
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
    void getHearingDurationWillNotReturnOverrideDurationWhenPresent(Integer overrideDuration, int expectedResult) throws ListingException {
        given(refData.getHearingDurations()).willReturn(hearingDurations);
        given(hearingDurations.getHearingDurationBenefitIssueCodes(caseData)).willReturn(expectedResult);

        caseData.getAppeal().getHearingOptions().setWantsToAttend("Yes");
        caseData.getSchedulingAndListingFields().getOverrideFields().setDuration(overrideDuration);

        int result = HearingsDurationMapping.getHearingDuration(caseData, refData);

        assertThat(result).isEqualTo(expectedResult);
    }

    @DisplayName("IBC case hearing duration is not set then is set to null")
    @Test
    void testIbcCaseHearingDurationNotSet() throws ListingException {
        caseData.setBenefitCode("093");
        given(refData.getHearingDurations()).willReturn(hearingDurations);
        ListingException exception = assertThrows(ListingException.class, () ->
                HearingsDurationMapping.getHearingDuration(caseData, refData)
        );
        assertThat(exception.getMessage()).isEqualTo("Hearing duration is required to list case");
    }

    @DisplayName("When IBC case hearing duration is not set throw listing exception")
    @Test
    void testIbcCaseHearingDurationAdjournmentNotSet() throws ListingException {
        caseData.setBenefitCode("093");
        given(refData.getHearingDurations()).willReturn(hearingDurations);
        caseData.getAdjournment().setNextHearingListingDurationType(AdjournCaseNextHearingDurationType.NON_STANDARD);
        ListingException exception = assertThrows(ListingException.class, () ->
                HearingsDurationMapping.getHearingDurationAdjournment(caseData, refData.getHearingDurations())
        );
        assertThat(exception.getMessage()).isEqualTo("Hearing duration is required to list case");
    }

    @DisplayName("When hearing duration is not set throw listing exception")
    @Test
    void testCaseHearingDurationNotSet() throws ListingException {
        given(refData.getHearingDurations()).willReturn(hearingDurations);
        given(hearingDurations.getHearingDurationBenefitIssueCodes(eq(caseData))).willReturn(null);

        ListingException exception = assertThrows(ListingException.class, () ->
                HearingsDurationMapping.getHearingDuration(caseData, refData)
        );
        assertThat(exception.getMessage()).isEqualTo("Hearing duration is required to list case");
    }

    @DisplayName("When hearing duration is set return the existing duration")
    @Test
    void testCaseHearingDurationSet() throws ListingException {
        given(refData.getHearingDurations()).willReturn(hearingDurations);
        caseData.getSchedulingAndListingFields().setDefaultListingValues(OverrideFields.builder().duration(60).build());
        Integer duration = HearingsDurationMapping.getHearingDuration(caseData, refData);
        assertThat(duration).isEqualTo(60);
    }

    @DisplayName("When an invalid adjournCaseDuration and adjournCaseDurationUnits is given and overrideDuration "
        + "is present then override the duration of hearing")
    @Test
    void getHearingDurationWillReturnOverrideDurationWhenPresent() throws ListingException {
        caseData.getAppeal().getHearingOptions().setWantsToAttend("Yes");
        caseData.getSchedulingAndListingFields().getOverrideFields().setDuration(DURATION_FACE_TO_FACE);

        int result = HearingsDurationMapping.getHearingDuration(caseData, refData);

        assertThat(result).isEqualTo(DURATION_FACE_TO_FACE);
    }
}
