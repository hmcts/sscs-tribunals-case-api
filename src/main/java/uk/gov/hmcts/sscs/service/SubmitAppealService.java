package uk.gov.hmcts.sscs.service;

import static com.google.common.collect.Lists.newArrayList;
import static uk.gov.hmcts.sscs.email.EmailAttachment.json;
import static uk.gov.hmcts.sscs.email.EmailAttachment.pdf;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import uk.gov.hmcts.sscs.email.RoboticsEmailTemplate;
import uk.gov.hmcts.sscs.email.SubmitYourAppealEmailTemplate;
import uk.gov.hmcts.sscs.exception.CcdException;
import uk.gov.hmcts.sscs.exception.PdfGenerationException;
import uk.gov.hmcts.sscs.model.ccd.CaseData;
import uk.gov.hmcts.sscs.model.ccd.SscsDocument;
import uk.gov.hmcts.sscs.model.ccd.Subscription;
import uk.gov.hmcts.sscs.model.pdf.PdfWrapper;
import uk.gov.hmcts.sscs.model.robotics.RoboticsWrapper;
import uk.gov.hmcts.sscs.model.tya.RegionalProcessingCenter;
import uk.gov.hmcts.sscs.service.referencedata.RegionalProcessingCenterService;
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
    private final SubmitYourAppealEmailTemplate submitYourAppealEmailTemplate;
    private final RoboticsEmailTemplate roboticsEmailTemplate;
    private final AirLookupService airLookupService;
    private final PdfStoreService pdfStoreService;
    private final Boolean roboticsEnabled;
    private final RegionalProcessingCenterService regionalProcessingCenterService;

    @Autowired
    SubmitAppealService(@Value("${appellant.appeal.html.template.path}") String appellantTemplatePath,
                        AppealNumberGenerator appealNumberGenerator,
                        SubmitYourAppealToCcdCaseDataDeserializer submitYourAppealToCcdCaseDataDeserializer,
                        CcdService ccdService,
                        PDFServiceClient pdfServiceClient,
                        EmailService emailService,
                        RoboticsService roboticsService,
                        SubmitYourAppealEmailTemplate submitYourAppealEmailTemplate,
                        RoboticsEmailTemplate roboticsEmailTemplate,
                        AirLookupService airLookupService,
                        RegionalProcessingCenterService regionalProcessingCenterService,
                        PdfStoreService pdfStoreService,
                        @Value("${robotics.email.enabled}") Boolean roboticsEnabled) {

        this.appellantTemplatePath = appellantTemplatePath;
        this.appealNumberGenerator = appealNumberGenerator;
        this.submitYourAppealToCcdCaseDataDeserializer = submitYourAppealToCcdCaseDataDeserializer;
        this.ccdService = ccdService;
        this.pdfServiceClient = pdfServiceClient;
        this.emailService = emailService;
        this.roboticsService = roboticsService;
        this.submitYourAppealEmailTemplate = submitYourAppealEmailTemplate;
        this.roboticsEmailTemplate = roboticsEmailTemplate;
        this.airLookupService = airLookupService;
        this.regionalProcessingCenterService = regionalProcessingCenterService;
        this.pdfStoreService = pdfStoreService;
        this.roboticsEnabled = roboticsEnabled;
    }

    public void submitAppeal(SyaCaseWrapper appeal) {
        //add RPC and venue
        String postcode = getFirstHalfOfPostcode(appeal.getAppellant().getContactDetails().getPostCode());
        String region = airLookupService.lookupRegionalCentre(postcode);
        String venue = airLookupService.lookupAirVenueNameByPostCode(postcode);
        RegionalProcessingCenter rpc = regionalProcessingCenterService.getByName(region);

        CaseData caseData;
        if (rpc == null) {
            caseData = transformAppealToCaseData(appeal);
        } else {
            caseData = transformAppealToCaseData(appeal, rpc.getName(), rpc);
        }

        CaseDetails caseDetails = createCaseInCcd(caseData);

        log.info("Appeal successfully created in CCD for  Nino - {} and benefit type {}",
                appeal.getAppellant().getNino(), appeal.getBenefitType().getCode());

        byte[] pdf = generatePdf(appeal, caseDetails.getId(), caseData);

        String fileName = generateUniqueEmailId(appeal.getAppellant()) + ".pdf";
        List<SscsDocument> pdfDocuments = pdfStoreService.store(pdf, fileName);

        log.info("Appeal PDF stored in DM for  Nino - {} and benefit type {}", appeal.getAppellant().getNino(),
                appeal.getBenefitType().getCode());

        List<SscsDocument> allDocuments = combineEvidenceAndAppealPdf(caseData, pdfDocuments);

        CaseData caseDataWithAppealPdf = caseData.toBuilder().sscsDocument(allDocuments).build();
        updateCaseInCcd(caseDataWithAppealPdf, caseDetails.getId(), "uploadDocument");

        sendPdfByEmail(appeal.getAppellant(), pdf);

        log.info("PDF email sent successfully for  Nino - {} and benefit type {}", appeal.getAppellant().getNino(),
                appeal.getBenefitType().getCode());

        if (roboticsEnabled) {
            JSONObject roboticsJson = roboticsService.createRobotics(RoboticsWrapper.builder().syaCaseWrapper(appeal)
                    .ccdCaseId(caseDetails.getId()).venueName(venue).build());

            sendJsonByEmail(appeal.getAppellant(), roboticsJson, pdf);
            log.info("Robotics email sent successfully for  Nino - {} and benefit type {}", appeal.getAppellant().getNino(),
                    appeal.getBenefitType().getCode());
        }
    }

    private List<SscsDocument> combineEvidenceAndAppealPdf(CaseData caseData, List<SscsDocument> pdfDocuments) {
        List<SscsDocument> evidenceDocuments = caseData.getSscsDocument();
        List<SscsDocument> allDocuments = new ArrayList<>();
        if (evidenceDocuments != null) {
            allDocuments.addAll(evidenceDocuments);
        }
        allDocuments.addAll(pdfDocuments);
        return allDocuments;
    }


    protected String getFirstHalfOfPostcode(String postcode) {
        if (postcode != null && postcode.length() > 3) {
            return postcode.substring(0, postcode.length() - 3).trim();
        }
        return "";
    }

    private CaseDetails createCaseInCcd(CaseData caseData) {
        try {
            return ccdService.createCase(caseData);
        } catch (CcdException ccdEx) {
            log.error("Failed to create ccd case for Nino - {} and Benefit type - {} but carrying on ",
                    caseData.getGeneratedNino(), caseData.getAppeal().getBenefitType().getCode(), ccdEx);
            return CaseDetails.builder().build();
        }
    }

    private CaseDetails updateCaseInCcd(CaseData caseData, Long caseId, String eventId) {
        try {
            return ccdService.updateCase(caseData, caseId, eventId);
        } catch (CcdException ccdEx) {
            log.error("Failed to update ccd case but carrying on [" + caseId + "] ["
                    + caseData.getCaseReference() + "] with event [" + eventId + "]", ccdEx);
            return CaseDetails.builder().build();
        }
    }

    protected CaseData transformAppealToCaseData(SyaCaseWrapper appeal) {
        CaseData caseData = submitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData(appeal);

        return sendToCcd(caseData);
    }

    protected CaseData transformAppealToCaseData(SyaCaseWrapper appeal, String region, RegionalProcessingCenter rpc) {

        CaseData caseData = submitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData(appeal, region, rpc);

        return sendToCcd(caseData);
    }

    private CaseData sendToCcd(CaseData caseData) {
        try {
            Subscription subscription = caseData.getSubscriptions().getAppellantSubscription().toBuilder()
                    .tya(appealNumberGenerator.generate())
                    .build();
            caseData.getSubscriptions().setAppellantSubscription(subscription);
            return caseData;
        } catch (CcdException e) {
            log.error("Appeal number is not generated for Nino - {} and Benefit Type - {}",
                    caseData.getGeneratedNino(), caseData.getAppeal().getBenefitType().getCode(), e);
            return caseData;
        }
    }

    private void sendPdfByEmail(SyaAppellant appeal, byte[] pdf) {
        String appellantUniqueId = generateUniqueEmailId(appeal);
        emailService.sendEmail(submitYourAppealEmailTemplate.generateEmail(
                appellantUniqueId,
                newArrayList(pdf(pdf, appellantUniqueId + ".pdf")))
        );
    }

    private void sendJsonByEmail(SyaAppellant appellant, JSONObject json, byte[] pdf) {
        String appellantUniqueId = generateUniqueEmailId(appellant);
        emailService.sendEmail(roboticsEmailTemplate.generateEmail(
                appellantUniqueId,
                newArrayList(json(json.toString().getBytes(), appellantUniqueId + ".txt"),
                        pdf(pdf, appellantUniqueId + ".pdf"))
        ));
    }

    private String generateUniqueEmailId(SyaAppellant appellant) {
        String appellantLastName = appellant.getLastName();
        String nino = appellant.getNino();
        return String.format(ID_FORMAT, appellantLastName, nino.substring(nino.length() - 3));
    }

    private byte[] generatePdf(SyaCaseWrapper appeal, Long caseDetailsId, CaseData caseData) {
        byte[] template;
        try {
            template = getTemplate();
        } catch (IOException e) {
            throw new PdfGenerationException("Error getting template", e);
        }
        PdfWrapper pdfWrapper = PdfWrapper.builder().syaCaseWrapper(appeal).ccdCaseId(caseDetailsId).caseData(caseData).build();
        Map<String, Object> placeholders = Collections.singletonMap(PDF_WRAPPER, pdfWrapper);
        return pdfServiceClient.generateFromHtml(template, placeholders);
    }

    private byte[] getTemplate() throws IOException {
        InputStream in = getClass().getResourceAsStream(appellantTemplatePath);
        return IOUtils.toByteArray(in);
    }

}
