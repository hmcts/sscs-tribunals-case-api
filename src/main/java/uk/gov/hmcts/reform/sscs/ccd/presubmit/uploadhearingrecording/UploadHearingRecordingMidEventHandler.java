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
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
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

        if ("selectHearing".equalsIgnoreCase(callback.getPageId()) && caseData.getSscsHearingRecordingCaseData().getSscsHearingRecordings() != null) {
            populateHearingRecordings(caseData);
        } else if ("addRecording".equalsIgnoreCase(callback.getPageId()) && caseData.getSscsHearingRecordingCaseData().getHearingRecording() != null) {
            validateHearingRecordings(caseData, response);
        }

        return response;
    }

    private void populateHearingRecordings(SscsCaseData caseData) {
        String hearingId = caseData.getSscsHearingRecordingCaseData().getSelectHearingDetails().getValue().getCode();
        SscsHearingRecordingDetails existing = caseData.getSscsHearingRecordingCaseData().getSscsHearingRecordings().stream()
                .filter(hearing -> hearing.getValue().getHearingId().equalsIgnoreCase(hearingId))
                .map(SscsHearingRecording::getValue)
                .findFirst().orElse(null);
        caseData.getSscsHearingRecordingCaseData().setExistingHearingRecordings(existing);
        caseData.getSscsHearingRecordingCaseData().setHearingRecordingExist(existing == null ? YesNo.NO : YesNo.YES);
    }

    private void validateHearingRecordings(final SscsCaseData caseData, PreSubmitCallbackResponse<SscsCaseData> response) {
        emptyIfNull(caseData.getSscsHearingRecordingCaseData().getHearingRecording().getRecordings())
                .stream()
                .filter(Objects::nonNull)
                .filter(hearingRecording -> hearingRecording.getValue().getDocumentFilename() != null)
                .filter(hearingRecording -> isInvalidFile(hearingRecording.getValue().getDocumentFilename()))
                .findAny().ifPresent(d -> response.addError("The file type you uploaded is not accepted"));

        if (!response.getErrors().isEmpty()) {
            return;
        }

        emptyIfNull(caseData.getSscsHearingRecordingCaseData().getHearingRecording().getRecordings())
                .stream()
                .filter(Objects::nonNull)
                .filter(recordingDetails -> recordingDetails.getValue().getDocumentBinaryUrl() != null)
                .filter(recordingDetails ->
                        documentDownloadService.getFileSize(recordingDetails.getValue().getDocumentBinaryUrl())
                                > (500 * 1024 * 1024))
                .findAny().ifPresent(d -> response.addError("The upload file size is more than the allowed limit"));
    }

    @NotNull
    boolean isInvalidFile(String fileName) {
        return !(RECORDING_FILE_PATTERN.matcher(fileName.toLowerCase()).find());
    }
}
