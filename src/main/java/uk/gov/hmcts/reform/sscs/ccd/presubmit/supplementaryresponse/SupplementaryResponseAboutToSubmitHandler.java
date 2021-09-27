package uk.gov.hmcts.reform.sscs.ccd.presubmit.supplementaryresponse;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.apache.commons.collections4.ListUtils.union;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReferralReason.REVIEW_AUDIO_VIDEO_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.REVIEW_BY_JUDGE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.REVIEW_BY_TCW;
import static uk.gov.hmcts.reform.sscs.util.AudioVideoEvidenceUtil.setHasUnprocessedAudioVideoEvidenceFlag;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentSubtype;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.util.DocumentUtil;

@Service
@Slf4j
public class SupplementaryResponseAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {


    public static final String SUPPLEMENTARY_RESPONSE_DOCUMENT_CANNOT_BE_EMPTY = "Supplementary response document cannot be empty";

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");


        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.DWP_SUPPLEMENTARY_RESPONSE;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback.");
        }
        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        List<DwpResponseDocument> responseDocuments = new ArrayList<>();

        PreSubmitCallbackResponse<SscsCaseData> callbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        if (sscsCaseData.getDwpSupplementaryResponseDoc() != null && sscsCaseData.getDwpSupplementaryResponseDoc().getDocumentLink() != null) {
            responseDocuments.add(sscsCaseData.getDwpSupplementaryResponseDoc());
            sscsCaseData.setDwpSupplementaryResponseDoc(null);
        } else {
            callbackResponse.addError(SUPPLEMENTARY_RESPONSE_DOCUMENT_CANNOT_BE_EMPTY);
        }

        if (sscsCaseData.getDwpOtherDoc() != null && sscsCaseData.getDwpOtherDoc().getDocumentLink() != null) {
            if (DocumentUtil.isFileAMedia(sscsCaseData.getDwpOtherDoc().getDocumentLink())) {
                addAudioVideoEvidence(sscsCaseData);
                if (!REVIEW_BY_JUDGE.getId().equals(sscsCaseData.getInterlocReviewState())) {
                    sscsCaseData.setInterlocReviewState(REVIEW_BY_TCW.getId());
                }
                sscsCaseData.setInterlocReferralReason(REVIEW_AUDIO_VIDEO_EVIDENCE.getId());
            } else {
                responseDocuments.add(sscsCaseData.getDwpOtherDoc());
            }
            sscsCaseData.setDwpOtherDoc(null);
            sscsCaseData.setRip1Doc(null);
        }

        sscsCaseData.setShowRip1DocPage(null);

        if (responseDocuments.size() > 0) {
            sscsCaseData.setScannedDocuments(buildScannedDocsList(sscsCaseData, responseDocuments));
            sscsCaseData.setEvidenceHandled(YesNo.NO.getValue());
            sscsCaseData.setDwpState(DwpState.SUPPLEMENTARY_RESPONSE.getId());
        }

        setHasUnprocessedAudioVideoEvidenceFlag(sscsCaseData);
        return callbackResponse;
    }

    private void addAudioVideoEvidence(SscsCaseData sscsCaseData) {
        AudioVideoEvidence audioVideoEvidence = AudioVideoEvidence.builder()
                .value(AudioVideoEvidenceDetails.builder()
                        .documentLink(sscsCaseData.getDwpOtherDoc().getDocumentLink())
                        .fileName(sscsCaseData.getDwpOtherDoc().getDocumentLink().getDocumentFilename())
                        .rip1Document(sscsCaseData.getRip1Doc())
                        .dateAdded(LocalDate.now())
                        .partyUploaded(UploadParty.DWP)
                        .build())
                .build();

        if (sscsCaseData.getAudioVideoEvidence() == null) {
            sscsCaseData.setAudioVideoEvidence(new ArrayList<>());
        }
        sscsCaseData.getAudioVideoEvidence().add(audioVideoEvidence);
    }

    private List<ScannedDocument> buildScannedDocsList(SscsCaseData sscsCaseData, List<DwpResponseDocument> responseDocuments) {
        List<ScannedDocument> scannedDocs = new ArrayList<>();
        for (DwpResponseDocument responseDocument : responseDocuments) {
            ScannedDocument scannedDocument = ScannedDocument.builder().value(
                ScannedDocumentDetails.builder()
                        .type("other")
                        .url(responseDocument.getDocumentLink())
                        .fileName(responseDocument.getDocumentLink().getDocumentFilename())
                        .scannedDate(LocalDateTime.now().toString())
                        .subtype(DocumentSubtype.DWP_EVIDENCE.getValue())
                        .build()).build();

            scannedDocs.add(scannedDocument);
        }

        return union(
                emptyIfNull(sscsCaseData.getScannedDocuments()),
                emptyIfNull(scannedDocs)
        );
    }
}
