package uk.gov.hmcts.reform.sscs.service.email;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.sscs.service.pdf.data.UploadedEvidence.pdf;

import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.domain.email.Email;
import uk.gov.hmcts.reform.sscs.domain.email.EmailAttachment;
import uk.gov.hmcts.reform.sscs.service.EmailService;

public class CorEmailServiceTest {

    private EmailService emailService;
    private String fromEmailAddress;
    private String dwpEmailAddress;
    private String caseReference;
    private String pdfFileName;

    @Before
    public void setUp() {
        emailService = mock(EmailService.class);
        fromEmailAddress = "from@example.com";
        dwpEmailAddress = "to@example.com";
        caseReference = "caseReference";
        pdfFileName = "pdfName.pdf";
    }

    @Test
    public void canSendEmailWithSubjectAndMessage() {
        CorEmailService corEmailService = new CorEmailService(emailService, fromEmailAddress, dwpEmailAddress);
        byte[] pdfContent = {2, 4, 6, 0, 1};
        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder()
                .data(SscsCaseData.builder().caseReference(caseReference).build())
                .build();
        String message = "Some message";
        String subject = "subject";
        corEmailService.sendFileToDwp(pdf(pdfContent, pdfFileName), subject, message, 1L);

        verify(emailService).sendEmail(1L, Email.builder()
                .from(fromEmailAddress)
                .to(dwpEmailAddress)
                .subject(subject)
                .message(message)
                .attachments(singletonList(EmailAttachment.pdf(pdfContent, pdfFileName)))
                .build());
    }
}
