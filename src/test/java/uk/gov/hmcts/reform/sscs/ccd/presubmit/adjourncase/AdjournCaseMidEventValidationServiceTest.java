package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateOrPeriod.PROVIDE_DATE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateType.FIRST_AVAILABLE_DATE_AFTER;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import javax.validation.Validation;
import javax.validation.Validator;
import junitparams.JUnitParamsRunner;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseDaysOffset;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateOrPeriod;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Adjournment;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@RunWith(JUnitParamsRunner.class)
public class AdjournCaseMidEventValidationServiceTest {

    private AdjournCaseMidEventValidationService adjournCaseMidEventValidationService;

    protected static Validator validator;

    private SscsCaseData sscsCaseData;

    @BeforeEach
    public void setUp() {
        validator = Validation.byDefaultProvider()
                .configure()
                .messageInterpolator(new ParameterMessageInterpolator())
                .buildValidatorFactory()
                .getValidator();
        adjournCaseMidEventValidationService = new AdjournCaseMidEventValidationService(validator);
        sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("ccdId")
                .directionTypeDl(new DynamicList(DirectionType.APPEAL_TO_PROCEED.toString()))
                .regionalProcessingCenter(RegionalProcessingCenter.builder().name("Birmingham").build())
                .adjournment(Adjournment.builder().build())
                .appeal(Appeal.builder()
                        .appellant(Appellant.builder()
                                .name(Name.builder().firstName("APPELLANT")
                                        .lastName("LastNamE")
                                        .build())
                                .identity(Identity.builder().build())
                                .build())
                        .build()).build();
    }

    @Test
    void givenDirectionsDueDateIsAfterTodayAndDaysOffsetSpecified_ThenDisplayAnError() {
        sscsCaseData.getAdjournment().setAreDirectionsBeingMadeToParties(YES);
        sscsCaseData.getAdjournment().setDirectionsDueDate(LocalDate.now().plusDays(1));
        sscsCaseData.getAdjournment().setDirectionsDueDateDaysOffset(AdjournCaseDaysOffset.FOURTEEN_DAYS);
        LinkedHashSet<String> exception = (LinkedHashSet<String>)  adjournCaseMidEventValidationService.checkDirectionsDueDateInvalid(sscsCaseData);
        assertEquals("Cannot specify both directions due date and directions due days offset", exception.toArray()[0]);
    }

    @Test
    void givenDirectionsDueDateIsAfterTodayAndDaysOffsetSpecifiedButZero_ThenDoNotDisplayAnError() {
        sscsCaseData.getAdjournment().setAreDirectionsBeingMadeToParties(YES);
        sscsCaseData.getAdjournment().setDirectionsDueDate(LocalDate.now().plusDays(1));
        sscsCaseData.getAdjournment().setDirectionsDueDateDaysOffset(AdjournCaseDaysOffset.OTHER);
        assertDoesNotThrow(() -> adjournCaseMidEventValidationService.checkDirectionsDueDateInvalid(sscsCaseData));
    }

    @Test
    void givenDirectionsDueDateIsNotSpecifiedAndDaysOffsetSpecified_ThenDoNotDisplayAnError() {
        sscsCaseData.getAdjournment().setAreDirectionsBeingMadeToParties(YES);
        sscsCaseData.getAdjournment().setDirectionsDueDateDaysOffset(AdjournCaseDaysOffset.FOURTEEN_DAYS);
        assertDoesNotThrow(() -> adjournCaseMidEventValidationService.checkDirectionsDueDateInvalid(sscsCaseData));
    }

    @Test
    void givenNeitherDirectionsDueDateOrOffsetSpecified_ThenDisplayAnError() {
        sscsCaseData.getAdjournment().setAreDirectionsBeingMadeToParties(YES);
        LinkedHashSet<String> exception = (LinkedHashSet<String>) adjournCaseMidEventValidationService.checkDirectionsDueDateInvalid(sscsCaseData);
        assertEquals("At least one of directions due date or directions due date offset must be specified", exception.toArray()[0]);

    }

    @Test
    void givenDirectionsDueDateIsAfterToday_ThenDoNotDisplayAnError() {
        sscsCaseData.getAdjournment().setAreDirectionsBeingMadeToParties(NO);
        sscsCaseData.getAdjournment().setDirectionsDueDate(LocalDate.now().plusDays(1));
        assertDoesNotThrow(() -> adjournCaseMidEventValidationService.checkDirectionsDueDateInvalid(sscsCaseData));
    }

    @Test
    void givenNextHearingDateRequiredForDateOrPeriod_ThenReturnCorrectBoolean() {
        sscsCaseData.getAdjournment().setNextHearingDateOrPeriod(AdjournCaseNextHearingDateOrPeriod.PROVIDE_DATE);
        assertTrue(adjournCaseMidEventValidationService.adjournCaseNextHearingDateOrPeriodIsProvideDate(sscsCaseData));
        sscsCaseData.getAdjournment().setNextHearingDateOrPeriod(AdjournCaseNextHearingDateOrPeriod.PROVIDE_PERIOD);
        assertFalse(adjournCaseMidEventValidationService.adjournCaseNextHearingDateOrPeriodIsProvideDate(sscsCaseData));
    }

    @Test
    void givenNextHearingDateType_ThenReturnCorrectBoolean() {
        sscsCaseData.getAdjournment().setNextHearingDateType(AdjournCaseNextHearingDateType.FIRST_AVAILABLE_DATE_AFTER);
        assertTrue(adjournCaseMidEventValidationService.adjournCaseNextHearingDateTypeIsFirstAvailableDateAfter(sscsCaseData));
        sscsCaseData.getAdjournment().setNextHearingDateType(AdjournCaseNextHearingDateType.DATE_TO_BE_FIXED);
        assertFalse(adjournCaseMidEventValidationService.adjournCaseNextHearingDateOrPeriodIsProvideDate(sscsCaseData));
        sscsCaseData.getAdjournment().setNextHearingDateType(AdjournCaseNextHearingDateType.FIRST_AVAILABLE_DATE);
        assertFalse(adjournCaseMidEventValidationService.adjournCaseNextHearingDateOrPeriodIsProvideDate(sscsCaseData));
    }

    @Test
    void givenNextHearingDateRequiredForDateOrPeriodAndNextHearingDateNotSet_ThenDisplayAnError() {
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        sscsCaseData.getAdjournment().setNextHearingDateOrPeriod(PROVIDE_DATE);

        IllegalStateException exception = assertThrows(IllegalStateException.class,() -> adjournCaseMidEventValidationService.isNextHearingFirstAvailableDateAfterDateInvalid(sscsCaseData));
        assertEquals("'First available date after' date must be provided", exception.getMessage());
    }

    @Test
    void givenIsNextHearingFirstAvailableDateAfterDateInvalid_ThenReturnTrue() {
        sscsCaseData.getAdjournment().setNextHearingFirstAvailableDateAfterDate(LocalDate.now().minusDays(1));
        assertTrue(adjournCaseMidEventValidationService.isNextHearingFirstAvailableDateAfterDateInvalid(sscsCaseData));
    }

    @Test
    void givenIsNextHearingFirstAvailableDateAfterDateIsValid_ThenReturnFalse() {
        sscsCaseData.getAdjournment().setNextHearingFirstAvailableDateAfterDate(LocalDate.now().plusDays(1));
        assertFalse(adjournCaseMidEventValidationService.isNextHearingFirstAvailableDateAfterDateInvalid(sscsCaseData));
    }

}
