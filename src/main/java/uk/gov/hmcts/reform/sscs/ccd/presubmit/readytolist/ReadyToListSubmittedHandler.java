package uk.gov.hmcts.reform.sscs.ccd.presubmit.readytolist;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;
import uk.gov.hmcts.reform.sscs.service.hmc.topic.HearingMessageService;
import uk.gov.hmcts.reform.sscs.service.servicebus.SendCallbackHandler;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Service
@Slf4j
public class ReadyToListSubmittedHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final RegionalProcessingCenterService regionalProcessingCenterService;
    private final HearingMessageService hearingsMessageService;
    private final SendCallbackHandler sendCallbackHandler;

    public ReadyToListSubmittedHandler(@Autowired RegionalProcessingCenterService regionalProcessingCenterService,
                                       @Autowired HearingMessageService hearingsMessageService,
                                       @Autowired SendCallbackHandler sendCallbackHandler) {
        this.regionalProcessingCenterService = regionalProcessingCenterService;
        this.hearingsMessageService = hearingsMessageService;
        this.sendCallbackHandler = sendCallbackHandler;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
            && callback.getEvent() == EventType.READY_TO_LIST;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback.");
        }

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        if (!sscsCaseData.isIbcCase() && HearingRoute.GAPS == sscsCaseData.getSchedulingAndListingFields().getHearingRoute()) {
            return HearingHandler.GAPS.handle(sscsCaseData, hearingsMessageService);
        }
        
        String region = sscsCaseData.getRegion();

        if (sscsCaseData.isIbcCase()) {
            SscsUtil.setListAssistRoutes(sscsCaseData);
            return HearingHandler.valueOf(HearingRoute.LIST_ASSIST.name()).handle(sscsCaseData, hearingsMessageService);
        }
        Map<String, RegionalProcessingCenter> rpcMap = regionalProcessingCenterService.getRegionalProcessingCenterMap();

        HearingRoute route = rpcMap.values()
                .stream()
                .filter(rpc -> rpc.getName().equalsIgnoreCase(region))
                .map(RegionalProcessingCenter::getHearingRoute)
                .findFirst().orElse(HearingRoute.GAPS);

        var response = HearingHandler.valueOf(route.name()).handle(sscsCaseData, hearingsMessageService);
        sendCallbackHandler.handle(callback);
        return response;
    }
}
