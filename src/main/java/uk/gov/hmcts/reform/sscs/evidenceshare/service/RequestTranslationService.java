package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import static uk.gov.hmcts.reform.sscs.domain.email.EmailAttachment.file;
import static uk.gov.hmcts.reform.sscs.domain.email.EmailAttachment.pdf;

import java.util.*;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Template;
import uk.gov.hmcts.reform.sscs.docmosis.service.DocmosisPdfGenerationService;
import uk.gov.hmcts.reform.sscs.domain.email.EmailAttachment;
import uk.gov.hmcts.reform.sscs.domain.email.RequestTranslationTemplate;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.service.PdfStoreService;

@Component
@Slf4j
public class RequestTranslationService {

    private final PdfStoreService pdfStoreService;
    private final EmailService emailService;
    private final RequestTranslationTemplate requestTranslationTemplate;
    private DocmosisPdfGenerationService pdfGenerationService;
    private final IdamService idamService;

    @Value("${wlu.email.from}")
    private String fromEmail;
    @Value("${wlu.email.dateOfReturn}")
    private String translationReturnDate;

    @Autowired
    public RequestTranslationService(
        PdfStoreService pdfStoreService,
        EmailService emailService,
        RequestTranslationTemplate requestTranslationTemplate,
        DocmosisPdfGenerationService pdfGenerationService,
        IdamService idamService) {
        this.pdfStoreService = pdfStoreService;
        this.emailService = emailService;
        this.requestTranslationTemplate = requestTranslationTemplate;
        this.pdfGenerationService = pdfGenerationService;
        this.idamService = idamService;
    }

    public boolean sendCaseToWlu(CaseDetails<SscsCaseData> caseDetails) {
        log.info("Case sent to wlu for case id {} ", caseDetails.getId());
        boolean status = false;
        SscsCaseData caseData = caseDetails.getCaseData();
        Map<String, Object> placeholderMap = placeHolderMap(caseDetails);

        log.info("Downloading additional evidence for wlu for case id {} ", caseDetails.getId());
        Map<String, byte[]> additionalEvidence = downloadEvidence(caseData, Long.valueOf(caseData.getCcdCaseId()));

        if (!additionalEvidence.isEmpty()) {
            log.info("Generate tranlsation request form from wlu for casedetails id {} ", caseDetails.getId());
            byte[] wluRequestForm = pdfGenerationService.generatePdf(DocumentHolder.builder()
                .template(new Template("TB-SCS-EML-ENG-00530.docx",
                    "WLU Request Form")).placeholders(placeholderMap).build());

            status = sendEmailToWlu(caseDetails.getId(), caseData, wluRequestForm, additionalEvidence);
            log.info("Case {} successfully sent for benefit type {} to wlu", caseDetails.getId(),
                caseData.getAppeal().getBenefitType().getCode());
        }
        return status;
    }

    private Map<String, Object> placeHolderMap(CaseDetails<SscsCaseData> caseDetails) {
        UserDetails userDetails = idamService.getUserDetails(idamService.getIdamOauth2Token());
        Map<String, Object> dataMap = new HashMap<>();
        if (userDetails != null) {
            dataMap.put("name", String.join(" ", userDetails.getForename(), userDetails.getSurname()));
            dataMap.put("telephone", userDetails.getEmail());
        }
        dataMap.put("email", fromEmail);
        dataMap.put("ccdId", caseDetails.getId());
        dataMap.put("department", "SSCS Requestor");
        dataMap.put("workdescription", "Translation required");
        dataMap.put("translation", caseDetails.getCaseData().getLanguagePreference().getCode().toUpperCase());
        dataMap.put("translation_return_date", translationReturnDate);
        return dataMap;
    }

    private Map<String, byte[]> downloadEvidence(SscsCaseData sscsCaseData, Long caseId) {
        if (hasEvidence(sscsCaseData)) {
            Map<String, byte[]> map = new HashMap<>();
            map = buildMapOfEvidence(sscsCaseData.getSscsDocument(), caseId, map);
            map = buildMapOfEvidence(sscsCaseData.getDwpDocuments(), caseId, map);

            return map;
        } else {
            return Collections.emptyMap();
        }
    }

