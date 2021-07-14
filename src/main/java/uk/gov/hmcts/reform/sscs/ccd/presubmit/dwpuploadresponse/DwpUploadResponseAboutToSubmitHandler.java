package uk.gov.hmcts.reform.sscs.ccd.presubmit.dwpuploadresponse;

import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.CollectionUtils.*;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReferralReason.REVIEW_AUDIO_VIDEO_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.REVIEW_BY_JUDGE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.REVIEW_BY_TCW;
import static uk.gov.hmcts.reform.sscs.util.AudioVideoEvidenceUtil.setHasUnprocessedAudioVideoEvidenceFlag;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.*;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.ResponseEventsAboutToSubmit;
import uk.gov.hmcts.reform.sscs.model.AppConstants;
import uk.gov.hmcts.reform.sscs.service.DwpDocumentService;

@Component
@Slf4j
public class DwpUploadResponseAboutToSubmitHandler extends ResponseEventsAboutToSubmit implements PreSubmitCallbackHandler<SscsCaseData> {

    private static final DateTimeFormatter DD_MM_YYYY_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private final DwpDocumentService dwpDocumentService;

    @Autowired
    public DwpUploadResponseAboutToSubmitHandler(DwpDocumentService dwpDocumentService) {
        this.dwpDocumentService = dwpDocumentService;
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

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = checkErrors(sscsCaseData);

        if (isNotEmpty(preSubmitCallbackResponse.getErrors())) {
            return preSubmitCallbackResponse;
        }

        setCaseCode(sscsCaseData, callback.getEvent());

        sscsCaseData.setDwpResponseDate(LocalDate.now().toString());

        handleEditedDocuments(sscsCaseData);
        handleAudioVideoDocuments(sscsCaseData);
        dwpDocumentService.moveDocsToCorrectCollection(sscsCaseData);

        checkMandatoryFields(preSubmitCallbackResponse, sscsCaseData);

        setHasUnprocessedAudioVideoEvidenceFlag(sscsCaseData);

        return preSubmitCallbackResponse;
    }

    protected void handleAudioVideoDocuments(SscsCaseData sscsCaseData) {
        if (isEmpty(sscsCaseData.getDwpUploadAudioVideoEvidence())) {
            return;
        }

        List<AudioVideoEvidence> audioVideoEvidence = sscsCaseData.getAudioVideoEvidence();
        if (audioVideoEvidence == null) {
            audioVideoEvidence = new ArrayList<>();
            sscsCaseData.setAudioVideoEvidence(audioVideoEvidence);
        }

        List<AudioVideoEvidence> dwpAudioVideoEvidence = sscsCaseData.getDwpUploadAudioVideoEvidence();

        for (AudioVideoEvidence audioVideo: dwpAudioVideoEvidence) {
            audioVideo.getValue().setDateAdded(LocalDate.now());
            audioVideo.getValue().setFileName(audioVideo.getValue().getDocumentLink().getDocumentFilename());
            audioVideo.getValue().setPartyUploaded(UploadParty.DWP);
            sscsCaseData.getAudioVideoEvidence().add(audioVideo);
        }
        log.info("DWP audio video documents moved into case audio video {}", sscsCaseData.getCcdCaseId());
        sort(sscsCaseData.getAudioVideoEvidence());

        sscsCaseData.setDwpUploadAudioVideoEvidence(null);

        if (StringUtils.equalsIgnoreCase(sscsCaseData.getDwpEditedEvidenceReason(), "phme")) {
            sscsCaseData.setInterlocReviewState(REVIEW_BY_JUDGE.getId());
        } else {
            if (!REVIEW_BY_JUDGE.getId().equals(sscsCaseData.getInterlocReviewState())) {
                sscsCaseData.setInterlocReviewState(REVIEW_BY_TCW.getId());
            }
            sscsCaseData.setInterlocReferralReason(REVIEW_AUDIO_VIDEO_EVIDENCE.getId());
        }
    }

