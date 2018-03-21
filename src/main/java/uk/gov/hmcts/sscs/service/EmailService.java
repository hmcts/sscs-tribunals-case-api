package uk.gov.hmcts.sscs.service;

import javax.mail.internet.MimeMessage;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.sscs.email.Email;
import uk.gov.hmcts.sscs.email.EmailAttachment;
import uk.gov.hmcts.sscs.exception.EmailSendFailedException;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender javaMailSender;

    @Autowired
    public EmailService(final JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Retryable(value = EmailSendFailedException.class,
            backoff = @Backoff(delay = 100, maxDelay = 500))
    public void sendEmail(final Email email) {
        try {
            final MimeMessage message = javaMailSender.createMimeMessage();
            final MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(message, true);

            mimeMessageHelper.setFrom(email.getFrom());
            mimeMessageHelper.setTo(email.getTo());
            mimeMessageHelper.setSubject(email.getSubject());
            mimeMessageHelper.setText(email.getMessage());

            if (email.hasAttachments()) {
                for (EmailAttachment emailAttachment : email.getAttachments()) {
                    mimeMessageHelper.addAttachment(emailAttachment.getFilename(),
                            emailAttachment.getData(),
                            emailAttachment.getContentType());
                }
            }

            javaMailSender.send(message);
        } catch (Exception e) {
            log.error("Error while sending email" + e);
            throw new EmailSendFailedException(e);
        }
    }
}
