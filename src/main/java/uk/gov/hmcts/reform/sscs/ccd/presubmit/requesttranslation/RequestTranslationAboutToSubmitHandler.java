package uk.gov.hmcts.reform.sscs.ccd.presubmit.requesttranslation;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.domain.email.EmailAttachment.file;
import static uk.gov.hmcts.reform.sscs.domain.email.EmailAttachment.pdf;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.domain.email.EmailAttachment;
import uk.gov.hmcts.reform.sscs.domain.email.RequestTranslationTemplate;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.service.EmailService;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementService;
import uk.gov.hmcts.reform.sscs.thirdparty.pdfservice.DocmosisPdfService;

@Service
@Slf4j
public class RequestTranslationAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private DocmosisPdfService docmosisPdfService;
    private EvidenceManagementService evidenceManagementService;
    private EmailService emailService;
    private RequestTranslationTemplate requestTranslationTemplate;
    private IdamService idamService;

    @Value("${wlu.email.request.template}")
    private String wluEmailTemplate;

    @Value("${wlu.email.from}")
    private String loggedInUserEmail;

    @Value("${wlu.email.required.date.of.return}")
    private String translationReturnDate;

    @Autowired
    public RequestTranslationAboutToSubmitHandler(DocmosisPdfService docmosisPdfService,
                                                  EvidenceManagementService evidenceManagementService,
                                                  RequestTranslationTemplate requestTranslationTemplate,
                                                  EmailService emailService,
                                                  IdamService idamService) {
        this.docmosisPdfService = docmosisPdfService;
        this.evidenceManagementService = evidenceManagementService;
        this.emailService = emailService;
        this.requestTranslationTemplate = requestTranslationTemplate;
        this.idamService = idamService;
    }


    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent().equals(EventType.REQUEST_TRANSLATION_FROM_WLU);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        sendCaseToWlu(callback.getCaseDetails());
        return new PreSubmitCallbackResponse<>(caseData);
    }

    public void sendCaseToWlu(CaseDetails<SscsCaseData> caseDetails) {
        log.info("Case sent to wlu for case id {} ", caseDetails.getId());

        SscsCaseData caseData = caseDetails.getCaseData();
        Map<String, Object> placeholderMap = placeHolderMap(caseDetails);

        log.info("Downloading additional evidence for wlu for case id {} ", caseDetails.getId());
        Map<SscsDocument, byte[]> additionalEvidence = downloadEvidence(caseData, Long.valueOf(caseData.getCcdCaseId()));

        if (!additionalEvidence.isEmpty()) {
            log.info("Generate tranlsation request form from wlu for casedetails id {} ", caseDetails.getId());
            byte[] wluRequestForm = docmosisPdfService.createPdf(placeholderMap, wluEmailTemplate);

            sendEmailToWlu(caseDetails.getId(), caseData, wluRequestForm, additionalEvidence);
            log.info("Case {} successfully sent for benefit type {} to wlu", caseDetails.getId(),
                    caseData.getAppeal().getBenefitType().getCode());
        }
    }

    private Map<String, Object> placeHolderMap(CaseDetails<SscsCaseData> caseDetails) {
        UserDetails userDetails = idamService.getUserDetails(idamService.getIdamOauth2Token());
        Map<String, Object> dataMap = new HashMap<>();
        if (userDetails != null) {
            dataMap.put("name", String.join(" ",userDetails.getForename(), userDetails.getSurname()));
            dataMap.put("telephone", userDetails.getEmail());
        }
        dataMap.put("email", loggedInUserEmail);
        dataMap.put("ccdId", caseDetails.getId());
        dataMap.put("department", "SSCS Requestor");
        dataMap.put("workdescription", "Translation required");
        dataMap.put("translation", caseDetails.getCaseData().getLanguagePreference().getCode().toUpperCase());
        dataMap.put("translation_return_date", translationReturnDate);
        return dataMap;
    }

    private Map<SscsDocument, byte[]> downloadEvidence(SscsCaseData sscsCaseData, Long caseId) {
        if (hasEvidence(sscsCaseData)) {
            Map<SscsDocument, byte[]> map = new LinkedHashMap<>();
            for (SscsDocument doc : sscsCaseData.getSscsDocument()) {
                if (doc.getValue().getDocumentTranslationStatus() != null
                        && doc.getValue().getDocumentTranslationStatus().equals(SscsDocumentTranslationStatus.TRANSLATION_REQUIRED)) {
                    if (doc.getValue().getDocumentType() != null
                            && (doc.getValue().getDocumentType().equalsIgnoreCase("appellantEvidence")
                            || doc.getValue().getDocumentType().equalsIgnoreCase("Decision Notice")
                            || doc.getValue().getDocumentType().equalsIgnoreCase("Direction Notice")
                            || doc.getValue().getDocumentType().equalsIgnoreCase("sscs1"))) {
                        doc.getValue().setDocumentTranslationStatus(SscsDocumentTranslationStatus
                                .TRANSLATION_REQUESTED);
                        map.put(doc, downloadBinary(doc, caseId));

                    }
                }
            }
            return map;
        } else {
            return Collections.emptyMap();
        }
    }

    private byte[] downloadBinary(SscsDocument doc, Long caseId) {
        log.info("About to download binary to attach to wlu for caseId {}", caseId);
        if (doc.getValue().getDocumentLink() != null) {
            return evidenceManagementService.download(URI.create(doc.getValue().getDocumentLink().getDocumentUrl()), null);
        } else {
            return new byte[0];
        }
    }

    private boolean hasEvidence(SscsCaseData sscsCaseData) {
        return CollectionUtils.isNotEmpty(sscsCaseData.getSscsDocument());
    }

    private void sendEmailToWlu(long caseId, SscsCaseData caseData, byte[] requestFormPdf,
                                   Map<SscsDocument, byte[]> additionalEvidence) {

        log.info("Add request and sscs1 default attachments for case id {}", caseId);
        List<EmailAttachment> attachments = addDefaultAttachment(requestFormPdf, caseId);
        addAdditionalEvidenceAttachments(additionalEvidence, attachments);
        if (attachments.size() > 1) {
            log.info("Case {} wlu email sent successfully. for benefit type {}  ",
                    caseId, caseData.getAppeal().getBenefitType().getCode());
            emailService.sendEmail(caseId, requestTranslationTemplate.generateEmail(attachments, loggedInUserEmail));
        }
    }

    private void addAdditionalEvidenceAttachments(Map<SscsDocument, byte[]> additionalEvidence, List<EmailAttachment> attachments) {
        for (SscsDocument sscsDocument : additionalEvidence.keySet()) {
            if (sscsDocument != null) {
                if (sscsDocument.getValue().getDocumentLink() != null && sscsDocument.getValue().getDocumentLink().getDocumentFilename() != null) {
                    byte[] content = additionalEvidence.get(sscsDocument);
                    if (content != null) {
                        attachments.add(file(content, sscsDocument.getValue().getDocumentLink().getDocumentFilename()));
                    }
                }
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
