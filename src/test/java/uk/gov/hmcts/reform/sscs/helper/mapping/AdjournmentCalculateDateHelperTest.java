package uk.gov.hmcts.reform.sscs.helper.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.hmcts.reform.sscs.helper.adjournment.AdjournmentCalculateDateHelper.DAYS_TO_ADD_HEARING_WINDOW_TODAY_ADJOURNMENT;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateOrPeriod;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateType;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingPeriod;
import uk.gov.hmcts.reform.sscs.ccd.domain.Adjournment;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.helper.adjournment.AdjournmentCalculateDateHelper;

class AdjournmentCalculateDateHelperTest {

    SscsCaseData caseData;

    @BeforeEach
    void setUp() {
        caseData = SscsCaseData.builder()
            .adjournment(Adjournment.builder()
                .adjournmentInProgress(YesNo.YES)
                .build())
            .build();
    }

    @DisplayName("When case is adjourned with date type 'date to be fixed' "
        + "returns null date and saves 'date to be fixed' to 'Other' field")
    @Test
    void whenCaseIsAdjournedWithDateTypeToBeFixed_returnsNullDate_andSavesDateToBeFixedInOtherField() {
        caseData.getAdjournment().setNextHearingDateType(AdjournCaseNextHearingDateType.DATE_TO_BE_FIXED);

        LocalDate result = AdjournmentCalculateDateHelper.getHearingWindowStart(caseData);

        assertThat(result).isNull();
        // TODO assert Other field contains "Date to be fixed" when implemented SSCS-11224
    }

    @DisplayName("When case is adjourned with date type 'first available date' "
        + "returns correct date and saves time in 'Other' field")
    @Test
    void whenCaseIsAdjournedWithDateTypeFirstAvailableDate_returnsCorrectDate_andSavesTimeInOtherField() {
        caseData.getAdjournment().setNextHearingDateType(AdjournCaseNextHearingDateType.FIRST_AVAILABLE_DATE);

        LocalDate result = AdjournmentCalculateDateHelper.getHearingWindowStart(caseData);

        assertThat(result).isEqualTo(LocalDate.now().plusDays(DAYS_TO_ADD_HEARING_WINDOW_TODAY_ADJOURNMENT));
        // TODO assert Other field contains time details when implemented SSCS-11224
    }

    @DisplayName("When case is adjourned with date type 'first available date after' and 'provide period' "
        + "returns correct date and saves time in 'Other' field")
    @ParameterizedTest
    @CsvSource(value = {
        "NINETY_DAYS,90",
        "FORTY_TWO_DAYS,42",
        "TWENTY_EIGHT_DAYS,28"
    })
    void whenCaseIsAdjournedWithDateTypeFirstAvailableDateAfter_andProvidePeriod_returnsCorrectDate_andSavesTimeInOtherField(
        AdjournCaseNextHearingPeriod period,
        Integer expectedDaysAfter
    ) {
        caseData.getAdjournment().setNextHearingDateType(AdjournCaseNextHearingDateType.FIRST_AVAILABLE_DATE_AFTER);
        caseData.getAdjournment().setNextHearingDateOrPeriod(AdjournCaseNextHearingDateOrPeriod.PROVIDE_PERIOD);
        caseData.getAdjournment().setNextHearingFirstAvailableDateAfterPeriod(period);

        LocalDate result = AdjournmentCalculateDateHelper.getHearingWindowStart(caseData);

        LocalDate expected = LocalDate.now().plusDays(expectedDaysAfter);
        assertThat(result).isEqualTo(expected);
        // TODO assert Other field contains time details when implemented SSCS-11224
    }

    @DisplayName("When case is adjourned with date type 'first available date after' and 'provide date' "
        + "returns correct date and saves time in 'Other' field")
    @Test
    void whenCaseIsAdjournedWithDateTypeFirstAvailableDateAfter_andProvideDate_returnsCorrectDate_andSavesTimeInOtherField() {
        LocalDate target = LocalDate.now().plusDays(50);
        caseData.getAdjournment().setNextHearingDateType(AdjournCaseNextHearingDateType.FIRST_AVAILABLE_DATE_AFTER);
        caseData.getAdjournment().setNextHearingDateOrPeriod(AdjournCaseNextHearingDateOrPeriod.PROVIDE_DATE);
        caseData.getAdjournment().setNextHearingFirstAvailableDateAfterDate(target);

        LocalDate result = AdjournmentCalculateDateHelper.getHearingWindowStart(caseData);

        assertThat(result).isEqualTo(target);
        // TODO assert Other field contains time details when implemented SSCS-11224
    }

    @Nested
    class ErrorCase {
        @DisplayName("When case is adjourned and hearingDateType is invalid, throws exception")
        @Test
        void throwsExceptionWhenInvalidHearingDateType() {
            caseData.getAdjournment().setNextHearingDateType(null);
            assertExceptionWhenInvalidData(caseData, "Unexpected nextHearingDateType");
        }

        @DisplayName("When case is adjourned and nextHearingDateType is first available date after "
            + "and nextHearingDateOrPeriod is invalid, throws exception")
        @Test
        void throwsExceptionWhenInvalidDateOrPeriod() {
            caseData.getAdjournment().setNextHearingDateType(AdjournCaseNextHearingDateType.FIRST_AVAILABLE_DATE_AFTER);
            caseData.getAdjournment().setNextHearingDateOrPeriod(null);
            assertExceptionWhenInvalidData(caseData, "Unexpected nextHearingDateOrPeriod");
        }

        @DisplayName("When case is adjourned and nextHearingDateType is first available date after "
            + "and nextHearingDateOrPeriod is provide period and nextHearingFirstAvailableDateAfterPeriod is null, "
            + "throws exception")
        @Test
        void throwsExceptionWhenInvalidPeriod() {
            caseData.getAdjournment().setNextHearingDateType(AdjournCaseNextHearingDateType.FIRST_AVAILABLE_DATE_AFTER);
            caseData.getAdjournment().setNextHearingDateOrPeriod(AdjournCaseNextHearingDateOrPeriod.PROVIDE_PERIOD);
            caseData.getAdjournment().setNextHearingFirstAvailableDateAfterPeriod(null);
            assertExceptionWhenInvalidData(caseData, "firstAvailableDateAfterPeriod unexpectedly null");
        }

        @DisplayName("When case is adjourned and nextHearingDateType is first available date after "
            + "and nextHearingDateOrPeriod is provide period and nextHearingFirstAvailableDateAfterDate is null, "
            + "throws exception")
        @Test
        void throwsExceptionWhenInvalidDate() {
            caseData.getAdjournment().setNextHearingDateType(AdjournCaseNextHearingDateType.FIRST_AVAILABLE_DATE_AFTER);
            caseData.getAdjournment().setNextHearingDateOrPeriod(AdjournCaseNextHearingDateOrPeriod.PROVIDE_DATE);
            caseData.getAdjournment().setNextHearingFirstAvailableDateAfterDate(null);
            assertExceptionWhenInvalidData(caseData, "firstAvailableDateAfterDate unexpectedly null");
        }

        private void assertExceptionWhenInvalidData(SscsCaseData caseData, String errorMessage) {
            assertThatThrownBy(() -> AdjournmentCalculateDateHelper.getHearingWindowStart(caseData))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(errorMessage + " for case id ");
        }
    }

    @DisplayName("When .. is given getFirstDateTimeMustBe returns the valid LocalDateTime")
    @Test
    void testGetFirstDateTimeMustBe() {
        // TODO Finish Test when method done
        LocalDateTime result = HearingsWindowMapping.getFirstDateTimeMustBe();

        assertThat(result).isNull();
    }
}
