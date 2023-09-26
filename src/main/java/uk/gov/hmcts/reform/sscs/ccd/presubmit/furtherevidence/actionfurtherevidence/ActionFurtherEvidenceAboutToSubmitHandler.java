package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence;

import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.RequestOutcome.GRANTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isNoOrNull;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.*;
import static uk.gov.hmcts.reform.sscs.model.PartyItemList.*;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.getOtherPartyName;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.ScannedDocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.model.PartyItemList;
import uk.gov.hmcts.reform.sscs.service.BundleAdditionFilenameBuilder;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;
import uk.gov.hmcts.reform.sscs.util.AddedDocumentsUtil;
import uk.gov.hmcts.reform.sscs.util.PartiesOnCaseUtil;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Slf4j
@RequiredArgsConstructor
@Service
public class ActionFurtherEvidenceAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    private static final Enum<EventType> EVENT_TYPE = EventType.ACTION_FURTHER_EVIDENCE;
    public static final String YES = YesNo.YES.getValue();
    public static final String NO = YesNo.NO.getValue();
    public static final String POSTPONEMENT_DETAILS_IS_MANDATORY = "Postponement Details is mandatory for postponement requests.";
    public static final String FURTHER_EVIDENCE_RECEIVED = "furtherEvidenceReceived";
    private static final String COVERSHEET = "coversheet";
    protected static final List<String> ACTIONS_THAT_REQUIRES_EVIDENCE_ISSUED_SET_TO_YES_AND_NOT_BULK_PRINTED = Stream.of(
                    OTHER_DOCUMENT_MANUAL, INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE, INFORMATION_RECEIVED_FOR_INTERLOC_TCW,
                    SEND_TO_INTERLOC_REVIEW_BY_JUDGE, SEND_TO_INTERLOC_REVIEW_BY_TCW)
            .map(FurtherEvidenceActionDynamicListItems::getCode)
            .toList();
    private static final Set<State> ADDITION_VALID_STATES = Set.of(State.DORMANT_APPEAL_STATE,
            State.RESPONSE_RECEIVED,
            State.READY_TO_LIST,
            State.LISTING_ERROR,
            State.HANDLING_ERROR,
            State.HEARING,
            State.NOT_LISTABLE,
            State.WITH_DWP,
            State.POST_HEARING);

    private static final Set<DocumentType> SENDER_VALID_STATES = Set.of(POSTPONEMENT_REQUEST,
            SET_ASIDE_APPLICATION);

    private final FooterService footerService;
    private final BundleAdditionFilenameBuilder bundleAdditionFilenameBuilder;
    private final UserDetailsService userDetailsService;
    private final AddedDocumentsUtil addedDocumentsUtil;

    @Value("${feature.postHearings.enabled}")
    private final boolean isPostHearingsEnabled;

    @Value("${feature.postHearingsB.enabled}")
    private final boolean isPostHearingsBEnabled;

    public static void checkWarningsAndErrors(SscsCaseData sscsCaseData, ScannedDocument scannedDocument, String caseId,
                                              boolean ignoreWarnings,
                                              PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse,
                                              boolean isPostHearingsEnabled,
                                              boolean isPostHearingsBEnabled) {

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

        String actionCode = sscsCaseData.getFurtherEvidenceAction().getValue().getCode();

        if (ScannedDocumentType.CONFIDENTIALITY_REQUEST.getValue().equals(scannedDocument.getValue().getType())) {

            if (isNoOrNull(sscsCaseData.getJointParty().getHasJointParty())) {
                preSubmitCallbackResponse.addError(
                    "Document type \"Confidentiality Request\" is invalid as there is no joint party on the case");
            }

            if (!SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getCode().equals(actionCode)
                && !INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE.getCode().equals(actionCode)) {
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

        String scannedDocumentType = scannedDocument.getValue().getType();

        if (isPostHearingApplicationWithWrongActionCode(actionCode, scannedDocumentType)) {
            preSubmitCallbackResponse.addError(String
                .format("'Further Evidence Action' must be set to '%s'",
                    SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getLabel()));
        }

        if (isCorrectionApplicationWithWrongActionCode(actionCode, scannedDocumentType)) {
            preSubmitCallbackResponse.addError(String
                .format("'Further Evidence Action' must be set to '%s' or '%s'",
                    SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getLabel(), ADMIN_ACTION_CORRECTION.getLabel()));
        }

        if (ScannedDocumentType.URGENT_HEARING_REQUEST.getValue().equals(scannedDocumentType)
                && !OTHER_DOCUMENT_MANUAL.getCode().equals(actionCode)) {
            preSubmitCallbackResponse.addError(String
                .format("Further evidence action must be '%s' for a %s", OTHER_DOCUMENT_MANUAL.getLabel(),
                    URGENT_HEARING_REQUEST.getLabel()));
        }

        if (PartiesOnCaseUtil.isChildSupportAppeal(sscsCaseData) && isNotEmpty(sscsCaseData.getOtherParties())
            && sscsCaseData.getOriginalSender() != null
            && sscsCaseData.getOriginalSender().getValue() != null
            && scannedDocument != null && scannedDocument.getValue() != null
            && scannedDocument.getValue().getOriginalSenderOtherPartyId() != null) {
            if (!sscsCaseData.getOriginalSender().getValue().getCode().equalsIgnoreCase(OTHER_PARTY.getCode() + scannedDocument.getValue().getOriginalSenderOtherPartyId())) {
                preSubmitCallbackResponse.addError("The PDF evidence does not match the Original Sender selected");
            }
        }

        if (isPostHearingsEnabled && SscsUtil.isGapsCase(sscsCaseData) && isPostHearingRequest(sscsCaseData, isPostHearingsBEnabled)) {
            preSubmitCallbackResponse.addError("Cannot upload post hearing requests on GAPS cases");
        }
    }

    private static boolean isPostHearingApplicationWithWrongActionCode(String actionCode, String scannedDocumentType) {
        boolean isDocTypeRequiresReviewByJudge =
            ScannedDocumentType.SET_ASIDE_APPLICATION.getValue().equals(scannedDocumentType)
            || ScannedDocumentType.STATEMENT_OF_REASONS_APPLICATION.getValue().equals(scannedDocumentType)
            || ScannedDocumentType.LIBERTY_TO_APPLY_APPLICATION.getValue().equals(scannedDocumentType)
            || ScannedDocumentType.PERMISSION_TO_APPEAL_APPLICATION.getValue().equals(scannedDocumentType)
            || ScannedDocumentType.POST_HEARING_OTHER.getValue().equals(scannedDocumentType);
        boolean isNotInterlocReviewByJudge = !SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getCode().equals(actionCode);
        return isDocTypeRequiresReviewByJudge && isNotInterlocReviewByJudge;
    }

    private static boolean isCorrectionApplicationWithWrongActionCode(String actionCode, String scannedDocumentType) {
        boolean isDocTypeCorrectionApplication = ScannedDocumentType.CORRECTION_APPLICATION.getValue().equals(scannedDocumentType);
        boolean isNotInterlocReviewByJudge = !SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getCode().equals(actionCode);
        boolean isNotAdminActionCorrection = !ADMIN_ACTION_CORRECTION.getCode().equals(actionCode);
        return isDocTypeCorrectionApplication
            && isNotInterlocReviewByJudge
            && isNotAdminActionCorrection;
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
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        if (!callback.isIgnoreWarnings()) {
            checkForWarnings(preSubmitCallbackResponse);
        }

        addedDocumentsUtil.clearAddedDocumentsBeforeEventSubmit(sscsCaseData);

        if (isFurtherEvidenceActionCode(callback.getCaseDetails().getCaseData().getFurtherEvidenceAction(),
            ISSUE_FURTHER_EVIDENCE.getCode())) {

            checkAddressesValidToIssueEvidenceToAllParties(sscsCaseData, preSubmitCallbackResponse);

            if (!preSubmitCallbackResponse.getErrors().isEmpty()) {
                return preSubmitCallbackResponse;
            }

            if (!State.WITH_DWP.equals(callback.getCaseDetails().getState()) && !isOriginalSenderDwp(sscsCaseData)) {
                sscsCaseData.setDwpFurtherEvidenceStates(FURTHER_EVIDENCE_RECEIVED);
                sscsCaseData.setDwpState(DwpState.FE_RECEIVED);
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

        if (isPostHearingsEnabled) {
            sscsCaseData.getPostHearing().setRequestType(null);

            if (isSetAsideApplication(sscsCaseData)) {
                sscsCaseData.getPostHearing().setRequestType(PostHearingRequestType.SET_ASIDE);
                if (isOriginalSenderDwp(sscsCaseData)) {
                    sscsCaseData.setDwpState(DwpState.SET_ASIDE_REQUESTED);
                }
            }

            if (isCorrectionApplication(sscsCaseData)) {
                sscsCaseData.getPostHearing().setRequestType(PostHearingRequestType.CORRECTION);
                if (isOriginalSenderDwp(sscsCaseData)) {
                    sscsCaseData.setDwpState(DwpState.CORRECTION_REQUESTED);
                }
            }

            if (isStatementOfReasonsApplication(sscsCaseData)) {
                sscsCaseData.getPostHearing().setRequestType(PostHearingRequestType.STATEMENT_OF_REASONS);
                if (isOriginalSenderDwp(sscsCaseData)) {
                    sscsCaseData.setDwpState(DwpState.STATEMENT_OF_REASONS_REQUESTED);
                }
            }

            if (isPostHearingsBEnabled) {
                if (isLibertyToApplyApplication(sscsCaseData)) {
                    sscsCaseData.getPostHearing().setRequestType(PostHearingRequestType.LIBERTY_TO_APPLY);
                    if (isOriginalSenderDwp(sscsCaseData)) {
                        sscsCaseData.setDwpState(DwpState.LIBERTY_TO_APPLY_REQUESTED);
                    }
                }

                if (isPermissionToAppealApplication(sscsCaseData)) {
                    sscsCaseData.getPostHearing().setRequestType(PostHearingRequestType.PERMISSION_TO_APPEAL);
                    if (isOriginalSenderDwp(sscsCaseData)) {
                        sscsCaseData.setDwpState(DwpState.PERMISSION_TO_APPEAL_REQUESTED);
                    }
                }
            }
        }

        buildSscsDocumentFromScan(sscsCaseData, caseDetails.getState(), callback.isIgnoreWarnings(), preSubmitCallbackResponse);

        return preSubmitCallbackResponse;
    }

    private static boolean isOriginalSenderDwp(SscsCaseData sscsCaseData) {
        return PartyItemList.DWP.getCode().equals(sscsCaseData.getOriginalSender().getValue().getCode());
    }

    private static boolean isDocumentType(DocumentType documentType, SscsCaseData sscsCaseData) {
        return emptyIfNull(sscsCaseData.getScannedDocuments()).stream()
            .anyMatch(doc -> doc.getValue() != null && isNotBlank(doc.getValue().getType())
                && documentType.getValue().equals(doc.getValue().getType()));
    }

    private static boolean isPostHearingRequest(SscsCaseData sscsCaseData, boolean isPostHearingsBEnabled) {
        boolean isPostHearingsBRequest = isPostHearingsBEnabled
            && (isLibertyToApplyApplication(sscsCaseData)
                || isPermissionToAppealApplication(sscsCaseData)
                || isPostHearingOther(sscsCaseData));

        return isSetAsideApplication(sscsCaseData)
            || isCorrectionApplication(sscsCaseData)
            || isStatementOfReasonsApplication(sscsCaseData)
            || isPostHearingsBRequest;
    }

    private boolean isPostponementRequest(SscsCaseData sscsCaseData) {
        return isDocumentType(POSTPONEMENT_REQUEST, sscsCaseData);
    }

    private static boolean isSetAsideApplication(SscsCaseData sscsCaseData) {
        return isDocumentType(SET_ASIDE_APPLICATION, sscsCaseData);
    }

    private static boolean isCorrectionApplication(SscsCaseData sscsCaseData) {
        return isDocumentType(CORRECTION_APPLICATION, sscsCaseData);
    }

    private static boolean isLibertyToApplyApplication(SscsCaseData sscsCaseData) {
        return isDocumentType(LIBERTY_TO_APPLY_APPLICATION, sscsCaseData);
    }

    private static boolean isStatementOfReasonsApplication(SscsCaseData sscsCaseData) {
        return isDocumentType(STATEMENT_OF_REASONS_APPLICATION, sscsCaseData);
    }
  
    private static boolean isPermissionToAppealApplication(SscsCaseData sscsCaseData) {
        return isDocumentType(PERMISSION_TO_APPEAL_APPLICATION, sscsCaseData);
    }

    private static boolean isPostHearingOther(SscsCaseData sscsCaseData) {
        return isDocumentType(POST_HEARING_OTHER, sscsCaseData);
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
                        preSubmitCallbackResponse, isPostHearingsEnabled, isPostHearingsBEnabled);

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
        } else {
            preSubmitCallbackResponse.addError("No further evidence to process");
        }

        addedDocumentsUtil.computeDocumentsAddedThisEvent(sscsCaseData, documentsAddedThisEvent, EVENT_TYPE);
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
        boolean isWarningAdded = !isBundleAdditionSelectedForActionType(sscsCaseData, scannedDocument) && !ignoreWarnings;

        if (isWarningAdded) {
            preSubmitCallbackResponse.addWarning(
                "No documents have been ticked to be added as an addition. These document(s) will NOT be added to "
                    + "the bundle. Are you sure?");
        }
        return isWarningAdded;
    }

    private void setReinstateCaseFieldsIfReinstatementRequest(SscsCaseData sscsCaseData, SscsDocument sscsDocument) {
        if (REINSTATEMENT_REQUEST.getValue().equals(sscsDocument.getValue().getDocumentType())) {
            if (checkIfPossibleToReinstateCaseFields(sscsCaseData.getFurtherEvidenceAction())) {
                setReinstateCaseFields(sscsCaseData);
            }
        }
    }

    private boolean checkIfPossibleToReinstateCaseFields(DynamicList furtherEvidenceAction) {
        return isFurtherEvidenceActionCode(furtherEvidenceAction, ISSUE_FURTHER_EVIDENCE.getCode())
                || isFurtherEvidenceActionCode(furtherEvidenceAction, OTHER_DOCUMENT_MANUAL.getCode())
                || isFurtherEvidenceActionCode(furtherEvidenceAction, INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE.getCode())
                || isFurtherEvidenceActionCode(furtherEvidenceAction, INFORMATION_RECEIVED_FOR_INTERLOC_TCW.getCode())
                || isFurtherEvidenceActionCode(furtherEvidenceAction, SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getCode())
                || isFurtherEvidenceActionCode(furtherEvidenceAction, SEND_TO_INTERLOC_REVIEW_BY_TCW.getCode());

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
            sscsCaseData.setInterlocReviewState(InterlocReviewState.REVIEW_BY_JUDGE);
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

        DocumentType documentType = getScannedDocumentType(sscsCaseData.getOriginalSender().getValue().getCode(), scannedDocument);

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

        if (isOriginalSenderValidForBundleAddition(documentType)) {
            requestingParty = originalSenderCode;
        }

        String fileName = bundleAdditionFilenameBuilder
            .build(documentType, bundleAddition, scannedDocument.getValue().getScannedDate());

        YesNo evidenceIssued = isEvidenceIssuedAndShouldNotBeSentToBulkPrint(sscsCaseData.getFurtherEvidenceAction()) ? YesNo.YES : YesNo.NO;

        String originalSenderOtherPartyId = scannedDocument.getValue().getOriginalSenderOtherPartyId();
        String originalSenderOtherPartyName = scannedDocument.getValue().getOriginalSenderOtherPartyName();

        if (originalSenderOtherPartyId == null) {
            originalSenderOtherPartyId = findOriginalSenderOtherPartyId(documentType, sscsCaseData.getOriginalSender().getValue().getCode());
            originalSenderOtherPartyName = getOtherPartyName(sscsCaseData, originalSenderOtherPartyId);
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
            .originalSenderOtherPartyName(originalSenderOtherPartyName)
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
            && isNotBlank(furtherEvidenceActionList.getValue().getCode())) {
            return ACTIONS_THAT_REQUIRES_EVIDENCE_ISSUED_SET_TO_YES_AND_NOT_BULK_PRINTED.contains(furtherEvidenceActionList.getValue().getCode());
        }
        return false;
    }

    private boolean isBundleAdditionSelectedForActionType(SscsCaseData sscsCaseData, ScannedDocument scannedDocument) {
        return isFurtherEvidenceActionCode(sscsCaseData.getFurtherEvidenceAction(), ISSUE_FURTHER_EVIDENCE.getCode())
            || ((isFurtherEvidenceActionCode(sscsCaseData.getFurtherEvidenceAction(), OTHER_DOCUMENT_MANUAL.getCode())
            || isFurtherEvidenceActionCode(sscsCaseData.getFurtherEvidenceAction(),
            SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getCode())
            || isFurtherEvidenceActionCode(sscsCaseData.getFurtherEvidenceAction(),
            SEND_TO_INTERLOC_REVIEW_BY_TCW.getCode())
            || isFurtherEvidenceActionCode(sscsCaseData.getFurtherEvidenceAction(),
            ADMIN_ACTION_CORRECTION.getCode())
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
            ADMIN_ACTION_CORRECTION.getCode())
            || isFurtherEvidenceActionCode(sscsCaseData.getFurtherEvidenceAction(),
            INFORMATION_RECEIVED_FOR_INTERLOC_TCW.getCode())
            || isFurtherEvidenceActionCode(sscsCaseData.getFurtherEvidenceAction(),
            INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE.getCode()))
            && YES.equalsIgnoreCase(scannedDocument.getValue().getIncludeInBundle())));
    }

    private Boolean isCaseStateAdditionValid(State caseState) {
        return ADDITION_VALID_STATES.contains(caseState);
    }

    private Boolean isOriginalSenderValidForBundleAddition(DocumentType documentType) {
        return SENDER_VALID_STATES.contains(documentType);
    }

    private DocumentType getScannedDocumentType(String originalSenderCode, ScannedDocument scannedDocument) {
        String docType = scannedDocument.getValue().getType();
        if (ScannedDocumentType.REINSTATEMENT_REQUEST.getValue().equals(docType)) {
            return REINSTATEMENT_REQUEST;
        }
        if (ScannedDocumentType.CONFIDENTIALITY_REQUEST.getValue().equals(docType)) {
            return CONFIDENTIALITY_REQUEST;
        }
        if (ScannedDocumentType.URGENT_HEARING_REQUEST.getValue().equals(docType)) {
            return URGENT_HEARING_REQUEST;
        }
        if (ScannedDocumentType.POSTPONEMENT_REQUEST.getValue().equals(docType)) {
            return POSTPONEMENT_REQUEST;
        }
        if (ScannedDocumentType.SET_ASIDE_APPLICATION.getValue().equals(docType)) {
            return SET_ASIDE_APPLICATION;
        }
        if (ScannedDocumentType.CORRECTION_APPLICATION.getValue().equals(docType)) {
            return CORRECTION_APPLICATION;
        }
        if (ScannedDocumentType.STATEMENT_OF_REASONS_APPLICATION.getValue().equals(docType)) {
            return STATEMENT_OF_REASONS_APPLICATION;
        }
        if (ScannedDocumentType.LIBERTY_TO_APPLY_APPLICATION.getValue().equals(docType)) {
            return LIBERTY_TO_APPLY_APPLICATION;
        }
        if (ScannedDocumentType.PERMISSION_TO_APPEAL_APPLICATION.getValue().equals(docType)) {
            return PERMISSION_TO_APPEAL_APPLICATION;
        }
        if (ScannedDocumentType.POST_HEARING_OTHER.getValue().equals(docType)) {
            return POST_HEARING_OTHER;
        }

        final Optional<DocumentType> optionalDocumentType = stream(PartyItemList.values())
            .filter(f -> originalSenderCode.startsWith(f.getCode()))
            .findFirst()
            .map(PartyItemList::getDocumentType);

        if (optionalDocumentType.isPresent()) {
            return optionalDocumentType.get();
        }
        throw new IllegalStateException("document Type could not be worked out");
    }

}