    private PreSubmitCallbackResponse<SscsCaseData> checkErrors(SscsCaseData sscsCaseData) {
        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        if (sscsCaseData.getDwpFurtherInfo() == null) {
            preSubmitCallbackResponse.addError("Further information to assist the tribunal cannot be empty.");
        }

        if (sscsCaseData.getDwpResponseDocument() == null) {
            preSubmitCallbackResponse.addError("DWP response document cannot be empty.");
        }

        if (sscsCaseData.getDwpEvidenceBundleDocument() == null) {
            preSubmitCallbackResponse.addError("DWP evidence bundle cannot be empty.");
        }

        if (sscsCaseData.getDwpEditedEvidenceReason() != null) {
            if (sscsCaseData.getDwpEditedResponseDocument() == null || sscsCaseData.getDwpEditedResponseDocument().getDocumentLink() == null) {
                preSubmitCallbackResponse.addError("You must upload an edited DWP response document");
            }

            if (sscsCaseData.getDwpEditedEvidenceBundleDocument() == null || sscsCaseData.getDwpEditedEvidenceBundleDocument().getDocumentLink() == null) {
                preSubmitCallbackResponse.addError("You must upload an edited DWP evidence bundle");
            }
        }
        if (sscsCaseData.getDwpUploadAudioVideoEvidence() != null) {
            for (AudioVideoEvidence audioVideoEvidence : sscsCaseData.getDwpUploadAudioVideoEvidence()) {
                if (audioVideoEvidence.getValue().getRip1Document() != null && audioVideoEvidence.getValue().getDocumentLink() == null) {
                    preSubmitCallbackResponse.addError("You must upload an audio/video document when submitting a RIP 1 document");
                }
            }
        }
        return preSubmitCallbackResponse;
    }

    private void handleEditedDocuments(SscsCaseData sscsCaseData) {
        String todayDate = LocalDate.now().format(DD_MM_YYYY_FORMAT);
        if (sscsCaseData.getDwpEditedEvidenceBundleDocument() != null
                && sscsCaseData.getDwpEditedResponseDocument() != null
                && sscsCaseData.getDwpEditedResponseDocument().getDocumentLink() != null) {

            sscsCaseData.setInterlocReviewState(REVIEW_BY_JUDGE.getId());

            if (StringUtils.equalsIgnoreCase(sscsCaseData.getDwpEditedEvidenceReason(), "phme")) {
                sscsCaseData.setInterlocReferralReason(InterlocReferralReason.PHME_REQUEST.getId());
            }

            sscsCaseData.setDwpEditedResponseDocument(buildDwpResponseDocumentWithDate(
                    AppConstants.DWP_DOCUMENT_EDITED_RESPONSE_FILENAME_PREFIX,
                    todayDate,
                    sscsCaseData.getDwpEditedResponseDocument().getDocumentLink()));

            sscsCaseData.setDwpEditedEvidenceBundleDocument(buildDwpResponseDocumentWithDate(
                    AppConstants.DWP_DOCUMENT_EDITED_EVIDENCE_FILENAME_PREFIX,
                    todayDate,
                    sscsCaseData.getDwpEditedEvidenceBundleDocument().getDocumentLink()));

            if (!StringUtils.equalsIgnoreCase(sscsCaseData.getDwpFurtherInfo(), "Yes")) {
                DynamicListItem reviewByJudgeItem = new DynamicListItem("reviewByJudge", null);

                if (sscsCaseData.getSelectWhoReviewsCase() == null) {
                    sscsCaseData.setSelectWhoReviewsCase(new DynamicList(reviewByJudgeItem, null));

                } else {
                    sscsCaseData.getSelectWhoReviewsCase().setValue(reviewByJudgeItem);
                }
            }
        }
    }

    private DwpResponseDocument buildDwpResponseDocumentWithDate(String documentType, String dateForFile, DocumentLink documentLink) {

        if (documentLink == null || documentLink.getDocumentFilename() == null) {
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
