package uk.gov.hmcts.reform.sscs.ccd.presubmit.dwpuploadresponse;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.REVIEW_BY_JUDGE;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.ResponseEventsAboutToSubmit;
import uk.gov.hmcts.reform.sscs.model.AppConstants;
import uk.gov.hmcts.reform.sscs.service.DwpDocumentService;

@Component
@Slf4j
public class DwpUploadResponseAboutToSubmitHandler extends ResponseEventsAboutToSubmit implements PreSubmitCallbackHandler<SscsCaseData> {

    private DwpDocumentService dwpDocumentService;

    private boolean dwpDocumentsBundleFeature;

    @Autowired
    public DwpUploadResponseAboutToSubmitHandler(DwpDocumentService dwpDocumentService,
             @Value("${feature.dwp-documents-bundle.enabled}") boolean dwpDocumentsBundleFeature) {
        this.dwpDocumentService = dwpDocumentService;
        this.dwpDocumentsBundleFeature = dwpDocumentsBundleFeature;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.DWP_UPLOAD_RESPONSE;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        if (sscsCaseData.getDwpFurtherInfo() == null) {
            preSubmitCallbackResponse.addError("Further information to assist the tribunal cannot be empty.");
        }

        setCaseCode(sscsCaseData, callback.getEvent());

        sscsCaseData.setDwpResponseDate(LocalDate.now().toString());

        String todayDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        moveDocsToCorrectCollection(sscsCaseData, todayDate);

        handleEditedDocuments(sscsCaseData, todayDate, preSubmitCallbackResponse);

        checkMandatoryFields(preSubmitCallbackResponse, sscsCaseData);

        return preSubmitCallbackResponse;
    }

    private void moveDocsToCorrectCollection(SscsCaseData sscsCaseData, String todayDate) {
        //FIXME: Clear this up after dwpDocumentsBundleFeature switched on
        if (sscsCaseData.getDwpAT38Document() != null) {
            sscsCaseData.setDwpAT38Document(buildDwpResponseDocumentWithDate(
                    AppConstants.DWP_DOCUMENT_AT38_FILENAME_PREFIX,
                    todayDate,
                    sscsCaseData.getDwpAT38Document().getDocumentLink()));

            if (dwpDocumentsBundleFeature) {
                dwpDocumentService.addToDwpDocuments(sscsCaseData, sscsCaseData.getDwpAT38Document(), DwpDocumentType.AT_38);
                sscsCaseData.setDwpAT38Document(null);
            }
        }

        if (sscsCaseData.getDwpResponseDocument() != null) {
            sscsCaseData.setDwpResponseDocument(buildDwpResponseDocumentWithDate(
                    AppConstants.DWP_DOCUMENT_RESPONSE_FILENAME_PREFIX,
                    todayDate,
                    sscsCaseData.getDwpResponseDocument().getDocumentLink()));

            if (dwpDocumentsBundleFeature) {
                dwpDocumentService.addToDwpDocuments(sscsCaseData, sscsCaseData.getDwpResponseDocument(), DwpDocumentType.DWP_RESPONSE);
                sscsCaseData.setDwpResponseDocument(null);
            }
        }

        if (sscsCaseData.getDwpEvidenceBundleDocument() != null) {
            sscsCaseData.setDwpEvidenceBundleDocument(buildDwpResponseDocumentWithDate(
                    AppConstants.DWP_DOCUMENT_EVIDENCE_FILENAME_PREFIX,
                    todayDate,
                    sscsCaseData.getDwpEvidenceBundleDocument().getDocumentLink()));

            if (dwpDocumentsBundleFeature) {
                dwpDocumentService.addToDwpDocuments(sscsCaseData, sscsCaseData.getDwpEvidenceBundleDocument(), DwpDocumentType.DWP_EVIDENCE_BUNDLE);
                sscsCaseData.setDwpEvidenceBundleDocument(null);
            }
        }

        if (sscsCaseData.getAppendix12Doc() != null) {
            dwpDocumentService.addToDwpDocuments(sscsCaseData, sscsCaseData.getAppendix12Doc(), DwpDocumentType.APPENDIX_12);
        }
    }

