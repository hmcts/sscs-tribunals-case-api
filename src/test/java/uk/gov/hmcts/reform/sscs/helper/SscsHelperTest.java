package uk.gov.hmcts.reform.sscs.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.helper.SscsHelper.hasHearingScheduledInTheFuture;
import static uk.gov.hmcts.reform.sscs.helper.SscsHelper.updateDirectionDueDateByAnAmountOfDays;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
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
    public void givenThereAreSomeHearingsInTheFuture_WhenTheHearingDataIsInvalid_ThenReturnFalse() {
        HearingDetails hearingDetails1 = HearingDetails.builder()
                .hearingDate("")
                .start(LocalDateTime.now().plusDays(5))
                .hearingId(String.valueOf(1))
                .venue(Venue.builder().name("Venue 1").build())
                .time("12:00")
                .build();
        Hearing hearing1 = Hearing.builder().value(hearingDetails1).build();

        HearingDetails hearingDetails2 = HearingDetails.builder()
                .hearingDate(LocalDate.now().plusDays(5).toString())
                .start(LocalDateTime.now().plusDays(5))
                .hearingId(String.valueOf(1))
                .venue(Venue.builder().name("").build())
                .time("12:00")
                .build();
        Hearing hearing2 = Hearing.builder().value(hearingDetails2).build();

        HearingDetails hearingDetails3 = HearingDetails.builder()
                .hearingDate(LocalDate.now().plusDays(5).toString())
                .start(LocalDateTime.now().plusDays(5))
                .hearingId(String.valueOf(1))
                .time("12:00")
                .build();
        Hearing hearing3 = Hearing.builder().value(hearingDetails3).build();

        sscsCaseData.setHearings(List.of(hearing1, hearing2, hearing3));
        assertFalse(hasHearingScheduledInTheFuture(sscsCaseData));
    }
}
