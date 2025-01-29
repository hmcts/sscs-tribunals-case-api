package uk.gov.hmcts.reform.sscs.ccd.presubmit.amendhearingoutcome;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Component
@Slf4j
public class AmendHearingOutcomeAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && callback.getEvent().equals(EventType.AMEND_HEARING_OUTCOME);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        SscsCaseData sscsCaseData = caseDetails.getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        List<String> hearingsSelected = new ArrayList<>();

        if (sscsCaseData.getHearingOutcomes().isEmpty()) {
            sscsCaseData.setHearingOutcomes(null);
            return new PreSubmitCallbackResponse<>(sscsCaseData);
        } else {
            for (HearingOutcome selectedHearingOutcome :  sscsCaseData.getHearingOutcomes()) {
                String selectedHearingId =
                        selectedHearingOutcome.getValue().getCompletedHearings().getValue().getCode();
                if (hearingsSelected == null) {
                    hearingsSelected.add(selectedHearingId);
                } else if (hearingsSelected.contains(selectedHearingId)) {
                    log.info("Hearing outcome {} selected more than once for case {}",
                            selectedHearingOutcome.getValue().getCompletedHearings().getValue(),
                            caseDetails.getId());
                    preSubmitCallbackResponse.addError("This hearing already has an outcome recorded: "
                            + selectedHearingOutcome.getValue().getCompletedHearings().getValue().getLabel());
                    return preSubmitCallbackResponse;
                } else {
                    hearingsSelected.add(selectedHearingId);
                }
            }
        }

        int hearingOutcomesSize = sscsCaseData.getHearingOutcomes().size();

        for (HearingOutcome hearingOutcome : sscsCaseData.getHearingOutcomes()) {

            String selectedHearingId =  hearingOutcome.getValue().getCompletedHearings().getValue().getCode();

            if (!selectedHearingId.equalsIgnoreCase(hearingOutcome.getValue().getCompletedHearingId())) {

                log.info("Amending hearing outcome on case {} from hearing id: {} to be for hearing id: {}",
                        caseDetails.getId(), hearingOutcome.getValue().getCompletedHearingId(), selectedHearingId);

                HearingDetails selectedHearingDetails = sscsCaseData.getCompletedHearingsList().stream()
                        .filter(hearing -> hearing.getValue().getHearingId().equalsIgnoreCase(selectedHearingId))
                        .findFirst().orElse(Hearing.builder().build()).getValue();

                hearingOutcome.getValue().setCompletedHearingId(selectedHearingDetails.getHearingId());
                hearingOutcome.getValue().setHearingStartDateTime(selectedHearingDetails.getStart());
                hearingOutcome.getValue().setHearingEndDateTime(selectedHearingDetails.getEnd());
                hearingOutcome.getValue().setHearingChannelId(selectedHearingDetails.getHearingChannel());
                hearingOutcome.getValue().setVenue(selectedHearingDetails.getVenue());
                hearingOutcome.getValue().setEpimsId(selectedHearingDetails.getEpimsId());
            }

        }

        sscsCaseData.setCompletedHearingsList(null);

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }
}