    private PreSubmitCallbackResponse<SscsCaseData> handleEditedDocuments(SscsCaseData sscsCaseData, String todayDate, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        if (sscsCaseData.getDwpEditedEvidenceBundleDocument() != null || sscsCaseData.getDwpEditedResponseDocument() != null) {
            if (sscsCaseData.getDwpEditedEvidenceBundleDocument() == null || sscsCaseData.getDwpEditedResponseDocument() == null) {
                preSubmitCallbackResponse.addError("You must submit both an edited response document and an edited evidence bundle");
                return preSubmitCallbackResponse;
            }
            if (sscsCaseData.getDwpEditedEvidenceReason() == null) {
                preSubmitCallbackResponse.addError("If edited evidence is added a reason must be selected");
                return preSubmitCallbackResponse;
            }

            sscsCaseData.setInterlocReviewState(REVIEW_BY_JUDGE.getId());

            if (StringUtils.equalsIgnoreCase(sscsCaseData.getDwpEditedEvidenceReason(), "phme")) {
                sscsCaseData.setInterlocReferralReason(InterlocReferralReason.PHME_REQUEST.getId());
            }

            //FIXME: Clear this up after dwpDocumentsBundleFeature switched on
            sscsCaseData.setDwpEditedResponseDocument(buildDwpResponseDocumentWithDate(
                    AppConstants.DWP_DOCUMENT_EDITED_RESPONSE_FILENAME_PREFIX,
                    todayDate,
                    sscsCaseData.getDwpEditedResponseDocument().getDocumentLink()));

            if (dwpDocumentsBundleFeature) {
                dwpDocumentService.addToDwpDocuments(sscsCaseData, sscsCaseData.getDwpEditedResponseDocument(), DwpDocumentType.DWP_EDITED_RESPONSE);
                sscsCaseData.setDwpEditedResponseDocument(null);
            }

            //FIXME: Clear this up after dwpDocumentsBundleFeature switched on
            sscsCaseData.setDwpEditedEvidenceBundleDocument(buildDwpResponseDocumentWithDate(
                    AppConstants.DWP_DOCUMENT_EDITED_EVIDENCE_FILENAME_PREFIX,
                    todayDate,
                    sscsCaseData.getDwpEditedEvidenceBundleDocument().getDocumentLink()));

            if (dwpDocumentsBundleFeature) {
                dwpDocumentService.addToDwpDocuments(sscsCaseData, sscsCaseData.getDwpEditedEvidenceBundleDocument(), DwpDocumentType.DWP_EDITED_EVIDENCE_BUNDLE);
                sscsCaseData.setDwpEditedEvidenceBundleDocument(null);
            }

            if (!StringUtils.equalsIgnoreCase(sscsCaseData.getDwpFurtherInfo(), "Yes")) {
                DynamicListItem reviewByJudgeItem = new DynamicListItem("reviewByJudge", null);

                if (sscsCaseData.getSelectWhoReviewsCase() == null) {
                    sscsCaseData.setSelectWhoReviewsCase(new DynamicList(reviewByJudgeItem, null));
                } else {
                    sscsCaseData.getSelectWhoReviewsCase().getListItems().add(reviewByJudgeItem);
                }
            }
        }
        return preSubmitCallbackResponse;
    }

    private DwpResponseDocument buildDwpResponseDocumentWithDate(String documentType, String dateForFile, DocumentLink documentLink) {

        if (documentLink.getDocumentFilename() == null) {
            return null;
        }

        String fileExtension = documentLink.getDocumentFilename().substring(documentLink.getDocumentFilename().lastIndexOf("."));
        return (DwpResponseDocument.builder()
                .documentFileName(documentType + " on " + dateForFile)
                .documentLink(
                        DocumentLink.builder()
                                .documentBinaryUrl(documentLink.getDocumentBinaryUrl())
                                .documentUrl(documentLink.getDocumentUrl())
                                .documentFilename(documentType + " on " + dateForFile + fileExtension)
                                .build()
                ).build());
    }

}