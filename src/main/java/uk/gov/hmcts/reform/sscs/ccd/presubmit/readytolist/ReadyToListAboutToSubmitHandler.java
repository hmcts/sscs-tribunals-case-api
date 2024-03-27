package uk.gov.hmcts.reform.sscs.ccd.presubmit.readytolist;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.helper.SscsHelper;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;
import uk.gov.hmcts.reform.sscs.service.servicebus.HearingMessagingServiceFactory;

@Service
@Slf4j
public class ReadyToListAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final boolean gapsSwitchOverFeature;

    private final RegionalProcessingCenterService regionalProcessingCenterService;

    private final HearingMessagingServiceFactory hearingMessagingServiceFactory;

    public ReadyToListAboutToSubmitHandler(@Value("${feature.gaps-switchover.enabled}") boolean gapsSwitchOverFeature,
                                           @Autowired RegionalProcessingCenterService regionalProcessingCenterService,
                                           @Autowired HearingMessagingServiceFactory hearingMessagingServiceFactory) {
        this.gapsSwitchOverFeature = gapsSwitchOverFeature;
        this.regionalProcessingCenterService = regionalProcessingCenterService;
        this.hearingMessagingServiceFactory = hearingMessagingServiceFactory;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && callback.getEvent() == EventType.READY_TO_LIST;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback.");
        }

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        if (HearingRoute.GAPS == sscsCaseData.getSchedulingAndListingFields().getHearingRoute()) {

            if (!callback.isIgnoreWarnings()) {
                PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(callback.getCaseDetails().getCaseData());
                String gapsProceedWarning = "This is a GAPS case, If you do want to proceed, "
                    + "then please change the hearing route to List Assist";
                response.addWarning(gapsProceedWarning);
                return response;
            }

            return HearingHandler.GAPS.handle(sscsCaseData, gapsSwitchOverFeature,
                hearingMessagingServiceFactory.getMessagingService(HearingRoute.GAPS));
        }

        if (SscsHelper.hasHearingScheduledInTheFuture(sscsCaseData)
                && !callback.isIgnoreWarnings()) {
            PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(callback.getCaseDetails().getCaseData());
            String listAssistExistsWarning = "There is already a hearing request in List assist, "
                + "are you sure you want to send another request? If you do proceed, then please cancel the existing hearing request first";
            response.addWarning(listAssistExistsWarning);
            return response;
        }
        
        String region = sscsCaseData.getRegion();

        Map<String, RegionalProcessingCenter> regionalProcessingCenterMap = regionalProcessingCenterService
            .getRegionalProcessingCenterMap();

        HearingRoute route = regionalProcessingCenterMap.values().stream()
            .filter(rpc -> rpc.getName().equalsIgnoreCase(region))
            .map(RegionalProcessingCenter::getHearingRoute)
            .findFirst().orElse(HearingRoute.LIST_ASSIST);

        return HearingHandler.valueOf(route.name()).handle(sscsCaseData, gapsSwitchOverFeature,
            hearingMessagingServiceFactory.getMessagingService(route));
    }
}
