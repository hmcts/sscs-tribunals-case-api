package uk.gov.hmcts.reform.sscs.callback.handlers.evidenceshare;

import static java.util.Objects.requireNonNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.handlers.EvidenceCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.evidenceshare.domain.FurtherEvidenceLetterType;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.FurtherEvidenceService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@Service
public class ReissueFurtherEvidenceHandler implements EvidenceCallbackHandler<SscsCaseData> {

    private final FurtherEvidenceService furtherEvidenceService;
    private final IdamService idamService;
    private final UpdateCcdCaseService updateCcdCaseService;

    @Autowired
    public ReissueFurtherEvidenceHandler(FurtherEvidenceService furtherEvidenceService,
                                         IdamService idamService,
                                         UpdateCcdCaseService updateCcdCaseService) {
        this.furtherEvidenceService = furtherEvidenceService;
        this.idamService = idamService;
        this.updateCcdCaseService = updateCcdCaseService;
    }

    @Override
    public void handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        final SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        ReissueArtifactUi reissueArtifactUi = caseData.getReissueArtifactUi();
        final AbstractDocument selectedDocument =
            getSelectedDocumentInUiFromCaseData(caseData, reissueArtifactUi);

        final DocumentType documentType = Arrays.stream(DocumentType.values())
            .filter(f -> f.getValue().equals(selectedDocument.getValue().getDocumentType()))
            .findFirst()
            .orElse(DocumentType.APPELLANT_EVIDENCE);

        List<FurtherEvidenceLetterType> allowedLetterTypes = getAllowedFurtherEvidenceLetterTypes(caseData);

        furtherEvidenceService.issue(Collections.singletonList(selectedDocument), caseData, documentType,
            allowedLetterTypes, getOtherPartyToResendOriginalSenderId(reissueArtifactUi, selectedDocument));

