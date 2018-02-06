package uk.gov.hmcts.sscs.service;

import static com.google.common.collect.Lists.newArrayList;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pdf.service.client.PDFServiceClient;
import uk.gov.hmcts.sscs.domain.wrapper.SyaCaseWrapper;
import static uk.gov.hmcts.sscs.email.EmailAttachment.pdf;
import uk.gov.hmcts.sscs.email.SubmitYourAppealEmail;
import uk.gov.hmcts.sscs.exception.EmailSendFailedException;
import uk.gov.hmcts.sscs.exception.PdfGenerationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

@Service
public class SubmitAppealService {
    private static final Logger LOG = getLogger(SubmitAppealService.class);

    public static final String SYA_CASE_WRAPPER = "SyaCaseWrapper";

    private String appellantTemplatePath;
    private final PDFServiceClient pdfServiceClient;
    private final EmailService emailService;
    private SubmitYourAppealEmail submitYourAppealEmail;

    @Autowired
    public SubmitAppealService(@Value("${appellant.appeal.html.template.path}")
                                           String appellantTemplatePath,
                               PDFServiceClient pdfServiceClient, EmailService emailService,
                               SubmitYourAppealEmail submitYourAppealEmail) {

        this.appellantTemplatePath = appellantTemplatePath;
        this.pdfServiceClient = pdfServiceClient;
        this.emailService = emailService;
        this.submitYourAppealEmail = submitYourAppealEmail;
    }

    public void submitAppeal(SyaCaseWrapper appeal, String appellantUniqueId) {
        try {
            Map<String, Object> placeholders = Collections.singletonMap(SYA_CASE_WRAPPER, appeal);
            byte[] pdf = pdfServiceClient.generateFromHtml(getTemplate(), placeholders);

            submitYourAppealEmail.setAttachments(newArrayList(pdf(pdf,appellantUniqueId + ".pdf")));
            emailService.sendEmail(submitYourAppealEmail);
        } catch (EmailSendFailedException ex) {
            LOG.error("Error while emailing pdf ", ex);
            throw ex;
        } catch (Exception ex) {
            LOG.error("Error while generating pdf ", ex);
            throw new PdfGenerationException(ex.getMessage());
        }
    }

    private byte[] getTemplate() throws IOException {
        InputStream in = getClass().getResourceAsStream(appellantTemplatePath);
        return IOUtils.toByteArray(in);
    }
}
