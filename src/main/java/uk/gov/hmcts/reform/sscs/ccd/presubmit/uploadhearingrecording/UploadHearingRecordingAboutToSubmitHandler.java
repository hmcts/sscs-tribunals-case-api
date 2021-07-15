package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploadhearingrecording;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.uploadhearingrecording.HearingTypeForRecording.ADJOURNED;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.uploadhearingrecording.HearingTypeForRecording.FINAL;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
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

    private static final String RECORDING_DATE_FORMATTER = "dd-MM-yyyy hh:mm:ss a";
    private static final String HEARING_TIME_FORMATTER = "HH:mm:ss dd MMM yyyy";
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
        final String venueDate =
            getHearingVenueDate(sscsCaseData.getSscsHearingRecordingCaseData().getSelectHearingDetails());

        List<SscsHearingRecording> existingSscsHearingRecordings =
            sscsCaseData.getSscsHearingRecordingCaseData().getSscsHearingRecordings();
        int adjournedCount = 1;
        int finalCount = 1;

        if (existingSscsHearingRecordings == null || existingSscsHearingRecordings.isEmpty()) {
            existingSscsHearingRecordings = new ArrayList<>();
        } else {
            adjournedCount =
                (int) (countExistingRecordings(existingSscsHearingRecordings, ADJOURNED.getValue() + " "
                    + venueDate, ADJOURNED.getKey()) + 1);
            finalCount =
                (int) (countExistingRecordings(existingSscsHearingRecordings, FINAL.getValue() + " " + venueDate,
                    FINAL.getKey()) + 1);
        }

        final HearingRecording hearingRecording = sscsCaseData.getSscsHearingRecordingCaseData().getHearingRecording();
        final List<HearingRecordingDetails> recordings =
            hearingRecording != null ? hearingRecording.getRecordings() : new ArrayList<>();

        if (recordings != null && !recordings.isEmpty()) {
            String hearingDateTime = LocalDateTime.parse(getHearingDateTime(
                sscsCaseData.getSscsHearingRecordingCaseData().getSelectHearingDetails()),
                DateTimeFormatter.ofPattern(HEARING_TIME_FORMATTER))
                .format(DateTimeFormatter.ofPattern(RECORDING_DATE_FORMATTER, Locale.UK)).toUpperCase();

            String hearingType = hearingRecording != null ? hearingRecording.getHearingType() : "";
            String hearingId = sscsCaseData.getSscsHearingRecordingCaseData().getSelectHearingDetails().getValue().getCode();
            String venueName = getVenueName(sscsCaseData, hearingId);

            Optional<SscsHearingRecording> sscsHearingRecordingOptional =
                selectSscsHearingRecording(existingSscsHearingRecordings, venueDate, hearingType);
            SscsHearingRecording sscsHearingRecording =
                sscsHearingRecordingOptional.isPresent() ? sscsHearingRecordingOptional.get() :
                    createSscsHearingRecording(
                        hearingDateTime,
                        hearingType,
                            hearingId,
                            venueName);
            if (!sscsHearingRecordingOptional.isPresent()) {
                existingSscsHearingRecordings.add(sscsHearingRecording);
            }

            List<HearingRecordingDetails> sscsRecordings = sscsHearingRecording.getValue().getRecordings();

            for (HearingRecordingDetails recordingDetails : recordings) {
                if (ADJOURNED.getKey().equals(hearingType)) {
                    sscsRecordings.add(
                        HearingRecordingDetails.builder().value(
                            DocumentLink.builder()
                                .documentFilename(createFileName(venueDate, adjournedCount, ADJOURNED.getValue())
                                    + getFileExtension(recordingDetails.getValue()))
                                .documentUrl(recordingDetails.getValue().getDocumentUrl())
                                .documentBinaryUrl(recordingDetails.getValue().getDocumentBinaryUrl())
                                .build()).build());
                    adjournedCount++;
                } else if (FINAL.getKey().equals(hearingType)) {
                    sscsRecordings.add(
                        HearingRecordingDetails.builder().value(
                            DocumentLink.builder()
                                .documentFilename(
                                    createFileName(venueDate, finalCount, FINAL.getValue())
                                        + getFileExtension(recordingDetails.getValue()))
                                .documentUrl(recordingDetails.getValue().getDocumentUrl())
                                .documentBinaryUrl(recordingDetails.getValue().getDocumentBinaryUrl())
                                .build()).build());
                    finalCount++;
                }
            }
        }

        if (!existingSscsHearingRecordings.isEmpty()) {
            existingSscsHearingRecordings.sort(Comparator.comparing(h -> LocalDate.parse(
                h.getValue().getHearingDate().toLowerCase(),
                DateTimeFormatter.ofPattern(RECORDING_DATE_FORMATTER, Locale.UK))));
            sscsCaseData.getSscsHearingRecordingCaseData().setSscsHearingRecordings(existingSscsHearingRecordings);
        }

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private String getVenueName(SscsCaseData sscsCaseData, String hearingId) {
        Hearing hearing = sscsCaseData.getHearings().stream().filter(h -> hearingId.equals(h.getValue().getHearingId())).findFirst().orElse(null);
        if (hearing != null && hearing.getValue() != null) {
            return hearing.getValue().getVenue().getName();
        }
        return "";
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
                                LocalDateTime.now().format(DateTimeFormatter.ofPattern(RECORDING_DATE_FORMATTER)).toUpperCase())
                        .build())
                .build();
    }

    private long countExistingRecordings(final List<SscsHearingRecording> sscsHearingRecordings,
                                         String hearingTypeVenueDate, String selectedHearingType) {
        AtomicLong count = new AtomicLong(0L);
        selectSscsHearingRecording(sscsHearingRecordings, hearingTypeVenueDate, selectedHearingType).ifPresent(
            sscsHearingRecording -> count.set(sscsHearingRecording.getValue().getRecordings().stream()
                .filter(recordingDetails -> recordingDetails.getValue().getDocumentFilename()
                    .contains(hearingTypeVenueDate)).count()));

        return count.get();
    }

    private Optional<SscsHearingRecording> selectSscsHearingRecording(
        final List<SscsHearingRecording> sscsHearingRecordings, String venueDate,
        String selectedHearingType) {

        return emptyIfNull(sscsHearingRecordings).stream()
            .filter(sscsHearingRecording -> sscsHearingRecording.getValue() != null)
            .filter(sscsHearingRecording -> sscsHearingRecording.getValue().getHearingDate() != null)
            .filter(sscsHearingRecording -> sscsHearingRecording.getValue().getHearingType() != null)
            .filter(
                sscsHearingRecording -> sscsHearingRecording.getValue().getHearingType().equals(selectedHearingType))
            .filter(sscsHearingRecording -> venueDate.contains(
                LocalDateTime.parse(
                    sscsHearingRecording.getValue().getHearingDate().toLowerCase(),
                    DateTimeFormatter.ofPattern(RECORDING_DATE_FORMATTER, Locale.UK))
                    .format(DateTimeFormatter.ofPattern("dd MMM yyyy"))))
            .filter(sscsHearingRecording -> sscsHearingRecording.getValue().getRecordings().stream()
                .allMatch(recordingDetails -> recordingDetails.getValue().getDocumentFilename().contains(venueDate)))
            .findFirst();
    }

    String createFileName(String venueDate, int fileCount, String hearingType) {
        return (fileCount == 1) ? (hearingType + " " + venueDate) :
            hearingType + " " + venueDate + " (" + fileCount + ")";
    }

    String getHearingVenueDate(DynamicList selectedHearings) {
        AtomicReference<String> result = new AtomicReference<>("");
        Optional.ofNullable(selectedHearings.getValue().getLabel())
            .ifPresent(v -> result.set(v.substring(0, v.length() - 21) + " " + v.substring(v.length() - 11)));
        return result.get();
    }

    String getHearingDateTime(DynamicList selectedHearings) {
        AtomicReference<String> result = new AtomicReference<>("");
        Optional.ofNullable(selectedHearings.getValue().getLabel())
            .ifPresent(d -> result.set(d.substring(d.length() - 20)));
        return result.get();
    }

    String getFileExtension(DocumentLink documentLink) {
        AtomicReference<String> result = new AtomicReference<>("");
        Optional.ofNullable(documentLink.getDocumentFilename())
            .ifPresent(f -> result.set(f.substring(f.length() - 4)));
        return result.get();
    }

}
