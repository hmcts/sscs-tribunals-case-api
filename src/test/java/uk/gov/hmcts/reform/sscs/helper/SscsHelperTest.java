package uk.gov.hmcts.reform.sscs.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.helper.SscsHelper.updateDirectionDueDateByAnAmountOfDays;

import java.time.LocalDate;
import java.util.Arrays;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@RunWith(JUnitParamsRunner.class)
public class SscsHelperTest {

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").build();
        Appeal appeal = Appeal.builder()
                .benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build()).build();
        sscsCaseData.setAppeal(appeal);
    }

    @Test
    public void givenNoResponseDueDate_WhenOtherPartyIsAdded_ThenSetResponseDueDateTo14DaysInTheFuture() {
        sscsCaseData.setDirectionDueDate(null);
        sscsCaseData.setOtherParties(Arrays.asList(buildOtherParty("No", null), buildOtherParty("Yes", NO)));

        updateDirectionDueDateByAnAmountOfDays(sscsCaseData);
        assertThat(sscsCaseData.getDirectionDueDate()).isEqualTo(LocalDate.now().plusDays(14).toString());

    }

    @Test
    public void givenResponseDueDateIsSet_WhenOtherPartyIsAdded_IfDueDateIsMoreThan14DaysOld_ThenDoNotUpdateDate() {
        sscsCaseData.setDirectionDueDate(LocalDate.now().plusDays(21).toString());
        sscsCaseData.setOtherParties(Arrays.asList(buildOtherParty("No", null), buildOtherParty("Yes", NO)));

        updateDirectionDueDateByAnAmountOfDays(sscsCaseData);
        assertThat(sscsCaseData.getDirectionDueDate()).isEqualTo(LocalDate.now().plusDays(21).toString());
    }

    @Test
    public void givenResponseDueDateIsSet_WhenOtherPartyIsAdded_IfDueDateIsNotMoreThan14DaysOld_ThenReSetDueDateTo14DaysInTheFuture() {
        sscsCaseData.setDirectionDueDate(LocalDate.now().plusDays(2).toString());
        sscsCaseData.setOtherParties(Arrays.asList(buildOtherParty("No", null), buildOtherParty("Yes", NO)));

        updateDirectionDueDateByAnAmountOfDays(sscsCaseData);
        assertThat(sscsCaseData.getDirectionDueDate()).isEqualTo(LocalDate.now().plusDays(14).toString());
    }

    @Test
    public void givenResponseDueDateIsSet_IfDueDateIsMoreThan14DaysOld_ThenDoNotUpdateDate() {
        sscsCaseData.setDirectionDueDate(LocalDate.now().plusDays(21).toString());

        updateDirectionDueDateByAnAmountOfDays(sscsCaseData);
        assertThat(sscsCaseData.getDirectionDueDate()).isEqualTo(LocalDate.now().plusDays(21).toString());
    }

    @Test
    public void givenResponseDueDateIsSet_IfDueDateIsNotMoreThan14DaysOld_ThenReSetDueDateTo14DaysInTheFuture() {
        sscsCaseData.setDirectionDueDate(LocalDate.now().plusDays(2).toString());

        updateDirectionDueDateByAnAmountOfDays(sscsCaseData);
        assertThat(sscsCaseData.getDirectionDueDate()).isEqualTo(LocalDate.now().plusDays(14).toString());
    }

    private CcdValue<OtherParty> buildOtherParty(String wantsToAttend, YesNo confidentiality) {
        return CcdValue.<OtherParty>builder().value(OtherParty.builder()
                .sendNewOtherPartyNotification(YES)
                .confidentialityRequired(confidentiality != null ? confidentiality : NO)
                .hearingOptions(HearingOptions.builder().wantsToAttend(wantsToAttend).build())
                .build()).build();
    }
}
