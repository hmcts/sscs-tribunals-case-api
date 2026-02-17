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
import uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.GenericLetterPlaceholderService;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderUtility;

@Slf4j
@Service
public class IssueHearingEnquiryFormHandler implements CallbackHandler<SscsCaseData> {

    private final GenericLetterPlaceholderService genericLetterPlaceholderService;
    private final BulkPrintService bulkPrintService;
    private final CoverLetterService coverLetterService;
    private final boolean cmOtherPartyConfidentialityEnabled;
    private final DocmosisTemplateConfig docmosisTemplateConfig;
    private String docmosisTemplate;
    private String docmosisCoverSheetTemplate;

    @Autowired
    public IssueHearingEnquiryFormHandler(BulkPrintService bulkPrintService,
        GenericLetterPlaceholderService genericLetterPlaceholderService, CoverLetterService coverLetterService,
        DocmosisTemplateConfig docmosisTemplateConfig,
        @Value("${feature.cm-other-party-confidentiality.enabled}") boolean cmOtherPartyConfidentialityEnabled) {
        this.bulkPrintService = bulkPrintService;
        this.coverLetterService = coverLetterService;
        this.docmosisTemplateConfig = docmosisTemplateConfig;
        this.cmOtherPartyConfidentialityEnabled = cmOtherPartyConfidentialityEnabled;
        this.genericLetterPlaceholderService = genericLetterPlaceholderService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        log.info("IssueGenericLetterHandler canHandle method called for caseId {} and callbackType {} and event {}",
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

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        long caseDetailsId = callback.getCaseDetails().getId();

        docmosisTemplate = getDocmosisTemplate(caseData);
        docmosisCoverSheetTemplate = getDocmosisCoverSheet(caseData);


        process(caseDetailsId, caseData);
    }

    private String getDocmosisTemplate(SscsCaseData caseData) {
        return docmosisTemplateConfig.getTemplate().get(caseData.getLanguagePreference())
            .get("hearing-enquiry-form").get("name");
    }

    private String getDocmosisCoverSheet(SscsCaseData caseData) {
        return docmosisTemplateConfig.getTemplate().get(caseData.getLanguagePreference())
            .get("hearing-enquiry-form").get("cover");
    }

    private void process(long caseId, SscsCaseData caseData) {
        List<Pdf> documents = new ArrayList<>();

        if (YesNo.isYes(caseData.getAddDocuments())) {
            documents.addAll(coverLetterService.getSelectedDocuments(caseData));
        }

        if (YesNo.isYes(caseData.getSendToApellant())) {
            sendToAppellant(caseId, caseData, documents);
        }

        if (YesNo.isYes(caseData.getSendToOtherParties())) {
            sendToOtherParties(caseId, caseData, documents);
        }

    }

    private static boolean filterByEntityID(String entityId, OtherParty o) {
        return entityId.contains(o.getId())
            || (o.hasRepresentative() && entityId.contains(o.getRep().getId()))
            || (o.hasAppointee() && entityId.contains(o.getAppointee().getId()));
    }

    private OtherParty getOtherPartyByEntityId(String entityId, List<CcdValue<OtherParty>> otherParties) {
        return otherParties.stream()
            .map(CcdValue::getValue)
            .filter(o -> filterByEntityID(entityId, o)).findFirst()
            .orElse(null);
    }

    private void sendToOtherParties(long caseId, SscsCaseData caseData, List<Pdf> documents) {
        log.info("Sending letter to other party");
        var selectedOtherParties = caseData.getOtherPartySelection();
        List<CcdValue<OtherParty>> otherParties = caseData.getOtherParties();

        if (nonNull(selectedOtherParties)) {
            for (var party : selectedOtherParties) {
                String entityId = party.getValue().getOtherPartiesList().getValue().getCode();
                OtherParty otherParty = getOtherPartyByEntityId(entityId, otherParties);

                if (otherParty != null) {
                    FurtherEvidenceLetterType letterType = getLetterType(otherParty, entityId);
                    String recipient = PlaceholderUtility.getName(caseData, letterType, entityId);
                    List<Pdf> letter = getLetterPdfs(caseData, documents, letterType, entityId);
                    bulkPrintService.sendToBulkPrint(caseId, caseData, letter, EventType.ISSUE_HEARING_ENQUIRY_FORM, recipient);
                }
            }
        }
    }

    private void sendToAppellant(long caseId, SscsCaseData caseData, List<Pdf> documents) {
        List<Pdf> letter = getLetterPdfs(caseData, documents, FurtherEvidenceLetterType.APPELLANT_LETTER, null);
        String recipient = PlaceholderUtility.getName(caseData, FurtherEvidenceLetterType.APPELLANT_LETTER, null);
        bulkPrintService.sendToBulkPrint(caseId, caseData, letter, EventType.ISSUE_HEARING_ENQUIRY_FORM,
            recipient);
    }

    private FurtherEvidenceLetterType getLetterType(OtherParty otherParty, String entityId) {
        boolean hasRepresentative = otherParty.hasRepresentative() && entityId.contains(otherParty.getRep().getId());

        return hasRepresentative ? FurtherEvidenceLetterType.OTHER_PARTY_REP_LETTER : FurtherEvidenceLetterType.OTHER_PARTY_LETTER;
    }

    private static String getLetterName(Map<String, Object> placeholders) {
        return String.format(LETTER_NAME, placeholders.get(ADDRESS_NAME), LocalDateTime.now());
    }
    
    private List<Pdf> getLetterPdfs(SscsCaseData caseData, List<Pdf> documents, FurtherEvidenceLetterType letterType, String entityId) {
        var placeholders = genericLetterPlaceholderService.populatePlaceholders(caseData,
            letterType,
            entityId);

        String letterName = getLetterName(placeholders);

        var generatedPdf = coverLetterService.generateCoverLetterRetry(letterType,
            docmosisTemplate, letterName, placeholders, 1);

        var coverSheet = coverLetterService.generateCoverSheet(docmosisCoverSheetTemplate,
            "coversheet", placeholders);

        var bundledLetter = bulkPrintService.buildBundledLetter(coverSheet, generatedPdf);

        Pdf pdf = new Pdf(bundledLetter, letterName);
        List<Pdf> letter = new ArrayList<>();
        letter.add(pdf);
        letter.addAll(documents);
        return letter;
    }

}
