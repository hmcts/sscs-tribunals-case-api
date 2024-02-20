package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.ActionFurtherEvidenceAboutToSubmitHandler.checkWarningsAndErrors;
import static uk.gov.hmcts.reform.sscs.domain.wrapper.pdf.PdfState.PASSWORD_ENCRYPTED;
import static uk.gov.hmcts.reform.sscs.domain.wrapper.pdf.PdfState.UNREADABLE;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.domain.wrapper.pdf.PdfState;
import uk.gov.hmcts.reform.sscs.model.PartyItemList;
import uk.gov.hmcts.reform.sscs.service.FooterService;

@Component
@Slf4j
@AllArgsConstructor
public class ActionFurtherEvidenceMidEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    public static final String POSTPONEMENTS_REVIEWED_BY_TCW_OR_JUDGE = "Postponement requests need to be reviewed by TCW or Judge";
    public static final String POSTPONEMENTS_NOT_POSSIBLE_GAPS = "Postponement requests cannot be made for hearings listed in GAPS";
    public static final String POSTPONEMENT_IN_HEARING_STATE = "You can only submit a postponement request on cases in 'hearing' state";
    public static final String ONLY_ONE_POSTPONEMENT_AT_A_TIME = "Only one request for postponement can be submitted at a time";
    public static final String OTHER_PARTY_ORIGINAL_PARTY_ERROR = "You cannot select 'Other party hearing preferences' as a Document Type as an Other party not selected from Original Sender list";
    private final FooterService footerService;
    @Value("${feature.postHearings.enabled}")
    private final boolean isPostHearingsEnabled;
    @Value("${feature.postHearings.enabled}")
    private final boolean isPostHearingsBEnabled;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.MID_EVENT)
                && callback.getEvent() == EventType.ACTION_FURTHER_EVIDENCE;
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

        buildSscsDocumentFromScan(sscsCaseData, caseDetails.getId(), callback.isIgnoreWarnings(),
                preSubmitCallbackResponse, isPostHearingsEnabled, isPostHearingsBEnabled);

        if (showPostponementDetailsPage(preSubmitCallbackResponse)) {
            sscsCaseData.getPostponementRequest().setShowPostponementDetailsPage(YesNo.YES);
        }

        validatePostponementRequests(caseDetails, sscsCaseData, preSubmitCallbackResponse);
        validateOtherPartyHearingPartyRequests(sscsCaseData, preSubmitCallbackResponse);

        return preSubmitCallbackResponse;
    }

    private void validatePostponementRequests(CaseDetails<SscsCaseData> caseDetails, SscsCaseData sscsCaseData,
                                              PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {

        Optional<ScannedDocument> postponementRequest = emptyIfNull(sscsCaseData.getScannedDocuments()).stream()
                .filter(doc -> StringUtils.isNotBlank(doc.getValue().getType())
                        && doc.getValue().getType().equals(DocumentType.POSTPONEMENT_REQUEST.getValue())).findAny();

        if (postponementRequest.isPresent()) {
            if (!(sscsCaseData.getFurtherEvidenceAction().getValue().getCode()
                    .equals(FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_TCW.getCode())
                    || sscsCaseData.getFurtherEvidenceAction().getValue().getCode()
                    .equals(FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getCode()))
            ) {
                preSubmitCallbackResponse.addError(POSTPONEMENTS_REVIEWED_BY_TCW_OR_JUDGE);
            }

            if (HearingRoute.GAPS.equals(sscsCaseData.getSchedulingAndListingFields().getHearingRoute())) {
                //since S&L is null then it's a GAPS which should not allow postponement request
                preSubmitCallbackResponse.addError(POSTPONEMENTS_NOT_POSSIBLE_GAPS);
            }

            if (!caseDetails.getState().equals(State.HEARING)) {
                preSubmitCallbackResponse.addError(POSTPONEMENT_IN_HEARING_STATE);
            }
        }
    }

    private void validateOtherPartyHearingPartyRequests(SscsCaseData sscsCaseData,
                                              PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {

        Optional<ScannedDocument> otherPartyHearingPreferenceDoc = emptyIfNull(sscsCaseData.getScannedDocuments()).stream()
                .filter(doc -> StringUtils.isNotBlank(doc.getValue().getType())
                        && doc.getValue().getType().equals(DocumentType.OTHER_PARTY_HEARING_PREFERENCES.getValue())).findAny();

        if (otherPartyHearingPreferenceDoc.isPresent()) {

            if (!PartyItemList.isOtherPartyItemType(sscsCaseData.getOriginalSender().getValue().getCode())) {
                preSubmitCallbackResponse.addError(OTHER_PARTY_ORIGINAL_PARTY_ERROR);
            }
        }
    }

    private boolean showPostponementDetailsPage(PreSubmitCallbackResponse<SscsCaseData> callbackResponse) {

        long requestHearingCount = getNumberOfPostponementRequests(callbackResponse.getData().getScannedDocuments());

        if (requestHearingCount > 1) {
            callbackResponse.addError(ONLY_ONE_POSTPONEMENT_AT_A_TIME);
        }

        return requestHearingCount == 1;
    }

    private long getNumberOfPostponementRequests(List<ScannedDocument> scannedDocuments) {
        return emptyIfNull(scannedDocuments).stream()
                .filter(doc -> doc.getValue().getType() != null
                        && doc.getValue().getType().equals(DocumentType.POSTPONEMENT_REQUEST.getValue()))
                .count();
    }

    private void buildSscsDocumentFromScan(SscsCaseData sscsCaseData, long caseId, Boolean ignoreWarnings,
                                           PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse,
                                           boolean isPostHearingsEnabled, boolean isPostHearingsBEnabled) {

        if (isEmpty(sscsCaseData.getScannedDocuments())) {
            preSubmitCallbackResponse.addError("Please add a scanned document");
            return;
        }
        List<PdfReadable> pdfReadableErrorList = getPdfReadableErrorList(sscsCaseData);

        if (pdfReadableErrorList.isEmpty()) {
            checkForWarningsAndErrorsOnScannedDocuments(sscsCaseData, ignoreWarnings,
                preSubmitCallbackResponse, isPostHearingsEnabled, isPostHearingsBEnabled);
        }

        addErrorsIfPdfsHaveErrors(caseId, preSubmitCallbackResponse, pdfReadableErrorList);

    }

    private void addErrorsIfPdfsHaveErrors(long caseId, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse, List<PdfReadable> pdfReadableErrorList) {
        addErrorsIfAnyPdfsAreUnreadable(caseId, preSubmitCallbackResponse, pdfReadableErrorList);
        addErrorsIfAnyPdfsAreEncrypted(caseId, preSubmitCallbackResponse, pdfReadableErrorList);
    }

    private void addErrorsIfAnyPdfsAreEncrypted(long caseId, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse, List<PdfReadable> pdfReadableErrorList) {
        List<String> encryptedPdfLinks = getPdfLinksWithError(pdfReadableErrorList, PASSWORD_ENCRYPTED);
        if (isNotEmpty(encryptedPdfLinks)) {
            addPdfEncryptedError(caseId, preSubmitCallbackResponse, encryptedPdfLinks);
        }
    }

    private void addErrorsIfAnyPdfsAreUnreadable(long caseId, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse, List<PdfReadable> pdfReadableErrorList) {
        List<String> unreadablePdfLinks = getPdfLinksWithError(pdfReadableErrorList, UNREADABLE);
        if (isNotEmpty(unreadablePdfLinks)) {
            addPdfUnreadableError(caseId, preSubmitCallbackResponse, unreadablePdfLinks);
        }
    }

    @NotNull
    private List<String> getPdfLinksWithError(List<PdfReadable> pdfReadableErrorList, PdfState pdfState) {
        return pdfReadableErrorList.stream()
                .filter(pdfReadable -> pdfReadable.getPdfState().equals(pdfState))
                .map(PdfReadable::getFilename)
                .toList();
    }

    private void checkForWarningsAndErrorsOnScannedDocuments(SscsCaseData sscsCaseData, Boolean ignoreWarnings,
                                                             PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse,
                                                             boolean isPostHearingsEnabled,
                                                             boolean isPostHearingsBEnabled) {
        emptyIfNull(sscsCaseData.getScannedDocuments())
                .forEach(scannedDocument -> checkWarningsAndErrors(
                        sscsCaseData,
                        scannedDocument,
                        sscsCaseData.getCcdCaseId(),
                        ignoreWarnings,
                        preSubmitCallbackResponse,
                        isPostHearingsEnabled,
                        isPostHearingsBEnabled));
    }

    @NotNull
    private List<PdfReadable> getPdfReadableErrorList(SscsCaseData sscsCaseData) {
        return emptyIfNull(sscsCaseData.getScannedDocuments()).stream()
                .filter(Objects::nonNull)
                .filter(doc -> doc.getValue() != null)
                .filter(doc -> doc.getValue().getUrl() != null)
                .filter(doc -> doc.getValue().getUrl().getDocumentUrl() != null)
                .map(doc -> new PdfReadable(doc.getValue().getFileName(), footerService.isReadablePdf(doc.getValue().getUrl().getDocumentUrl())))
                .filter(pdfReadable -> pdfReadable.getPdfState().equals(UNREADABLE) || pdfReadable.getPdfState().equals(PASSWORD_ENCRYPTED))
                .collect(toUnmodifiableList());
    }

    private void addPdfEncryptedError(long caseId, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse, List<String> encryptedPdfLinks) {
        preSubmitCallbackResponse.addError(
                "The below PDF document(s) cannot be password protected, please correct this");
        addFileErrors(encryptedPdfLinks, preSubmitCallbackResponse);
        log.error("{} – {} failed due to encrypted PDF(s)\n{}", caseId, EventType.ACTION_FURTHER_EVIDENCE,
                getFormattedFileUrl(encryptedPdfLinks));
    }

    private void addPdfUnreadableError(long caseId, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse, List<String> unreadablePdfLinks) {
        preSubmitCallbackResponse
                .addError("The below PDF document(s) are not readable, please correct this");
        addFileErrors(unreadablePdfLinks, preSubmitCallbackResponse);
        log.error("{} – {} failed due to broken PDF(s)\n{}", caseId, EventType.ACTION_FURTHER_EVIDENCE,
                getFormattedFileUrl(unreadablePdfLinks));
    }

    private String getFormattedFileUrl(List<String> errors) {
        StringBuilder fileUrls = new StringBuilder("");
        errors.forEach(
                error -> fileUrls.append(error).append("\n")
        );
        fileUrls.delete(fileUrls.lastIndexOf("\n"), fileUrls.length());
        return fileUrls.toString();
    }

    private void addFileErrors(List<String> errors, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        for (String error : errors) {
            preSubmitCallbackResponse.addError(error);
        }
    }

    @Getter
    @AllArgsConstructor
    private static class PdfReadable {
        private final String filename;
        private final PdfState pdfState;

    }
}
