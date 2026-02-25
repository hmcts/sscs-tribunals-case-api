package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_HEARING_ENQUIRY_FORM;
import static uk.gov.hmcts.reform.sscs.evidenceshare.domain.FurtherEvidenceLetterType.OTHER_PARTY_LETTER;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.evidenceshare.config.DocmosisTemplateConfig;
import uk.gov.hmcts.reform.sscs.evidenceshare.exception.BulkPrintException;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.BulkPrintService;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.CoverLetterService;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.HearingEnquiryFormPlaceholderService;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderUtility;

@Slf4j
@Service
public class IssueHearingEnquiryFormHandler implements CallbackHandler<SscsCaseData> {

    private static final String HEARING_ENQUIRY_FORM = "hearing-enquiry-form";
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
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");
        log.info("IssueHearingEnquiryFormHandler canHandle method called for caseId {} and callbackType {} and event {}",
            callback.getCaseDetails().getId(), callbackType, callback.getEvent());

        return cmOtherPartyConfidentialityEnabled && callbackType.equals(CallbackType.SUBMITTED) && (callback.getEvent()
            == ISSUE_HEARING_ENQUIRY_FORM);
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
        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();

        final List<Pdf> documents = new ArrayList<>();
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
        return docmosisTemplateConfig.getTemplate().get(caseData.getLanguagePreference()).get(HEARING_ENQUIRY_FORM).get("name");
    }

    private String getHefTemplateName(SscsCaseData caseData) {
        return docmosisTemplateConfig.getTemplate().get(caseData.getLanguagePreference()).get(HEARING_ENQUIRY_FORM)
            .get("form");
    }

    private String getCoverSheetTemplateName(SscsCaseData caseData) {
        return docmosisTemplateConfig.getTemplate().get(caseData.getLanguagePreference()).get(HEARING_ENQUIRY_FORM)
            .get("cover");
    }

    private void sendToOtherParties(long caseId, SscsCaseData caseData, List<Pdf> documents) {
        log.info("Sending HEF letter to other parties for case id: {}", caseId);
        final var selectedOtherParties = caseData.getOtherPartySelection();

        if (isNotEmpty(selectedOtherParties)) {
            for (var party : selectedOtherParties) {
                if (party.getValue() == null
                    || party.getValue().getOtherPartiesList() == null
                    || party.getValue().getOtherPartiesList().getValue() == null) {
                    log.warn("Skipping party with incomplete selection data for case id: {}", caseId);
                    continue;
                }
                final String entityId = party.getValue().getOtherPartiesList().getValue().getCode();
                final String recipient = PlaceholderUtility.getName(caseData, OTHER_PARTY_LETTER,
                    entityId);
                final List<Pdf> letter = getLetterPdfs(caseData, documents, entityId);
                bulkPrintService.sendToBulkPrint(caseId, caseData, letter, ISSUE_HEARING_ENQUIRY_FORM, recipient);
            }
        }
    }

    private List<Pdf> getLetterPdfs(SscsCaseData caseData, List<Pdf> documents, String entityId) {
        var placeholders = hearingEnquiryFormPlaceholderService.populatePlaceholders(caseData,
            OTHER_PARTY_LETTER, entityId);

        final String letterName = getLetterName(placeholders);

        var coverLetter = coverLetterService.generateCoverLetterRetry(OTHER_PARTY_LETTER,
            getCoverLetterTemplateName(caseData), letterName, placeholders, 1);

        var hefForm = coverLetterService.generateCoverLetterRetry(OTHER_PARTY_LETTER,
            getHefTemplateName(caseData), letterName, placeholders, 1);

        var coverSheet = coverLetterService.generateCoverSheet(getCoverSheetTemplateName(caseData), "coversheet", placeholders);

        final Optional<byte[]> bundledLetterOpt = bulkPrintService.buildBundledLetter(List.of(coverLetter, hefForm, coverSheet));
        if (bundledLetterOpt.isEmpty()) {
            log.error("Failed to bundle documents for hearing enquiry form, case id: {}", caseData.getCcdCaseId());
            throw new BulkPrintException(
                "Failed to bundle documents for hearing enquiry form, case id: %s".formatted(caseData.getCcdCaseId()));
        }

        final Pdf pdf = new Pdf(bundledLetterOpt.get(), letterName);
        final List<Pdf> letter = new ArrayList<>();
        letter.add(pdf);
        letter.addAll(documents);

        return letter;
    }


}
