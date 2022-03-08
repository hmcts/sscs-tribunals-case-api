package uk.gov.hmcts.reform.sscs.ccd.presubmit.readytolist;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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

    @Value("${feature.scheduling-and-listing.enabled}")
    private boolean schedulingAndListingFeature;

    @Autowired
    private RegionalProcessingCenterService regionalProcessingCenterService;

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

        final SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        if (schedulingAndListingFeature && checkIfListAssist(sscsCaseData)) {
            sscsCaseData.setHearingRoute(HearingRoute.LIST_ASSIST);
            sscsCaseData.setHearingState(StateOfHearing.HEARING_CREATED);
            return new PreSubmitCallbackResponse<>(sscsCaseData);
        } else {
            return handleCallbackResponse(sscsCaseData);
        }
    }

    private boolean checkIfListAssist(SscsCaseData sscsCaseData) {
        String region = sscsCaseData.getRegion();
        Map<String, RegionalProcessingCenter> regionalProcessingCenterMap =  regionalProcessingCenterService
            .getRegionalProcessingCenterMap();
        Optional<Boolean> isListAssistOptional = regionalProcessingCenterMap.values().stream()
            .filter(rpc -> rpc.getName().equalsIgnoreCase(region))
            .map(RegionalProcessingCenter::isListAssist)
            .findFirst();
        return isListAssistOptional.orElse(true);
    }

    private PreSubmitCallbackResponse<SscsCaseData> handleCallbackResponse(SscsCaseData sscsCaseData) {
        sscsCaseData.setHearingRoute(HearingRoute.GAPS);
        PreSubmitCallbackResponse<uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData> callbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
        log.info(String.format("createdInGapsFrom is %s for caseId %s", sscsCaseData.getCreatedInGapsFrom(), sscsCaseData.getCcdCaseId()));
        if (sscsCaseData.getCreatedInGapsFrom() == null
            || StringUtils.equalsIgnoreCase(sscsCaseData.getCreatedInGapsFrom(), State.VALID_APPEAL.getId())) {
            callbackResponse.addError("Case already created in GAPS at valid appeal.");
            log.warn(String.format("Case already created in GAPS at valid appeal for caseId %s.", sscsCaseData.getCcdCaseId()));
        }
        return callbackResponse;
    }
}
