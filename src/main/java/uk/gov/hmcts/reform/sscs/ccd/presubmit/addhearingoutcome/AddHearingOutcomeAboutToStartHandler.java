package uk.gov.hmcts.reform.sscs.ccd.presubmit.addhearingoutcome;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
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
import uk.gov.hmcts.reform.sscs.service.hmc.topic.HearingUpdateService;


@Component
@Slf4j
public class AddHearingOutcomeAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final HmcHearingsApiService hmcHearingsApiService;
    private final HearingUpdateService hearingUpdateService;

    public AddHearingOutcomeAboutToStartHandler(HmcHearingsApiService hmcHearingsApiService,
                                                HearingUpdateService hearingUpdateService) {
        this.hmcHearingsApiService = hmcHearingsApiService;
        this.hearingUpdateService = hearingUpdateService;
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

        try {
            HearingsGetResponse response = hmcHearingsApiService.getHearingsRequest(Long.toString(caseDetails.getId()), HmcStatus.COMPLETED);
            List<CaseHearing> hmcHearings = response.getCaseHearings();
            if (!hmcHearings.isEmpty()) {
                Map<String, CaseHearing> hmcHearingsMap = hmcHearings.stream()
                    .collect(Collectors.toMap(caseHearing -> caseHearing.getHearingId().toString(),
                        caseHearing -> caseHearing));

                if (sscsCaseData.getCompletedHearingsList() == null) {
                    sscsCaseData.setCompletedHearingsList(new ArrayList<>());
                }

                sscsCaseData.getCompletedHearingsList().addAll(sscsCaseData.getHearings()
                    .stream()
                    .filter(value -> value.getValue().getStart() != null)
                    .filter(hearingDetails -> {
                        CaseHearing hmcHearing = hmcHearingsMap.get(hearingDetails.getValue().getHearingId());
                        if (hmcHearing != null) {
                            hearingDetails.getValue().setStart(hearingUpdateService.convertUtcToUk(hmcHearing.getHearingDaySchedule().get(0).getHearingStartDateTime()));
                            hearingDetails.getValue().setEnd(hearingUpdateService.convertUtcToUk(hmcHearing.getHearingDaySchedule().get(0).getHearingEndDateTime()));
                            hearingDetails.getValue().setEpimsId(hmcHearing.getHearingDaySchedule().get(0).getHearingVenueEpimsId());
                            hearingDetails.getValue().setHearingChannel(hmcHearing.getHearingChannels().get(0));
                            return true;
                        }
                        return false;
                    })
                    .sorted(Comparator.reverseOrder())
                    .toList());

                sscsCaseData.setHearingOutcomeValue(HearingOutcomeValue.builder().build());
                sscsCaseData.getHearingOutcomeValue().setCompletedHearings(setHearingOutcomeCompletedHearings(sscsCaseData.getCompletedHearingsList()));
            } else {
                preSubmitCallbackResponse.addError("There are no completed hearings on the case.");
            }
        } catch (Exception e) {
            log.info("AddHearingOutcome failed for caseId {} with error {}", callback.getCaseDetails().getId(), e.getMessage());

            preSubmitCallbackResponse.addError("There was an error while retrieving hearing details; please try again after some time.");
        }
        return preSubmitCallbackResponse;
    }

    private DynamicList setHearingOutcomeCompletedHearings(List<Hearing> hearings) {
        return new DynamicList(new DynamicListItem("", ""), hearings.stream()
            .map(Hearing::getValue)
            .map(hearing -> {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm", Locale.ENGLISH);
                String hearingLabel = hearing.getStart().format(formatter)
                    + "-" + hearing.getEnd().toLocalTime()
                    + ", " + hearing.getVenue().getName();
                return new DynamicListItem(hearing.getHearingId(), hearingLabel);
            }).toList());
    }
}
