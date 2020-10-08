package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.*;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.*;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.*;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.BundleAdditionFilenameBuilder;
import uk.gov.hmcts.reform.sscs.service.FooterService;

@Component
@Slf4j
public class ActionFurtherEvidenceAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    private static final String FURTHER_EVIDENCE_RECEIVED = "furtherEvidenceReceived";
    private static final String COVERSHEET = "coversheet";
    public static final String YES = "Yes";

    private PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse;
    private final FooterService footerService;
    private final BundleAdditionFilenameBuilder bundleAdditionFilenameBuilder;

    @Autowired
    public ActionFurtherEvidenceAboutToSubmitHandler(FooterService footerService, BundleAdditionFilenameBuilder bundleAdditionFilenameBuilder) {
        this.footerService = footerService;
        this.bundleAdditionFilenameBuilder = bundleAdditionFilenameBuilder;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && callback.getEvent() == EventType.ACTION_FURTHER_EVIDENCE
            && caseData.getFurtherEvidenceAction() != null
            && caseData.getOriginalSender() != null;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        if (isIssueFurtherEvidenceToAllParties(callback.getCaseDetails().getCaseData().getFurtherEvidenceAction())) {
            checkAddressesValidToIssueEvidenceToAllParties(sscsCaseData);

            if (preSubmitCallbackResponse.getErrors().size() > 0) {
                return preSubmitCallbackResponse;
            }

            sscsCaseData.setDwpFurtherEvidenceStates(FURTHER_EVIDENCE_RECEIVED);
        }

        buildSscsDocumentFromScan(sscsCaseData, caseDetails.getState(), callback.isIgnoreWarnings());

        return preSubmitCallbackResponse;
    }

    private boolean isIssueFurtherEvidenceToAllParties(DynamicList furtherEvidenceActionList) {
        if (furtherEvidenceActionList != null && furtherEvidenceActionList.getValue() != null
            && isNotBlank(furtherEvidenceActionList.getValue().getCode())) {
            return furtherEvidenceActionList.getValue().getCode().equals(ISSUE_FURTHER_EVIDENCE.getCode());
        }
        return false;
    }

    private boolean isOtherDocumentTypeActionManually(DynamicList furtherEvidenceActionList) {
        if (furtherEvidenceActionList != null && furtherEvidenceActionList.getValue() != null
            && isNotBlank(furtherEvidenceActionList.getValue().getCode())) {
            return furtherEvidenceActionList.getValue().getCode().equals(OTHER_DOCUMENT_MANUAL.getCode());
        }
        return false;
    }

    public void checkAddressesValidToIssueEvidenceToAllParties(SscsCaseData sscsCaseData) {
        if (isAppellantOrAppointeeAddressInvalid(sscsCaseData)) {
            String party = null != sscsCaseData.getAppeal().getAppellant() && "yes".equalsIgnoreCase(sscsCaseData.getAppeal().getAppellant().getIsAppointee()) ? "Appointee" : "Appellant";
            preSubmitCallbackResponse.addError(buildErrorMessage(party, sscsCaseData.getCcdCaseId()));
        }
        if (isRepAddressInvalid(sscsCaseData)) {
            preSubmitCallbackResponse.addError((buildErrorMessage("Representative", sscsCaseData.getCcdCaseId())));
        }
    }

    private boolean isAppellantOrAppointeeAddressInvalid(SscsCaseData caseData) {
        if (null != caseData.getAppeal().getAppellant() && "yes".equalsIgnoreCase(caseData.getAppeal().getAppellant().getIsAppointee())) {
            return null == caseData.getAppeal().getAppellant().getAppointee()
                || isAddressInvalid(caseData.getAppeal().getAppellant().getAppointee().getAddress());
        } else {
            return null == caseData.getAppeal().getAppellant()
                || isAddressInvalid(caseData.getAppeal().getAppellant().getAddress());
        }
    }

    private boolean isRepAddressInvalid(SscsCaseData caseData) {
        Representative rep = caseData.getAppeal().getRep();

        return null != rep
            && "yes".equalsIgnoreCase(rep.getHasRepresentative())
            && isAddressInvalid(rep.getAddress());
    }

    private boolean isAddressInvalid(Address address) {
        return null == address
            || address.isAddressEmpty()
            || isBlank(address.getLine1())
            || isBlank(address.getPostcode());
    }

    private String buildErrorMessage(String party, String caseId) {
        log.info("Issuing further evidence to all parties rejected, {} is missing address details for caseId {}", party, caseId);

        return "Address details are missing for the " + party + ", please validate or process manually";
    }

    private void buildSscsDocumentFromScan(SscsCaseData sscsCaseData, State caseState, Boolean ignoreWarnings) {

        if (sscsCaseData.getScannedDocuments() != null) {
            for (ScannedDocument scannedDocument : sscsCaseData.getScannedDocuments()) {
                if (scannedDocument != null && scannedDocument.getValue() != null) {

                    if (ScannedDocumentType.CONFIDENTIALITY_REQUEST.getValue().equals(scannedDocument.getValue().getType())) {
                        setConfidentialCaseFields(sscsCaseData);
                    }

                    checkWarningsAndErrors(sscsCaseData, scannedDocument, sscsCaseData.getCcdCaseId(), ignoreWarnings);

                    List<SscsDocument> documents = new ArrayList<>();

                    if (!equalsIgnoreCase(scannedDocument.getValue().getType(), COVERSHEET)) {

                        SscsDocument sscsDocument = buildSscsDocument(sscsCaseData, scannedDocument, caseState);

                        if (REINSTATEMENT_REQUEST.getValue().equals(sscsDocument.getValue().getDocumentType())) {
                            if (isOtherDocumentTypeActionManually(sscsCaseData.getFurtherEvidenceAction())) {
                                setReinstateCaseFields(sscsCaseData);
                            }
                        }

                        documents.add(sscsDocument);
                        if (sscsCaseData.isLanguagePreferenceWelsh()) {
                            sscsCaseData.setTranslationWorkOutstanding(YES);
                            log.info("Set the TranslationWorkOutstanding flag to YES,  for case id : {}", sscsCaseData.getCcdCaseId());
                        }
                    }
                    if (sscsCaseData.getSscsDocument() != null) {
                        documents.addAll(sscsCaseData.getSscsDocument());
                    }

                    if (documents.size() > 0) {
                        sscsCaseData.setSscsDocument(documents);
                    }

                    sscsCaseData.setEvidenceHandled(YES);


                } else {
                    log.info("Not adding any scanned document as there aren't any or the type is a coversheet for case Id {}.", sscsCaseData.getCcdCaseId());
                }
            }
        } else {
            preSubmitCallbackResponse.addError("No further evidence to process");
        }

        sscsCaseData.setScannedDocuments(null);
    }

    private void setConfidentialCaseFields(SscsCaseData sscsCaseData) {
        if (OriginalSenderItemList.APPELLANT.getCode().equals(sscsCaseData.getOriginalSender().getValue().getCode())) {
            sscsCaseData.setConfidentialityRequestOutcomeAppellant(DatedRequestOutcome.builder()
                .requestOutcome(RequestOutcome.IN_PROGRESS).date(LocalDate.now()).build());
        } else if (OriginalSenderItemList.JOINT_PARTY.getCode().equals(sscsCaseData.getOriginalSender().getValue().getCode())) {
            sscsCaseData.setConfidentialityRequestOutcomeJointParty(DatedRequestOutcome.builder()
                .requestOutcome(RequestOutcome.IN_PROGRESS).date(LocalDate.now()).build());
        }
    }

    private void setReinstateCaseFields(SscsCaseData sscsCaseData) {
        sscsCaseData.setReinstatementRegistered(LocalDate.now());
        sscsCaseData.setReinstatementOutcome(RequestOutcome.IN_PROGRESS);
        sscsCaseData.setInterlocReviewState(InterlocReviewState.REVIEW_BY_JUDGE.getId());
        State previousState = sscsCaseData.getPreviousState();
        if (previousState == null || State.DORMANT_APPEAL_STATE == previousState || State.VOID_STATE == previousState) {
            log.info("{} setting previousState from {}} to interlocutoryReviewState}", sscsCaseData.getCcdCaseId(), previousState);
            sscsCaseData.setPreviousState(State.INTERLOCUTORY_REVIEW_STATE);
        }
    }

    private void checkWarningsAndErrors(SscsCaseData sscsCaseData, ScannedDocument scannedDocument, String caseId, boolean ignoreWarnings) {
        if (scannedDocument.getValue().getUrl() == null) {
            preSubmitCallbackResponse.addError("No document URL so could not process");
        }

        if (isBlank(scannedDocument.getValue().getFileName())) {
            preSubmitCallbackResponse.addError("No document file name so could not process");
        }

        if (isBlank(scannedDocument.getValue().getType())) {
            if (!ignoreWarnings) {
                preSubmitCallbackResponse.addWarning("Document type is empty, are you happy to proceed?");
            } else {
                log.info("Document type is empty for {}} - {}}", caseId, scannedDocument.getValue().getUrl().getDocumentUrl());
            }
        }
        if (equalsIgnoreCase(scannedDocument.getValue().getType(), COVERSHEET)) {
            if (!ignoreWarnings) {
                preSubmitCallbackResponse.addWarning("Coversheet will be ignored, are you happy to proceed?");
            } else {
                log.info("Coversheet not moved over for {}} - {}}", caseId, scannedDocument.getValue().getUrl().getDocumentUrl());
            }
        }

        if (ScannedDocumentType.CONFIDENTIALITY_REQUEST.getValue().equals(scannedDocument.getValue().getType())) {

            if (!SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getCode().equals(sscsCaseData.getFurtherEvidenceAction().getValue().getCode())
                && !INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE.getCode().equals(sscsCaseData.getFurtherEvidenceAction().getValue().getCode())) {
                preSubmitCallbackResponse.addError("Further evidence action must be 'Send to Interloc - Review by Judge' or 'Information received for Interloc - send to Judge' for a confidential document");
            }

            if (!OriginalSenderItemList.APPELLANT.getCode().equals(sscsCaseData.getOriginalSender().getValue().getCode())
                && !OriginalSenderItemList.JOINT_PARTY.getCode().equals(sscsCaseData.getOriginalSender().getValue().getCode())) {
                preSubmitCallbackResponse.addError("Original sender must be appellant or joint party for a confidential document");
            }
        }
    }

    private SscsDocument buildSscsDocument(SscsCaseData sscsCaseData, ScannedDocument scannedDocument, State caseState) {

        String scannedDate = null;
        if (scannedDocument.getValue().getScannedDate() != null) {
            scannedDate = LocalDateTime.parse(scannedDocument.getValue().getScannedDate()).toLocalDate().format(DateTimeFormatter.ISO_DATE);
        }

        DocumentLink url = scannedDocument.getValue().getUrl();

        DocumentType documentType = getSubtype(
                sscsCaseData.getFurtherEvidenceAction().getValue().getCode(), sscsCaseData.getOriginalSender().getValue().getCode(), scannedDocument);

        String bundleAddition = null;
        if (caseState != null && isIssueFurtherEvidenceToAllParties(sscsCaseData.getFurtherEvidenceAction())
            && (caseState.equals(State.DORMANT_APPEAL_STATE)
            || caseState.equals(State.RESPONSE_RECEIVED)
            || caseState.equals(State.READY_TO_LIST))) {
            log.info("adding footer appendix document link: {} and caseId {}", url, sscsCaseData.getCcdCaseId());

            String originalSenderCode = sscsCaseData.getOriginalSender().getValue().getCode();
            String documentFooterText = OriginalSenderItemList.APPELLANT.getCode().equals(originalSenderCode) ? "Appellant evidence" : "Representative evidence";

            bundleAddition = footerService.getNextBundleAddition(sscsCaseData.getSscsDocument());

            url = footerService.addFooter(url, documentFooterText, bundleAddition);
        }

        String fileName = bundleAdditionFilenameBuilder.build(documentType, bundleAddition, scannedDocument.getValue().getScannedDate());

        return SscsDocument.builder().value(SscsDocumentDetails.builder()
            .documentType(documentType.getValue())
            .documentFileName(fileName)
            .bundleAddition(bundleAddition)
            .documentLink(url)
            .documentDateAdded(scannedDate)
            .controlNumber(scannedDocument.getValue().getControlNumber())
            .evidenceIssued("No")
            .documentTranslationStatus(sscsCaseData.isLanguagePreferenceWelsh() ? SscsDocumentTranslationStatus.TRANSLATION_REQUIRED : null)
            .build()).build();
    }

    private DocumentType getSubtype(String furtherEvidenceActionItemCode, String originalSenderCode, ScannedDocument scannedDocument) {
        if (ScannedDocumentType.REINSTATEMENT_REQUEST.getValue().equals(scannedDocument.getValue().getType())) {
            return REINSTATEMENT_REQUEST;
        }
        if (ScannedDocumentType.CONFIDENTIALITY_REQUEST.getValue().equals(scannedDocument.getValue().getType())) {
            return CONFIDENTIALITY_REQUEST;
        }
        if (OTHER_DOCUMENT_MANUAL.getCode().equals(furtherEvidenceActionItemCode)) {
            return OTHER_DOCUMENT;
        }
        if (OriginalSenderItemList.APPELLANT.getCode().equals(originalSenderCode)) {
            return APPELLANT_EVIDENCE;
        }
        if (OriginalSenderItemList.REPRESENTATIVE.getCode().equals(originalSenderCode)) {
            return REPRESENTATIVE_EVIDENCE;
        }
        if (OriginalSenderItemList.DWP.getCode().equals(originalSenderCode)) {
            return DWP_EVIDENCE;
        }
        if (OriginalSenderItemList.JOINT_PARTY.getCode().equals(originalSenderCode)) {
            return JOINT_PARTY_EVIDENCE;
        }
        throw new IllegalStateException("document Type could not be worked out");
    }

}
