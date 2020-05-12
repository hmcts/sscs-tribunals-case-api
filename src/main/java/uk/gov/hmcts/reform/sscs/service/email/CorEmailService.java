package uk.gov.hmcts.reform.sscs.service.email;

import static java.util.Arrays.asList;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.domain.email.Email;
import uk.gov.hmcts.reform.sscs.domain.email.EmailAttachment;
import uk.gov.hmcts.reform.sscs.service.EmailService;
import uk.gov.hmcts.reform.sscs.service.pdf.data.UploadedEvidence;

@Slf4j
@Service
public class CorEmailService {
    private final EmailService emailService;
    private final String fromEmailAddress;
    private final String dwpEmailAddress;

    public CorEmailService(
            @Autowired EmailService emailService,
            @Value("${appeal.email.from}") String fromEmailAddress,
            @Value("${appeal.email.dwpEmailAddress}") String dwpEmailAddress
    ) {
        this.emailService = emailService;
        this.fromEmailAddress = fromEmailAddress;
        this.dwpEmailAddress = dwpEmailAddress;
    }

    public void sendFileToDwp(UploadedEvidence pdf, String subject, String message, Long caseId) {
        log.info("Sending email and PDf with subject [" + subject + "] to DWP");
        emailService.sendEmail(caseId, Email.builder()
                .from(fromEmailAddress)
                .to(dwpEmailAddress)
                .subject(subject)
                .message(message)
                .attachments(asList(EmailAttachment.builder()
                        .contentType(pdf.getContentType())
                        .data(pdf.getContent())
                        .filename(pdf.getName())
                        .build()
                ))
                .build());
    }
}
