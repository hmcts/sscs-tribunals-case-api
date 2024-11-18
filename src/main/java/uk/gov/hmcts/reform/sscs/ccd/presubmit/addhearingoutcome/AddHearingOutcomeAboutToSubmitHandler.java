package uk.gov.hmcts.reform.sscs.ccd.presubmit.addhearingoutcome;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcomeDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Component
@Slf4j
public class AddHearingOutcomeAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent().equals(EventType.ADD_HEARING_OUTCOME);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        log.info("Setting outcome on appeal for case ID:{}", callback.getCaseDetails().getId());

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        DynamicList completedHearings = sscsCaseData.getHearingOutcomeValue().getCompletedHearings();

        String selectedHearingId = completedHearings.getValue().getCode();
        String selectedHearing = completedHearings.getValue().getLabel();

        log.info("Add hearing outcome for selected item {} with hearing ID:{} for case ID:{}",
                selectedHearing, selectedHearingId, callback.getCaseDetails().getId());

        HearingDetails selectedHearingDetails = sscsCaseData.getHearings().stream()
                .filter(hearing -> hearing.getValue().getHearingId().equalsIgnoreCase(selectedHearingId))
                .findFirst().orElse(Hearing.builder().build()).getValue();

        HearingOutcomeDetails hearingOutcomeDetails = HearingOutcomeDetails.builder()
                .completedHearingId(selectedHearingDetails.getHearingId())
                .hearingStartDateTime(selectedHearingDetails.getStart())
                .hearingEndDateTime(selectedHearingDetails.getEnd())
                .hearingOutcomeId(sscsCaseData.getHearingOutcomeValue().getHearingOutcomeId())
                .didPoAttendHearing(sscsCaseData.getHearingOutcomeValue().getDidPoAttendHearing())
                .hearingChannelId(selectedHearingDetails.getHearingChannel())
                .venue(selectedHearingDetails.getVenue())
                .epimsId(selectedHearingDetails.getEpimsId())
                .build();

        HearingOutcome hearingOutcome = HearingOutcome.builder().value(hearingOutcomeDetails).build();

        if (sscsCaseData.getHearingOutcomes() == null) {
            sscsCaseData.setHearingOutcomes(new ArrayList<>());
        }

        HearingOutcome checkExisting = sscsCaseData.getHearingOutcomes().stream()
                .filter(hearingOutcome1 -> hearingOutcome1.getValue().getCompletedHearingId()
                        .equalsIgnoreCase(selectedHearingId))
                .findFirst()
                .orElse(null);

        if (checkExisting != null) {
            log.info("Add Hearing Outcome: There is already an existing hearing outcome with hearing ID:{} " +
                            "and value {} for case ID:{}",
                    selectedHearingId, selectedHearing, callback.getCaseDetails().getId());
            preSubmitCallbackResponse.addError("A hearing outcome already exists for this hearing date. " +
                    "Please select a different hearing date");
            return preSubmitCallbackResponse;
        }

        sscsCaseData.getHearingOutcomes().add(hearingOutcome);

        List<HearingOutcome> hearingOutcomeSortedList = sscsCaseData.getHearingOutcomes().stream()
                .sorted(Comparator.comparing(a -> a.getValue().getHearingStartDateTime()))
                        .collect(Collectors.toList());
        sscsCaseData.setHearingOutcomes(hearingOutcomeSortedList);

        sscsCaseData.getHearingOutcomeValue().setHearingOutcomeId(null);
        sscsCaseData.getHearingOutcomeValue().setCompletedHearings(null);
        sscsCaseData.getHearingOutcomeValue().setDidPoAttendHearing(null);

        return preSubmitCallbackResponse;
    }
}