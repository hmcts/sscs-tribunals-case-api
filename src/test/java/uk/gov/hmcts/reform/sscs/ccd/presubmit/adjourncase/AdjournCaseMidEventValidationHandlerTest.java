package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateOrPeriod.PROVIDE_DATE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateOrPeriod.PROVIDE_PERIOD;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateType.FIRST_AVAILABLE_DATE_AFTER;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.time.LocalDate;
import javax.validation.Validation;
import javax.validation.Validator;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseDaysOffset;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@ExtendWith(MockitoExtension.class)
class AdjournCaseMidEventValidationHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private AdjournCaseMidEventValidationHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private IdamClient idamClient;

    @Mock
    private UserDetails userDetails;

    private SscsCaseData sscsCaseData;

    protected static Validator validator = Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory()
            .getValidator();

    @BeforeEach
    void setUp() {
        handler = new AdjournCaseMidEventValidationHandler(validator);

        sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("ccdId")
            .directionTypeDl(new DynamicList(DirectionType.APPEAL_TO_PROCEED.toString()))
            .regionalProcessingCenter(RegionalProcessingCenter.builder().name("Birmingham").build())
            .appeal(Appeal.builder()
                .appellant(Appellant.builder()
                    .name(Name.builder().firstName("APPELLANT")
                        .lastName("LastNamE")
                        .build())
                    .identity(Identity.builder().build())
                    .build())
                .build()).build();
    }

    @Nested
    class Main {

        @BeforeEach
        void setUp() {
            when(callback.getEvent()).thenReturn(EventType.ADJOURN_CASE);
            when(callback.getCaseDetails()).thenReturn(caseDetails);

            when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        }

        @Test
        void givenDirectionsDueDateIsToday_ThenDisplayAnError() {

            sscsCaseData.getAdjournment().setAreDirectionsBeingMadeToParties(YES);

            sscsCaseData.getAdjournment().setDirectionsDueDate(LocalDate.now());

            when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

            PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

            String error = response.getErrors().stream().findFirst().orElse("");
            assertThat(error).isEqualTo("Directions due date must be in the future");
        }

        @Test
        void givenDirectionsDueDateIsBeforeToday_ThenDisplayAnError() {

            sscsCaseData.getAdjournment().setAreDirectionsBeingMadeToParties(YES);

            sscsCaseData.getAdjournment().setDirectionsDueDate(LocalDate.now().minusDays(1));

            when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

            PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

            String error = response.getErrors().stream().findFirst().orElse("");
            assertThat(error).isEqualTo("Directions due date must be in the future");
        }

        @Test
        void givenDirectionsDueDateIsAfterTodayAndDaysOffsetSpecified_ThenDisplayAnError() {

            sscsCaseData.getAdjournment().setAreDirectionsBeingMadeToParties(YES);

            sscsCaseData.getAdjournment().setDirectionsDueDate(LocalDate.now().plusDays(1));
            sscsCaseData.getAdjournment().setDirectionsDueDateDaysOffset(AdjournCaseDaysOffset.FOURTEEN_DAYS);
            when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

            PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

            String error = response.getErrors().stream().findFirst().orElse("");
            assertThat(error).isEqualTo("Cannot specify both directions due date and directions due days offset");
        }

        @Test
        void givenDirectionsDueDateIsAfterTodayAndDaysOffsetSpecifiedButZero_ThenDoNotDisplayAnError() {

            sscsCaseData.getAdjournment().setAreDirectionsBeingMadeToParties(YES);

            sscsCaseData.getAdjournment().setDirectionsDueDate(LocalDate.now().plusDays(1));
            sscsCaseData.getAdjournment().setDirectionsDueDateDaysOffset(AdjournCaseDaysOffset.OTHER);
            when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

            PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

            assertThat(response.getErrors()).isEmpty();
        }

        @Test
        void givenDirectionsDueDateIsNotSpecifiedAndDaysOffsetSpecified_ThenDoNotDisplayAnError() {

            sscsCaseData.getAdjournment().setAreDirectionsBeingMadeToParties(YES);

            sscsCaseData.getAdjournment().setDirectionsDueDateDaysOffset(AdjournCaseDaysOffset.FOURTEEN_DAYS);
            when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

            PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

            assertThat(response.getErrors()).isEmpty();
        }

        @Test
        void givenNeitherDirectionsDueDateOrOffsetSpecified_ThenDisplayAnError() {

            sscsCaseData.getAdjournment().setAreDirectionsBeingMadeToParties(YES);

            when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

            PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

            String error = response.getErrors().stream().findFirst().orElse("");
            assertThat(error).isEqualTo("At least one of directions due date or directions due date offset must be specified");
        }

        @Test
        void givenNeitherDirectionsDueDateOrOffsetSpecifiedButNoDirectionsToParties_ThenDoNotDisplayAnError() {

            sscsCaseData.getAdjournment().setAreDirectionsBeingMadeToParties(NO);

            when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

            PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

            assertThat(response.getErrors()).isEmpty();
        }

        @Test
        void givenDirectionsDueDateIsAfterToday_ThenDoNotDisplayAnError() {

            sscsCaseData.getAdjournment().setAreDirectionsBeingMadeToParties(NO);

            sscsCaseData.getAdjournment().setDirectionsDueDate(LocalDate.now().plusDays(1));

            when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

            PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

            assertThat(response.getErrors()).isEmpty();
        }

        @Test
        void givenNextHearingDateRequiredForDateOrPeriodAndNextHearingAndDateIsAfterToday_ThenDoNotDisplayAnError() {

            sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
            sscsCaseData.getAdjournment().setNextHearingDateOrPeriod(PROVIDE_DATE);
            sscsCaseData.getAdjournment().setNextHearingFirstAvailableDateAfterDate(LocalDate.now().plusDays(1));

            when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

            PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

            assertThat(response.getErrors()).isEmpty();
        }

        @Test
        void givenNextHearingDateRequiredForDateOrPeriodAndNextHearingAndDateIsToday_ThenDoNotDisplayAnError() {

            sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
            sscsCaseData.getAdjournment().setNextHearingDateOrPeriod(PROVIDE_DATE);
            sscsCaseData.getAdjournment().setNextHearingFirstAvailableDateAfterDate(LocalDate.now());

            when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

            PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

            assertThat(response.getErrors()).isEmpty();
        }

        @Test
        void givenNextHearingDateRequiredForDateOrPeriodAndNextHearingAndDateBeforeToday_ThenDisplayAnError() {

            sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
            sscsCaseData.getAdjournment().setNextHearingDateOrPeriod(PROVIDE_DATE);
            sscsCaseData.getAdjournment().setNextHearingFirstAvailableDateAfterDate(LocalDate.now().minusDays(1));

            when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

            PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

            String error = response.getErrors().stream().findFirst().orElse("");
            assertThat(error).isEqualTo("'First available date after' date cannot be in the past");
        }

        @Test
        void givenNextHearingDateNotRequiredForDateOrPeriodAndNextHearingAndDateBeforeToday_ThenDoNotDisplayAnError() {

            sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
            sscsCaseData.getAdjournment().setNextHearingDateOrPeriod(PROVIDE_PERIOD);
            sscsCaseData.getAdjournment().setNextHearingFirstAvailableDateAfterDate(LocalDate.now().minusDays(1));

            when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

            PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

            assertThat(response.getErrors()).isEmpty();

        }

        @Test
        void givenNextHearingDateRequiredForDateOrPeriodAndNextHearingDateNotSet_ThenDisplayAnError() {

            sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
            sscsCaseData.getAdjournment().setNextHearingDateOrPeriod(PROVIDE_DATE);

            when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

            PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

            String error = response.getErrors().stream().findFirst().orElse("");
            assertThat(error).isEqualTo("'First available date after' date must be provided");

        }

    }

    @Nested
    class Other {

        @Test
        void throwsExceptionIfItCannotHandleTheEvent() {
            when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

            assertThatThrownBy(() -> handler.handle(MID_EVENT, callback, USER_AUTHORISATION))
                .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void givenANonAdjournCaseEvent_thenReturnFalse() {
            when(callback.getEvent()).thenReturn(EventType.WRITE_FINAL_DECISION);
            assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
        }

        @ParameterizedTest
        @EnumSource(value = CallbackType.class, names = {"ABOUT_TO_START", "ABOUT_TO_SUBMIT", "SUBMITTED"})
        void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
            assertThat(handler.canHandle(callbackType, callback)).isFalse();
        }

    }
}
