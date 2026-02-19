package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.ADDRESS_NAME;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.LETTER_NAME;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.evidenceshare.config.DocmosisTemplateConfig;
import uk.gov.hmcts.reform.sscs.evidenceshare.domain.FurtherEvidenceLetterType;
import uk.gov.hmcts.reform.sscs.evidenceshare.exception.BulkPrintException;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.BulkPrintService;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.CoverLetterService;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.HearingEnquiryFormPlaceholderService;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderUtility;

@Slf4j
@Service
public class IssueHearingEnquiryFormHandler implements CallbackHandler<SscsCaseData> {

    private final HearingEnquiryFormPlaceholderService hearingEnquiryFormPlaceholderService;
    private final BulkPrintService bulkPrintService;
    private final CoverLetterService coverLetterService;
    private final boolean cmOtherPartyConfidentialityEnabled;
    private final DocmosisTemplateConfig docmosisTemplateConfig;


    @Autowired
    public IssueHearingEnquiryFormHandler(BulkPrintService bulkPrintService,
        HearingEnquiryFormPlaceholderService hearingEnquiryFormPlaceholderService, CoverLetterService coverLetterService,
        DocmosisTemplateConfig docmosisTemplateConfig,
        @Value("${feature.cm-other-party-confidentiality.enabled}") boolean cmOtherPartyConfidentialityEnabled) {
        this.bulkPrintService = bulkPrintService;
        this.coverLetterService = coverLetterService;
        this.docmosisTemplateConfig = docmosisTemplateConfig;
        this.cmOtherPartyConfidentialityEnabled = cmOtherPartyConfidentialityEnabled;
        this.hearingEnquiryFormPlaceholderService = hearingEnquiryFormPlaceholderService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        log.info("IssueHearingEnquiryFormHandler canHandle method called for caseId {} and callbackType {} and event {}",
            callback.getCaseDetails().getId(), callbackType, callback.getEvent());
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return cmOtherPartyConfidentialityEnabled && callbackType.equals(CallbackType.SUBMITTED) && (callback.getEvent()
            == EventType.ISSUE_HEARING_ENQUIRY_FORM);
    }

    @Override
    public DispatchPriority getPriority() {
        return DispatchPriority.LATEST;
    }

    @Override
    public void handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!canHandle(callbackType, callback)) {
            log.info("Cannot handle this event for case id: {}", callback.getCaseDetails().getId());
            throw new IllegalStateException("Cannot handle callback");
        }
        CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();

        List<Pdf> documents = new ArrayList<>();
        final SscsCaseData caseData = caseDetails.getCaseData();
        if (YesNo.isYes(caseData.getAddDocuments())) {
            documents.addAll(coverLetterService.getSelectedDocuments(caseData));
        }

        sendToOtherParties(caseDetails.getId(), caseData, documents);

    }

    private static String getLetterName(Map<String, Object> placeholders) {
        return String.format(LETTER_NAME, placeholders.get(ADDRESS_NAME), LocalDateTime.now());
    }

    private String getCoverLetterTemplateName(SscsCaseData caseData) {
        return docmosisTemplateConfig.getTemplate().get(caseData.getLanguagePreference()).get("hearing-enquiry-form").get("name");
    }

    private String getHefTemplateName(SscsCaseData caseData) {
        return docmosisTemplateConfig.getTemplate().get(caseData.getLanguagePreference()).get("hearing-enquiry-form")
            .get("form");
    }

    private String getCoverSheetTemplateName(SscsCaseData caseData) {
        return docmosisTemplateConfig.getTemplate().get(caseData.getLanguagePreference()).get("hearing-enquiry-form")
            .get("cover");
    }

    private void sendToOtherParties(long caseId, SscsCaseData caseData, List<Pdf> documents) {
        log.info("Sending letter to other party");
        var selectedOtherParties = caseData.getOtherPartySelection();

        if (nonNull(selectedOtherParties)) {
            for (var party : selectedOtherParties) {
                String entityId = party.getValue().getOtherPartiesList().getValue().getCode();
                String recipient = PlaceholderUtility.getName(caseData, FurtherEvidenceLetterType.OTHER_PARTY_LETTER, entityId);
                List<Pdf> letter = getLetterPdfs(caseData, documents, FurtherEvidenceLetterType.OTHER_PARTY_LETTER, entityId);
                if (letter.isEmpty()) {
                    continue;
                }
                bulkPrintService.sendToBulkPrint(caseId, caseData, letter, EventType.ISSUE_HEARING_ENQUIRY_FORM, recipient);
            }
        }
    }

    private List<Pdf> getLetterPdfs(SscsCaseData caseData, List<Pdf> documents, FurtherEvidenceLetterType letterType,
        String entityId) {
        var placeholders = hearingEnquiryFormPlaceholderService.populatePlaceholders(caseData, letterType, entityId);

        String letterName = getLetterName(placeholders);

        var hefLetter = coverLetterService.generateCoverLetterRetry(letterType, getCoverLetterTemplateName(caseData), letterName,
            placeholders, 1);

        var coverLetter = coverLetterService.generateCoverLetterRetry(letterType, getHefTemplateName(caseData), letterName,
            placeholders, 1);

        var coverSheet = coverLetterService.generateCoverSheet(getCoverSheetTemplateName(caseData), "coversheet", placeholders);

        Optional<byte[]> bundledLetterOpt = bulkPrintService.buildBundledLetter(List.of(coverLetter, hefLetter, coverSheet));
        if (bundledLetterOpt.isEmpty()) {
            log.error("Failed to bundle documents for hearing enquiry form, case id: {}", caseData.getCcdCaseId());
            throw new BulkPrintException(
                "Failed to bundle documents for hearing enquiry form, case id: %s".formatted(caseData.getCcdCaseId()));
        }

        Pdf pdf = new Pdf(bundledLetterOpt.get(), letterName);
        List<Pdf> letter = new ArrayList<>();
        letter.add(pdf);
        letter.addAll(documents);

        return letter;
    }


}
