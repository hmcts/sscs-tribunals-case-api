package uk.gov.hmcts.reform.sscs.ccd.presubmit.addhearingoutcome;

import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcomeValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus;
import uk.gov.hmcts.reform.sscs.model.multi.hearing.CaseHearing;
import uk.gov.hmcts.reform.sscs.model.multi.hearing.HearingsGetResponse;
import uk.gov.hmcts.reform.sscs.service.HmcHearingsApiService;


@Component
@Slf4j
public class AddHearingOutcomeAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final HmcHearingsApiService hmcHearingsApiService;

    public AddHearingOutcomeAboutToStartHandler(HmcHearingsApiService hmcHearingsApiService) {
        this.hmcHearingsApiService = hmcHearingsApiService;
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
        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();
        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
        HearingsGetResponse response = hmcHearingsApiService.getHearingsRequest(Long.toString(caseDetails.getId()), HmcStatus.COMPLETED);
        List<CaseHearing> hmcHearings = response.getCaseHearings();
        if (!hmcHearings.isEmpty()) {
            List<Hearing> selectedHearings = sscsCaseData.getHearings().stream()
                    .filter(hearing -> hmcHearings.stream()
                            .anyMatch(hmcHearing -> Objects
                                    .equals(hmcHearing.getHearingId().toString(), hearing.getValue().getHearingId()))).toList();
            sscsCaseData.setHearingOutcomeValue(HearingOutcomeValue.builder().build());
            sscsCaseData.getHearingOutcomeValue().setCompletedHearings(setHearingOutcomeCompletedHearings(selectedHearings));
        } else {
            preSubmitCallbackResponse.addError("There are no completed hearings on the case");
        }
        return preSubmitCallbackResponse;
    }

    private DynamicList setHearingOutcomeCompletedHearings(List<Hearing> hearings) {
        return new DynamicList(new DynamicListItem("", ""), hearings.stream()
                .map(hearing -> {
                    String hearingLabel = hearing.getValue().getStart().toString()
                            + "-" + hearing.getValue().getEnd().toLocalTime()
                            + ", " + hearing.getValue().getVenue().getName();
                    return new DynamicListItem(hearing.getValue().getHearingId(), hearingLabel);
                }).toList());
    }
}
