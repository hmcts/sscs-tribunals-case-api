package uk.gov.hmcts.reform.sscs.ccd.presubmit.dwpuploadresponse;

import static java.lang.String.format;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.CollectionUtils.*;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DWP_UPLOAD_RESPONSE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReferralReason.REVIEW_AUDIO_VIDEO_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.REVIEW_BY_JUDGE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.REVIEW_BY_TCW;
import static uk.gov.hmcts.reform.sscs.util.AudioVideoEvidenceUtil.setHasUnprocessedAudioVideoEvidenceFlag;
import static uk.gov.hmcts.reform.sscs.util.DocumentUtil.isFileAPdf;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import uk.gov.hmcts.reform.sscs.service.AddNoteService;
import uk.gov.hmcts.reform.sscs.service.DwpDocumentService;
import uk.gov.hmcts.reform.sscs.util.DateTimeUtils;

@Component
@Slf4j
public class DwpUploadResponseAboutToSubmitHandler extends ResponseEventsAboutToSubmit implements PreSubmitCallbackHandler<SscsCaseData> {

    private static final DateTimeFormatter DD_MM_YYYY_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    public static final int NEW_OTHER_PARTY_RESPONSE_DUE_DAYS = 14;
    private final DwpDocumentService dwpDocumentService;
    private final AddNoteService addNoteService;
    public static final String APPENDIX_12_DOC_NOT_FOR_SSCS5_CONFIDENTIALITY = "You cannot upload an Appendix 12 document when submitting Confidentiality documents";

    @Autowired
    public DwpUploadResponseAboutToSubmitHandler(DwpDocumentService dwpDocumentService, AddNoteService addNoteService) {
        this.dwpDocumentService = dwpDocumentService;
        this.addNoteService = addNoteService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == DWP_UPLOAD_RESPONSE;
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

        setCaseCode(preSubmitCallbackResponse, callback);

        sscsCaseData.setDwpResponseDate(LocalDate.now().toString());

        handleEditedDocuments(sscsCaseData, userAuthorisation);
        handleAudioVideoDocuments(sscsCaseData);
        dwpDocumentService.moveDocsToCorrectCollection(sscsCaseData);

        checkMandatoryFields(preSubmitCallbackResponse, sscsCaseData);

        setHasUnprocessedAudioVideoEvidenceFlag(sscsCaseData);

        checkSscs2Confidentiality(preSubmitCallbackResponse, sscsCaseData);

        if (Benefit.CHILD_SUPPORT.getShortName().equals(sscsCaseData.getAppeal().getBenefitType().getCode())
                && sscsCaseData.getOtherParties() != null) {
            assignNewOtherPartyData(sscsCaseData.getOtherParties(), DWP_UPLOAD_RESPONSE);
            updateOtherPartyUcb(sscsCaseData);
            if (sscsCaseData.getOtherParties().stream().anyMatch(o -> YesNo.isYes(o.getValue().getSendNewOtherPartyNotification()))) {
                sscsCaseData.setDirectionDueDate(DateTimeUtils.generateDwpResponseDueDate(NEW_OTHER_PARTY_RESPONSE_DUE_DAYS));
                sscsCaseData.setDwpDueDate(null);
            }
        }

        return preSubmitCallbackResponse;
    }

    private void checkSscs2Confidentiality(PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse, SscsCaseData sscsCaseData) {
        if (Benefit.CHILD_SUPPORT.getShortName().equals(sscsCaseData.getAppeal().getBenefitType().getCode())) {
            if (sscsCaseData.getDwpEditedEvidenceReason() == null) {
                if (otherPartyHasConfidentiality(sscsCaseData)) {
                    preSubmitCallbackResponse.addError("Other Party requires confidentiality, upload edited and unedited responses");
                    sscsCaseData.setIsConfidentialCase(YesNo.YES);
                }
                if (sscsCaseData.getAppeal().getAppellant() != null && YesNo.isYes(sscsCaseData.getAppeal().getAppellant().getConfidentialityRequired())) {
                    preSubmitCallbackResponse.addError("Appellant requires confidentiality, upload edited and unedited responses");
                    sscsCaseData.setIsConfidentialCase(YesNo.YES);
                }
            } else {
                if (otherPartyHasConfidentiality(sscsCaseData)) {
                    sscsCaseData.setIsConfidentialCase(YesNo.YES);
                }
                if (sscsCaseData.getAppeal().getAppellant() != null && YesNo.isYes(sscsCaseData.getAppeal().getAppellant().getConfidentialityRequired())) {
                    sscsCaseData.setIsConfidentialCase(YesNo.YES);
                }
            }
        }
    }

    private boolean otherPartyHasConfidentiality(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getOtherParties() != null) {
            Optional otherParty = sscsCaseData.getOtherParties().stream().filter(op -> YesNo.isYes(op.getValue().getConfidentialityRequired())).findAny();
            if (otherParty.isPresent()) {
                return true;
            }
        }
        return false;
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

        for (AudioVideoEvidence audioVideo : dwpAudioVideoEvidence) {
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

        dwpDocumentService.validateEditedEvidenceReason(sscsCaseData, preSubmitCallbackResponse,
                sscsCaseData.getDwpEditedEvidenceReason());

        validateDwpResponseDocuments(sscsCaseData, preSubmitCallbackResponse);

        validateDwpAudioVideoEvidence(sscsCaseData, preSubmitCallbackResponse);



        return preSubmitCallbackResponse;
    }