    private Map<String, byte[]> buildMapOfEvidence(List<? extends AbstractDocument> docs, Long caseId, Map<String, byte[]> map) {

        ListUtils.emptyIfNull(docs).stream().filter(doc -> SscsDocumentTranslationStatus.TRANSLATION_REQUIRED.equals(doc.getValue().getDocumentTranslationStatus()))
            .forEach(doc -> {
                doc.getValue().setDocumentTranslationStatus(SscsDocumentTranslationStatus.TRANSLATION_REQUESTED);
                if (doc instanceof SscsDocument) {
                    final String sscsFilename = getDocumentFileName.apply(doc.getValue());
                    if (sscsFilename != null) {
                        map.put(sscsFilename, downloadBinary((SscsDocument) doc, caseId));
                    }
                } else if (doc instanceof DwpDocument) {
                    final String sscsFilename = getDwpDocumentFileName.apply((DwpDocumentDetails) doc.getValue());
                    if (sscsFilename != null) {
                        map.put(sscsFilename, downloadBinary((DwpDocument) doc, caseId));
                    }
                }
            });

        return map;
    }

    private final Function<AbstractDocumentDetails, String> getDocumentFileName =
        sscsDocumentDetails -> (sscsDocumentDetails.getDocumentLink() != null
            && sscsDocumentDetails.getDocumentLink().getDocumentFilename() != null
            && sscsDocumentDetails.getDocumentLink().getDocumentUrl() != null)
            ? sscsDocumentDetails.getDocumentLink().getDocumentFilename() + "." + System.nanoTime() : null;


    private final Function<DwpDocumentDetails, String> getDwpDocumentFileName =
        dwpDocumentDetails -> dwpDocumentDetails.getDocumentLink() != null && dwpDocumentDetails.getDocumentLink().getDocumentUrl() != null ? dwpDocumentDetails.getDocumentLink().getDocumentFilename() + "." + System.nanoTime() : null;

    private byte[] downloadBinary(DwpDocument doc, Long caseId) {
        log.info("About to download binary to attach to wlu for caseId {}", caseId);
        if (doc.getValue().getDocumentLink() != null && doc.getValue().getDocumentLink().getDocumentUrl() != null && doc.getValue().getDocumentLink().getDocumentFilename() != null) {
            return pdfStoreService.download(doc.getValue().getDocumentLink().getDocumentUrl());
        } else {
            return new byte[0];
        }
    }

    private byte[] downloadBinary(SscsDocument doc, Long caseId) {
        log.info("About to download binary to attach to wlu for caseId {}", caseId);
        if (doc.getValue().getDocumentLink() != null && doc.getValue().getDocumentLink().getDocumentUrl() != null && doc.getValue().getDocumentLink().getDocumentFilename() != null) {
            return pdfStoreService.download(doc.getValue().getDocumentLink().getDocumentUrl());
        } else {
            return new byte[0];
        }
    }

    private boolean hasEvidence(SscsCaseData sscsCaseData) {
        return CollectionUtils.isNotEmpty(sscsCaseData.getSscsDocument()) || CollectionUtils.isNotEmpty(sscsCaseData.getDwpDocuments());
    }

    private boolean sendEmailToWlu(long caseId, SscsCaseData caseData, byte[] requestFormPdf,
                                   Map<String, byte[]> additionalEvidence) {

        log.info("Add request and sscs1 default attachments for case id {}", caseId);
        List<EmailAttachment> attachments = addDefaultAttachment(requestFormPdf, caseId);
        addAdditionalEvidenceAttachments(additionalEvidence, attachments);
        if (attachments.size() > 1) {
            log.info("Successfully email sent to wlu for CaseId {}, benefit type {} and number of attachments {}",
                caseId, caseData.getAppeal().getBenefitType().getCode(), attachments.size());
            emailService.sendEmail(caseId, requestTranslationTemplate.generateEmail(attachments, caseId));
            return true;
        }
        return false;
    }

    private void addAdditionalEvidenceAttachments(Map<String, byte[]> additionalEvidence,
                                                  List<EmailAttachment> attachments) {
        for (String filename : additionalEvidence.keySet()) {
            byte[] content = additionalEvidence.get(filename);
            if (content != null) {
                attachments.add(file(content, filename.substring(0, filename.lastIndexOf("."))));
            }
        }
    }

    private List<EmailAttachment> addDefaultAttachment(byte[] requestForm, long caseId) {
        List<EmailAttachment> emailAttachments = new ArrayList<>();
        if (requestForm != null) {
            emailAttachments.add(pdf(requestForm, "RequestTranslationForm-" + caseId + ".pdf"));
        }
        return emailAttachments;
    }
}
