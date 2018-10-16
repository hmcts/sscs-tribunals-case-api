package uk.gov.hmcts.reform.sscs.service;

import static com.google.common.collect.Lists.newArrayList;
import static uk.gov.hmcts.reform.sscs.email.EmailAttachment.json;
import static uk.gov.hmcts.reform.sscs.email.EmailAttachment.pdf;

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
import uk.gov.hmcts.reform.pdf.service.client.PDFServiceClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.domain.robotics.RoboticsWrapper;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.email.RoboticsEmailTemplate;
import uk.gov.hmcts.reform.sscs.email.SubmitYourAppealEmailTemplate;
import uk.gov.hmcts.reform.sscs.exception.PdfGenerationException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.pdf.PdfWrapper;
import uk.gov.hmcts.reform.sscs.service.PdfStoreService;
import uk.gov.hmcts.reform.sscs.service.RoboticsService;
import uk.gov.hmcts.reform.sscs.service.referencedata.RegionalProcessingCenterService;
import uk.gov.hmcts.reform.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer;

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
    private final RegionalProcessingCenterService regionalProcessingCenterService;
    private final IdamService idamService;

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
                        IdamService idamService) {

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
        this.idamService = idamService;
    }

    public void submitAppeal(SyaCaseWrapper appeal) {
        String postcode = getFirstHalfOfPostcode(appeal.getAppellant().getContactDetails().getPostCode());

        SscsCaseData caseData = prepareCaseForCcd(appeal, postcode);
        IdamTokens idamTokens = idamService.getIdamTokens();
        SscsCaseDetails caseDetails = createCaseInCcd(caseData, idamTokens);

        byte[] pdf = generatePdf(appeal, caseDetails.getId(), caseData);


        sendPdfByEmail(caseData.getAppeal().getAppellant(), pdf);

        log.info("PDF email sent successfully for Nino - {} and benefit type {}", caseData.getAppeal().getAppellant().getIdentity().getNino(),
                caseData.getAppeal().getBenefitType().getCode());

        prepareCaseForPdf(caseDetails.getId(), caseData, pdf, idamTokens);

        sendCaseToRobotics(caseData, caseDetails.getId(), postcode, pdf);
    }

    private SscsCaseData prepareCaseForCcd(SyaCaseWrapper appeal, String postcode) {
        String region = airLookupService.lookupRegionalCentre(postcode);
        RegionalProcessingCenter rpc = regionalProcessingCenterService.getByName(region);

        if (rpc == null) {
            return transformAppealToCaseData(appeal);
        } else {
            return transformAppealToCaseData(appeal, rpc.getName(), rpc);
        }
    }

    private void prepareCaseForPdf(Long caseId, SscsCaseData caseData, byte[] pdf, IdamTokens idamTokens) {
        String fileName = generateUniqueEmailId(caseData.getAppeal().getAppellant()) + ".pdf";
        List<SscsDocument> pdfDocuments = pdfStoreService.store(pdf, fileName);

        log.info("Appeal PDF stored in DM for Nino - {} and benefit type {}", caseData.getAppeal().getAppellant().getIdentity().getNino(),
                caseData.getAppeal().getBenefitType().getCode());

        if (caseId == null) {
            log.info("caseId is empty - skipping step to update CCD with PDF");
        } else {
            List<SscsDocument> allDocuments = combineEvidenceAndAppealPdf(caseData, pdfDocuments);
            SscsCaseData caseDataWithAppealPdf = caseData.toBuilder().sscsDocument(allDocuments).build();
            updateCaseInCcd(caseDataWithAppealPdf, caseId, "uploadDocument", idamTokens);
        }
    }


    private void sendCaseToRobotics(SscsCaseData caseData, Long caseId, String postcode, byte[] pdf) {
        String venue = airLookupService.lookupAirVenueNameByPostCode(postcode);

        JSONObject roboticsJson = roboticsService.createRobotics(RoboticsWrapper.builder().sscsCaseData(caseData)
                .ccdCaseId(caseId).venueName(venue).evidencePresent(caseData.getEvidencePresent()).build());

        sendJsonByEmail(caseData.getAppeal().getAppellant(), roboticsJson, pdf);
        log.info("Robotics email sent successfully for Nino - {} and benefit type {}", caseData.getAppeal().getAppellant().getIdentity().getNino(),
                caseData.getAppeal().getBenefitType().getCode());
    }

    private List<SscsDocument> combineEvidenceAndAppealPdf(SscsCaseData caseData, List<SscsDocument> pdfDocuments) {
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

    private SscsCaseDetails createCaseInCcd(SscsCaseData caseData, IdamTokens idamTokens) {
        try {
            SscsCaseDetails caseDetails = ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(caseData, idamTokens);
            if (caseDetails == null) {
                caseDetails = ccdService.createCase(caseData, idamTokens);
                log.info("Appeal successfully created in CCD for Nino - {} and benefit type {}",
                        caseData.getGeneratedNino(), caseData.getAppeal().getBenefitType().getCode());
                return caseDetails;
            } else {
                log.info("Duplicate case found for Nino {} and benefit type {} so not creating in CCD", caseData.getGeneratedNino(), caseData.getAppeal().getBenefitType().getCode());
                return caseDetails;
            }
        } catch (CcdException ccdEx) {
            log.error("Failed to create ccd case for Nino - {} and Benefit type - {} but carrying on ",
                    caseData.getGeneratedNino(), caseData.getAppeal().getBenefitType().getCode(), ccdEx);
            return SscsCaseDetails.builder().build();
        }
    }

    private SscsCaseDetails updateCaseInCcd(SscsCaseData caseData, Long caseId, String eventId, IdamTokens idamTokens) {
        try {
            return ccdService.updateCase(caseData, caseId, eventId, "SSCS - appeal updated event", "Updated SSCS", idamTokens);
        } catch (CcdException ccdEx) {
            log.error("Failed to update ccd case but carrying on [" + caseId + "] ["
                    + caseData.getCaseReference() + "] with event [" + eventId + "]", ccdEx);
            return SscsCaseDetails.builder().build();
        }
    }

    protected SscsCaseData transformAppealToCaseData(SyaCaseWrapper appeal) {
        SscsCaseData caseData = submitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData(appeal);

        return updateCaseData(caseData);
    }

    protected SscsCaseData transformAppealToCaseData(SyaCaseWrapper appeal, String region, RegionalProcessingCenter rpc) {

        SscsCaseData caseData = submitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData(appeal, region, rpc);

        return updateCaseData(caseData);
    }

    private SscsCaseData updateCaseData(SscsCaseData caseData) {
        try {
            Subscription subscription = caseData.getSubscriptions().getAppellantSubscription().toBuilder()
                    .tya(appealNumberGenerator.generate())
                    .build();

            caseData.setSubscriptions(caseData.getSubscriptions().toBuilder().appellantSubscription(subscription).build());
            return caseData;
        } catch (CcdException e) {
            log.error("Appeal number is not generated for Nino - {} and Benefit Type - {}",
                    caseData.getGeneratedNino(), caseData.getAppeal().getBenefitType().getCode(), e);
            return caseData;
        }
    }

    private void sendPdfByEmail(Appellant appellant, byte[] pdf) {
        String appellantUniqueId = generateUniqueEmailId(appellant);
        emailService.sendEmail(submitYourAppealEmailTemplate.generateEmail(
                appellantUniqueId,
                newArrayList(pdf(pdf, appellantUniqueId + ".pdf")))
        );
    }

    private void sendJsonByEmail(Appellant appellant, JSONObject json, byte[] pdf) {
        String appellantUniqueId = generateUniqueEmailId(appellant);
        emailService.sendEmail(roboticsEmailTemplate.generateEmail(
                appellantUniqueId,
                newArrayList(json(json.toString().getBytes(), appellantUniqueId + ".txt"),
                        pdf(pdf, appellantUniqueId + ".pdf"))
        ));
    }

    private String generateUniqueEmailId(Appellant appellant) {
        String appellantLastName = appellant.getName().getLastName();
        String nino = appellant.getIdentity().getNino();
        return String.format(ID_FORMAT, appellantLastName, nino.substring(nino.length() - 3));
    }

    private byte[] generatePdf(SyaCaseWrapper appeal, Long caseDetailsId, SscsCaseData caseData) {
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
