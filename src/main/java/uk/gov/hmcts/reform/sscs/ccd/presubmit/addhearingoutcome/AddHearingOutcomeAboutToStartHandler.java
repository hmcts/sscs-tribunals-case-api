package uk.gov.hmcts.reform.sscs.ccd.presubmit.addhearingoutcome;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcomeValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus;
import uk.gov.hmcts.reform.sscs.model.multi.hearing.CaseHearing;
import uk.gov.hmcts.reform.sscs.model.multi.hearing.HearingsGetResponse;
import uk.gov.hmcts.reform.sscs.service.HearingOutcomeService;
import uk.gov.hmcts.reform.sscs.service.HmcHearingApiService;
import uk.gov.hmcts.reform.sscs.service.VenueService;
import uk.gov.hmcts.reform.sscs.service.hmc.topic.HearingUpdateService;

@Component
@Slf4j
public class AddHearingOutcomeAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final HmcHearingApiService hmcHearingApiService;
    private final HearingUpdateService hearingUpdateService;
    private final VenueService venueService;
    private final HearingOutcomeService hearingOutcomeService;

    public AddHearingOutcomeAboutToStartHandler(HmcHearingApiService hmcHearingApiService,
                                                HearingUpdateService hearingUpdateService, VenueService venueService,
                                                HearingOutcomeService hearingOutcomeService) {
        this.hmcHearingApiService = hmcHearingApiService;
        this.hearingUpdateService = hearingUpdateService;
        this.venueService = venueService;
        this.hearingOutcomeService = hearingOutcomeService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType.equals(CallbackType.ABOUT_TO_START)
                && callback.getEvent() == EventType.ADD_HEARING_OUTCOME;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType,
                                                          Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();
        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
        log.info("Add hearing outcome started");

        try {
            HearingsGetResponse response = hmcHearingApiService.getHearingsRequest(Long.toString(caseDetails.getId()), HmcStatus.COMPLETED);
            List<CaseHearing> hmcHearings = response.getCaseHearings();
            log.info("Retrieved {} completed hearings for caseId {}", hmcHearings.size(), callback.getCaseDetails().getId());
            if (!hmcHearings.isEmpty()) {

                if (CollectionUtils.isEmpty(sscsCaseData.getCompletedHearingsList())) {
                    sscsCaseData.setCompletedHearingsList(new ArrayList<>());
                }

                sscsCaseData.getCompletedHearingsList().addAll(hmcHearings.stream()
                    .map(hearingOutcomeService::mapCaseHearingToHearing)
                    .filter(hearing -> hearing.getValue().getStart() != null)
                    .sorted(Comparator.reverseOrder())
                    .toList());

                sscsCaseData.setHearingOutcomeValue(HearingOutcomeValue.builder().build());
                sscsCaseData.getHearingOutcomeValue().setCompletedHearings(hearingOutcomeService.setHearingOutcomeCompletedHearings(sscsCaseData.getCompletedHearingsList()));
            } else {
                preSubmitCallbackResponse.addError("There are no completed hearings on the case.");
            }
        } catch (Exception e) {
            log.info("AddHearingOutcome failed for caseId {} with error {}", callback.getCaseDetails().getId(), e.getMessage());

            preSubmitCallbackResponse.addError("There was an error while retrieving hearing details; please try again after some time.");
        }
        return preSubmitCallbackResponse;
    }

}
