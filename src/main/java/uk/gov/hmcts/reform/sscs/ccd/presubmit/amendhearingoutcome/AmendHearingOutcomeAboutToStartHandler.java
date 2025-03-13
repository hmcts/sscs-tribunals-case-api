package uk.gov.hmcts.reform.sscs.ccd.presubmit.amendhearingoutcome;

import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus;
import uk.gov.hmcts.reform.sscs.model.multi.hearing.CaseHearing;
import uk.gov.hmcts.reform.sscs.model.multi.hearing.HearingsGetResponse;
import uk.gov.hmcts.reform.sscs.service.HearingOutcomeService;
import uk.gov.hmcts.reform.sscs.service.HmcHearingApiService;

@Component
@Slf4j
public class AmendHearingOutcomeAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final HmcHearingApiService hmcHearingApiService;
    private final HearingOutcomeService hearingOutcomeService;


    public AmendHearingOutcomeAboutToStartHandler(HmcHearingApiService hmcHearingApiService, HearingOutcomeService hearingOutcomeService) {
        this.hmcHearingApiService = hmcHearingApiService;
        this.hearingOutcomeService = hearingOutcomeService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType.equals(CallbackType.ABOUT_TO_START)
                && callback.getEvent() == EventType.AMEND_HEARING_OUTCOME;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        if (sscsCaseData.getHearingOutcomes() == null) {
            preSubmitCallbackResponse.addError("There are no hearing outcomes recorded on the case. Please add a hearing outcome using 'Add a Hearing Outcome' event");
            return preSubmitCallbackResponse;
        }

        HearingsGetResponse response = hmcHearingApiService.getHearingsRequest(Long.toString(caseDetails.getId()), HmcStatus.COMPLETED);
        List<CaseHearing> hmcHearings = response.getCaseHearings();
        sscsCaseData.setCompletedHearingsList(hmcHearings.stream()
                .map(hearingOutcomeService::mapCaseHearingToHearing)
                .filter(hearing -> hearing.getValue().getStart() != null)
                .sorted(Comparator.reverseOrder())
                .toList());
        DynamicList hearingList = hearingOutcomeService.setHearingOutcomeCompletedHearings(sscsCaseData.getCompletedHearingsList());
        for (HearingOutcome hearingOutcome: sscsCaseData.getHearingOutcomes()) {
            List<DynamicListItem> listItems = hearingList.getListItems();
            DynamicListItem selectedHearing = listItems.stream()
                    .filter(item -> item.getCode().equals(hearingOutcome.getValue().getCompletedHearingId()))
                    .findFirst()
                    .orElse(new DynamicListItem("", ""));
            hearingOutcome.getValue().setCompletedHearings(new DynamicList(selectedHearing, listItems));
        }

        return preSubmitCallbackResponse;
    }
}