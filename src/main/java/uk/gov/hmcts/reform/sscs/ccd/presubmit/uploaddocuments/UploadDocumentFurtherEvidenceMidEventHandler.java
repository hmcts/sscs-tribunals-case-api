package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments.DocumentType.REQUEST_FOR_HEARING_RECORDING;
import static uk.gov.hmcts.reform.sscs.util.PartiesOnCaseUtil.getPartiesOnCase;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.domain.wrapper.pdf.PdfState;
import uk.gov.hmcts.reform.sscs.model.PartyItemList;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.service.HearingRecordingRequestService;
import uk.gov.hmcts.reform.sscs.util.DocumentUtil;

@Service
@Slf4j
public class UploadDocumentFurtherEvidenceMidEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final HearingRecordingRequestService hearingRecordingRequestService;
    private final FooterService footerService;

    @Autowired
    public UploadDocumentFurtherEvidenceMidEventHandler(HearingRecordingRequestService hearingRecordingRequestService,
                                                        FooterService footerService) {
        this.hearingRecordingRequestService = hearingRecordingRequestService;
        this.footerService = footerService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return (callbackType.equals(CallbackType.MID_EVENT)
                && callback.getEvent() == EventType.UPLOAD_DOCUMENT_FURTHER_EVIDENCE);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback.");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();
        PreSubmitCallbackResponse<SscsCaseData> callbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        log.info(String.format("Handling uploadDocumentFurtherEvidence event for caseId %s", sscsCaseData.getCcdCaseId()));

        checkForErrors(callbackResponse);
        buildRequestHearingPageIfRequired(callbackResponse);

        return callbackResponse;
    }

    private PreSubmitCallbackResponse<SscsCaseData> buildRequestHearingPageIfRequired(PreSubmitCallbackResponse<SscsCaseData> response) {
        if (showRequestHearingsPage(response)) {
            response.getData().getSscsHearingRecordingCaseData().setShowRequestingPartyPage(YesNo.YES);
            setPartiesToRequestInfoFrom(response.getData());

            if (response.getData().getSscsHearingRecordingCaseData().getRequestingParty() != null) {

                return hearingRecordingRequestService.buildHearingRecordingUi(response,
                        PartyItemList.findPartyItemByCode(response.getData().getSscsHearingRecordingCaseData().getRequestingParty().getValue().getCode()));
            }
        }
        return response;
    }

    private PreSubmitCallbackResponse<SscsCaseData> checkForErrors(PreSubmitCallbackResponse<SscsCaseData> response) {
        if (!validDraftFurtherEvidenceDocument(response.getData().getDraftSscsFurtherEvidenceDocument())) {
            response.addError("You need to provide a file and a document type");
        } else if (!isFileUploadedAValid(response.getData().getDraftSscsFurtherEvidenceDocument())) {
            response.addError("You need to upload PDF, MP3 or MP4 file only");
        }

        PdfState pdfState = isPdfReadable(response.getData().getDraftSscsFurtherEvidenceDocument());
        switch (pdfState) {
            case UNKNOWN, UNREADABLE -> {
                initDraftSscsFurtherEvidenceDocument(response.getData());
                response.addError("Your PDF Document is not readable.");
                return response;
            }
            case PASSWORD_ENCRYPTED -> {
                initDraftSscsFurtherEvidenceDocument(response.getData());
                response.addError("Your PDF Document cannot be password protected.");
                return response;
            }
            default -> {
            }
        }
        return response;
    }

    private void initDraftSscsFurtherEvidenceDocument(SscsCaseData caseData) {
        caseData.setDraftSscsFurtherEvidenceDocument(null);
    }

    private boolean showRequestHearingsPage(PreSubmitCallbackResponse<SscsCaseData> callbackResponse) {

        long requestHearingCount = countNumberOfHearingRecordingRequests(callbackResponse.getData().getDraftSscsFurtherEvidenceDocument());

        if (requestHearingCount > 1) {
            callbackResponse.addError("Only one request for hearing recording can be submitted at a time");
        }

        return requestHearingCount == 1;
    }

    private long countNumberOfHearingRecordingRequests(List<SscsFurtherEvidenceDoc> sscsFurtherEvidenceDocList) {
        return emptyIfNull(sscsFurtherEvidenceDocList).stream()
                .filter(e -> REQUEST_FOR_HEARING_RECORDING.getId().equals(e.getValue().getDocumentType()))
                .count();
    }

    private void setPartiesToRequestInfoFrom(SscsCaseData sscsCaseData) {
        List<DynamicListItem> listOptions = getPartiesOnCase(sscsCaseData);

        DynamicListItem selectedValue = sscsCaseData.getSscsHearingRecordingCaseData().getRequestingParty() != null
                && sscsCaseData.getSscsHearingRecordingCaseData().getRequestingParty().getValue() != null
                ? sscsCaseData.getSscsHearingRecordingCaseData().getRequestingParty().getValue() : listOptions.get(0);

        sscsCaseData.getSscsHearingRecordingCaseData().setRequestingParty(new DynamicList(selectedValue, listOptions));
    }

    private boolean validDraftFurtherEvidenceDocument(List<SscsFurtherEvidenceDoc> draftSscsFurtherEvidenceDocuments) {
        if (draftSscsFurtherEvidenceDocuments != null && !draftSscsFurtherEvidenceDocuments.isEmpty()) {
            return draftSscsFurtherEvidenceDocuments.stream()
                    .allMatch(doc -> {
                        String docType = doc.getValue() != null ? doc.getValue().getDocumentType() : null;
                        return isValidDocumentType(docType) && isFileUploaded(doc);
                    });
        }
        return false;
    }

    private boolean isFileUploadedAValid(List<SscsFurtherEvidenceDoc> draftSscsFurtherEvidenceDocuments) {
        return draftSscsFurtherEvidenceDocuments.stream().allMatch(doc ->
                DocumentUtil.isFileAPdf(doc.getValue().getDocumentLink())
                        || DocumentUtil.isFileAMedia(doc.getValue().getDocumentLink()));
    }

    private PdfState isPdfReadable(List<SscsFurtherEvidenceDoc> docs) {
        PdfState pdfState = PdfState.UNKNOWN;
        if (CollectionUtils.isNotEmpty(docs)) {
            for (SscsFurtherEvidenceDoc doc : docs) {
                if (DocumentUtil.isFileAPdf(doc.getValue().getDocumentLink())) {
                    pdfState = isPdfReadable(doc);
                    if (!PdfState.OK.equals(pdfState)) {
                        return pdfState;
                    }
                } else if (DocumentUtil.isFileAMedia(doc.getValue().getDocumentLink())) {
                    pdfState = PdfState.OK;
                }
            }
        }
        return pdfState;
    }

    private PdfState isPdfReadable(SscsFurtherEvidenceDoc doc) {
        if (doc.getValue().getDocumentLink() != null) {
            return footerService.isReadablePdf(doc.getValue().getDocumentLink().getDocumentUrl());
        }
        return PdfState.UNKNOWN;
    }

    private boolean isFileUploaded(SscsFurtherEvidenceDoc doc) {
        return doc.getValue().getDocumentLink() != null
                && isNotBlank(doc.getValue().getDocumentLink().getDocumentUrl());
    }

    private boolean isValidDocumentType(String docType) {
        return DocumentType.MEDICAL_EVIDENCE.getId().equals(docType)
                || DocumentType.OTHER_EVIDENCE.getId().equals(docType)
                || DocumentType.APPELLANT_EVIDENCE.getId().equals(docType)
                || DocumentType.REPRESENTATIVE_EVIDENCE.getId().equals(docType)
                || DocumentType.REQUEST_FOR_HEARING_RECORDING.getId().equals(docType);
    }
}
