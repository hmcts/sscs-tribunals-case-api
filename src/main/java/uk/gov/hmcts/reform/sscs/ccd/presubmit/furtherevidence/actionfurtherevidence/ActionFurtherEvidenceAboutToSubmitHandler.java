package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence;

import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.RequestOutcome.GRANTED;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.*;
import static uk.gov.hmcts.reform.sscs.model.PartyItemList.APPELLANT;
import static uk.gov.hmcts.reform.sscs.model.PartyItemList.JOINT_PARTY;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.*;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.model.PartyItemList;
import uk.gov.hmcts.reform.sscs.service.BundleAdditionFilenameBuilder;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;
import uk.gov.hmcts.reform.sscs.util.AddedDocumentsUtil;

@Component
@Slf4j
public class ActionFurtherEvidenceAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    private static final Enum<EventType> EVENT_TYPE = EventType.ACTION_FURTHER_EVIDENCE;
    public static final String YES = YesNo.YES.getValue();
    public static final String NO = YesNo.NO.getValue();
    public static final String POSTPONEMENT_DETAILS_IS_MANDATORY = "Postponement Details is mandatory for postponement requests.";
    public static final String FURTHER_EVIDENCE_RECEIVED = "furtherEvidenceReceived";
    private static final String COVERSHEET = "coversheet";
    protected static final List<String> ACTIONS_THAT_REQUIRES_EVIDENCE_ISSUED_SET_TO_YES_AND_NOT_BULK_PRINTED = List.of(
            OTHER_DOCUMENT_MANUAL, INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE, INFORMATION_RECEIVED_FOR_INTERLOC_TCW,
            SEND_TO_INTERLOC_REVIEW_BY_JUDGE, SEND_TO_INTERLOC_REVIEW_BY_TCW).stream()
        .map(FurtherEvidenceActionDynamicListItems::getCode)
        .collect(Collectors.toUnmodifiableList());
    private static final Set<State> ADDITION_VALID_STATES = Set.of(State.DORMANT_APPEAL_STATE, State.RESPONSE_RECEIVED, State.READY_TO_LIST, State.HEARING, State.NOT_LISTABLE, State.WITH_DWP);

    private final FooterService footerService;
    private final BundleAdditionFilenameBuilder bundleAdditionFilenameBuilder;
    private final UserDetailsService userDetailsService;
    private final AddedDocumentsUtil addedDocumentsUtil;

    @Autowired
    public ActionFurtherEvidenceAboutToSubmitHandler(FooterService footerService,
                                                     BundleAdditionFilenameBuilder bundleAdditionFilenameBuilder,
                                                     UserDetailsService userDetailsService,
                                                     AddedDocumentsUtil addedDocumentsUtil) {
        this.footerService = footerService;
        this.bundleAdditionFilenameBuilder = bundleAdditionFilenameBuilder;
        this.userDetailsService = userDetailsService;
        this.addedDocumentsUtil = addedDocumentsUtil;
    }

    public static void checkWarningsAndErrors(SscsCaseData sscsCaseData, ScannedDocument scannedDocument, String caseId,
                                              boolean ignoreWarnings,
                                              PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {

        if (scannedDocument.getValue().getUrl() == null) {
            preSubmitCallbackResponse.addError("No document URL so could not process");
        }

        if (!YesNo.YES.equals(preSubmitCallbackResponse.getData().getIsConfidentialCase())
            && scannedDocument.getValue().getEditedUrl() != null) {
            preSubmitCallbackResponse
                .addError("Case is not marked as confidential so cannot upload an edited document");
        }

        if (isBlank(scannedDocument.getValue().getFileName())) {
            preSubmitCallbackResponse.addError("No document file name so could not process");
        }

        if (isBlank(scannedDocument.getValue().getType())) {
            if (!ignoreWarnings) {
                preSubmitCallbackResponse.addWarning("Document type is empty, are you happy to proceed?");
            } else {
                log.info("Document type is empty for {}} - {}}", caseId,
                    scannedDocument.getValue().getUrl() == null ? null :
                        scannedDocument.getValue().getUrl().getDocumentUrl());
            }
        }
        if (equalsIgnoreCase(scannedDocument.getValue().getType(), COVERSHEET)) {
            if (!ignoreWarnings) {
                preSubmitCallbackResponse.addWarning("Coversheet will be ignored, are you happy to proceed?");
            } else {
                log.info("Coversheet not moved over for {}} - {}}", caseId,
                    scannedDocument.getValue().getUrl().getDocumentUrl());
            }
        }

        if (ScannedDocumentType.CONFIDENTIALITY_REQUEST.getValue().equals(scannedDocument.getValue().getType())) {

            if (!YES.equalsIgnoreCase(sscsCaseData.getJointParty())) {
                preSubmitCallbackResponse.addError(
                    "Document type \"Confidentiality Request\" is invalid as there is no joint party on the case");
            }

            if (!SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getCode()
                .equals(sscsCaseData.getFurtherEvidenceAction().getValue().getCode())
                && !INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE.getCode()
                .equals(sscsCaseData.getFurtherEvidenceAction().getValue().getCode())) {
                preSubmitCallbackResponse.addError(
                    "Further evidence action must be 'Send to Interloc - Review by Judge' or 'Information received for Interloc - send to Judge' for a confidential document");
            }

            if (!APPELLANT.getCode()
                .equals(sscsCaseData.getOriginalSender().getValue().getCode())
                && !JOINT_PARTY.getCode()
                .equals(sscsCaseData.getOriginalSender().getValue().getCode())) {
                preSubmitCallbackResponse
                    .addError("Original sender must be appellant or joint party for a confidential document");
            }
        }

        if (ScannedDocumentType.URGENT_HEARING_REQUEST.getValue().equals((scannedDocument.getValue().getType()))
            && !OTHER_DOCUMENT_MANUAL.getCode().equals(sscsCaseData.getFurtherEvidenceAction().getValue().getCode())) {
            preSubmitCallbackResponse.addError(String
                .format("Further evidence action must be '%s' for a %s", OTHER_DOCUMENT_MANUAL.getLabel(),
                    URGENT_HEARING_REQUEST.getLabel()));
        }
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && callback.getEvent() == EVENT_TYPE
            && caseData.getFurtherEvidenceAction() != null
            && caseData.getOriginalSender() != null;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse =
            new PreSubmitCallbackResponse<>(sscsCaseData);

        if (!callback.isIgnoreWarnings()) {
            checkForWarnings(preSubmitCallbackResponse);
        }

        if (isFurtherEvidenceActionCode(callback.getCaseDetails().getCaseData().getFurtherEvidenceAction(),
            ISSUE_FURTHER_EVIDENCE.getCode())) {

            checkAddressesValidToIssueEvidenceToAllParties(sscsCaseData, preSubmitCallbackResponse);

            if (!preSubmitCallbackResponse.getErrors().isEmpty()) {
                return preSubmitCallbackResponse;
            }

            if (!State.WITH_DWP.equals(callback.getCaseDetails().getState())) {
                sscsCaseData.setDwpFurtherEvidenceStates(FURTHER_EVIDENCE_RECEIVED);
                sscsCaseData.setDwpState(DwpState.FE_RECEIVED.getId());
            }

        }

        if (isPostponementRequest(sscsCaseData)) {
            String details = sscsCaseData.getPostponementRequest().getPostponementRequestDetails();
            if (StringUtils.isBlank(details)) {
                preSubmitCallbackResponse.addError(POSTPONEMENT_DETAILS_IS_MANDATORY);
            } else {
                if (sscsCaseData.getAppealNotePad() == null) {
                    sscsCaseData.setAppealNotePad(NotePad.builder().notesCollection(new ArrayList<>()).build());
                }
                sscsCaseData.getAppealNotePad().getNotesCollection()
                    .add(createPostponementRequestNote(userAuthorisation, details));
                sscsCaseData.setPostponementRequest(PostponementRequest.builder().unprocessedPostponementRequest(YesNo.YES).build());
            }
        }

        buildSscsDocumentFromScan(sscsCaseData, caseDetails.getState(), callback.isIgnoreWarnings(),
            preSubmitCallbackResponse);

        return preSubmitCallbackResponse;
    }

    private boolean isPostponementRequest(SscsCaseData sscsCaseData) {
        return emptyIfNull(sscsCaseData.getScannedDocuments()).stream()
            .anyMatch(doc -> doc.getValue() != null && StringUtils.isNotBlank(doc.getValue().getType())
                && doc.getValue().getType().equals(DocumentType.POSTPONEMENT_REQUEST.getValue()));
    }

    private Note createPostponementRequestNote(String userAuthorisation, String details) {
        return Note.builder().value(NoteDetails.builder().noteDetail(details)
            .author(userDetailsService.buildLoggedInUserName(userAuthorisation))
            .noteDate(LocalDate.now().toString()).build()).build();
    }

    private void checkForWarnings(PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        if (isConfidentialChildSupportCase(preSubmitCallbackResponse.getData())
            || (null != preSubmitCallbackResponse.getData().getConfidentialityRequestOutcomeAppellant()
            && GRANTED
            .equals(preSubmitCallbackResponse.getData().getConfidentialityRequestOutcomeAppellant().getRequestOutcome())
            && APPELLANT.getCode()
            .equals(preSubmitCallbackResponse.getData().getOriginalSender().getValue().getCode()))
            || (null != preSubmitCallbackResponse.getData().getConfidentialityRequestOutcomeJointParty()
            && GRANTED.equals(
            preSubmitCallbackResponse.getData().getConfidentialityRequestOutcomeJointParty().getRequestOutcome())
            && JOINT_PARTY.getCode()
            .equals(preSubmitCallbackResponse.getData().getOriginalSender().getValue().getCode()))) {
            preSubmitCallbackResponse.addWarning("This case has a confidentiality flag, ensure any evidence from the "
                + preSubmitCallbackResponse.getData().getOriginalSender().getValue().getLabel().toLowerCase()
                + " has confidential information redacted");
        }
    }

    private boolean isConfidentialChildSupportCase(SscsCaseData sscsCaseData) {
        return sscsCaseData.getAppeal().getBenefitType() != null
            && Benefit.CHILD_SUPPORT.getShortName().equalsIgnoreCase(sscsCaseData.getAppeal().getBenefitType().getCode())
            && YesNo.YES.equals(sscsCaseData.getIsConfidentialCase());
    }

    private boolean isFurtherEvidenceActionCode(DynamicList furtherEvidenceActionList, String code) {
        if (furtherEvidenceActionList != null && furtherEvidenceActionList.getValue() != null
            && isNotBlank(furtherEvidenceActionList.getValue().getCode())) {
            return furtherEvidenceActionList.getValue().getCode().equals(code);
        }
        return false;
    }

    public void checkAddressesValidToIssueEvidenceToAllParties(SscsCaseData sscsCaseData,
                                                               PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        if (isAppellantOrAppointeeAddressInvalid(sscsCaseData)) {
            String party = null != sscsCaseData.getAppeal().getAppellant()
                && YES.equalsIgnoreCase(sscsCaseData.getAppeal().getAppellant().getIsAppointee()) ? "Appointee" :
                "Appellant";
            preSubmitCallbackResponse.addError(buildErrorMessage(party, sscsCaseData.getCcdCaseId()));
        }
        if (isRepAddressInvalid(sscsCaseData)) {
            preSubmitCallbackResponse.addError((buildErrorMessage("Representative", sscsCaseData.getCcdCaseId())));
        }
    }

    private boolean isAppellantOrAppointeeAddressInvalid(SscsCaseData caseData) {
        if (null != caseData.getAppeal().getAppellant()
            && YES.equalsIgnoreCase(caseData.getAppeal().getAppellant().getIsAppointee())) {
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
            && YES.equalsIgnoreCase(rep.getHasRepresentative())
            && isAddressInvalid(rep.getAddress());
    }

    private boolean isAddressInvalid(Address address) {
        return null == address
            || address.isAddressEmpty()
            || isBlank(address.getLine1())
            || isBlank(address.getPostcode());
    }

    private String buildErrorMessage(String party, String caseId) {
        log.info("Issuing further evidence to all parties rejected, {} is missing address details for caseId {}", party,
            caseId);

        return "Address details are missing for the " + party + ", please validate or process manually";
    }

    private void buildSscsDocumentFromScan(SscsCaseData sscsCaseData, State caseState, Boolean ignoreWarnings,
                                           PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        List<String> documentsAddedThisEvent = new ArrayList<>();
        if (sscsCaseData.getScannedDocuments() != null) {
            for (ScannedDocument scannedDocument : sscsCaseData.getScannedDocuments()) {
                if (scannedDocument != null && scannedDocument.getValue() != null) {

                    checkWarningsAndErrors(sscsCaseData, scannedDocument, sscsCaseData.getCcdCaseId(), ignoreWarnings,
                        preSubmitCallbackResponse);

                    setCofidentialCaseFields(sscsCaseData, preSubmitCallbackResponse, scannedDocument);

                    if (warningAddedForBundleAddition(sscsCaseData, ignoreWarnings, preSubmitCallbackResponse,
                        scannedDocument)) {
                        return;
                    }

                    if (!equalsIgnoreCase(scannedDocument.getValue().getType(), COVERSHEET)) {
                        SscsDocument sscsDocument = buildSscsDocument(sscsCaseData, scannedDocument, caseState);
                        documentsAddedThisEvent.add(sscsDocument.getValue().getDocumentType());
                        addSscsDocumentToCaseData(sscsCaseData, sscsDocument);
                        setReinstateCaseFieldsIfReinstatementRequest(sscsCaseData, sscsDocument);
                        setTranslationWorkOutstanding(sscsCaseData);
                    }

                    sscsCaseData.setEvidenceHandled(YES);

                } else {
                    log.info(
                        "Not adding any scanned document as there aren't any or the type is a coversheet for case Id {}.",
                        sscsCaseData.getCcdCaseId());
                }
            }

            addedDocumentsUtil.computeDocumentsAddedThisEvent(sscsCaseData, documentsAddedThisEvent, EVENT_TYPE);

        } else {
            preSubmitCallbackResponse.addError("No further evidence to process");
        }

        sscsCaseData.setScannedDocuments(null);
    }

    private void addSscsDocumentToCaseData(SscsCaseData sscsCaseData, SscsDocument sscsDocument) {
        List<SscsDocument> documents = new ArrayList<>();
        documents.add(sscsDocument);

        if (sscsCaseData.getSscsDocument() != null) {
            documents.addAll(sscsCaseData.getSscsDocument());
        }

        sscsCaseData.setSscsDocument(documents);
    }

    private boolean warningAddedForBundleAddition(SscsCaseData sscsCaseData, Boolean ignoreWarnings,
                                                  PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse,
                                                  ScannedDocument scannedDocument) {
        //check warning for bundle addition
        if (!ignoreWarnings && !isBundleAdditionSelectedForActionType(sscsCaseData, scannedDocument)) {
            preSubmitCallbackResponse.addWarning(
                "No documents have been ticked to be added as an addition. These document(s) will NOT be added to "
                    + "the bundle. Are you sure?");
            return true;
        }
        return false;
    }

    private void setReinstateCaseFieldsIfReinstatementRequest(SscsCaseData sscsCaseData, SscsDocument sscsDocument) {
        if (REINSTATEMENT_REQUEST.getValue().equals(sscsDocument.getValue().getDocumentType())) {
            if (isFurtherEvidenceActionCode(sscsCaseData.getFurtherEvidenceAction(), OTHER_DOCUMENT_MANUAL.getCode())) {
                setReinstateCaseFields(sscsCaseData);
            }
        }
    }

    private void setTranslationWorkOutstanding(SscsCaseData sscsCaseData) {
        if (sscsCaseData.isLanguagePreferenceWelsh()) {
            sscsCaseData.setTranslationWorkOutstanding(YES);
            log.info("Set the TranslationWorkOutstanding flag to YES,  for case id : {}",
                sscsCaseData.getCcdCaseId());
        }
    }

    private void setCofidentialCaseFields(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData>
        preSubmitCallbackResponse, ScannedDocument scannedDocument) {
        if (ScannedDocumentType.CONFIDENTIALITY_REQUEST.getValue()
            .equals(scannedDocument.getValue().getType())) {
            if (preSubmitCallbackResponse.getErrors().size() == 0) {
                setConfidentialCaseFields(sscsCaseData);
            }
        }
    }

    private void setConfidentialCaseFields(SscsCaseData sscsCaseData) {
        if (APPELLANT.getCode().equals(sscsCaseData.getOriginalSender().getValue().getCode())) {
            sscsCaseData.setConfidentialityRequestOutcomeAppellant(DatedRequestOutcome.builder()
                .requestOutcome(RequestOutcome.IN_PROGRESS).date(LocalDate.now()).build());
        } else if (JOINT_PARTY.getCode()
            .equals(sscsCaseData.getOriginalSender().getValue().getCode())) {
            sscsCaseData.setConfidentialityRequestOutcomeJointParty(DatedRequestOutcome.builder()
                .requestOutcome(RequestOutcome.IN_PROGRESS).date(LocalDate.now()).build());
        }
    }

    private void setReinstateCaseFields(SscsCaseData sscsCaseData) {
        if (!sscsCaseData.isLanguagePreferenceWelsh()) {
            sscsCaseData.setReinstatementRegistered(LocalDate.now());
            sscsCaseData.setReinstatementOutcome(RequestOutcome.IN_PROGRESS);
            sscsCaseData.setInterlocReviewState(InterlocReviewState.REVIEW_BY_JUDGE.getId());
            State previousState = sscsCaseData.getPreviousState();
            if (previousState == null || State.DORMANT_APPEAL_STATE == previousState
                || State.VOID_STATE == previousState) {
                log.info("{} setting previousState from {} to interlocutoryReviewState", sscsCaseData.getCcdCaseId(),
                    previousState);
                sscsCaseData.setPreviousState(State.INTERLOCUTORY_REVIEW_STATE);
            }
        } else {
            log.info("{} suppressing reinstatement request fields for welsh case", sscsCaseData.getCcdCaseId());
        }
    }

    private SscsDocument buildSscsDocument(SscsCaseData sscsCaseData, ScannedDocument scannedDocument,
                                           State caseState) {

        String scannedDate = null;
        if (scannedDocument.getValue().getScannedDate() != null) {
            scannedDate = LocalDateTime.parse(scannedDocument.getValue().getScannedDate()).toLocalDate()
                .format(DateTimeFormatter.ISO_DATE);
        }

        DocumentLink url = scannedDocument.getValue().getUrl();

        DocumentType documentType = getSubtype(sscsCaseData.getOriginalSender().getValue().getCode(), scannedDocument);

        String bundleAddition = null;
        String originalSenderCode = sscsCaseData.getOriginalSender().getValue().getCode();
        if (caseState != null
            && isCorrectActionTypeForBundleAddition(sscsCaseData, scannedDocument)
            && isCaseStateAdditionValid(caseState)) {

            log.info("adding footer appendix document link: {} and caseId {}", url, sscsCaseData.getCcdCaseId());

            String documentFooterText = stream(PartyItemList.values())
                .filter(f -> f.getCode().equals(originalSenderCode))
                .findFirst()
                .map(PartyItemList::getDocumentFooter).orElse(EMPTY);

            bundleAddition = footerService.getNextBundleAddition(sscsCaseData.getSscsDocument());

            url = footerService.addFooter(url, documentFooterText, bundleAddition);
        }

        String requestingParty = null;
        if (documentType.equals(POSTPONEMENT_REQUEST)) {
            requestingParty = originalSenderCode;
        }

        String fileName = bundleAdditionFilenameBuilder
            .build(documentType, bundleAddition, scannedDocument.getValue().getScannedDate());

        YesNo evidenceIssued = isEvidenceIssuedAndShouldNotBeSentToBulkPrint(sscsCaseData.getFurtherEvidenceAction()) ? YesNo.YES : YesNo.NO;

        String originalSenderOtherPartyId = scannedDocument.getValue().getOriginalSenderOtherPartyId();

        if (originalSenderOtherPartyId == null) {
            originalSenderOtherPartyId = findOriginalSenderOtherPartyId(documentType, sscsCaseData.getOriginalSender().getValue().getCode());
        }

        return SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentType(documentType.getValue())
                .documentFileName(fileName)
                .originalPartySender(requestingParty)
                .bundleAddition(bundleAddition)
                .documentLink(url)
                .editedDocumentLink(scannedDocument.getValue().getEditedUrl())
                .documentDateAdded(scannedDate)
                .controlNumber(scannedDocument.getValue().getControlNumber())
                .evidenceIssued(evidenceIssued.getValue())
                .originalSenderOtherPartyId(originalSenderOtherPartyId)
                .originalSenderOtherPartyName(scannedDocument.getValue().getOriginalSenderOtherPartyName())
                .documentTranslationStatus(
                        sscsCaseData.isLanguagePreferenceWelsh() ? SscsDocumentTranslationStatus.TRANSLATION_REQUIRED : null)
                .build()).build();
    }

    private String findOriginalSenderOtherPartyId(DocumentType documentType, String originalSender) {

        if (OTHER_PARTY_EVIDENCE.equals(documentType) || OTHER_PARTY_REPRESENTATIVE_EVIDENCE.equals(documentType)) {
            String originalSenderOtherPartyId = originalSender.replaceAll("[A-Za-z]", "");
            return !originalSenderOtherPartyId.equals("") && originalSenderOtherPartyId != null ? originalSenderOtherPartyId : null;
        }
        return null;
    }

    public static boolean isEvidenceIssuedAndShouldNotBeSentToBulkPrint(DynamicList furtherEvidenceActionList) {
        if (furtherEvidenceActionList != null && furtherEvidenceActionList.getValue() != null
            && StringUtils.isNotBlank(furtherEvidenceActionList.getValue().getCode())) {
            return ACTIONS_THAT_REQUIRES_EVIDENCE_ISSUED_SET_TO_YES_AND_NOT_BULK_PRINTED.contains(furtherEvidenceActionList.getValue().getCode());
        }
        return false;
    }

    private boolean isBundleAdditionSelectedForActionType(SscsCaseData sscsCaseData, ScannedDocument
        scannedDocument) {
        return isFurtherEvidenceActionCode(sscsCaseData.getFurtherEvidenceAction(), ISSUE_FURTHER_EVIDENCE.getCode())
            || ((isFurtherEvidenceActionCode(sscsCaseData.getFurtherEvidenceAction(), OTHER_DOCUMENT_MANUAL.getCode())
            || isFurtherEvidenceActionCode(sscsCaseData.getFurtherEvidenceAction(),
            SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getCode())
            || isFurtherEvidenceActionCode(sscsCaseData.getFurtherEvidenceAction(),
            SEND_TO_INTERLOC_REVIEW_BY_TCW.getCode())
            || isFurtherEvidenceActionCode(sscsCaseData.getFurtherEvidenceAction(),
            INFORMATION_RECEIVED_FOR_INTERLOC_TCW.getCode())
            || isFurtherEvidenceActionCode(sscsCaseData.getFurtherEvidenceAction(),
            INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE.getCode()))
            && (YES.equalsIgnoreCase(scannedDocument.getValue().getIncludeInBundle())
            || NO.equalsIgnoreCase(scannedDocument.getValue().getIncludeInBundle())));
    }

    private boolean isCorrectActionTypeForBundleAddition(SscsCaseData sscsCaseData, ScannedDocument scannedDocument) {
        return (isFurtherEvidenceActionCode(sscsCaseData.getFurtherEvidenceAction(), ISSUE_FURTHER_EVIDENCE.getCode())
            || ((isFurtherEvidenceActionCode(sscsCaseData.getFurtherEvidenceAction(), OTHER_DOCUMENT_MANUAL.getCode())
            || isFurtherEvidenceActionCode(sscsCaseData.getFurtherEvidenceAction(),
            SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getCode())
            || isFurtherEvidenceActionCode(sscsCaseData.getFurtherEvidenceAction(),
            SEND_TO_INTERLOC_REVIEW_BY_TCW.getCode())
            || isFurtherEvidenceActionCode(sscsCaseData.getFurtherEvidenceAction(),
            INFORMATION_RECEIVED_FOR_INTERLOC_TCW.getCode())
            || isFurtherEvidenceActionCode(sscsCaseData.getFurtherEvidenceAction(),
            INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE.getCode()))
            && YES.equalsIgnoreCase(scannedDocument.getValue().getIncludeInBundle())));
    }

    private Boolean isCaseStateAdditionValid(State caseState) {
        return ADDITION_VALID_STATES.contains(caseState);
    }

    private DocumentType getSubtype(String originalSenderCode, ScannedDocument scannedDocument) {
        if (ScannedDocumentType.REINSTATEMENT_REQUEST.getValue().equals(scannedDocument.getValue().getType())) {
            return REINSTATEMENT_REQUEST;
        }
        if (ScannedDocumentType.CONFIDENTIALITY_REQUEST.getValue().equals(scannedDocument.getValue().getType())) {
            return CONFIDENTIALITY_REQUEST;
        }
        if (ScannedDocumentType.URGENT_HEARING_REQUEST.getValue().equals(scannedDocument.getValue().getType())) {
            return URGENT_HEARING_REQUEST;
        }
        if (ScannedDocumentType.POSTPONEMENT_REQUEST.getValue().equals(scannedDocument.getValue().getType())) {
            return POSTPONEMENT_REQUEST;
        }

        String originalSenderStripped  = originalSenderCode.replaceAll("\\d","");

        final Optional<DocumentType> optionalDocumentType = stream(PartyItemList.values())
                .filter(f -> f.getCode().startsWith(originalSenderStripped))
                .findFirst()
                .map(PartyItemList::getDocumentType);
        if (optionalDocumentType.isPresent()) {
            return optionalDocumentType.get();
        }
        throw new IllegalStateException("document Type could not be worked out");
    }

}
