package uk.gov.hmcts.reform.sscs.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.helper.SscsHelper.isEmailValid;
import static uk.gov.hmcts.reform.sscs.helper.SscsHelper.updateDirectionDueDateByAnAmountOfDays;

import java.time.LocalDate;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

    @ParameterizedTest
    @CsvSource({
        "email@example.com,true",
        "firstname.lastname@example.com,true",
        "email@subdomain.example.com,true",
        "firstname+lastname@example.com,true",
        "email@123.123.123.123,true",
        "1234567890@example.com,true",
        "email@example-one.com,true",
        "_______@example.com,true",
        "email@example.name,true",
        "email@example.museum,true",
        "email@example.co.jp,true",
        "firstname-lastname@example.com,true",
        "plainaddress,false",
        "#@%^%#$@#$@#.com,false",
        "@example.com,false",
        "Joe Smith <email@example.com>,false",
        "email.example.com,false",
        "email@example@example.com,false",
        ".email@example.com,false",
        "email.@example.com,false",
        "email..email@example.com,false",
        "あいうえお@example.com,false",
        "email@example.com (Joe Smith),false",
        "email@example..com,false",
        "Abc..123@example.com,false",
    })
    public void givenAListOfEmails_ThenVerifyIfEmailIsValidOrNot(String email, boolean isValid) {
        assertThat(isEmailValid(email)).isEqualTo(isValid);
    }
}
