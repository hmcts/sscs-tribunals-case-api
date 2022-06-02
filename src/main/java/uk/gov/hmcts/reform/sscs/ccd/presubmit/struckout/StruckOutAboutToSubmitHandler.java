package uk.gov.hmcts.reform.sscs.ccd.presubmit.struckout;

import static java.util.Objects.requireNonNull;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.dormant.DormantEventsAboutToSubmitHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;

@Service
@Slf4j
public class StruckOutAboutToSubmitHandler extends DormantEventsAboutToSubmitHandler {

    public StruckOutAboutToSubmitHandler(
            ListAssistHearingMessageHelper hearingMessageHelper,
            @Value("${feature.snl.enabled}") boolean isScheduleListingEnabled) {
        super(hearingMessageHelper, isScheduleListingEnabled);
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.STRUCK_OUT;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        super.handle(callbackType, callback, userAuthorisation);

        final SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        log.info(String.format("Handling struckOut event for caseId %s", sscsCaseData.getCcdCaseId()));

        sscsCaseData.setDwpState(DwpState.STRIKE_OUT_ACTIONED.getId());

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }
}
