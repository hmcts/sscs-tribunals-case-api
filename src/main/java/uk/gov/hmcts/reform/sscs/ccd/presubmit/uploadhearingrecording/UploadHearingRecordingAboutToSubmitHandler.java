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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRecording;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsHearingRecording;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsHearingRecordingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
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
                    + venueDate) + 1);
            finalCount =
                (int) (countExistingRecordings(existingSscsHearingRecordings, FINAL.getValue() + " " + venueDate) + 1);
        }

        final List<HearingRecording> hearingRecordings =
            sscsCaseData.getSscsHearingRecordingCaseData().getHearingRecordings();
        List<SscsHearingRecording> sscsHearingRecordings = new ArrayList<>();

        if (hearingRecordings != null && !hearingRecordings.isEmpty()) {
            String hearingDateTime = LocalDateTime.parse(getHearingDateTime(
                sscsCaseData.getSscsHearingRecordingCaseData().getSelectHearingDetails()),
                DateTimeFormatter.ofPattern(HEARING_TIME_FORMATTER))
                .format(DateTimeFormatter.ofPattern(RECORDING_DATE_FORMATTER)).toUpperCase();

            for (HearingRecording hearingRecording : hearingRecordings) {
                if (ADJOURNED.getKey().equals(hearingRecording.getValue().getHearingType())) {
                    sscsHearingRecordings.add(createSscsHearingRecording(
                        hearingRecording,
                        createFileName(venueDate, adjournedCount, ADJOURNED.getValue())
                            + getFileExtension(hearingRecording),
                        hearingDateTime,
                        ADJOURNED.getKey()));
                    adjournedCount++;
                } else if (FINAL.getKey().equals(hearingRecording.getValue().getHearingType())) {
                    sscsHearingRecordings.add(createSscsHearingRecording(
                        hearingRecording,
                        createFileName(venueDate, finalCount, FINAL.getValue()) + getFileExtension(hearingRecording),
                        hearingDateTime,
                        FINAL.getKey()));
                    finalCount++;
                }
            }
        }

        if (!sscsHearingRecordings.isEmpty()) {
            existingSscsHearingRecordings.addAll(sscsHearingRecordings);
            existingSscsHearingRecordings.sort(Comparator.comparing(h -> LocalDate.parse(
                h.getValue().getHearingDate().toLowerCase(), DateTimeFormatter.ofPattern(RECORDING_DATE_FORMATTER))));
            sscsCaseData.getSscsHearingRecordingCaseData().setSscsHearingRecordings(existingSscsHearingRecordings);
            sscsCaseData.getSscsHearingRecordingCaseData().setShowHearingRecordings(YesNo.YES);
        }

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private SscsHearingRecording createSscsHearingRecording(final HearingRecording hearingRecording, String fileName,
                                                            String hearingDate, String type) {
        return SscsHearingRecording.builder()
            .value(SscsHearingRecordingDetails.builder()
                .documentLink(DocumentLink.builder()
                    .documentFilename(fileName)
                    .documentBinaryUrl(hearingRecording.getValue().getDocumentLink().getDocumentBinaryUrl())
                    .documentUrl(hearingRecording.getValue().getDocumentLink().getDocumentUrl())
                    .build())
                .hearingDate(hearingDate)
                .hearingType(type)
                .uploadDate(
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern(RECORDING_DATE_FORMATTER)).toUpperCase())
                .build())
            .build();
    }

    private long countExistingRecordings(final List<SscsHearingRecording> hearingRecordings, String recordingName) {
        return emptyIfNull(hearingRecordings).stream()
            .filter(hearingRecording -> hearingRecording.getValue().getDocumentLink().getDocumentFilename()
                .contains(recordingName))
            .count();
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

    String getFileExtension(HearingRecording hearingRecording) {
        AtomicReference<String> result = new AtomicReference<>("");
        Optional.ofNullable(hearingRecording.getValue().getDocumentLink().getDocumentFilename())
            .ifPresent(f -> result.set(f.substring(f.length() - 4)));
        return result.get();
    }
}
