package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.domain.email.EmailAttachment.*;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.domain.email.EmailAttachment;
import uk.gov.hmcts.reform.sscs.domain.email.RoboticsEmailTemplate;
import uk.gov.hmcts.reform.sscs.evidenceshare.config.EvidenceShareConfig;
import uk.gov.hmcts.reform.sscs.helper.EmailHelper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.model.dwp.OfficeMapping;
import uk.gov.hmcts.reform.sscs.robotics.RoboticsJsonMapper;
import uk.gov.hmcts.reform.sscs.robotics.RoboticsJsonValidator;
import uk.gov.hmcts.reform.sscs.robotics.RoboticsWrapper;
import uk.gov.hmcts.reform.sscs.service.DwpAddressLookupService;
import uk.gov.hmcts.reform.sscs.service.PdfStoreService;

@Component
@Slf4j
public class RoboticsService {

    private static final String GLASGOW = "GLASGOW";
    private static final String PIP_AE = "DWP PIP (AE)";
    private final PdfStoreService pdfStoreService;
    private final EmailService emailService;
    private final EmailHelper emailHelper;
    private final RoboticsJsonMapper roboticsJsonMapper;
    private final RoboticsJsonValidator roboticsJsonValidator;
    private final RoboticsEmailTemplate roboticsEmailTemplate;
    private final EvidenceShareConfig evidenceShareConfig;
    private final DwpAddressLookupService dwpAddressLookupService;
    private final CcdService ccdService;
    private final IdamService idamService;

    private final int englishRoboticCount;
    private final int scottishRoboticCount;

    private final Random rn;

    @Autowired
    public RoboticsService(
        PdfStoreService pdfStoreService,
        EmailService emailService,
        EmailHelper emailHelper,
        RoboticsJsonMapper roboticsJsonMapper,
        RoboticsJsonValidator roboticsJsonValidator,
        RoboticsEmailTemplate roboticsEmailTemplate,
        EvidenceShareConfig evidenceShareConfig,
        DwpAddressLookupService dwpAddressLookupService,
        CcdService ccdService,
        IdamService idamService,
        @Value("${robotics.englishCount}") int englishRoboticCount,
        @Value("${robotics.scottishCount}") int scottishRoboticCount
    ) {
        this.pdfStoreService = pdfStoreService;
        this.emailService = emailService;
        this.emailHelper = emailHelper;
        this.roboticsJsonMapper = roboticsJsonMapper;
        this.roboticsJsonValidator = roboticsJsonValidator;
        this.roboticsEmailTemplate = roboticsEmailTemplate;
        this.evidenceShareConfig = evidenceShareConfig;
        this.dwpAddressLookupService = dwpAddressLookupService;
        this.ccdService = ccdService;
        this.idamService = idamService;
        this.englishRoboticCount = englishRoboticCount;
        this.scottishRoboticCount = scottishRoboticCount;
        rn = new SecureRandom();
    }

    public JSONObject sendCaseToRobotics(CaseDetails<SscsCaseData> caseDetails) {
        log.info("Case sent to robotics service for case id {} ", caseDetails.getId());

        SscsCaseData caseData = caseDetails.getCaseData();

        updateClosedOffices(caseData);
        log.info("Creating robotics for case id {} ", caseDetails.getId());

        RoboticsWrapper wrapper = RoboticsWrapper
            .builder()
            .sscsCaseData(caseData)
            .ccdCaseId(caseDetails.getId())
            .evidencePresent(caseData.getEvidencePresent())
            .state(caseDetails.getState()).build();

        JSONObject roboticsJson = createRobotics(wrapper);

        log.info("Downloading additional evidence for robotics for case id {} ", caseDetails.getId());
        Map<SscsDocument, byte[]> additionalEvidence = downloadEvidence(caseData, Long.valueOf(caseData.getCcdCaseId()));

        log.info("Downloading SSCS1 for robotics for case id {} ", caseDetails.getId());
        byte[] sscs1Form = downloadSscs1(caseData, Long.valueOf(caseData.getCcdCaseId()));

        sendJsonByEmail(caseDetails.getId(), caseData, roboticsJson, sscs1Form, additionalEvidence);

        log.info("Case {} Robotics JSON successfully sent for benefit type {}", caseDetails.getId(),
            caseData.getAppeal().getBenefitType().getCode());

        return roboticsJson;
    }

