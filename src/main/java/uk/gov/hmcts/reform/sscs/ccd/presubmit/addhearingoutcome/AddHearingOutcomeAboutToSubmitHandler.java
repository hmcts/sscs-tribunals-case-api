package uk.gov.hmcts.reform.sscs.ccd.presubmit.addhearingoutcome;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcomeDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

import static java.util.Objects.requireNonNull;

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

        log.info("Setting outcome on appeal for case id: {}", callback.getCaseDetails().getId());

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        DynamicList completedHearings = sscsCaseData.getHearingOutcomeValue().getCompletedHearings();

        HearingDetails hearingWithOutcome;

        HearingOutcomeDetails hearingOutcomeDetails = HearingOutcomeDetails.builder()
                .completedHearingId()
                .hearingStartDateTime()
                .hearingEndDateTime()
                .hearingOutcomeId(sscsCaseData.getHearingOutcomeValue().getHearingOutcomeId())
                .didPoAttendHearing(sscsCaseData.getHearingOutcomeValue().getDidPoAttendHearing())
                .hearingChannelId()
                .venue()
                .epimsId()
                .build();

        HearingOutcome hearingOutcome = HearingOutcome.builder().value(hearingOutcomeDetails).build();
        sscsCaseData.getHearingOutcomes().add(hearingOutcome);
        sscsCaseData.getHearingOutcomeValue().setHearingOutcomeId(null);
        sscsCaseData.getHearingOutcomeValue().setCompletedHearings(null);
        sscsCaseData.getHearingOutcomeValue().setDidPoAttendHearing(null);


        return preSubmitCallbackResponse;
    }
}
