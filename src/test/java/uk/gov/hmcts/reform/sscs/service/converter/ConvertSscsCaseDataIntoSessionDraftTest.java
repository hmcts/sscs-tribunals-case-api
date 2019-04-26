package uk.gov.hmcts.reform.sscs.service.converter;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.model.draft.SessionDraft;

public class ConvertSscsCaseDataIntoSessionDraftTest {
    @Test(expected = NullPointerException.class)
    public void attemptToConvertNull() {
        new ConvertSscsCaseDataIntoSessionDraft().convert(null);
    }

    @Test(expected = NullPointerException.class)
    public void attemptToConvertNullAppeal() {
        SscsCaseData caseData = SscsCaseData.builder().build();
        new ConvertSscsCaseDataIntoSessionDraft().convert(caseData);
    }

    @Test
    public void convertPopulatedCaseData() {
        SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder()
                    .code("PIP")
                    .description("Personal Independence Payment")
                    .build()
                )
                .appellant(Appellant.builder()
                    .address(Address.builder()
                        .postcode("AP1 14NT")
                        .build()
                    )
                    .build()
                )
                .mrnDetails(MrnDetails.builder()
                    .mrnDate("01-02-2010")
                    .mrnLateReason("Forgot to send it")
                    .build()
                )
                .build()
            )
            .build();

        SessionDraft actual = new ConvertSscsCaseDataIntoSessionDraft().convert(caseData);
        assertEquals("Personal Independence Payment (PIP)", actual.getBenefitType().getBenefitType());
        assertEquals("AP1 14NT", actual.getPostcode().getPostcode());
        assertEquals("yes", actual.getCreateAccount().getCreateAccount());
        assertEquals("yes", actual.getHaveAMrn().getHaveAMrn());
        assertEquals("01", actual.getMrnDate().getMrnDateDetails().getDay());
        assertEquals("02", actual.getMrnDate().getMrnDateDetails().getMonth());
        assertEquals("2010", actual.getMrnDate().getMrnDateDetails().getYear());
        assertEquals("yes", actual.getCheckMrn().getCheckedMrn());
        assertEquals("Forgot to send it", actual.getMrnOverThirteenMonthsLate().getReasonForBeingLate());
    }

    @Test
    public void convertPopulatedCaseDataWithoutMrnDate() {
        SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder()
                    .code("PIP")
                    .description("Personal Independence Payment")
                    .build()
                )
                .appellant(Appellant.builder()
                    .address(Address.builder()
                        .postcode("AP1 14NT")
                        .build()
                    )
                    .build()
                )
                .mrnDetails(MrnDetails.builder()
                    .build()
                )
                .build()
            )
            .build();

        SessionDraft actual = new ConvertSscsCaseDataIntoSessionDraft().convert(caseData);
        assertEquals("Personal Independence Payment (PIP)", actual.getBenefitType().getBenefitType());
        assertEquals("AP1 14NT", actual.getPostcode().getPostcode());
        assertEquals("yes", actual.getCreateAccount().getCreateAccount());
        assertEquals("no", actual.getHaveAMrn().getHaveAMrn());
        assertNull(actual.getMrnDate());
        assertEquals("no", actual.getCheckMrn().getCheckedMrn());
        assertNull(actual.getMrnOverThirteenMonthsLate());
    }
}
