package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploadhearingrecording;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@Service
@Slf4j
public class UploadHearingRecordingAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private static final DateTimeFormatter RECORDING_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm:ss a", Locale.UK);
    private static final DateTimeFormatter HEARING_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss yyyy-MM-dd");
    private static final DateTimeFormatter HEARING_DOCUMENT_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private final IdamService idamService;

    @Autowired
    public UploadHearingRecordingAboutToSubmitHandler(IdamService idamService) {
        this.idamService = idamService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.UPLOAD_HEARING_RECORDING;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();
        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);

        final HearingRecording hearingRecording = sscsCaseData.getSscsHearingRecordingCaseData().getHearingRecording();

        if (hearingRecording == null || hearingRecording.getRecordings().isEmpty()) {
            clearTransientFields(sscsCaseData);
            return response;
        }

        List<SscsHearingRecording> sscsHearingRecordings = sscsCaseData.getSscsHearingRecordingCaseData().getSscsHearingRecordings();
        if (sscsHearingRecordings == null) {
            sscsHearingRecordings = new ArrayList<>();
            sscsCaseData.getSscsHearingRecordingCaseData().setSscsHearingRecordings(sscsHearingRecordings);
        }

        //New hearing recording data
        final List<HearingRecordingDetails> recordings = hearingRecording.getRecordings();
        HearingTypeForRecording hearingType = HearingTypeForRecording.valueOf(hearingRecording.getHearingType().toUpperCase());

        //Information of the hearing that recordings are uploaded
        String hearingId = sscsCaseData.getSscsHearingRecordingCaseData().getSelectHearingDetails().getValue().getCode();
        HearingDetails hearingDetails = sscsCaseData.getHearings().stream()
                .filter(hearing -> hearing.getValue().getHearingId().equalsIgnoreCase(hearingId))
                .findFirst().orElseThrow().getValue();
        LocalDateTime hearingDateTime = parseHearingDateTime(hearingDetails);
        String venueName = hearingDetails.getVenue().getName();

        //Existing sscs hearing recording for the same hearing
        Optional<SscsHearingRecording> existingSscsHearingRecordings = selectSscsHearingRecording(sscsHearingRecordings, hearingId);

        SscsHearingRecordingDetails sscsHearingRecordingDetails;
        if (existingSscsHearingRecordings.isPresent()) {
            //Update existing sscs hearing recording data
            sscsHearingRecordingDetails = existingSscsHearingRecordings.get().getValue();
            sscsHearingRecordingDetails.setHearingType(hearingType.getKey());
            sscsHearingRecordingDetails.setUploadDate(LocalDateTime.now().format(RECORDING_DATE_FORMATTER).toUpperCase());
            response.addWarning("The hearing recording you have just uploaded will replace the existing hearing recording(s)");
        } else {
            //Create new sscs hearing recording
            SscsHearingRecording newSscsHearingRecording = createSscsHearingRecording(
                    hearingDateTime.format(RECORDING_DATE_FORMATTER).toUpperCase(),
                    hearingType.getKey(),
                    hearingId,
                    venueName);
            sscsHearingRecordings.add(newSscsHearingRecording);
            sscsHearingRecordingDetails = newSscsHearingRecording.getValue();
        }

        //Create new collection of hearing recordings
        List<HearingRecordingDetails> hearingRecordings =
                createHearingRecordingDocuments(recordings, hearingType.getValue(), venueName, hearingDateTime.format(HEARING_DOCUMENT_TIME_FORMATTER));
        sscsHearingRecordingDetails.setRecordings(hearingRecordings);

        sscsHearingRecordings.sort(Comparator.comparing(h ->
                LocalDate.parse( h.getValue().getHearingDate().toLowerCase(), RECORDING_DATE_FORMATTER)));

        clearTransientFields(sscsCaseData);

        return response;
    }

    private void clearTransientFields(SscsCaseData sscsCaseData) {
//        sscsCaseData.getSscsHearingRecordingCaseData().setExistingHearingRecordings(null);
//        sscsCaseData.getSscsHearingRecordingCaseData().setHearingRecordingExist(null);
    }

    protected SscsHearingRecording createSscsHearingRecording(String hearingDate, String type, String hearingId, String venueName) {
        return SscsHearingRecording.builder()
                .value(SscsHearingRecordingDetails.builder()
                        .recordings(new ArrayList<>())
                        .hearingDate(hearingDate)
                        .hearingType(type)
                        .hearingId(hearingId)
                        .venue(venueName)
                        .uploadDate(
                                LocalDateTime.now().format(RECORDING_DATE_FORMATTER).toUpperCase())
                        .build())
                .build();
    }

    private List<HearingRecordingDetails> createHearingRecordingDocuments(List<HearingRecordingDetails> recordings,
                                                                           String hearingType, String venue, String date) {
        List<HearingRecordingDetails> sscsRecordings = new ArrayList<>();
        int count = 1;
        for (HearingRecordingDetails recordingDetails : recordings) {
            sscsRecordings.add(HearingRecordingDetails.builder()
                    .value(DocumentLink.builder()
                            .documentFilename(createFileName(venue, date, count++, hearingType, recordingDetails.getValue()))
                            .documentUrl(recordingDetails.getValue().getDocumentUrl())
                            .documentBinaryUrl(recordingDetails.getValue().getDocumentBinaryUrl())
                            .build())
                    .build());
        }

        return sscsRecordings;
    }

    @NotNull
    private LocalDateTime parseHearingDateTime(HearingDetails hearingDetails) {
        String hearingTime = (hearingDetails.getTime().length() == 5) ? (hearingDetails.getTime() + ":00") : hearingDetails.getTime();
        return LocalDateTime.parse(hearingTime + " " + hearingDetails.getHearingDate(), HEARING_TIME_FORMATTER);
    }

    private Optional<SscsHearingRecording> selectSscsHearingRecording(
            final List<SscsHearingRecording> sscsHearingRecordings, String hearingId) {

        return emptyIfNull(sscsHearingRecordings).stream()
                .filter(sscsHearingRecording -> sscsHearingRecording.getValue() != null)
                .filter(sscsHearingRecording -> sscsHearingRecording.getValue().getHearingId().equalsIgnoreCase(hearingId))
                .findFirst();
    }

    protected String createFileName(String venue, String hearingDate, int fileCount, String hearingType, DocumentLink documentLink) {
        StringBuilder builder = new StringBuilder();
        builder.append(hearingType).append(" ")
                .append(venue).append(" ")
                .append(hearingDate);
        if (fileCount > 1) {
            builder.append(" (").append(fileCount).append(")");
        }
        builder.append(getFileExtension(documentLink));
        return builder.toString();
    }

    protected String getFileExtension(DocumentLink documentLink) {
        AtomicReference<String> result = new AtomicReference<>("");
        Optional.ofNullable(documentLink.getDocumentFilename())
            .ifPresent(f -> result.set(f.substring(f.length() - 4)));
        return result.get();
    }
}
