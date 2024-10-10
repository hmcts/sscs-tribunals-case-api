package uk.gov.hmcts.reform.sscs.helper.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDurationUnits.SESSIONS;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingDuration;
import uk.gov.hmcts.reform.sscs.service.holder.ReferenceDataServiceHolder;

class HearingsDurationMappingAdjournmentTest extends HearingsMappingBase {

    @Mock
    private ReferenceDataServiceHolder refData;

    private void setAdjournmentDurationAndUnits(Integer duration, AdjournCaseNextHearingDurationUnits units) {
        caseData.getAdjournment().setNextHearingListingDuration(duration);
        caseData.getAdjournment().setNextHearingListingDurationUnits(units);
    }

    @BeforeEach
    void setUp() {
        caseData.getAdjournment().setNextHearingListingDurationType(AdjournCaseNextHearingDurationType.NON_STANDARD);
        caseData.getAdjournment().setAdjournmentInProgress(YesNo.YES);

        OverrideFields defaultListingValues = OverrideFields.builder()
            .duration(45)
            .build();
        SchedulingAndListingFields slFields = SchedulingAndListingFields.builder()
            .defaultListingValues(defaultListingValues)
            .overrideFields(defaultListingValues)
            .build();

        caseData.setSchedulingAndListingFields(slFields);
    }

    @DisplayName("When a valid adjournCaseDuration and adjournCaseDurationUnits is given "
        + "getHearingDuration returns the correct duration Parameterized Tests")
    @ParameterizedTest
    @CsvSource(value = {
        "120,MINUTES,120",
        "70,MINUTES,70",
        "1,SESSIONS,165",
        "2,SESSIONS,330",
        "3,SESSIONS,495"
    })
    void getHearingDuration(
        Integer adjournCaseDuration,
        AdjournCaseNextHearingDurationUnits adjournCaseDurationUnits,
        int expected) {
        given(refData.isAdjournmentFlagEnabled()).willReturn(true);

        setAdjournmentDurationAndUnits(adjournCaseDuration, adjournCaseDurationUnits);
        Integer result = HearingsDurationMapping.getHearingDuration(caseData, refData);

        assertThat(result).isEqualTo(expected);
    }

    @DisplayName("When adjournment flag is enabled but getHearingDurationAdjournment returns null "
        + "uses default hearing duration")
    @Test
    void getHearingDurationAdjournmentReturnsNullWithFeatureFlagEnabled() {
        OverrideFields defaultListingValues = OverrideFields.builder()
            .duration(null)
            .build();
        SchedulingAndListingFields slFields = SchedulingAndListingFields.builder()
            .defaultListingValues(defaultListingValues)
            .build();

        caseData = SscsCaseData.builder()
            .benefitCode(BENEFIT_CODE)
            .issueCode(ISSUE_CODE)
            .appeal(Appeal.builder()
                .hearingOptions(HearingOptions.builder()
                    .wantsToAttend("Yes")
                    .build())
                .build())
            .schedulingAndListingFields(slFields)
            .build();

        given(refData.getHearingDurations()).willReturn(hearingDurations);

        given(hearingDurations.getHearingDurationBenefitIssueCodes(caseData)).willReturn(null);

        Integer durationAdjourned = HearingsDurationMapping.getHearingDurationAdjournment(caseData, refData.getHearingDurations());
        assertThat(durationAdjourned).isNull();

        Integer result = HearingsDurationMapping.getHearingDuration(
            caseData,
            refData
        );

        assertThat(result).isEqualTo(HearingsDurationMapping.DURATION_DEFAULT);
    }

    @DisplayName("When a valid duration is given but adjournCaseDurationUnits is not provided "
        + "getHearingDuration returns the default adjournment duration")
    @Test
    void getHearingDurationWithNullUnits() {
        given(hearingDurations.getHearingDurationBenefitIssueCodes(caseData)).willReturn(HearingsDurationMappingTest.DURATION_PAPER);

        given(refData.getHearingDurations()).willReturn(hearingDurations);
        setAdjournmentDurationAndUnits(2, null);
        caseData.getSchedulingAndListingFields().getDefaultListingValues().setDuration(null);

        int result = HearingsDurationMapping.getHearingDuration(caseData, refData);

        assertThat(result).isEqualTo(HearingsDurationMappingTest.DURATION_PAPER);
    }

