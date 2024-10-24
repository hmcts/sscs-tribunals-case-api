package uk.gov.hmcts.reform.sscs.ccd.presubmit.addhearingoutcome;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${feature.add-hearing-outcome.enabled}")
    private boolean isAddHearingOutcomeEnabled;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent().equals(EventType.ADD_HEARING_OUTCOME)
                && isAddHearingOutcomeEnabled;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        log.info("Setting outcome on appeal for case id: {}", callback.getCaseDetails().getId());

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        DynamicList completedHearings = sscsCaseData.getHearingOutcomeValue().getCompletedHearings();

        // depends on how dynamic list is set out in SSCSCI-1175;
        String hearingId = completedHearings.getValue().getCode();

        HearingDetails hearingWithOutcome = sscsCaseData.getHearings().stream()
                .filter(hearing -> hearing.getValue().getHearingId().equalsIgnoreCase(hearingId))
                .findFirst().orElse(Hearing.builder().build()).getValue();

        if (hearingWithOutcome == null) {
            log.info("Add Hearing Outcome: Cannot find hearing with hearing ID {} for Case ID {}",
                    hearingId, callback.getCaseDetails().getId());
            preSubmitCallbackResponse.addError("Cannot find hearing with hearing ID");
        } else {
            HearingOutcomeDetails hearingOutcomeDetails = HearingOutcomeDetails.builder()
                    .completedHearingId(hearingWithOutcome.getHearingId())
                    .hearingStartDateTime(hearingWithOutcome.getStart())
                    .hearingEndDateTime(hearingWithOutcome.getEnd())
                    .hearingOutcomeId(sscsCaseData.getHearingOutcomeValue().getHearingOutcomeId())
                    .didPoAttendHearing(sscsCaseData.getHearingOutcomeValue().getDidPoAttendHearing())
                    .hearingChannelId(hearingWithOutcome.getHearingChannel())
                    .venue(hearingWithOutcome.getVenue())
                    .epimsId(hearingWithOutcome.getEpimsId())
                    .build();

            HearingOutcome hearingOutcome = HearingOutcome.builder().value(hearingOutcomeDetails).build();
            if (sscsCaseData.getHearingOutcomes() == null) {
                sscsCaseData.setHearingOutcomes(new ArrayList<>());
            }
            sscsCaseData.getHearingOutcomes().add(hearingOutcome);
            sscsCaseData.getHearingOutcomeValue().setHearingOutcomeId(null);
            sscsCaseData.getHearingOutcomeValue().setCompletedHearings(null);
            sscsCaseData.getHearingOutcomeValue().setDidPoAttendHearing(null);
        }

        return preSubmitCallbackResponse;
    }
}
