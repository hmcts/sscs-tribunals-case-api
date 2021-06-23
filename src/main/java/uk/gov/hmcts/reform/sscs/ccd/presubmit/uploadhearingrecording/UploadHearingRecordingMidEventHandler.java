package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploadhearingrecording;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import java.util.Objects;
import java.util.regex.Pattern;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.DocumentDownloadService;

@Component
@Slf4j
public class UploadHearingRecordingMidEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private static final Pattern RECORDING_FILE_PATTERN = Pattern.compile("(\\.mp3|\\.mp4)$");
    private final IdamService idamService;
    private DocumentDownloadService documentDownloadService;

    @Autowired
    public UploadHearingRecordingMidEventHandler(DocumentDownloadService documentDownloadService,
                                                 IdamService idamService) {
        this.documentDownloadService = documentDownloadService;
        this.idamService = idamService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.MID_EVENT)
            && callback.getEvent() == EventType.UPLOAD_HEARING_RECORDING;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);

        emptyIfNull(caseData.getSscsHearingRecordingCaseData().getHearingRecordings())
            .stream()
            .filter(Objects::nonNull)
            .filter(hearingRecording -> hearingRecording.getValue() != null)
            .filter(hearingRecording -> hearingRecording.getValue().getDocumentLink() != null)
            .map(hearingRecording -> hearingRecording.getValue().getDocumentLink())
            .filter(documentLink -> isInvalidFile(documentLink.getDocumentFilename()))
            .findAny().ifPresent(d -> response.addError("The file type you uploaded is not accepted"));

        if (!response.getErrors().isEmpty()) {
            return response;
        }

        emptyIfNull(caseData.getSscsHearingRecordingCaseData().getHearingRecordings())
            .stream()
            .filter(Objects::nonNull)
            .filter(hearingRecording -> hearingRecording.getValue() != null)
            .filter(hearingRecording -> hearingRecording.getValue().getDocumentLink() != null)
            .map(hearingRecording -> hearingRecording.getValue().getDocumentLink())
            .filter(documentLink -> documentDownloadService.getFileSize(documentLink.getDocumentBinaryUrl())
                > (500 * 1024 * 1024))
            .findAny().ifPresent(d -> response.addError("The upload file size is more than the allowed limit"));

        return response;
    }

    @NotNull
    boolean isInvalidFile(String fileName) {
        return !(RECORDING_FILE_PATTERN.matcher(fileName.toLowerCase()).find());
    }
}