    @DisplayName("When an invalid adjournCaseDuration and valid adjournCaseDurationUnits is given "
        + "getHearingDuration a null pointer exception is thrown")
    @ParameterizedTest
    @CsvSource(value = {
        "null,SESSIONS",
        "0,SESSIONS",
        "null,MINUTES",
        "0,MINUTES"
    }, nullValues = {"null"})
    void getHearingDurationWithInvalidUnitsThrowsException(
        Integer adjournCaseDuration,
        AdjournCaseNextHearingDurationUnits adjournCaseDurationUnits
    ) {
        setAdjournmentDurationAndUnits(adjournCaseDuration, adjournCaseDurationUnits);
        caseData.getSchedulingAndListingFields().getDefaultListingValues().setDuration(null);

        assertThatThrownBy(() -> HearingsDurationMapping.getHearingDuration(caseData, refData))
            .isInstanceOf(NullPointerException.class);
    }

    @DisplayName("When getAdjournCaseNextHearingListingDurationType is non standard and  "
        + "nextHearingListingDuration is blank, getHearingDurationAdjournment returns null")
    @Test
    void getHearingDurationAdjournment_nextHearingListingDurationIsBlank() {
        given(refData.getHearingDurations()).willReturn(hearingDurations);
        given(hearingDurations.getHearingDurationBenefitIssueCodes(caseData)).willReturn(null);

        setAdjournmentDurationAndUnits(null, SESSIONS);

        HearingDuration duration = new HearingDuration();
        duration.setBenefitCode(BenefitCode.PIP_NEW_CLAIM);
        duration.setIssue(Issue.DD);
        List<HearingDuration> durationsList = new ArrayList<>();
        durationsList.add(duration);
        refData.getHearingDurations().setHearingDurations(durationsList);

        Integer result = HearingsDurationMapping.getHearingDurationAdjournment(caseData, refData.getHearingDurations());

        assertThat(result).isNull();
    }

    @DisplayName("When getAdjournCaseNextHearingListingDurationType is standard "
        + "getHearingDurationAdjournment returns existing duration")
    @Test
    void getHearingDurationAdjournment_existingHearingListingDurationTypeIsStandard() {
        given(refData.getHearingDurations()).willReturn(hearingDurations);

        setAdjournmentDurationAndUnits(null, SESSIONS);
        caseData.getAdjournment().setNextHearingListingDurationType(AdjournCaseNextHearingDurationType.STANDARD);

        Adjournment adjournment = caseData.getAdjournment();

        adjournment.setNextHearingListingDurationType(AdjournCaseNextHearingDurationType.STANDARD);
        adjournment.setTypeOfHearing(AdjournCaseTypeOfHearing.PAPER);
        adjournment.setTypeOfNextHearing(AdjournCaseTypeOfHearing.PAPER);

        HearingDuration duration = new HearingDuration();
        duration.setBenefitCode(BenefitCode.PIP_NEW_CLAIM);
        duration.setIssue(Issue.DD);
        List<HearingDuration> durationsList = new ArrayList<>();
        durationsList.add(duration);
        refData.getHearingDurations().setHearingDurations(durationsList);

        Integer result = HearingsDurationMapping.getHearingDurationAdjournment(caseData, refData.getHearingDurations());

        assertThat(result).isEqualTo(45);
    }

    @DisplayName("When typeOfHearing is not equal to nextTypeOfHearing "
        + "getHearingDurationAdjournment returns duration based on benefit code")
    @Test
    void getHearingDurationAdjournment_nextTypeOfHearing() {
        given(refData.getHearingDurations()).willReturn(hearingDurations);

        setAdjournmentDurationAndUnits(null, SESSIONS);

        Adjournment adjournment = caseData.getAdjournment();

        adjournment.setNextHearingListingDurationType(AdjournCaseNextHearingDurationType.STANDARD);
        adjournment.setTypeOfHearing(AdjournCaseTypeOfHearing.PAPER);
        adjournment.setTypeOfNextHearing(AdjournCaseTypeOfHearing.FACE_TO_FACE);

        caseData.getSchedulingAndListingFields().getDefaultListingValues().setDuration(30);
        caseData.getSchedulingAndListingFields().getOverrideFields().setDuration(30);

        HearingDuration duration = new HearingDuration();
        duration.setBenefitCode(BenefitCode.PIP_NEW_CLAIM);
        duration.setIssue(Issue.DD);
        duration.setDurationInterpreter(45);
        duration.setDurationPaper(30);
        duration.setDurationFaceToFace(60);
        List<HearingDuration> durationsList = new ArrayList<>();
        durationsList.add(duration);
        refData.getHearingDurations().setHearingDurations(durationsList);

        given(hearingDurations.getHearingDurationBenefitIssueCodes(caseData)).willReturn(60);

        Integer result = HearingsDurationMapping.getHearingDurationAdjournment(caseData, refData.getHearingDurations());

        assertThat(result).isEqualTo(60);
    }

}
