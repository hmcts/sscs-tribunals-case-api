package uk.gov.hmcts.sscs.service;

import static com.google.common.collect.Lists.newArrayList;
import static uk.gov.hmcts.sscs.email.EmailAttachment.json;
import static uk.gov.hmcts.sscs.email.EmailAttachment.pdf;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.pdf.service.client.PDFServiceClient;
import uk.gov.hmcts.sscs.domain.wrapper.SyaAppellant;
import uk.gov.hmcts.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.sscs.email.RoboticsEmail;
import uk.gov.hmcts.sscs.email.SubmitYourAppealEmail;
import uk.gov.hmcts.sscs.exception.CcdException;
import uk.gov.hmcts.sscs.exception.PdfGenerationException;
import uk.gov.hmcts.sscs.model.ccd.CaseData;
import uk.gov.hmcts.sscs.model.ccd.Subscription;
import uk.gov.hmcts.sscs.model.pdf.PdfWrapper;
import uk.gov.hmcts.sscs.model.robotics.RoboticsWrapper;
import uk.gov.hmcts.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer;

@Service
@Slf4j
public class SubmitAppealService {

    private static final String PDF_WRAPPER = "PdfWrapper";
    private static final String ID_FORMAT = "%s_%s";

    private final String appellantTemplatePath;
    private final AppealNumberGenerator appealNumberGenerator;
    private final SubmitYourAppealToCcdCaseDataDeserializer submitYourAppealToCcdCaseDataDeserializer;
    private final CcdService ccdService;
    private final PDFServiceClient pdfServiceClient;
    private final EmailService emailService;
    private final RoboticsService roboticsService;
    private final SubmitYourAppealEmail submitYourAppealEmail;
    private final RoboticsEmail roboticsEmail;
    private final Boolean roboticsEnabled;

    @Autowired
    SubmitAppealService(@Value("${appellant.appeal.html.template.path}") String appellantTemplatePath,
                        AppealNumberGenerator appealNumberGenerator,
                        SubmitYourAppealToCcdCaseDataDeserializer submitYourAppealToCcdCaseDataDeserializer,
                        CcdService ccdService,
                        PDFServiceClient pdfServiceClient,
                        EmailService emailService,
                        RoboticsService roboticsService,
                        SubmitYourAppealEmail submitYourAppealEmail,
                        RoboticsEmail roboticsEmail,
                        @Value("${robotics.email.enabled}") Boolean roboticsEnabled) {

        this.appellantTemplatePath = appellantTemplatePath;
        this.appealNumberGenerator = appealNumberGenerator;
        this.submitYourAppealToCcdCaseDataDeserializer = submitYourAppealToCcdCaseDataDeserializer;
        this.ccdService = ccdService;
        this.pdfServiceClient = pdfServiceClient;
        this.emailService = emailService;
        this.roboticsService = roboticsService;
        this.submitYourAppealEmail = submitYourAppealEmail;
        this.roboticsEmail = roboticsEmail;
        this.roboticsEnabled = roboticsEnabled;
    }

    public void submitAppeal(SyaCaseWrapper appeal) {
        CaseData caseData = transformAppealToCaseData(appeal);
        CaseDetails caseDetails = createCaseInCcd(caseData);

        byte[] pdf = generatePdf(appeal, caseDetails.getId());
        sendPdfByEmail(appeal.getAppellant(), pdf);

        if (roboticsEnabled) {
            JSONObject roboticsJson = roboticsService.createRobotics(RoboticsWrapper.builder().syaCaseWrapper(appeal).ccdCaseId(caseDetails.getId()).build());
            sendJsonByEmail(appeal.getAppellant(), roboticsJson);
        }
    }

    private CaseDetails createCaseInCcd(CaseData caseData) {
        try {
            return ccdService.createCase(caseData);
        } catch (CcdException ccdEx) {
            return CaseDetails.builder().build();
        }
    }

    private CaseData transformAppealToCaseData(SyaCaseWrapper appeal) {
        CaseData caseData = submitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData(appeal);
        try {
            Subscription subscription = caseData.getSubscriptions().getAppellantSubscription().toBuilder()
                    .tya(appealNumberGenerator.generate())
                    .build();
            caseData.getSubscriptions().setAppellantSubscription(subscription);
            return caseData;
        } catch (CcdException e) {
            log.info("CCD is down. Therefore the appeal number is not generated");
            return caseData;
        }
    }

    private void sendPdfByEmail(SyaAppellant appeal, byte[] pdf) {
        String appellantUniqueId = generateUniqueEmailId(appeal);
        submitYourAppealEmail.setSubject(appellantUniqueId);
        submitYourAppealEmail.setAttachments(newArrayList(pdf(pdf, appellantUniqueId + ".pdf")));
        emailService.sendEmail(submitYourAppealEmail);
    }

    private void sendJsonByEmail(SyaAppellant appellant, JSONObject json) {
        String appellantUniqueId = generateUniqueEmailId(appellant);
        roboticsEmail.setSubject(appellantUniqueId);
        roboticsEmail.setAttachments(newArrayList(json(json.toString().getBytes(), appellantUniqueId + ".json")));
        emailService.sendEmail(roboticsEmail);
    }

    private String generateUniqueEmailId(SyaAppellant appellant) {
        String appellantLastName = appellant.getLastName();
        String nino = appellant.getNino();
        return String.format(ID_FORMAT, appellantLastName, nino.substring(nino.length() - 3));
    }

    private byte[] generatePdf(SyaCaseWrapper appeal, Long caseDetailsId) {
        byte[] template;
        try {
            template = getTemplate();
        } catch (IOException e) {
            throw new PdfGenerationException("Error getting template", e);
        }
        PdfWrapper pdfWrapper = PdfWrapper.builder().syaCaseWrapper(appeal).ccdCaseId(caseDetailsId).build();
        Map<String, Object> placeholders = Collections.singletonMap(PDF_WRAPPER, pdfWrapper);
        return pdfServiceClient.generateFromHtml(template, placeholders);
    }

    private byte[] getTemplate() throws IOException {
        InputStream in = getClass().getResourceAsStream(appellantTemplatePath);
        return IOUtils.toByteArray(in);
    }
}