        if (CollectionUtils.isNotEmpty(allowedLetterTypes)) {
            updateCaseV2(caseData, selectedDocument);
        }
    }

    private String getOtherPartyToResendOriginalSenderId(ReissueArtifactUi reissueArtifactUi,
                                                         AbstractDocument selectedDocument) {
        String docOtherPartyOriginalSenderId = selectedDocument.getValue().getOriginalSenderOtherPartyId();
        String otherPartyToResendOriginalSender = null;
        if (isAnyOtherPartyOptionToResendMatchedDocOriginalSender(reissueArtifactUi, docOtherPartyOriginalSenderId)) {
            otherPartyToResendOriginalSender = docOtherPartyOriginalSenderId;
        }
        return otherPartyToResendOriginalSender;
    }

    private boolean isAnyOtherPartyOptionToResendMatchedDocOriginalSender(ReissueArtifactUi reissueArtifactUi,
                                                                          String originalSenderOtherPartyId) {
        return reissueArtifactUi.getOtherPartyOptions() != null
            && reissueArtifactUi.getOtherPartyOptions().stream()
            .map(OtherPartyOption::getValue)
            .anyMatch(otherPartyOption -> isToResendOtherPartyMatchedId(originalSenderOtherPartyId, otherPartyOption));
    }

    private boolean isToResendOtherPartyMatchedId(String originalSenderOtherPartyId,
                                                  OtherPartyOptionDetails otherPartyOption) {
        return YesNo.isYes(otherPartyOption.getResendToOtherParty())
            && otherPartyOption.getOtherPartyOptionId().equals(originalSenderOtherPartyId);
    }

    private AbstractDocument<? extends AbstractDocumentDetails> getSelectedDocumentInUiFromCaseData(SscsCaseData caseData,
                                                                                                    ReissueArtifactUi reissueArtifactUi) {
        return Stream.of(caseData.getSscsDocument(), caseData.getSscsWelshDocuments())
            .flatMap(this::getStreamIfNonNull)
            .filter(document -> isDocumentSelectedInUiEqualsToStreamDocument(reissueArtifactUi, document))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(getNoSelectedDocumentErrorMessage(caseData)));
    }

    private boolean isDocumentSelectedInUiEqualsToStreamDocument(ReissueArtifactUi reissueArtifactUi,
                                                                 AbstractDocument<? extends AbstractDocumentDetails> document) {
        String expectedUrl = reissueArtifactUi.getReissueFurtherEvidenceDocument().getValue().getCode();

        return (document.getValue().getDocumentLink() != null
                && document.getValue().getDocumentLink().getDocumentUrl().equals(expectedUrl)
                || (document.getValue().getEditedDocumentLink() != null
                && document.getValue().getEditedDocumentLink().getDocumentUrl().equals(expectedUrl)));
    }

    private Stream<? extends AbstractDocument<? extends AbstractDocumentDetails>> getStreamIfNonNull(List<? extends AbstractDocument<? extends AbstractDocumentDetails>> documents) {
        return documents == null ? null : documents.stream();
    }

    private String getNoSelectedDocumentErrorMessage(SscsCaseData caseData) {
        return String.format("Cannot find the selected document to reissue with url %s for caseId %s.",
            caseData.getReissueArtifactUi().getReissueFurtherEvidenceDocument().getValue().getCode(),
            caseData.getCcdCaseId());
    }

    private List<FurtherEvidenceLetterType> getAllowedFurtherEvidenceLetterTypes(SscsCaseData caseData) {
        final boolean resendToAppellant = YesNo.isYes(caseData.getReissueArtifactUi().getResendToAppellant());
        boolean resendToRepresentative = YesNo.isYes(caseData.getReissueArtifactUi().getResendToRepresentative());

        List<FurtherEvidenceLetterType> allowedLetterTypes = new ArrayList<>();
        if (resendToAppellant) {
            allowedLetterTypes.add(FurtherEvidenceLetterType.APPELLANT_LETTER);
        }
        if (resendToRepresentative) {
            allowedLetterTypes.add(FurtherEvidenceLetterType.REPRESENTATIVE_LETTER);
        }

        if (isThereAnyOtherPartyOtherThanRepToReissue(getOtherPartiesToReissue(caseData))) {
            allowedLetterTypes.add(FurtherEvidenceLetterType.OTHER_PARTY_LETTER);
        }

        if (isThereAnyOtherPartyRepToReissue(getOtherPartiesToReissue(caseData))) {
            allowedLetterTypes.add(FurtherEvidenceLetterType.OTHER_PARTY_REP_LETTER);
        }

        return allowedLetterTypes;
    }

    private boolean isThereAnyOtherPartyOtherThanRepToReissue(List<OtherPartyOption> otherPartiesToReissue) {
        return !otherPartiesToReissue.isEmpty()
            && otherPartiesToReissue.stream().map(OtherPartyOption::getValue)
            .anyMatch(otherPartyOption -> YesNo.isYes(otherPartyOption.getResendToOtherParty())
                && !otherPartyOption.getOtherPartyOptionName().contains("Representative"));
    }

    private boolean isThereAnyOtherPartyRepToReissue(List<OtherPartyOption> otherPartiesToReissue) {
        return !otherPartiesToReissue.isEmpty()
            && otherPartiesToReissue.stream().map(OtherPartyOption::getValue)
            .anyMatch(otherPartyOption -> YesNo.isYes(otherPartyOption.getResendToOtherParty())
                && otherPartyOption.getOtherPartyOptionName().contains("Representative"));
    }

    private List<OtherPartyOption> getOtherPartiesToReissue(SscsCaseData caseData) {
        return caseData.getReissueArtifactUi().getOtherPartyOptions() != null
            ? caseData.getReissueArtifactUi().getOtherPartyOptions().stream()
            .filter(otherPartyOption -> otherPartyOption.getValue().getResendToOtherParty().equals(YesNo.YES))
            .collect(Collectors.toList())
            : Collections.emptyList();
    }

    private void setReissueFlagsToNull(SscsCaseData sscsCaseData) {
        ReissueArtifactUi reissueArtifactUi = sscsCaseData.getReissueArtifactUi();
        reissueArtifactUi.setReissueFurtherEvidenceDocument(null);
        reissueArtifactUi.setResendToAppellant(null);
        reissueArtifactUi.setResendToRepresentative(null);
        reissueArtifactUi.setResendToDwp(null);
        reissueArtifactUi.setOtherPartyOptions(null);
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        ReissueArtifactUi reissueArtifactUi = callback.getCaseDetails().getCaseData().getReissueArtifactUi();
        return callbackType.equals(CallbackType.SUBMITTED)
            && callback.getEvent() == EventType.REISSUE_FURTHER_EVIDENCE
            && Objects.nonNull(reissueArtifactUi.getReissueFurtherEvidenceDocument())
            && Objects.nonNull(reissueArtifactUi.getReissueFurtherEvidenceDocument().getValue())
            && Objects.nonNull(reissueArtifactUi.getReissueFurtherEvidenceDocument().getValue().getCode())
            && furtherEvidenceService.canHandleAnyDocument(callback.getCaseDetails().getCaseData().getSscsDocument());
    }

    private void setEvidenceIssuedFlagToYes(AbstractDocument doc) {
        if (doc.getValue().getEvidenceIssued() != null && doc.getValue().getEvidenceIssued().equals("No")) {
            doc.getValue().setEvidenceIssued("Yes");
        }
    }

    private void updateCaseV2(final SscsCaseData caseData, AbstractDocument selectedDocument) {

        updateCcdCaseService.updateCaseV2(
                Long.valueOf(caseData.getCcdCaseId()),
                EventType.UPDATE_CASE_ONLY.getCcdType(),
                "Update case data only",
                determineDescription(selectedDocument),
                idamService.getIdamTokens(),
                sscsCaseDetails -> {
                    final AbstractDocument documentIssuedFlagToYes =
                            getSelectedDocumentInUiFromCaseData(sscsCaseDetails.getData(), sscsCaseDetails.getData().getReissueArtifactUi());
                    setEvidenceIssuedFlagToYes(documentIssuedFlagToYes);
                    setReissueFlagsToNull(sscsCaseDetails.getData());
                });
    }

    public String determineDescription(AbstractDocument document) {
        final boolean hasResizedDocs = document.getValue().getResizedDocumentLink() != null;

        final String baseDescription = "Update document evidence reissued flags after re-issuing further evidence to DWP";

        return !hasResizedDocs ? baseDescription : baseDescription + " and attached resized document(s)";
    }

    @Override
    public DispatchPriority getPriority() {
        return DispatchPriority.LATEST;
    }
}
