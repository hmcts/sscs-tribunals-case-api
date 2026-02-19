package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.ADDRESS_NAME;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.LETTER_NAME;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.evidenceshare.config.DocmosisTemplateConfig;
import uk.gov.hmcts.reform.sscs.evidenceshare.domain.FurtherEvidenceLetterType;
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

    private String getDocmosisTemplate(SscsCaseData caseData) {
        return docmosisTemplateConfig.getTemplate().get(caseData.getLanguagePreference()).get("hearing-enquiry-form").get("name");
    }

    private String getDocmosisCoverSheet(SscsCaseData caseData) {
        return docmosisTemplateConfig.getTemplate().get(caseData.getLanguagePreference()).get("hearing-enquiry-form")
            .get("cover");
    }

    private OtherParty getOtherPartyByEntityId(String entityId, List<CcdValue<OtherParty>> otherParties) {
        return otherParties.stream().map(CcdValue::getValue).filter(o -> entityId.contains(o.getId())).findFirst().orElse(null);
    }

    private void sendToOtherParties(long caseId, SscsCaseData caseData, List<Pdf> documents) {
        log.info("Sending letter to other party");
        var selectedOtherParties = caseData.getOtherPartySelection();

        if (nonNull(selectedOtherParties)) {
            for (var party : selectedOtherParties) {
                String entityId = party.getValue().getOtherPartiesList().getValue().getCode();
                String recipient = PlaceholderUtility.getName(caseData, FurtherEvidenceLetterType.OTHER_PARTY_LETTER, entityId);
                List<Pdf> letter = getLetterPdfs(caseData, documents, FurtherEvidenceLetterType.OTHER_PARTY_LETTER, entityId);
                bulkPrintService.sendToBulkPrint(caseId, caseData, letter, EventType.ISSUE_HEARING_ENQUIRY_FORM, recipient);
                // bulkPrintService.sendToBulkPrint(letter, caseData, FurtherEvidenceLetterType.OTHER_PARTY_LETTER, EventType.ISSUE_HEARING_ENQUIRY_FORM, recipient);
            }
        }
    }

    private List<Pdf> getLetterPdfs(SscsCaseData caseData, List<Pdf> documents, FurtherEvidenceLetterType letterType,
        String entityId) {
        var placeholders = hearingEnquiryFormPlaceholderService.populatePlaceholders(caseData, letterType, entityId);

        String letterName = getLetterName(placeholders);

        var generatedPdf = coverLetterService.generateCoverLetterRetry(letterType, getDocmosisTemplate(caseData), letterName,
            placeholders, 1);

        var coverSheet = coverLetterService.generateCoverSheet(getDocmosisCoverSheet(caseData), "coversheet", placeholders);

        var bundledLetter = bulkPrintService.buildBundledLetter(coverSheet, generatedPdf);

        Pdf pdf = new Pdf(bundledLetter, letterName);
        List<Pdf> letter = new ArrayList<>();
        letter.add(pdf);
        letter.addAll(documents);


        // byte[] pdfBytes = null; // example source
        // try {
        //     Path saved = savePdf(documents.getFirst().getContent(), Path.of("out/generated.pdf"));
        //     System.out.println("Saved to: " + saved.toAbsolutePath());
        // } catch (IOException e) {
        //     throw new RuntimeException(e);
        // }


        return letter;
    }

    // public static Path savePdf(byte[] pdfBytes, Path outputPath) throws IOException {
    //     // Basic sanity check (optional): PDF files start with "%PDF"
    //     if (pdfBytes == null || pdfBytes.length < 4) {
    //         throw new IllegalArgumentException("pdfBytes is empty");
    //     }
    //
    //     Files.createDirectories(outputPath.getParent());
    //
    //     return Files.write(
    //         outputPath,
    //         pdfBytes,
    //         StandardOpenOption.CREATE,
    //         StandardOpenOption.TRUNCATE_EXISTING,
    //         StandardOpenOption.WRITE
    //     );
    // }

}
