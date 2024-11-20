package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers.HandlerUtils.distinctByKey;
import static uk.gov.hmcts.reform.sscs.evidenceshare.domain.FurtherEvidenceLetterType.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.evidenceshare.domain.FurtherEvidenceLetterType;
import uk.gov.hmcts.reform.sscs.evidenceshare.exception.IssueFurtherEvidenceException;
import uk.gov.hmcts.reform.sscs.evidenceshare.exception.PostIssueFurtherEvidenceTasksException;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.FurtherEvidenceService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Service
@Slf4j
public class IssueFurtherEvidenceHandler implements CallbackHandler<SscsCaseData> {

    private final FurtherEvidenceService furtherEvidenceService;
    private final UpdateCcdCaseService updateCcdCaseService;
    private final IdamService idamService;
    private final SscsCcdConvertService sscsCcdConvertService;
    private final CcdClient ccdClient;

    public IssueFurtherEvidenceHandler(FurtherEvidenceService furtherEvidenceService,
                                       UpdateCcdCaseService updateCcdCaseService,
                                       IdamService idamService,
                                       SscsCcdConvertService sscsCcdConvertService,
                                       CcdClient ccdClient
    ) {
        this.furtherEvidenceService = furtherEvidenceService;
        this.updateCcdCaseService = updateCcdCaseService;
        this.idamService = idamService;
        this.sscsCcdConvertService = sscsCcdConvertService;
        this.ccdClient = ccdClient;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        log.info("IssueFurtherEvidenceHandler canHandle method called for caseId {} and callbackType {}", callback.getCaseDetails().getId(), callbackType);
        requireNonNull(callback, "callback must not be null");
        return callbackType.equals(CallbackType.SUBMITTED)
            && callback.getEvent() == EventType.ISSUE_FURTHER_EVIDENCE
            && furtherEvidenceService.canHandleAnyDocument(callback.getCaseDetails().getCaseData().getSscsDocument());
    }

    @Override
    public void handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        var caseId = callback.getCaseDetails().getId();
        var idamTokens = idamService.getIdamTokens();

        log.info("Handling with Issue Further Evidence Handler for caseId {}", caseId);

