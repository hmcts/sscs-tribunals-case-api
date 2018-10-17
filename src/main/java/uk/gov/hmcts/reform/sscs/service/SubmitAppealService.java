package uk.gov.hmcts.reform.sscs.service;

import static com.google.common.collect.Lists.newArrayList;
import static uk.gov.hmcts.reform.sscs.domain.email.EmailAttachment.json;
import static uk.gov.hmcts.reform.sscs.domain.email.EmailAttachment.pdf;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.domain.email.RoboticsEmailTemplate;
import uk.gov.hmcts.reform.sscs.domain.robotics.RoboticsWrapper;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.referencedata.RegionalProcessingCenterService;
import uk.gov.hmcts.reform.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer;

@Service
@Slf4j
public class SubmitAppealService {

    private static final String ID_FORMAT = "%s_%s";

    private final AppealNumberGenerator appealNumberGenerator;
    private final SubmitYourAppealToCcdCaseDataDeserializer submitYourAppealToCcdCaseDataDeserializer;
    private final CcdService ccdService;
    private final SscsPdfService sscsPdfService;
    private final EmailService emailService;
    private final RoboticsService roboticsService;
    private final RoboticsEmailTemplate roboticsEmailTemplate;
    private final AirLookupService airLookupService;
    private final RegionalProcessingCenterService regionalProcessingCenterService;
    private final IdamService idamService;

    @Autowired
    SubmitAppealService(AppealNumberGenerator appealNumberGenerator,
                        SubmitYourAppealToCcdCaseDataDeserializer submitYourAppealToCcdCaseDataDeserializer,
                        CcdService ccdService,
                        SscsPdfService sscsPdfService,
                        EmailService emailService,
                        RoboticsService roboticsService,
                        RoboticsEmailTemplate roboticsEmailTemplate,
                        AirLookupService airLookupService,
                        RegionalProcessingCenterService regionalProcessingCenterService,
                        IdamService idamService) {

        this.appealNumberGenerator = appealNumberGenerator;
        this.submitYourAppealToCcdCaseDataDeserializer = submitYourAppealToCcdCaseDataDeserializer;
        this.ccdService = ccdService;
        this.sscsPdfService = sscsPdfService;
        this.emailService = emailService;
        this.roboticsService = roboticsService;
        this.roboticsEmailTemplate = roboticsEmailTemplate;
        this.airLookupService = airLookupService;
        this.regionalProcessingCenterService = regionalProcessingCenterService;
        this.idamService = idamService;
    }

    public void submitAppeal(SyaCaseWrapper appeal) {
        String postcode = getFirstHalfOfPostcode(appeal.getAppellant().getContactDetails().getPostCode());

        SscsCaseData caseData = prepareCaseForCcd(appeal, postcode);
        IdamTokens idamTokens = idamService.getIdamTokens();
        SscsCaseDetails caseDetails = createCaseInCcd(caseData, idamTokens);

        byte[] pdf = sscsPdfService.generateAndSendPdf(caseData, caseDetails.getId(), idamTokens);

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

    private void sendCaseToRobotics(SscsCaseData caseData, Long caseId, String postcode, byte[] pdf) {
        String venue = airLookupService.lookupAirVenueNameByPostCode(postcode);

        JSONObject roboticsJson = roboticsService.createRobotics(RoboticsWrapper.builder().sscsCaseData(caseData)
                .ccdCaseId(caseId).venueName(venue).evidencePresent(caseData.getEvidencePresent()).build());

        sendJsonByEmail(caseData.getAppeal().getAppellant(), roboticsJson, pdf);
        log.info("Robotics email sent successfully for Nino - {} and benefit type {}", caseData.getAppeal().getAppellant().getIdentity().getNino(),
                caseData.getAppeal().getBenefitType().getCode());
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
}
