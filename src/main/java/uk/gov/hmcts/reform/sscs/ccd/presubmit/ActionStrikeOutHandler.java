package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.STRIKE_OUT_ACTIONED;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Service
@Slf4j
public class ActionStrikeOutHandler extends EventToFieldPreSubmitCallbackHandler {

    private final ListAssistHearingMessageHelper hearingMessageHelper;
    private boolean isScheduleListingEnabled;

    ActionStrikeOutHandler(ListAssistHearingMessageHelper hearingMessageHelper,
        @Value("${feature.snl.enabled}") boolean isScheduleListingEnabled) {
        super(createMappings());
        this.hearingMessageHelper = hearingMessageHelper;
        this.isScheduleListingEnabled = isScheduleListingEnabled;
    }

    private static Map<EventType, String> createMappings() {
        Map<EventType, String> eventFieldMappings = new HashMap<>();
        eventFieldMappings.put(EventType.ACTION_STRIKE_OUT, STRIKE_OUT_ACTIONED.getId());
        return eventFieldMappings;
    }

    @Override
    protected SscsCaseData setField(SscsCaseData sscsCaseData, String newValue, EventType eventType) {
        sscsCaseData.setDwpState(newValue);
        return sscsCaseData;
    }

    @Override
    protected void cancelHearing(SscsCaseData sscsCaseData) {
        if (eligibleForHearingsCancel.test(sscsCaseData)) {
            log.info(String.format("Strike out - Sending cancel request for hearing for case id %s", sscsCaseData
                    .getCcdCaseId()));
            hearingMessageHelper.sendListAssistCancelHearingMessage(sscsCaseData.getCcdCaseId());
        }
    }

    private final Predicate<SscsCaseData> eligibleForHearingsCancel = sscsCaseData -> isScheduleListingEnabled
            && SscsUtil.isValidCaseStateForHearingCancel(sscsCaseData, List.of(State.HEARING, State.READY_TO_LIST))
            && SscsUtil.isSAndLCase(sscsCaseData);

}
