package uk.gov.hmcts.reform.sscs.ccd.presubmit.hmctsresponsereviewed;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.CHILD_SUPPORT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReferralReason.PHE_REQUEST;

import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.*;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.ResponseEventsAboutToSubmit;
import uk.gov.hmcts.reform.sscs.model.AppConstants;
import uk.gov.hmcts.reform.sscs.service.DwpDocumentService;


@Component
@Slf4j
public class HmctsResponseReviewedAboutToSubmitHandler extends ResponseEventsAboutToSubmit
    implements PreSubmitCallbackHandler<SscsCaseData> {
    private final DwpDocumentService dwpDocumentService;

    @Autowired
    public HmctsResponseReviewedAboutToSubmitHandler(DwpDocumentService dwpDocumentService) {
        this.dwpDocumentService = dwpDocumentService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && callback.getEvent() == EventType.HMCTS_RESPONSE_REVIEWED;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType,
                                                          Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse =
            new PreSubmitCallbackResponse<>(sscsCaseData);

        setCaseCode(preSubmitCallbackResponse, callback);
        checkMandatoryFields(preSubmitCallbackResponse, sscsCaseData);
        setDwpDocuments(sscsCaseData);

        if (isNull(sscsCaseData.getDwpResponseDate())) {
            sscsCaseData.setDwpResponseDate(LocalDate.now().toString());
        }

        validateInterlocReferralReason(sscsCaseData, preSubmitCallbackResponse);

        return preSubmitCallbackResponse;
    }

    private void validateInterlocReferralReason(SscsCaseData sscsCaseData,
                                                PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        if (sscsCaseData.isBenefitType(CHILD_SUPPORT)
            && sscsCaseData.getInterlocReferralReason() == PHE_REQUEST) {
            preSubmitCallbackResponse.addError("PHE request' is not a valid selection for child support cases");
        }
    }

    protected void setDwpDocuments(SscsCaseData sscsCaseData) {
        if (nonNull(sscsCaseData.getDwpDocuments())) {
            for (DwpDocument dwpDocument : sscsCaseData.getDwpDocuments()) {
                if (dwpDocument.getValue().getDocumentType().equals(DwpDocumentType.DWP_RESPONSE.getValue())) {
                    updateDocument(dwpDocument,
                        sscsCaseData.getDwpResponseDocument(),
                        AppConstants.DWP_DOCUMENT_RESPONSE_FILENAME_PREFIX);
                    sscsCaseData.setDwpResponseDocument(null);
                } else if (dwpDocument.getValue().getDocumentType().equals(DwpDocumentType.AT_38.getValue())) {
                    updateDocument(dwpDocument,
                        sscsCaseData.getDwpAT38Document(),
                        AppConstants.DWP_DOCUMENT_AT38_FILENAME_PREFIX);
                    sscsCaseData.setDwpAT38Document(null);
                } else if (dwpDocument.getValue().getDocumentType().equals(
                    DwpDocumentType.DWP_EVIDENCE_BUNDLE.getValue())) {
                    updateDocument(dwpDocument,
                        sscsCaseData.getDwpEvidenceBundleDocument(),
                        AppConstants.DWP_DOCUMENT_EVIDENCE_FILENAME_PREFIX);
                    sscsCaseData.setDwpEvidenceBundleDocument(null);
                }
            }
        }

        if (nonNull(sscsCaseData.getDwpAT38Document())
            || nonNull(sscsCaseData.getDwpEvidenceBundleDocument())
            || nonNull(sscsCaseData.getDwpResponseDocument())) {
            dwpDocumentService.moveDocsToCorrectCollection(sscsCaseData);
        }
    }

    private void updateDocument(DwpDocument dwpDocument,
                                DwpResponseDocument dwpResponseDocument,
                                String documentTypePrefix) {
        String todayDate = java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        if (nonNull(dwpResponseDocument) && nonNull(dwpResponseDocument.getDocumentLink())) {
            String fileExtension = dwpResponseDocument.getDocumentLink().getDocumentFilename()
                    .substring(dwpResponseDocument.getDocumentLink().getDocumentFilename().lastIndexOf("."));
            if (!dwpDocument.getValue().getDocumentLink().getDocumentUrl()
                    .equals(dwpResponseDocument.getDocumentLink().getDocumentUrl())) {
                dwpDocument.getValue().setDocumentLink(DocumentLink.builder()
                        .documentBinaryUrl(dwpResponseDocument.getDocumentLink().getDocumentBinaryUrl())
                        .documentUrl(dwpResponseDocument.getDocumentLink().getDocumentUrl())
                        .documentFilename(documentTypePrefix + " on " + todayDate + fileExtension)
                        .build());
                dwpDocument.getValue().setDocumentDateAdded(java.time.LocalDate.now().toString());
                dwpDocument.getValue().setDocumentFileName(documentTypePrefix + " on " + todayDate);
            }
        }
    }
}
