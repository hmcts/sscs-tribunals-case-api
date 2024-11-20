package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import jakarta.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import uk.gov.hmcts.reform.sscs.domain.email.Email;
import uk.gov.hmcts.reform.sscs.domain.email.EmailAttachment;
import uk.gov.hmcts.reform.sscs.exception.EmailSendFailedException;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

    private static final String EMAIL_FROM = "no-reply@example.com";
    private static final String EMAIL_TO = "user@example.com";
    private static final String EMAIL_SUBJECT = "My Test Subject";
    private static final String EMAIL_MESSAGE = "My Test Message";

    private EmailService emailService;

    @Mock
    private JavaMailSenderImpl javaMailSender;

    @Mock
    private MimeMessage mimeMessage;

    @BeforeEach
    public void beforeEachTest() {
        EmailSenderProvider emailSenderProvider = Mockito.mock(EmailSenderProvider.class);
        emailService = new EmailService(emailSenderProvider);
        when(emailSenderProvider.getMailSender()).thenReturn(javaMailSender);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    public void testSendEmailSuccess() {
        Email emailData = SampleEmailData.getDefault();
        doNothing().when(javaMailSender).send(any(MimeMessage.class));
        emailService.sendEmail(1L, emailData);
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    public void testSendEmailSuccessWithAttachmentsWithoutContentType() {
        Email emailData = SampleEmailData.getDefault();
        emailData.getAttachments().add(EmailAttachment.file("SomeFile".getBytes(), "SomeFile.doc"));
        doNothing().when(javaMailSender).send(any(MimeMessage.class));
        emailService.sendEmail(1L, emailData);
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    public void testSendEmailWithNoAttachments() {

        Email testEmail = Email.builder().from(EMAIL_FROM).to(EMAIL_TO).subject(EMAIL_SUBJECT).message(EMAIL_MESSAGE).attachments(emptyList()).build();
        doNothing().when(javaMailSender).send(any(MimeMessage.class));
        emailService.sendEmail(1L, testEmail);
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    public void testSendEmailThrowsMailException() {
        assertThrows(RuntimeException.class, () -> {
            Email emailData = SampleEmailData.getDefault();
            doThrow(mock(MailException.class)).when(javaMailSender).send(any(MimeMessage.class));
            emailService.sendEmail(1L, emailData);
        });
    }

    @Test
    public void testSendEmailThrowsInvalidArgumentExceptionForInvalidTo() {
        assertThrows(EmailSendFailedException.class, () -> {
            Email emailData = SampleEmailData.getWithToNull();
            emailService.sendEmail(1L, emailData);
        });
    }

    @Test
    public void testSendEmailThrowsInvalidArgumentExceptionForInvalidSubject() {
        assertThrows(EmailSendFailedException.class, () -> {
            Email emailData = SampleEmailData.getWithSubjectNull();
            emailService.sendEmail(1L, emailData);
        });
    }

    public static class SampleEmailData {

        static Email getDefault() {
            List<EmailAttachment> emailAttachmentList = new ArrayList<>();
            EmailAttachment emailAttachment =
                    EmailAttachment.pdf("hello".getBytes(), "Hello.pdf");
            emailAttachmentList.add(emailAttachment);
            return Email.builder().from(EMAIL_FROM).to(EMAIL_TO).subject(EMAIL_SUBJECT).message(EMAIL_MESSAGE).attachments(emailAttachmentList).build();
        }

        static Email getWithToNull() {
            return Email.builder().from(EMAIL_FROM).to(null).subject(EMAIL_SUBJECT).message(EMAIL_MESSAGE).attachments(emptyList()).build();
        }

        static Email getWithSubjectNull() {
            return Email.builder().from(EMAIL_FROM).to(EMAIL_TO).subject(null).message(EMAIL_MESSAGE).attachments(emptyList()).build();
        }
    }
}
