package uk.gov.hmcts.reform.sscs.service.email;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.service.pdf.util.PdfDateUtil;

public class EmailMessageBuilderTest {

    private SscsCaseDetails caseDetails;

    @Before
    public void setUp() {
        caseDetails = SscsCaseDetails.builder()
                .id(12345678L)
                .data(SscsCaseData.builder()
                        .appeal(Appeal.builder()
                                .appellant(Appellant.builder()
                                        .name(Name.builder()
                                                .firstName("Jean")
                                                .lastName("Valjean")
                                                .build())
                                        .identity(Identity.builder().nino("JV123456").build())
                                        .build())
                                .build())
                        .build())
                .build();
    }

    @Test
    public void buildAppellantStatement() {
        String message = new EmailMessageBuilder().getAppellantStatementMessage(caseDetails);

        assertThat(message, is(
                "Additional evidence submitted\n"
                + "\n"
                + "Appeal reference number: 12345678\n"
                + "Appellant name: Jean Valjean\n"
                + "Appellant NINO: JV123456\n"
                + "\n"
                + "The appellant has written a statement online and submitted it to the tribunal. Their statement is attached.\n"
                + "\n"
                + "PIP Benefit Appeals\n"
                + "HMCTS\n"));
    }

    @Test
    public void buildEvidenceSubmitted() {
        String message = new EmailMessageBuilder().getEvidenceSubmittedMessage(caseDetails);
        String submittedDate = PdfDateUtil.reformatDate(LocalDate.now());

        assertThat(message, is(
                "Additional evidence submitted by appellant\n"
                + "\n"
                + "Appeal reference number: 12345678\n"
                + "Appellant name: Jean Valjean\n"
                + "Appellant NINO: JV123456\n"
                + "\n"
                + "Additional evidence was received by the tribunal for the above appeal on "
                + submittedDate + ".\n"
                + "\n"
                + "PIP Benefit Appeals\n"
                + "HMCTS\n"));
    }
}
