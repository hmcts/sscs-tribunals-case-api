package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploadfurtherevidence;

import static java.util.Collections.emptyList;
import static java.util.Collections.sort;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReferralReason.REVIEW_AUDIO_VIDEO_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.REVIEW_BY_JUDGE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.REVIEW_BY_TCW;
import static uk.gov.hmcts.reform.sscs.util.AudioVideoEvidenceUtil.getOriginalSender;
import static uk.gov.hmcts.reform.sscs.util.AudioVideoEvidenceUtil.isValidAudioVideoDocumentType;
import static uk.gov.hmcts.reform.sscs.util.AudioVideoEvidenceUtil.setHasUnprocessedAudioVideoEvidenceFlag;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.util.DocumentUtil;

@Service
@Slf4j
public class UploadFurtherEvidenceAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final boolean uploadAudioVideoEvidenceEnabled;

    @Autowired
    public UploadFurtherEvidenceAboutToSubmitHandler(@Value("${feature.upload-audio-video-evidence.enabled}") boolean uploadAudioVideoEvidenceEnabled) {
        this.uploadAudioVideoEvidenceEnabled = uploadAudioVideoEvidenceEnabled;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent().equals(EventType.UPLOAD_FURTHER_EVIDENCE);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        final SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        log.info("About to submit Upload Further Evidence caseID:  {}", sscsCaseData.getCcdCaseId());
        final PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
        if (isNotEmpty(sscsCaseData.getDraftFurtherEvidenceDocuments())) {
            sscsCaseData.getDraftFurtherEvidenceDocuments().forEach(doc -> {
                if (isBlank(doc.getValue().getDocumentType())) {
                    preSubmitCallbackResponse.addError("Please select a document type");
                }
                if (isBlank(doc.getValue().getDocumentFileName())) {
                    preSubmitCallbackResponse.addError("Please add a file name");
                }
                if (isNull(doc.getValue().getDocumentLink()) || isBlank(doc.getValue().getDocumentLink().getDocumentUrl())) {
                    preSubmitCallbackResponse.addError("Please upload a file");
                } else if (!uploadAudioVideoEvidenceEnabled && !isFileAPdf(doc)) {
                    preSubmitCallbackResponse.addError("You need to upload PDF documents only");
                } else if (uploadAudioVideoEvidenceEnabled && !isFileAPdfOrMedia(doc)) {
                    preSubmitCallbackResponse.addError("You need to upload PDF, MP3 or MP4 documents only");
                }
            });
            if (!equalsIgnoreCase(sscsCaseData.getInterlocReviewState(), REVIEW_BY_TCW.getId())
                    && !equalsIgnoreCase(sscsCaseData.getInterlocReviewState(), REVIEW_BY_JUDGE.getId())
                    && hasMp3OrMp4(sscsCaseData.getDraftFurtherEvidenceDocuments())) {
                preSubmitCallbackResponse.addError("As you have an MP3 or MP4 file, please set interlocutory review state to 'Review by TCW'");
            }

            if (incorrectTypeSelectedForAudioVideoEvidence(sscsCaseData.getDraftFurtherEvidenceDocuments())) {
                preSubmitCallbackResponse.addError("Type not accepted for AV evidence. Select a Type for the party that originally submitted the audio/video evidence");
            }

            SscsCaseData beforeData = callback.getCaseDetailsBefore().map(CaseDetails::getCaseData).orElse(null);

            if (beforeData != null && REVIEW_BY_JUDGE.getId().equals(beforeData.getInterlocReviewState())) {
                sscsCaseData.setInterlocReviewState(REVIEW_BY_JUDGE.getId());
            }

            if (isEmpty(preSubmitCallbackResponse.getErrors())) {
                addToSscsDocuments(sscsCaseData);
                addToAudioVideoEvidence(sscsCaseData);
            }
        }
        if (isEmpty(preSubmitCallbackResponse.getErrors())) {
            sscsCaseData.setDraftFurtherEvidenceDocuments(null);
        }
        setHasUnprocessedAudioVideoEvidenceFlag(sscsCaseData);
        return preSubmitCallbackResponse;
    }

    private boolean incorrectTypeSelectedForAudioVideoEvidence(List<DraftSscsDocument> draftSscsDocuments) {
        return draftSscsDocuments.stream().anyMatch(doc -> DocumentUtil.isFileAMedia(doc.getValue().getDocumentLink()) && !isValidAudioVideoDocumentType(doc.getValue().getDocumentType()));
    }

    private void addToSscsDocuments(SscsCaseData sscsCaseData) {
        List<SscsDocument> newSscsDocuments = sscsCaseData.getDraftFurtherEvidenceDocuments().stream()
                .filter(this::isFileAPdf)
                .map(doc ->
                        SscsDocument.builder().value(SscsDocumentDetails.builder()
                                .documentLink(doc.getValue().getDocumentLink())
                                .documentFileName(doc.getValue().getDocumentFileName())
                                .documentType(doc.getValue().getDocumentType())
                                .documentDateAdded(LocalDate.now().format(DateTimeFormatter.ISO_DATE))
                                .build()).build()).collect(toList());
        if (!newSscsDocuments.isEmpty()) {
            List<SscsDocument> allDocuments = new ArrayList<>(ofNullable(sscsCaseData.getSscsDocument()).orElse(emptyList()));
            allDocuments.addAll(newSscsDocuments);
            sort(newSscsDocuments);
            sscsCaseData.setSscsDocument(allDocuments);
        }
    }

    private void addToAudioVideoEvidence(SscsCaseData sscsCaseData) {
        List<AudioVideoEvidence> newAudioVideoEvidence = sscsCaseData.getDraftFurtherEvidenceDocuments().stream()
                .filter(doc -> DocumentUtil.isFileAMedia(doc.getValue().getDocumentLink()))
                .map(doc ->
                        AudioVideoEvidence.builder().value(AudioVideoEvidenceDetails.builder()
                                .documentLink(doc.getValue().getDocumentLink())
                                .fileName(doc.getValue().getDocumentFileName())
                                .dateAdded(LocalDate.now())
                                .partyUploaded(UploadParty.CTSC)
                                .originalPartySender(getOriginalSender(doc.getValue().getDocumentType()))
                                .build()).build()).collect(toList());
        if (!newAudioVideoEvidence.isEmpty()) {
            List<AudioVideoEvidence> audioVideoEvidence = new ArrayList<>(ofNullable(sscsCaseData.getAudioVideoEvidence()).orElse(emptyList()));
            audioVideoEvidence.addAll(newAudioVideoEvidence);
            sort(newAudioVideoEvidence);
            sscsCaseData.setAudioVideoEvidence(audioVideoEvidence);
            sscsCaseData.setInterlocReferralReason(REVIEW_AUDIO_VIDEO_EVIDENCE.getId());
        }
    }

    private boolean isFileAPdf(DraftSscsDocument doc) {
        return doc.getValue().getDocumentLink() != null && DocumentUtil.isFileAPdf(doc.getValue().getDocumentLink());
    }

    private boolean hasMp3OrMp4(List<DraftSscsDocument> draftSscsFurtherEvidenceDocument) {
        return ofNullable(draftSscsFurtherEvidenceDocument).orElse(emptyList()).stream().anyMatch(doc -> DocumentUtil.isFileAMedia(doc.getValue().getDocumentLink()));
    }

    private boolean isFileAPdfOrMedia(DraftSscsDocument doc) {
        return doc.getValue().getDocumentLink() != null
                && (DocumentUtil.isFileAPdf(doc.getValue().getDocumentLink()) || DocumentUtil.isFileAMedia(doc.getValue().getDocumentLink()));
    }

}
