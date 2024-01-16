package uk.gov.hmcts.reform.sscs.util;

import static java.time.LocalDate.now;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.DatedRequestOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.RequestOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@RunWith(JUnitParamsRunner.class)
public class ConfidentialityRequestUtilTest {

    private SscsCaseData sscsCaseData;

    @Before
    public void setup() {
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
                .appeal(Appeal.builder().build())
                .dwpState(DwpState.FE_ACTIONED_NR)
                .interlocReviewState(InterlocReviewState.REVIEW_BY_JUDGE)
                .build();
    }

    @Test
    public void appellantRequestInProgressReturnsTrue() {
        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));

        assertThat(ConfidentialityRequestUtil.isAtLeastOneRequestInProgress(sscsCaseData), is(true));
    }

    @Test
    public void jointPartyRequestInProgressReturnsTrue() {
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));

        assertThat(ConfidentialityRequestUtil.isAtLeastOneRequestInProgress(sscsCaseData), is(true));
    }

    @Test
    @Parameters({"GRANTED", "null"})
    public void noRequestInProgressReturnsFalse(@Nullable RequestOutcome outcome) {
        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(outcome));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(outcome));

        assertThat(ConfidentialityRequestUtil.isAtLeastOneRequestInProgress(sscsCaseData), is(false));
    }

    private DatedRequestOutcome createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome outcome) {
        return outcome == null ? null :
                DatedRequestOutcome.builder().date(now().minusDays(1)).requestOutcome(outcome).build();
    }
}
