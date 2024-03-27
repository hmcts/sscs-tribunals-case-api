package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import javax.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.domain.email.Email;
import uk.gov.hmcts.reform.sscs.domain.email.EmailAttachment;
import uk.gov.hmcts.reform.sscs.exception.EmailSendFailedException;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class EmailService {

    private final EmailSenderProvider emailSenderProvider;

    @Retryable(value = EmailSendFailedException.class,
        backoff = @Backoff(delay = 100, maxDelay = 500))
    public void sendEmail(long caseId, final Email email) {
        try {
            JavaMailSender mailSender = emailSenderProvider.getMailSender();
            final MimeMessage message = mailSender.createMimeMessage();
            final MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(message, true);

            mimeMessageHelper.setFrom(email.getFrom());
            mimeMessageHelper.setTo(email.getTo());
            mimeMessageHelper.setSubject(email.getSubject());
            mimeMessageHelper.setText(email.getMessage());

            long attachmentsSize = 0;
            if (email.hasAttachments()) {
                for (EmailAttachment emailAttachment : email.getAttachments()) {
                    InputStreamSource data = emailAttachment.getData();
                    if (data instanceof ByteArrayResource) {
                        attachmentsSize += ((ByteArrayResource) data).contentLength();
                    } else {
                        log.error("Cannot calculate attachment size as not a ByteArrayResource when expected to be.");
                    }
                    mimeMessageHelper.addAttachment(emailAttachment.getFilename(),
                        emailAttachment.getData(),
                        emailAttachment.getContentType());
                }
            }

            log.info("Case [{}] sending email with subject [{}] of [{}] bytes with [{}] bytes of attachments.",
                caseId, email.getSubject(), message.getSize(), attachmentsSize);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Error while sending email {} ", e.getMessage(), e);
            throw new EmailSendFailedException("Error while sending email", e);
        }
    }
}