    private void updateClosedOffices(SscsCaseData sscsCaseData) {

        boolean issuingOfficeChanged = hasIssuingOfficeChanged(sscsCaseData);
        boolean originatingOfficeChanged = hasOriginatingOfficeChanged(sscsCaseData);
        boolean presentingOfficeChanged = hasPresentingOfficeChanged(sscsCaseData);

        if (issuingOfficeChanged || originatingOfficeChanged || presentingOfficeChanged) {
            log.info("Case {} automatically updating FTA office probably due to office closure", sscsCaseData.getCcdCaseId());
        }
    }

    private boolean hasIssuingOfficeChanged(SscsCaseData sscsCaseData) {
        String issuingOffice = sscsCaseData.getAppeal().getMrnDetails().getDwpIssuingOffice();

        if (issuingOffice != null) {
            Optional<OfficeMapping> dwpIssuingOfficeMapping = dwpAddressLookupService.getDwpMappingByOffice(sscsCaseData.getAppeal().getBenefitType().getCode(), issuingOffice);

            if (dwpIssuingOfficeMapping.isPresent()) {
                if (!dwpIssuingOfficeMapping.get().getMapping().getCcd().equals(issuingOffice)) {
                    sscsCaseData.getAppeal().getMrnDetails().setDwpIssuingOffice(dwpIssuingOfficeMapping.get().getMapping().getCcd());
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasOriginatingOfficeChanged(SscsCaseData sscsCaseData) {
        DynamicList originatingOffice = sscsCaseData.getDwpOriginatingOffice();

        if (originatingOffice != null && originatingOffice.getValue().getCode() != null) {
            Optional<OfficeMapping> dwpOriginatingOfficeMapping = dwpAddressLookupService.getDwpMappingByOffice(sscsCaseData.getAppeal().getBenefitType().getCode(), originatingOffice.getValue().getCode());

            if (dwpOriginatingOfficeMapping.isPresent()) {
                if (!dwpOriginatingOfficeMapping.get().getMapping().getCcd().equals(originatingOffice.getValue().getCode())) {
                    originatingOffice.setValue(new DynamicListItem(dwpOriginatingOfficeMapping.get().getMapping().getCcd(), dwpOriginatingOfficeMapping.get().getMapping().getCcd()));
                    sscsCaseData.setDwpOriginatingOffice(originatingOffice);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasPresentingOfficeChanged(SscsCaseData sscsCaseData) {
        DynamicList presentingOffice = sscsCaseData.getDwpPresentingOffice();

        if (presentingOffice != null && presentingOffice.getValue().getCode() != null) {
            Optional<OfficeMapping> dwpPresentingOfficeMapping = dwpAddressLookupService.getDwpMappingByOffice(sscsCaseData.getAppeal().getBenefitType().getCode(), presentingOffice.getValue().getCode());

            if (dwpPresentingOfficeMapping.isPresent()) {
                if (!dwpPresentingOfficeMapping.get().getMapping().getCcd().equals(presentingOffice.getValue().getCode())) {
                    presentingOffice.setValue(new DynamicListItem(dwpPresentingOfficeMapping.get().getMapping().getCcd(), dwpPresentingOfficeMapping.get().getMapping().getCcd()));
                    sscsCaseData.setDwpPresentingOffice(presentingOffice);
                    return true;
                }
            }
        }
        return false;
    }

    private byte[] downloadSscs1(SscsCaseData sscsCaseData, Long caseId) {
        if (hasEvidence(sscsCaseData)) {
            for (SscsDocument doc : sscsCaseData.getSscsDocument()) {
                if (doc.getValue().getDocumentType() != null && doc.getValue().getDocumentType().equalsIgnoreCase("sscs1")) {
                    return downloadBinary(doc, caseId);
                }
            }
        }
        return null;
    }

    private Map<SscsDocument, byte[]> downloadEvidence(SscsCaseData sscsCaseData, Long caseId) {
        if (hasEvidence(sscsCaseData) && !isEvidenceSentForBulkPrint(sscsCaseData)) {
            Map<SscsDocument, byte[]> map = new LinkedHashMap<>();
            for (SscsDocument doc : sscsCaseData.getSscsDocument()) {
                if (doc.getValue().getDocumentType() == null || doc.getValue().getDocumentType().equalsIgnoreCase("appellantEvidence")) {
                    map.put(doc, downloadBinary(doc, caseId));
                }
            }
            return map;
        } else {
            return Collections.emptyMap();
        }
    }

    private boolean isEvidenceSentForBulkPrint(SscsCaseData caseData) {
        return nonNull(caseData)
            && nonNull(caseData.getAppeal())
            && nonNull(caseData.getAppeal().getReceivedVia())
            && evidenceShareConfig.getSubmitTypes().stream()
            .anyMatch(caseData.getAppeal().getReceivedVia()::equalsIgnoreCase);
    }

    private byte[] downloadBinary(SscsDocument doc, Long caseId) {
        log.info("About to download binary to attach to robotics for caseId {}", caseId);
        if (doc.getValue().getDocumentLink() != null) {
            return pdfStoreService.download(doc.getValue().getDocumentLink().getDocumentUrl());
        } else {
            return new byte[0];
        }
    }

    private boolean hasEvidence(SscsCaseData sscsCaseData) {
        return CollectionUtils.isNotEmpty(sscsCaseData.getSscsDocument());
    }

    private JSONObject createRobotics(RoboticsWrapper appeal) {

        JSONObject roboticsAppeal = roboticsJsonMapper.map(appeal);

        Set<String> errorSet = roboticsJsonValidator.validate(roboticsAppeal, String.valueOf(appeal.getCcdCaseId()));

        if (CollectionUtils.isNotEmpty(errorSet)) {
            appeal.getSscsCaseData().setHmctsDwpState("failedRobotics");
            ccdService.updateCase(appeal.getSscsCaseData(), appeal.getCcdCaseId(),
                EventType.SEND_TO_ROBOTICS_ERROR.getCcdType(), "Flag error to Send to robotics",
                errorSet.stream().collect(Collectors.joining()), idamService.getIdamTokens());
        }
        return roboticsAppeal;
    }

    private void sendJsonByEmail(long caseId, SscsCaseData caseData, JSONObject json, byte[] pdf, Map<SscsDocument, byte[]> additionalEvidence) {
        boolean isScottish = Optional.ofNullable(caseData.getRegionalProcessingCenter()).map(f -> equalsIgnoreCase(f.getName(), GLASGOW)).orElse(false);
        boolean isPipAeTo = Optional.ofNullable(caseData.getAppeal().getMrnDetails()).map(m -> equalsIgnoreCase(m.getDwpIssuingOffice(), PIP_AE)).orElse(false);
        boolean isDigitalCase = Optional.ofNullable(caseData.getCreatedInGapsFrom()).map(d -> equalsIgnoreCase(d, READY_TO_LIST.getId())).orElse(false);

        Appellant appellant = caseData.getAppeal().getAppellant();

        String appellantUniqueId = emailHelper.generateUniqueEmailId(appellant);

        log.info("Add robotics default attachments for case id {}", caseId);
        List<EmailAttachment> attachments = addDefaultAttachment(json, pdf, appellantUniqueId);

        if (!isDigitalCase) {
            log.info("Add robotics additional evidence for non digital case and case id {}", caseId);
            addAdditionalEvidenceAttachments(additionalEvidence, attachments);
        }

        String subject = buildSubject(appellantUniqueId, isScottish);

        emailService.sendEmail(caseId,
            roboticsEmailTemplate.generateEmail(
                subject,
                attachments,
                isScottish,
                isPipAeTo
            )
        );

        log.info("Case {} robotics JSON email with subject '{}' sent successfully for benefit type {} isScottish {} isPipAe {}",
            caseId, subject, caseData.getAppeal().getBenefitType().getCode(), isScottish, isPipAeTo);
    }

    private String buildSubject(String appellantUniqueId, boolean isScottish) {
        int roboticCount = isScottish ? scottishRoboticCount : englishRoboticCount;
        int randomNumber = rn.nextInt(roboticCount) + 1;

        return appellantUniqueId + " for Robot [" + randomNumber + "]";
    }

    private void addAdditionalEvidenceAttachments(Map<SscsDocument, byte[]> additionalEvidence, List<EmailAttachment> attachments) {
        for (SscsDocument sscsDocument : additionalEvidence.keySet()) {
            if (sscsDocument != null) {
                if (sscsDocument.getValue().getDocumentLink().getDocumentFilename() != null) {
                    byte[] content = additionalEvidence.get(sscsDocument);
                    if (content != null) {
                        attachments.add(file(content, sscsDocument.getValue().getDocumentLink().getDocumentFilename()));
                    }
                }
            }
        }
    }

    private List<EmailAttachment> addDefaultAttachment(JSONObject json, byte[] pdf, String appellantUniqueId) {
        List<EmailAttachment> emailAttachments = new ArrayList<>();

        emailAttachments.add(json(json.toString().getBytes(), appellantUniqueId + ".txt"));

        if (pdf != null) {
            emailAttachments.add(pdf(pdf, appellantUniqueId + ".pdf"));
        }

        return emailAttachments;
    }
}
