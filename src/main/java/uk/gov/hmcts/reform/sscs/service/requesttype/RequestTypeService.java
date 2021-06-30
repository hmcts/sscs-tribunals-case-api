package uk.gov.hmcts.reform.sscs.service.requesttype;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.model.tya.HearingRecording;
import uk.gov.hmcts.reform.sscs.model.tya.HearingRecordingRequest;
import uk.gov.hmcts.reform.sscs.model.tya.HearingRecordingResponse;
import uk.gov.hmcts.reform.sscs.service.OnlineHearingService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;

@Slf4j
@Service
public class RequestTypeService {
    private static final String UPLOAD_DATE_FORMATTER = "yyyy-MM-dd";

    private final OnlineHearingService onlineHearingService;
    private final CcdService ccdService;
    private final IdamService idamService;


    public RequestTypeService(OnlineHearingService onlineHearingService, CcdService ccdService, IdamService idamService) {
        this.onlineHearingService = onlineHearingService;
        this.ccdService = ccdService;
        this.idamService = idamService;
    }

    public HearingRecordingResponse findHearingRecordings(String identifier) {
        Optional<SscsCaseDetails> caseDetails = onlineHearingService.getCcdCaseByIdentifier(identifier);
        return caseDetails.map(x -> mapToHearingRecording(x.getData())).orElse(new HearingRecordingResponse());
    }

    public boolean requestHearingRecordings(String identifier, List<String> hearingIds) {
        log.info("Hearing recordings request for {}", hearingIds);
        Optional<SscsCaseDetails> caseDetails = onlineHearingService.getCcdCaseByIdentifier(identifier);
        return caseDetails.map(x -> submitHearingRecordingRequest(x.getData(), x.getId(), hearingIds)).orElse(false);
    }

    private HearingRecordingResponse mapToHearingRecording(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getHearings() == null || sscsCaseData.getHearings().isEmpty()) {
            return new HearingRecordingResponse();
        } else {
            //FIXME pass request party
            List<HearingRecordingRequest> releasedRecordings = sscsCaseData.getSscsHearingRecordingCaseData().getReleasedHearings()
                    .stream()
                    .filter(request -> UploadParty.APPELLANT.equals(request.getValue().getRequestingParty()))
                    .flatMap(request -> request.getValue().getSscsHearingRecordingList().stream())
                    .map(request -> selectHearingRecordings(request.getValue()))
                    .collect(Collectors.toList());

            List<HearingRecordingRequest> requestedRecordings = sscsCaseData.getSscsHearingRecordingCaseData().getRequestedHearings()
                    .stream()
                    .filter(request -> UploadParty.APPELLANT.equals(request.getValue().getRequestingParty()))
                    .flatMap(request -> request.getValue().getSscsHearingRecordingList().stream())
                    .map(request -> selectHearingRecordings(request.getValue()))
                    .collect(Collectors.toList());

            List<String> allRequestedHearingIds = Stream.of(releasedRecordings, requestedRecordings)
                    .flatMap(Collection::stream)
                    .map(r -> r.getHearingId())
                    .collect(Collectors.toList());

            List<HearingRecordingRequest> requestabledRecordings = sscsCaseData.getHearings().stream()
                    .filter(hearing -> isHearingWithRecording(hearing, sscsCaseData.getSscsHearingRecordingCaseData()))
                    .filter(hearing -> !allRequestedHearingIds.contains(hearing.getValue().getHearingId()))
                    .map(hearing -> selectHearingRecordings(hearing, sscsCaseData.getSscsHearingRecordingCaseData()))
                    .collect(Collectors.toList());

            return new HearingRecordingResponse(releasedRecordings, requestedRecordings, requestabledRecordings);
        }
    }

    private boolean submitHearingRecordingRequest(SscsCaseData sscsCaseData, Long ccdCaseId, List<String> hearingIds) {
        List<SscsHearingRecording> sscsHearingRecordingList = sscsCaseData.getSscsHearingRecordingCaseData().getSscsHearingRecordings()
                .stream().filter(r -> hearingIds.contains(r.getValue().getHearingId())).collect(Collectors.toList());

        //FIXME change the upload party
        uk.gov.hmcts.reform.sscs.ccd.domain.HearingRecordingRequest hearingRecordingRequest = uk.gov.hmcts.reform.sscs.ccd.domain.HearingRecordingRequest.builder().value(HearingRecordingRequestDetails.builder()
                .requestingParty(UploadParty.APPELLANT.getValue()).status("requested")
                .dateRequested(LocalDateTime.now().format(DateTimeFormatter.ofPattern(UPLOAD_DATE_FORMATTER)))
                .sscsHearingRecordingList(sscsHearingRecordingList).build()).build();

        List<uk.gov.hmcts.reform.sscs.ccd.domain.HearingRecordingRequest> hearingRecordingRequests = sscsCaseData.getSscsHearingRecordingCaseData().getRequestedHearings();
        if (hearingRecordingRequests == null) {
            hearingRecordingRequests = new ArrayList<>();
        }
        hearingRecordingRequests.add(hearingRecordingRequest);

        sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearings(hearingRecordingRequests);
        sscsCaseData.getSscsHearingRecordingCaseData().setHearingRecordingRequestOutstanding(YesNo.YES);
        //FIXME change the event type
        ccdService.updateCase(sscsCaseData, ccdCaseId, DWP_REQUEST_HEARING_RECORDING.getCcdType(),
                "SSCS - hearing recording request from MYA",
                "Requested hearing recordings", idamService.getIdamTokens());
        return true;
    }

    private HearingRecordingRequest selectHearingRecordings(uk.gov.hmcts.reform.sscs.ccd.domain.SscsHearingRecordingDetails recording) {
        return HearingRecordingRequest.builder()
                .hearingId(recording.getHearingId())
                .venue("Venue")
                .hearingDate(recording.getHearingDate())
                .hearingTime("Time")
                .hearingRecordings(recording.getRecordings()
                        .stream()
                        .map(r -> HearingRecording.builder()
                                .fileName(r.getValue().getDocumentFilename())
                                .documentUrl(r.getValue().getDocumentUrl())
                                .documentBinaryUrl(r.getValue().getDocumentBinaryUrl())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    private HearingRecordingRequest selectHearingRecordings(Hearing hearing, SscsHearingRecordingCaseData hearingRecordingsData) {
        return HearingRecordingRequest.builder()
                .hearingId(hearing.getValue().getHearingId())
                .hearingDate(hearing.getValue().getHearingDate())
                .hearingTime(hearing.getValue().getTime())
                .venue(hearing.getValue().getVenue().getName())
                .build();
    }

    private boolean isHearingWithRecording(Hearing hearing, SscsHearingRecordingCaseData hearingRecordingsData) {
        List<SscsHearingRecording> sscsHearingRecordings = hearingRecordingsData.getSscsHearingRecordings();

        if (sscsHearingRecordings != null) {
            return sscsHearingRecordings.stream().anyMatch(r -> r.getValue().getHearingId().equals(hearing.getValue().getHearingId()));
        }
        return false;
    }
}
