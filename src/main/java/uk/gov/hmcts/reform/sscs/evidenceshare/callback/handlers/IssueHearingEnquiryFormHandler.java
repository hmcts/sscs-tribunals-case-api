package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.evidenceshare.domain.FurtherEvidenceLetterType;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.BulkPrintService;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.CoverLetterService;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderUtility;

@Slf4j
@Service
public class IssueHearingEnquiryFormHandler implements CallbackHandler<SscsCaseData> {

    private final BulkPrintService bulkPrintService;
    private final CoverLetterService coverLetterService;
    private final boolean cmOtherPartyConfidentialityEnabled;

    @Autowired
    public IssueHearingEnquiryFormHandler(BulkPrintService bulkPrintService, CoverLetterService coverLetterService,
        @Value("${feature.cm-other-party-confidentiality.enabled}") boolean cmOtherPartyConfidentialityEnabled) {
        this.bulkPrintService = bulkPrintService;
        this.coverLetterService = coverLetterService;
        this.cmOtherPartyConfidentialityEnabled = cmOtherPartyConfidentialityEnabled;
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

        process(caseDetailsId, caseData);
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

    private void sendToOtherParties(long caseId, SscsCaseData caseData, List<Pdf> documents) {
        log.info("Sending letter to other party");
        var selectedOtherParties = caseData.getOtherPartySelection();

        if (nonNull(selectedOtherParties)) {
            for (var party : selectedOtherParties) {
                String entityId = party.getValue().getOtherPartiesList().getValue().getCode();
                String recipient = PlaceholderUtility.getName(caseData, FurtherEvidenceLetterType.OTHER_PARTY_LETTER, entityId);
                bulkPrintService.sendToBulkPrint(caseId, caseData, new ArrayList<>(documents),
                    EventType.ISSUE_HEARING_ENQUIRY_FORM, recipient);
            }
        }
    }

    private void sendToAppellant(long caseId, SscsCaseData caseData, List<Pdf> documents) {
        String recipient = PlaceholderUtility.getName(caseData, FurtherEvidenceLetterType.APPELLANT_LETTER, null);
        bulkPrintService.sendToBulkPrint(caseId, caseData, new ArrayList<>(documents), EventType.ISSUE_HEARING_ENQUIRY_FORM,
            recipient);
    }

}
