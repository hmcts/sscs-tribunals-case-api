package uk.gov.hmcts.reform.sscs.ccd.presubmit.readytolist;

import static java.util.Objects.requireNonNull;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;


@Service
@Slf4j
public class ReadyToListAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final boolean gapsSwitchOverFeature;

    private final RegionalProcessingCenterService regionalProcessingCenterService;

    public ReadyToListAboutToSubmitHandler(@Value("${feature.gaps-switchover.enabled}")  boolean gapsSwitchOverFeature,
                                           @Autowired RegionalProcessingCenterService regionalProcessingCenterService) {
        this.gapsSwitchOverFeature = gapsSwitchOverFeature;
        this.regionalProcessingCenterService = regionalProcessingCenterService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && callback.getEvent() == EventType.READY_TO_LIST;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback.");
        }

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        if (!gapsSwitchOverFeature) {
            return HearingHandler.GAPS.handle(sscsCaseData, gapsSwitchOverFeature);
        }

        String region = sscsCaseData.getRegion();
        return HearingHandler.valueOf(regionalProcessingCenterService.getHearingRoute(region).name()).handle(sscsCaseData, gapsSwitchOverFeature);
    }
}
