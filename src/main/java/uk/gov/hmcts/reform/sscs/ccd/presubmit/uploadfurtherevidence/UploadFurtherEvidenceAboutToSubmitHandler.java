package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploadfurtherevidence;

import static java.util.Collections.emptyList;
import static java.util.Collections.sort;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.lang3.StringUtils.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
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

@Service
@Slf4j
public class UploadFurtherEvidenceAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private static final List<String> ALLOWED_FILE_TYPES = Arrays.asList("pdf", "mp3", "mp4");
    private static final String PDF_FILE_TYPE = "pdf";
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
        final PreSubmitCallbackResponse<SscsCaseData>  preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
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
            if (isEmpty(preSubmitCallbackResponse.getErrors())) {
                addToSscsDocuments(sscsCaseData);
            }
        }
        if (isEmpty(preSubmitCallbackResponse.getErrors())) {
            sscsCaseData.setDraftFurtherEvidenceDocuments(null);
        }
        return preSubmitCallbackResponse;
    }

    private void addToSscsDocuments(SscsCaseData sscsCaseData) {
        List<SscsDocument> newSscsDocuments = sscsCaseData.getDraftFurtherEvidenceDocuments().stream().map(doc ->
                SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentLink(doc.getValue().getDocumentLink())
                .documentFileName(doc.getValue().getDocumentFileName())
                .documentType(doc.getValue().getDocumentType())
                .documentDateAdded(LocalDate.now().format(DateTimeFormatter.ISO_DATE))
                .build()).build()).collect(toList());
        List<SscsDocument> allDocuments = new ArrayList<>(ofNullable(sscsCaseData.getSscsDocument()).orElse(emptyList()));
        allDocuments.addAll(newSscsDocuments);
        sort(newSscsDocuments);
        sscsCaseData.setSscsDocument(allDocuments);
    }

    private boolean isFileAPdf(DraftSscsDocument doc) {
        return doc.getValue().getDocumentLink() != null
                && isNotBlank(doc.getValue().getDocumentLink().getDocumentUrl())
                && PDF_FILE_TYPE.equalsIgnoreCase(getExtension(doc.getValue().getDocumentLink().getDocumentFilename()));
    }

    private boolean isFileAPdfOrMedia(DraftSscsDocument doc) {
        return doc.getValue().getDocumentLink() != null
                && isNotBlank(doc.getValue().getDocumentLink().getDocumentUrl())
                && ALLOWED_FILE_TYPES.contains(lowerCase(getExtension(doc.getValue().getDocumentLink().getDocumentFilename())));
    }

}
