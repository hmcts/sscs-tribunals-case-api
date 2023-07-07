package uk.gov.hmcts.reform.sscs.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.helper.SscsHelper.updateDirectionDueDateByAnAmountOfDays;
import static uk.gov.hmcts.reform.sscs.helper.SscsHelper.validateHearingOptionsAndExcludeDates;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

public class SscsHelperTest {

    public static final LocalDate NOW = LocalDate.now();
    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("ccdId")
                .otherParties(Arrays.asList(buildOtherParty(), buildOtherParty()))
                .build();
        Appeal appeal = Appeal.builder()
                .benefitType(BenefitType.builder()
                        .code(Benefit.CHILD_SUPPORT.getShortName())
                        .build())
                .build();
        sscsCaseData.setAppeal(appeal);
    }

    private CcdValue<OtherParty> buildOtherParty() {
        return CcdValue.<OtherParty>builder().value(OtherParty.builder()
                .sendNewOtherPartyNotification(YES)
                .confidentialityRequired(NO)
                .hearingOptions(HearingOptions.builder().wantsToAttend(YES.toString()).build())
                .build()).build();
    }

    @Test
    public void givenNoResponseDueDate_WhenOtherPartyIsAdded_ThenSetResponseDueDateTo14DaysInTheFuture() {
        sscsCaseData.setDirectionDueDate(null);

        updateDirectionDueDateByAnAmountOfDays(sscsCaseData);
        assertThat(sscsCaseData.getDirectionDueDate()).isEqualTo(NOW.plusDays(14).toString());
    }

    @Test
    public void givenResponseDueDateIsSet_WhenOtherPartyIsAdded_IfDueDateIsMoreThan14DaysOld_ThenDoNotUpdateDate() {
        sscsCaseData.setDirectionDueDate(NOW.plusDays(21).toString());

        updateDirectionDueDateByAnAmountOfDays(sscsCaseData);
        assertThat(sscsCaseData.getDirectionDueDate()).isEqualTo(NOW.plusDays(21).toString());
    }

    @Test
    public void givenResponseDueDateIsSet_WhenOtherPartyIsAdded_IfDueDateIsNotMoreThan14DaysOld_ThenReSetDueDateTo14DaysInTheFuture() {
        sscsCaseData.setDirectionDueDate(NOW.plusDays(2).toString());

        updateDirectionDueDateByAnAmountOfDays(sscsCaseData);
        assertThat(sscsCaseData.getDirectionDueDate()).isEqualTo(NOW.plusDays(14).toString());
    }

    @Test
    public void givenResponseDueDateIsSet_IfDueDateIsMoreThan14DaysOld_ThenDoNotUpdateDate() {
        sscsCaseData.setDirectionDueDate(NOW.plusDays(21).toString());
        sscsCaseData.setOtherParties(null);

        updateDirectionDueDateByAnAmountOfDays(sscsCaseData);
        assertThat(sscsCaseData.getDirectionDueDate()).isEqualTo(NOW.plusDays(21).toString());
    }

    @Test
    public void givenResponseDueDateIsSet_IfDueDateIsNotMoreThan14DaysOld_ThenReSetDueDateTo14DaysInTheFuture() {
        sscsCaseData.setDirectionDueDate(NOW.plusDays(2).toString());
        sscsCaseData.setOtherParties(null);

        updateDirectionDueDateByAnAmountOfDays(sscsCaseData);
        assertThat(sscsCaseData.getDirectionDueDate()).isEqualTo(NOW.plusDays(14).toString());
    }

    @Test
    public void givenResponseDueDateIsEmpty_WithNoOtherParty_ThenDoNotUpdateDueDate() {
        sscsCaseData.setDirectionDueDate("");
        sscsCaseData.setOtherParties(null);

        updateDirectionDueDateByAnAmountOfDays(sscsCaseData);
        assertThat(sscsCaseData.getDirectionDueDate()).isEqualTo("");
    }

    @Test
    public void givenAnyCaseWhenExcludeDatesAreNotProvided_thenThrowError() {
        CcdValue<OtherParty> otherParty = buildOtherParty();
        sscsCaseData.setOtherParties(List.of(otherParty));

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);
        validateHearingOptionsAndExcludeDates(response, otherParty.getValue().getHearingOptions());

        assertEquals(0, response.getWarnings().size());
        assertEquals(2, response.getErrors().size());

        Iterator<String> iterator = response.getErrors().iterator();
        String error1 = iterator.next();
        String error2 = iterator.next();
        assertEquals("Add a start date for unavailable dates", error1);
        assertEquals("Add an end date for unavailable dates", error2);
    }

    @Test
    public void givenAnyCaseWhenExcludeDatesAreNotEmpty_thenThrowError() {
        CcdValue<OtherParty> otherParty = buildOtherParty();

        ExcludeDate excludeDate1 = ExcludeDate.builder().value(DateRange.builder().start("").end("").build()).build();
        ExcludeDate excludeDate2 = ExcludeDate.builder().value(DateRange.builder().start(null).end(null).build()).build();
        ExcludeDate excludeDate3 = ExcludeDate.builder().value(DateRange.builder().start("2023-01-01").end("2023-01-02").build()).build();

        otherParty.getValue().getHearingOptions().setExcludeDates(List.of(excludeDate1, excludeDate2, excludeDate3));
        sscsCaseData.setOtherParties(List.of(otherParty));

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);
        validateHearingOptionsAndExcludeDates(response, otherParty.getValue().getHearingOptions());

        assertEquals(0, response.getWarnings().size());
        assertEquals(2, response.getErrors().size());

        Iterator<String> iterator = response.getErrors().iterator();
        String error1 = iterator.next();
        String error2 = iterator.next();
        assertEquals("Add a start date for unavailable dates", error1);
        assertEquals("Add an end date for unavailable dates", error2);
    }

    @Test
    public void givenAnyCaseWhenExcludeStartDateIsNotProvided_thenThrowError() {
        CcdValue<OtherParty> otherParty = buildOtherParty();

        ExcludeDate excludeDate1 = ExcludeDate.builder().value(DateRange.builder().start("").end("2023-01-01").build()).build();
        ExcludeDate excludeDate2 = ExcludeDate.builder().value(DateRange.builder().start(null).end("2023-02-01").build()).build();
        ExcludeDate excludeDate3 = ExcludeDate.builder().value(DateRange.builder().start("2023-03-01").end("2023-04-02").build()).build();

        otherParty.getValue().getHearingOptions().setExcludeDates(List.of(excludeDate1, excludeDate2, excludeDate3));
        sscsCaseData.setOtherParties(List.of(otherParty));

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);
        validateHearingOptionsAndExcludeDates(response, otherParty.getValue().getHearingOptions());

        assertEquals(0, response.getWarnings().size());
        assertEquals(1, response.getErrors().size());

        Iterator<String> iterator = response.getErrors().iterator();
        String error1 = iterator.next();
        assertEquals("Add a start date for unavailable dates", error1);
    }

    @Test
    public void givenAnyCaseWhenExcludeEndDateIsNotProvided_thenThrowError() {
        CcdValue<OtherParty> otherParty = buildOtherParty();

        ExcludeDate excludeDate1 = ExcludeDate.builder().value(DateRange.builder().start("2023-01-01").end("").build()).build();
        ExcludeDate excludeDate2 = ExcludeDate.builder().value(DateRange.builder().start("2023-02-01").end(null).build()).build();
        ExcludeDate excludeDate3 = ExcludeDate.builder().value(DateRange.builder().start("2023-03-01").end("2023-04-02").build()).build();

        otherParty.getValue().getHearingOptions().setExcludeDates(List.of(excludeDate1, excludeDate2, excludeDate3));
        sscsCaseData.setOtherParties(List.of(otherParty));

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);
        validateHearingOptionsAndExcludeDates(response, otherParty.getValue().getHearingOptions());

        assertEquals(0, response.getWarnings().size());
        assertEquals(1, response.getErrors().size());

        Iterator<String> iterator = response.getErrors().iterator();
        String error1 = iterator.next();
        assertEquals("Add an end date for unavailable dates", error1);
    }

    @Test
    public void givenAnyCaseWhenExcludeStartDateIsAfterEndDate_thenThrowError() {
        CcdValue<OtherParty> otherParty = buildOtherParty();

        ExcludeDate excludeDate1 = ExcludeDate.builder().value(DateRange.builder().start("2023-01-01").end("2023-01-01").build()).build();
        ExcludeDate excludeDate2 = ExcludeDate.builder().value(DateRange.builder().start("2023-01-02").end("2023-01-01").build()).build();
        ExcludeDate excludeDate3 = ExcludeDate.builder().value(DateRange.builder().start("2023-03-01").end("2023-04-02").build()).build();

        otherParty.getValue().getHearingOptions().setExcludeDates(List.of(excludeDate1, excludeDate2, excludeDate3));
        sscsCaseData.setOtherParties(List.of(otherParty));

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);
        validateHearingOptionsAndExcludeDates(response, otherParty.getValue().getHearingOptions());

        assertEquals(0, response.getWarnings().size());
        assertEquals(1, response.getErrors().size());

        Iterator<String> iterator = response.getErrors().iterator();
        String error1 = iterator.next();
        assertEquals("Start date must be before end date", error1);
    }
}
