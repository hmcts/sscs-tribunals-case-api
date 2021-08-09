package uk.gov.hmcts.reform.sscs.ccd.presubmit.processhearingrecordingrequest;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.model.PartyItemList;


@Service
@Slf4j
public class ProcessHearingRecordingRequestAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.PROCESS_HEARING_RECORDING_REQUEST;

    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {

        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        final SscsHearingRecordingCaseData sscsHearingRecordingCaseData =
                sscsCaseData.getSscsHearingRecordingCaseData();
        final List<HearingRecordingRequest> unprocessedHearingRecordingsRequests =
                Optional.ofNullable(sscsHearingRecordingCaseData.getRequestedHearings())
                        .orElse(Collections.emptyList());

        List<HearingRecordingRequest> allHearingRecordingsRequests = Stream.of(unprocessedHearingRecordingsRequests,
                        Optional.ofNullable(sscsHearingRecordingCaseData.getDwpReleasedHearings())
                                .orElse(Collections.emptyList()),
                        Optional.ofNullable(sscsHearingRecordingCaseData.getCitizenReleasedHearings())
                                .orElse(Collections.emptyList()),
                        Optional.ofNullable(sscsHearingRecordingCaseData.getRefusedHearings())
                                .orElse(Collections.emptyList()))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        processGrantedRequests(sscsCaseData, allHearingRecordingsRequests);
        processRefusedRequests(sscsCaseData, allHearingRecordingsRequests);

        if (unprocessedHearingRecordingsRequests.isEmpty()) {
            sscsHearingRecordingCaseData.setHearingRecordingRequestOutstanding(YesNo.NO);
        }

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private void processRefusedRequests(SscsCaseData sscsCaseData,
                                        List<HearingRecordingRequest> allHearingRecordingsRequests) {

        final SscsHearingRecordingCaseData sscsHearingRecordingCaseData = sscsCaseData.getSscsHearingRecordingCaseData();
        final List<HearingRecordingRequest> refusedHearingRecordingRequests =
                allHearingRecordingsRequests.stream()
                        .filter(req -> req.getValue().getStatus().equals(RequestStatus.REFUSED.getValue()))
                        .collect(Collectors.toList());

        sscsHearingRecordingCaseData.getRefusedHearings().addAll(refusedHearingRecordingRequests);
        sscsHearingRecordingCaseData.getRequestedHearings().removeAll(refusedHearingRecordingRequests);

        if (refusedHearingRecordingRequests.stream().filter(req -> req.getValue().getRequestingParty()
                .equals(PartyItemList.DWP.getCode())).findAny().isPresent()) {

            sscsHearingRecordingCaseData.getDwpReleasedHearings().removeAll(refusedHearingRecordingRequests);
            sscsCaseData.setDwpState(DwpState.HEARING_RECORDING_REFUSED.getLabel());
        } else {
            sscsHearingRecordingCaseData.getCitizenReleasedHearings().removeAll(refusedHearingRecordingRequests);
        }
    }

    private void processGrantedRequests(SscsCaseData sscsCaseData,
                                        List<HearingRecordingRequest> allHearingRecordingsRequests) {

        final SscsHearingRecordingCaseData sscsHearingRecordingCaseData = sscsCaseData.getSscsHearingRecordingCaseData();
        final List<HearingRecordingRequest> refusedHearingRecordingRequests =
                allHearingRecordingsRequests.stream()
                        .filter(req -> req.getValue().getStatus().equals(RequestStatus.GRANTED.getValue()))
                        .collect(Collectors.toList());

        sscsHearingRecordingCaseData.getRefusedHearings().removeAll(refusedHearingRecordingRequests);
        sscsHearingRecordingCaseData.getRequestedHearings().removeAll(refusedHearingRecordingRequests);

        if (refusedHearingRecordingRequests.stream().filter(req -> req.getValue().getRequestingParty()
                .equals(PartyItemList.DWP.getCode())).findAny().isPresent()) {

            sscsHearingRecordingCaseData.getDwpReleasedHearings().addAll(refusedHearingRecordingRequests);
            sscsCaseData.setDwpState(DwpState.HEARING_RECORDING_RELEASED.getLabel());
        } else {
            sscsHearingRecordingCaseData.getCitizenReleasedHearings().addAll(refusedHearingRecordingRequests);
        }
    }
}