        var updatedCaseData = issueFurtherEvidence(caseId, idamTokens);
        postIssueFurtherEvidenceTasks(caseId, idamTokens, updatedCaseData);
    }

    private SscsCaseData issueFurtherEvidence(long caseId, IdamTokens idamTokens) {
        log.info("Retrieving latest case date for caseId {} and submitted event type {} ", caseId, EventType.ISSUE_FURTHER_EVIDENCE.getCcdType());
        StartEventResponse startEventResponse = ccdClient.startEvent(idamTokens, caseId, EventType.ISSUE_FURTHER_EVIDENCE.getCcdType());
        SscsCaseDetails sscsCaseDetails = sscsCcdConvertService.getCaseDetails(startEventResponse);

        var caseData = sscsCaseDetails.getData();
        List<DocumentType> documentTypes = Arrays.asList(APPELLANT_EVIDENCE, REPRESENTATIVE_EVIDENCE, DWP_EVIDENCE, JOINT_PARTY_EVIDENCE, HMCTS_EVIDENCE);
        List<FurtherEvidenceLetterType> allowedLetterTypes = Arrays.asList(APPELLANT_LETTER, REPRESENTATIVE_LETTER, JOINT_PARTY_LETTER, OTHER_PARTY_LETTER, OTHER_PARTY_REP_LETTER);

        documentTypes.forEach(documentType -> issueEvidencePerDocumentType(caseData, allowedLetterTypes, documentType, null));
        issueFurtherEvidenceForEachOtherPartyThatIsOriginalSender(sscsCaseDetails.getData(), allowedLetterTypes);

        return caseData;
    }

    private void issueFurtherEvidenceForEachOtherPartyThatIsOriginalSender(SscsCaseData caseData, List<FurtherEvidenceLetterType> allowedLetterTypes) {
        List<SscsDocument> groupedOtherPartyDocuments = findUniqueOtherPartyDocumentsByOtherPartyId(caseData.getSscsDocument());

        if (groupedOtherPartyDocuments != null && !groupedOtherPartyDocuments.isEmpty()) {
            groupedOtherPartyDocuments.forEach(doc -> issueEvidencePerDocumentType(caseData, allowedLetterTypes, DocumentType.fromValue(doc.getValue().getDocumentType()), doc.getValue().getOriginalSenderOtherPartyId()));
        }
    }

    private List<SscsDocument> findUniqueOtherPartyDocumentsByOtherPartyId(List<SscsDocument> sscsDocuments) {
        // We need to find all the unissued other party documents for a given other party by Id, so we can iterate through and issue evidence for each other party that has unissued evidence.
        // To prevent multiple documents going to the same other party, we only need to find one distinct document as the document type and original sender id is all we care about here and they will always be the same if there were multiple documents for the same other party.
        // Further down the line, in the FurtherEvidenceService, we work out what documents to actually issue out.
        return sscsDocuments.stream()
            .filter(doc -> OTHER_PARTY_EVIDENCE.getValue().equals(doc.getValue().getDocumentType()) || OTHER_PARTY_REPRESENTATIVE_EVIDENCE.getValue().equals(doc.getValue().getDocumentType()))
            .filter(d -> "No".equals(d.getValue().getEvidenceIssued()))
            .filter(distinctByKey(p -> p.getValue().getOriginalSenderOtherPartyId()))
            .collect(Collectors.toList());
    }

    private void issueEvidencePerDocumentType(SscsCaseData caseData, List<FurtherEvidenceLetterType> allowedLetterTypes,
                                              DocumentType documentType, String otherPartyOriginalSenderId) {
        try {
            log.info("Issuing for {} for caseId {}", documentType.getValue(), caseData.getCcdCaseId());
            furtherEvidenceService.issue(caseData.getSscsDocument(), caseData, documentType, allowedLetterTypes, otherPartyOriginalSenderId);
        } catch (Exception e) {
            handleIssueFurtherEvidenceException(caseData);
            String errorMsg = "Failed sending further evidence for case(%s)...";
            throw new IssueFurtherEvidenceException(errorMsg.formatted(caseData.getCcdCaseId()), e);
        }
        log.info("Issued for caseId {}", caseData.getCcdCaseId());
    }

    private void postIssueFurtherEvidenceTasks(long caseId, IdamTokens idamTokens, SscsCaseData updatedCaseData) {
        log.debug("Post Issue Tasks for caseId {}", caseId);

        Map<String, SscsDocument> binaryDocumentUrlLinkCaseDataMap = updatedCaseData.getSscsDocument()
                .stream()
                .collect(toMap(document -> document.getValue().getDocumentLink().getDocumentBinaryUrl(), Function.identity()));

        try {
            updateCcdCaseService.updateCaseV2(
                    caseId,
                    EventType.UPDATE_CASE_ONLY.getCcdType(),
                    idamTokens,
                    sscsCaseDetails -> {
                        sscsCaseDetails.getData().getSscsDocument().forEach(
                                sscsDocument -> {
                                    String documentBinaryUrl = sscsDocument.getValue().getDocumentLink().getDocumentBinaryUrl();
                                    if (binaryDocumentUrlLinkCaseDataMap.containsKey(documentBinaryUrl)) {
                                        sscsDocument.getValue().setResizedDocumentLink(
                                                binaryDocumentUrlLinkCaseDataMap.get(documentBinaryUrl).getValue().getResizedDocumentLink()
                                        );
                                    }
                                }
                        );

                        final String description = determineDescription(sscsCaseDetails.getData().getSscsDocument());
                        setEvidenceIssuedFlagToYes(sscsCaseDetails.getData().getSscsDocument());
                        return new UpdateCcdCaseService.UpdateResult("Update case data", description);

                    }
            );
        } catch (Exception e) {
            String errorMsg = "Failed to update document evidence issued flags after issuing further evidence "
                    + "for case(%s)";
            throw new PostIssueFurtherEvidenceTasksException(errorMsg.formatted(caseId), e);
        }
    }

    public String determineDescription(List<SscsDocument> documents) {
        final boolean hasResizedDocs = documents.stream().anyMatch(document ->
            document.getValue().getResizedDocumentLink() != null && document.getValue().getEvidenceIssued().equals(NO.getValue())
        );

        final String baseDescription = "Update issued evidence document flags after issuing further evidence";

        return !hasResizedDocs ? baseDescription : baseDescription + " and attached resized document(s)";
    }

    private void handleIssueFurtherEvidenceException(SscsCaseData caseData) {
        updateCcdCaseService.updateCaseV2(Long.valueOf(caseData.getCcdCaseId()),
                EventType.SEND_FURTHER_EVIDENCE_ERROR.getCcdType(),
                "Failed to issue further evidence",
                "Review document tab to see document(s) that haven't been issued, then use the"
                        + " \"Reissue further evidence\" within next step and select affected document(s) to re-send",
                idamService.getIdamTokens(),
                sscsCaseData -> caseData.setHmctsDwpState("failedSendingFurtherEvidence")
        );
    }

    private void setEvidenceIssuedFlagToYes(List<SscsDocument> sscsDocuments) {
        if (sscsDocuments != null) {
            for (SscsDocument doc : sscsDocuments) {
                if (doc.getValue() != null && doc.getValue().getEvidenceIssued() != null
                    && "No".equalsIgnoreCase(doc.getValue().getEvidenceIssued())) {
                    doc.getValue().setEvidenceIssued(YES.getValue());
                }
            }
        }
    }

    @Override
    public DispatchPriority getPriority() {
        return DispatchPriority.LATEST;
    }
}
