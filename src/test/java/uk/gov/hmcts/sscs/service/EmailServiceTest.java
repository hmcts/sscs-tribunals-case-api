package uk.gov.hmcts.sscs.service;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.hmcts.sscs.email.Email;
import uk.gov.hmcts.sscs.email.EmailAttachment;
import uk.gov.hmcts.sscs.exception.EmailSendFailedException;

@RunWith(MockitoJUnitRunner.class)
@ContextConfiguration
public class EmailServiceTest {

    private static final String EMAIL_FROM = "no-reply@example.com";
    private static final String EMAIL_TO = "user@example.com";
    private static final String EMAIL_SUBJECT = "My Test Subject";
    private static final String EMAIL_MESSAGE = "My Test Message";

    @InjectMocks
    private EmailService emailService;

    @Mock
    private JavaMailSenderImpl javaMailSender;

    @Mock
    private MimeMessage mimeMessage;

    @Before
    public void beforeEachTest() {
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    public void testSendEmailSuccess() throws MessagingException {
        Email emailData = SampleEmailData.getDefault();
        doNothing().when(javaMailSender).send(any(MimeMessage.class));
        emailService.sendEmail(emailData);
        verify(javaMailSender).send(mimeMessage);
    }

    @Test(expected = RuntimeException.class)
    public void testSendEmailThrowsMailException() throws MessagingException {
        Email emailData = SampleEmailData.getDefault();
        doThrow(mock(MailException.class)).when(javaMailSender).send(any(MimeMessage.class));
        emailService.sendEmail(emailData);
    }

    @Test(expected = EmailSendFailedException.class)
    public void testSendEmailThrowsInvalidArgumentExceptionForInvalidTo() {
        Email emailData = SampleEmailData.getWithToNull();
        emailService.sendEmail(emailData);
    }

    @Test(expected = EmailSendFailedException.class)
    public void testSendEmailThrowsInvalidArgumentExceptionForInvalidSubject() {
        Email emailData = SampleEmailData.getWithSubjectNull();
        emailService.sendEmail(emailData);
    }

    public static class TestEmail extends Email {

        public TestEmail(String from, String to, String subject, String message) {
            this.from = from;
            this.to = to;
            this.subject = subject;
            this.message = message;
        }
    }

    public static class SampleEmailData {

        static Email getDefault() {
            List<EmailAttachment> emailAttachmentList = new ArrayList<>();
            EmailAttachment emailAttachment =
                    EmailAttachment.pdf("hello".getBytes(), "Hello.pdf");
            emailAttachmentList.add(emailAttachment);
            TestEmail testEmail = new TestEmail(EMAIL_FROM, EMAIL_TO, EMAIL_SUBJECT, EMAIL_MESSAGE);
            testEmail.setAttachments(emailAttachmentList);
            return testEmail;
        }

        static Email getWithToNull() {
            return new TestEmail(EMAIL_FROM, null, EMAIL_SUBJECT, EMAIL_MESSAGE);
        }

        static Email getWithSubjectNull() {
            return new TestEmail(EMAIL_FROM, EMAIL_TO, null, EMAIL_MESSAGE);
        }
    }
}
