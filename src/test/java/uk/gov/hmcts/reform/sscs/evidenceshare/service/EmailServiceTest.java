package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import javax.mail.internet.MimeMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import uk.gov.hmcts.reform.sscs.domain.email.Email;
import uk.gov.hmcts.reform.sscs.domain.email.EmailAttachment;
import uk.gov.hmcts.reform.sscs.exception.EmailSendFailedException;

@RunWith(MockitoJUnitRunner.class)
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

    @Before
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

    @Test(expected = RuntimeException.class)
    public void testSendEmailThrowsMailException() {
        Email emailData = SampleEmailData.getDefault();
        doThrow(mock(MailException.class)).when(javaMailSender).send(any(MimeMessage.class));
        emailService.sendEmail(1L, emailData);
    }

    @Test(expected = EmailSendFailedException.class)
    public void testSendEmailThrowsInvalidArgumentExceptionForInvalidTo() {
        Email emailData = SampleEmailData.getWithToNull();
        emailService.sendEmail(1L, emailData);
    }

    @Test(expected = EmailSendFailedException.class)
    public void testSendEmailThrowsInvalidArgumentExceptionForInvalidSubject() {
        Email emailData = SampleEmailData.getWithSubjectNull();
        emailService.sendEmail(1L, emailData);
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