    private void validateDwpAudioVideoEvidence(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        if (sscsCaseData.getDwpUploadAudioVideoEvidence() != null) {
            for (AudioVideoEvidence audioVideoEvidence : sscsCaseData.getDwpUploadAudioVideoEvidence()) {
                if (audioVideoEvidence.getValue().getRip1Document() != null && audioVideoEvidence.getValue().getDocumentLink() == null) {
                    preSubmitCallbackResponse.addError("You must upload an audio/video document when submitting a RIP 1 document");
                }
            }
        }
    }

    private void validateDwpResponseDocuments(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        validateDwpResponseDocument(sscsCaseData.getDwpResponseDocument(), preSubmitCallbackResponse);

        validateDwpAt38Document(sscsCaseData.getDwpAT38Document(), preSubmitCallbackResponse);

        validateDwpEvidenceBundle(sscsCaseData, preSubmitCallbackResponse);

        if (sscsCaseData.getDwpEditedEvidenceReason() != null) {
            validateEditedDwpResponseDocument(sscsCaseData.getDwpEditedResponseDocument(), preSubmitCallbackResponse);

            validateEditedDwpEvidenceBundle(sscsCaseData.getDwpEditedEvidenceBundleDocument(), preSubmitCallbackResponse);

            validateAppendix12(sscsCaseData.getAppendix12Doc(), preSubmitCallbackResponse);
        }
    }

    private void validateAppendix12(DwpResponseDocument appendix12Document, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        if (appendix12Document != null && appendix12Document.getDocumentLink() != null) {
            validateDocumentIsAPdf("Appendix 12 document", appendix12Document.getDocumentLink(), preSubmitCallbackResponse);
        }
    }

    private void validateEditedDwpEvidenceBundle(DwpResponseDocument dwpResponseDocument, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        if (dwpResponseDocument == null || dwpResponseDocument.getDocumentLink() == null) {
            preSubmitCallbackResponse.addError("You must upload an edited DWP evidence bundle");
        } else {
            validateDocumentIsAPdf("DWP edited evidence bundle", dwpResponseDocument.getDocumentLink(), preSubmitCallbackResponse);
        }
    }

    private void validateEditedDwpResponseDocument(DwpResponseDocument dwpEditedResponseDocument, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        if (dwpEditedResponseDocument == null || dwpEditedResponseDocument.getDocumentLink() == null) {
            preSubmitCallbackResponse.addError("You must upload an edited DWP response document");
        } else {
            validateDocumentIsAPdf("DWP edited response document", dwpEditedResponseDocument.getDocumentLink(), preSubmitCallbackResponse);
        }
    }

    private void validateDwpEvidenceBundle(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        if (sscsCaseData.getDwpEvidenceBundleDocument() == null || sscsCaseData.getDwpEvidenceBundleDocument().getDocumentLink() == null) {
            preSubmitCallbackResponse.addError("DWP evidence bundle cannot be empty.");
        } else {
            validateDocumentIsAPdf("DWP evidence bundle", sscsCaseData.getDwpEvidenceBundleDocument().getDocumentLink(), preSubmitCallbackResponse);
        }
    }

    private void validateDwpAt38Document(DwpResponseDocument dwpResponseDocument, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        if (dwpResponseDocument != null && dwpResponseDocument.getDocumentLink() != null) {
            validateDocumentIsAPdf("DWP AT38 document", dwpResponseDocument.getDocumentLink(), preSubmitCallbackResponse);
        }
    }

    private void validateDwpResponseDocument(DwpResponseDocument dwpResponseDocument, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        if (dwpResponseDocument == null || dwpResponseDocument.getDocumentLink() == null) {
            preSubmitCallbackResponse.addError("DWP response document cannot be empty.");
        } else {
            validateDocumentIsAPdf("DWP response document", dwpResponseDocument.getDocumentLink(), preSubmitCallbackResponse);
        }
    }

    private void validateDocumentIsAPdf(String documentMessage, DocumentLink documentLink, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        if (!isFileAPdf(documentLink)) {
            preSubmitCallbackResponse.addError(format("%s must be a PDF.", documentMessage));
        }
    }

    private void handleEditedDocuments(SscsCaseData sscsCaseData, String userAuthorisation) {
        String todayDate = LocalDate.now().format(DD_MM_YYYY_FORMAT);
        if (sscsCaseData.getDwpEditedEvidenceBundleDocument() != null
                && sscsCaseData.getDwpEditedResponseDocument() != null
                && sscsCaseData.getDwpEditedResponseDocument().getDocumentLink() != null) {

            sscsCaseData.setInterlocReviewState(REVIEW_BY_JUDGE.getId());

            if (StringUtils.equalsIgnoreCase(sscsCaseData.getDwpEditedEvidenceReason(), "phme")) {
                sscsCaseData.setInterlocReferralReason(InterlocReferralReason.PHE_REQUEST.getId());
                sscsCaseData.setInterlocReferralDate(LocalDate.now().toString());
                String note = "Referred to interloc for review by judge - PHE request";
                addNoteService.addNote(userAuthorisation, sscsCaseData, note);
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
