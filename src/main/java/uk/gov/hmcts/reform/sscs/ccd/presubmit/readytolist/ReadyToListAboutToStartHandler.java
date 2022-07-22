package uk.gov.hmcts.reform.sscs.ccd.presubmit.readytolist;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.GAPS;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReadyToListAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Value("${feature.gaps-switchover.enabled}")
    private boolean gapsSwitchOverFeature;

    private final RegionalProcessingCenterService regionalProcessingCenterService;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
            && callback.getEvent() == EventType.READY_TO_LIST;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback.");
        }

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        HearingRoute hearingRoute = GAPS;

        if (gapsSwitchOverFeature) {
            hearingRoute = getHearingRoute(sscsCaseData);
            sscsCaseData.getSchedulingAndListingFields().setHearingRoute(hearingRoute);
        }

        PreSubmitCallbackResponse<SscsCaseData> callbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        if (GAPS == hearingRoute) {
            handleGaps(sscsCaseData, callbackResponse);
        }

        return callbackResponse;
    }

    @NotNull
    private HearingRoute getHearingRoute(SscsCaseData sscsCaseData) {
        HearingRoute hearingRoute;
        String region = sscsCaseData.getRegion();

        Map<String, RegionalProcessingCenter> regionalProcessingCenterMap = regionalProcessingCenterService
            .getRegionalProcessingCenterMap();

        hearingRoute = regionalProcessingCenterMap.values().stream()
            .filter(rpc -> rpc.getName().equalsIgnoreCase(region))
            .map(RegionalProcessingCenter::getHearingRoute)
            .findFirst().orElse(HearingRoute.LIST_ASSIST);
        return hearingRoute;
    }

    public void handleGaps(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> callbackResponse) {
        log.info("createdInGapsFrom is {} for caseId {}", sscsCaseData.getCreatedInGapsFrom(), sscsCaseData.getCcdCaseId());

        if (sscsCaseData.getCreatedInGapsFrom() == null
            || StringUtils.equalsIgnoreCase(sscsCaseData.getCreatedInGapsFrom(), State.VALID_APPEAL.getId())) {
            callbackResponse.addError("Case already created in GAPS at valid appeal.");
            log.warn("Case already created in GAPS at valid appeal for caseId {}.", sscsCaseData.getCcdCaseId());
        }
    }
}
