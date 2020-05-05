package uk.gov.hmcts.reform.sscs.service.email;

import java.time.LocalDate;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.service.pdf.util.PdfDateUtil;

@Service
public class EmailMessageBuilder {
    private static final String HEADER_TEMPLATE = "Appeal reference number: {caseReference}\n"
            + "Appellant name: {firstName} {lastName}\n"
            + "Appellant NINO: {nino}\n";

    private static final String TEMPLATE = HEADER_TEMPLATE
            + "\n"
            + "{message}\n"
            + "\n"
            + "PIP Benefit Appeals\n"
            + "HMCTS\n";

    private static final String TEMPLATE_WITH_HEADING = "{heading}\n"
            + "\n"
            + TEMPLATE;

    public String getAppellantStatementMessage(SscsCaseDetails sscsCaseDetails) {
        return buildMessageWithHeading(
                sscsCaseDetails,
                "The appellant has written a statement online and submitted it to the tribunal. Their statement is attached.",
                "Additional evidence submitted"
        );
    }

    public String getEvidenceSubmittedMessage(SscsCaseDetails sscsCaseDetails) {
        String submittedDate = PdfDateUtil.reformatDate(LocalDate.now());
        return buildMessageWithHeading(sscsCaseDetails, "Additional evidence was received by the tribunal for the above appeal on "
                        + submittedDate + ".",
                "Additional evidence submitted by appellant");
    }

    private String buildMessageWithHeading(SscsCaseDetails caseDetails, String message, String heading) {
        String templateWithHeading = TEMPLATE_WITH_HEADING.replace("{heading}", heading);

        return buildMessageWithTemplate(caseDetails, message, templateWithHeading);
    }

    private String buildMessageWithTemplate(SscsCaseDetails caseDetails, String message, String template) {
        SscsCaseData data = caseDetails.getData();
        Appellant appellant = data.getAppeal().getAppellant();
        Name name = appellant.getName();
        return template.replace("{firstName}", name.getFirstName())
                .replace("{lastName}", name.getLastName())
                .replace("{caseReference}", caseDetails.getId().toString())
                .replace("{nino}", appellant.getIdentity().getNino())
                .replace("{message}", message);
    }
}
