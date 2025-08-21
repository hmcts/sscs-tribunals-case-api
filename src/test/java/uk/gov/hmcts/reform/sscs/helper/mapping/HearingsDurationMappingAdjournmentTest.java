package uk.gov.hmcts.reform.sscs.helper.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDurationUnits.SESSIONS;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseTypeOfHearing.FACE_TO_FACE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseTypeOfHearing.PAPER;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseTypeOfHearing.VIDEO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDurationType;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDurationUnits;
import uk.gov.hmcts.reform.sscs.ccd.domain.Adjournment;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitCode;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.Issue;
import uk.gov.hmcts.reform.sscs.ccd.domain.OverrideFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.exception.ListingException;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingDuration;
import uk.gov.hmcts.reform.sscs.service.holder.ReferenceDataServiceHolder;

@ExtendWith(MockitoExtension.class)
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
        int expected) throws ListingException {

        setAdjournmentDurationAndUnits(adjournCaseDuration, adjournCaseDurationUnits);
        Integer result = HearingsDurationMapping.getHearingDuration(caseData, refData);

        assertThat(result).isEqualTo(expected);
    }

    @DisplayName("When adjournment flag is enabled but getHearingDurationAdjournment returns null "
        + "getHearingDuration should throw a ListingException")
    @Test
    void getHearingDurationAdjournmentReturnsNullWithFeatureFlagEnabled() throws ListingException {
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
        caseData.getAdjournment().setTypeOfHearing(FACE_TO_FACE);
        caseData.getAdjournment().setTypeOfNextHearing(FACE_TO_FACE);
        caseData.getAdjournment().setInterpreterRequired(NO);

        given(refData.getHearingDurations()).willReturn(hearingDurations);
        given(hearingDurations.getHearingDuration(eq(caseData.getBenefitCode()), eq(caseData.getIssueCode())))
                .willReturn(new HearingDuration());

        given(hearingDurations.getHearingDurationBenefitIssueCodes(caseData)).willReturn(null);
        given(hearingDurations.addExtraTimeIfNeeded(any(), any(), any(), any())).willReturn(null);

        Integer durationAdjourned = HearingsDurationMapping.getHearingDurationAdjournment(caseData, refData.getHearingDurations());
        assertThat(durationAdjourned).isNull();

        assertThrows(ListingException.class, () -> HearingsDurationMapping.getHearingDuration(
                caseData,
                refData
        ));
    }

    @DisplayName("When a valid duration is given but adjournCaseDurationUnits is not provided "
        + "getHearingDuration returns the default adjournment duration")
    @Test
    void getHearingDurationWithNullUnits() throws ListingException {

        given(refData.getHearingDurations()).willReturn(hearingDurations);
        setAdjournmentDurationAndUnits(2, null);
        caseData.getSchedulingAndListingFields().getDefaultListingValues().setDuration(null);

        int result = HearingsDurationMapping.getHearingDuration(caseData, refData);

        assertThat(result).isEqualTo(HearingsDurationMappingTest.DURATION_FACE_TO_FACE);
    }

    @DisplayName("When an invalid adjournCaseDuration and valid adjournCaseDurationUnits is given "
        + "getHearingDuration a null pointer exception is thrown")
    @ParameterizedTest
    @CsvSource(value = {
        "null,SESSIONS",
        "null,MINUTES"
    }, nullValues = {"null"})
    void getHearingDurationWithInvalidUnitsThrowsException(
        Integer adjournCaseDuration,
        AdjournCaseNextHearingDurationUnits adjournCaseDurationUnits
    ) {
        setAdjournmentDurationAndUnits(adjournCaseDuration, adjournCaseDurationUnits);
        caseData.getSchedulingAndListingFields().getDefaultListingValues().setDuration(null);
        given(refData.getHearingDurations()).willReturn(hearingDurations);
        given(hearingDurations.getHearingDurationBenefitIssueCodes(caseData)).willReturn(null);

        assertThatThrownBy(() -> HearingsDurationMapping.getHearingDuration(caseData, refData))
            .isInstanceOf(ListingException.class);
    }

    @DisplayName("When getAdjournCaseNextHearingListingDurationType is non standard and  "
        + "nextHearingListingDuration is blank, getHearingDurationAdjournment returns null")
    @Test
    void getHearingDurationAdjournment_nextHearingListingDurationIsBlank() throws ListingException {
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

    @DisplayName("When a adjournment hearing has an interpreter, use interpreter value from hearingDuration")
    @Test
    void getHearingDurationAdjournmentWithInterpreter_ReturnInterpreterValue() throws ListingException {
        Adjournment adjournment = caseData.getAdjournment();
        adjournment.setNextHearingListingDurationType(AdjournCaseNextHearingDurationType.STANDARD);
        adjournment.setInterpreterRequired(YesNo.YES);
        adjournment.setTypeOfNextHearing(FACE_TO_FACE);
        adjournment.setTypeOfHearing(FACE_TO_FACE);

        HearingDuration hearingDuration = new HearingDuration();
        hearingDuration.setDurationInterpreter(90);
        hearingDuration.setDurationFaceToFace(60);

        caseData.getSchedulingAndListingFields().getOverrideFields().setDuration(null);
        given(refData.getHearingDurations()).willReturn(hearingDurations);
        given(hearingDurations.getHearingDuration(eq(caseData.getBenefitCode()), eq(caseData.getIssueCode()))).willReturn(hearingDuration);
        given(hearingDurations.addExtraTimeIfNeeded(any(), any(), any(), any())).willReturn(hearingDuration.getDurationInterpreter());

        Integer result = HearingsDurationMapping.getHearingDurationAdjournment(caseData, refData.getHearingDurations());

        assertThat(result).isEqualTo(90);
    }

    @DisplayName("When a adjournment hearing does not have interpreter, use face to face value from hearingDuration")
    @Test
    void getHearingDurationAdjournmentWithoutInterpreter_ReturnFaceToFaceValue() throws ListingException {
        Adjournment adjournment = caseData.getAdjournment();
        adjournment.setNextHearingListingDurationType(AdjournCaseNextHearingDurationType.STANDARD);
        adjournment.setInterpreterRequired(NO);
        adjournment.setTypeOfNextHearing(FACE_TO_FACE);
        adjournment.setTypeOfHearing(FACE_TO_FACE);
        HearingDuration hearingDuration = new HearingDuration();
        hearingDuration.setDurationInterpreter(90);
        hearingDuration.setDurationFaceToFace(60);
        caseData.getSchedulingAndListingFields().getOverrideFields().setDuration(null);
        given(refData.getHearingDurations()).willReturn(hearingDurations);
        given(hearingDurations.getHearingDuration(eq(caseData.getBenefitCode()), eq(caseData.getIssueCode()))).willReturn(hearingDuration);
        given(hearingDurations.addExtraTimeIfNeeded(any(), any(), any(), any())).willReturn(hearingDuration.getDurationFaceToFace());

        Integer result = HearingsDurationMapping.getHearingDurationAdjournment(caseData, refData.getHearingDurations());

        assertThat(result).isEqualTo(60);
    }

    @DisplayName("When a adjournment hearing is to be a paper hearing, use paper value from hearingDuration")
    @Test
    void getHearingDurationAdjournmentWithPaperHearing_ReturnPaperValue() throws ListingException {
        Adjournment adjournment = caseData.getAdjournment();
        adjournment.setNextHearingListingDurationType(null);
        adjournment.setTypeOfNextHearing(PAPER);
        adjournment.setTypeOfHearing(FACE_TO_FACE);
        adjournment.setInterpreterRequired(NO);
        HearingDuration hearingDuration = new HearingDuration();
        hearingDuration.setDurationPaper(30);
        caseData.getSchedulingAndListingFields().getOverrideFields().setDuration(null);
        given(refData.getHearingDurations()).willReturn(hearingDurations);
        given(hearingDurations.getHearingDuration(eq(caseData.getBenefitCode()), eq(caseData.getIssueCode()))).willReturn(hearingDuration);

        Integer result = HearingsDurationMapping.getHearingDurationAdjournment(caseData, refData.getHearingDurations());

        assertThat(result).isEqualTo(30);
    }

    @DisplayName("When a adjournment hearing is changed from paper to face to face, and there is no value in the config, "
            + "return a listing error")
    @Test
    void getHearingDurationAdjournmentChangedFromPaperToFaceToFaceAndNoValueInConfig_ReturnError() {
        Adjournment adjournment = caseData.getAdjournment();
        adjournment.setNextHearingListingDurationType(AdjournCaseNextHearingDurationType.STANDARD);
        adjournment.setTypeOfNextHearing(PAPER);
        adjournment.setTypeOfHearing(FACE_TO_FACE);
        adjournment.setInterpreterRequired(NO);
        HearingDuration hearingDuration = new HearingDuration();
        hearingDuration.setDurationPaper(null);
        caseData.getSchedulingAndListingFields().getOverrideFields().setDuration(null);
        given(refData.getHearingDurations()).willReturn(hearingDurations);
        given(hearingDurations.getHearingDuration(eq(caseData.getBenefitCode()), eq(caseData.getIssueCode()))).willReturn(hearingDuration);
        assertThatThrownBy(() -> HearingsDurationMapping.getHearingDurationAdjournment(caseData, refData.getHearingDurations()))
                .isInstanceOf(ListingException.class);
    }

    @DisplayName("When a adjournment hearing changes from paper to face to face, use face to face value from hearingDuration")
    @Test
    void getHearingDurationAdjournmentChangedFromPaperToFaceToFace_ReturnFaceToFaceValue() throws ListingException {
        Adjournment adjournment = caseData.getAdjournment();
        adjournment.setNextHearingListingDurationType(AdjournCaseNextHearingDurationType.STANDARD);
        adjournment.setTypeOfNextHearing(PAPER);
        adjournment.setTypeOfHearing(FACE_TO_FACE);
        adjournment.setInterpreterRequired(NO);
        caseData.getSchedulingAndListingFields().getDefaultListingValues().setDuration(30);
        caseData.getSchedulingAndListingFields().getOverrideFields().setDuration(null);
        HearingDuration hearingDuration = new HearingDuration();
        hearingDuration.setDurationPaper(60);
        given(refData.getHearingDurations()).willReturn(hearingDurations);
        given(hearingDurations.getHearingDuration(eq(caseData.getBenefitCode()), eq(caseData.getIssueCode()))).willReturn(hearingDuration);
        Integer result = HearingsDurationMapping.getHearingDurationAdjournment(caseData, refData.getHearingDurations());
        assertThat(result).isEqualTo(60);
    }

    @DisplayName("When a adjournment hearing has an override, and the channel has not changed, use the override and not config value")
    @Test
    void getHearingDurationAdjournmentHasOverride_ReturnOverride() throws ListingException {
        Adjournment adjournment = caseData.getAdjournment();
        adjournment.setNextHearingListingDurationType(AdjournCaseNextHearingDurationType.STANDARD);
        adjournment.setTypeOfNextHearing(FACE_TO_FACE);
        adjournment.setTypeOfHearing(FACE_TO_FACE);
        adjournment.setInterpreterRequired(NO);
        caseData.getSchedulingAndListingFields().getOverrideFields().setDuration(75);
        HearingDuration hearingDuration = new HearingDuration();
        hearingDuration.setDurationFaceToFace(60);
        given(refData.getHearingDurations()).willReturn(hearingDurations);
        given(hearingDurations.getHearingDuration(eq(caseData.getBenefitCode()), eq(caseData.getIssueCode()))).willReturn(hearingDuration);
        Integer result = HearingsDurationMapping.getHearingDurationAdjournment(caseData, refData.getHearingDurations());
        assertThat(result).isEqualTo(75);
    }

    @DisplayName("When a adjournment hearing has an override, and the channel has changed from an in person hearing channel"
            + "to another in person hearing channel, use the override and not config value")
    @Test
    void getHearingDurationAdjournmentHasOverrideAndChannelChangedInPerson_ReturnOverride() throws ListingException {
        Adjournment adjournment = caseData.getAdjournment();
        adjournment.setNextHearingListingDurationType(AdjournCaseNextHearingDurationType.STANDARD);
        adjournment.setTypeOfNextHearing(FACE_TO_FACE);
        adjournment.setTypeOfHearing(VIDEO);
        adjournment.setInterpreterRequired(NO);
        caseData.getSchedulingAndListingFields().getOverrideFields().setDuration(75);
        HearingDuration hearingDuration = new HearingDuration();
        hearingDuration.setDurationFaceToFace(60);
        given(refData.getHearingDurations()).willReturn(hearingDurations);
        given(hearingDurations.getHearingDuration(eq(caseData.getBenefitCode()), eq(caseData.getIssueCode()))).willReturn(hearingDuration);
        Integer result = HearingsDurationMapping.getHearingDurationAdjournment(caseData, refData.getHearingDurations());
        assertThat(result).isEqualTo(75);
    }

}
