package uk.gov.hmcts.reform.sscs.ccd.presubmit.readytolist;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;
import uk.gov.hmcts.reform.sscs.service.hmc.topic.HearingRequestHandler;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReadyToListAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    static final String GAPS_CASE_WARNING = "This is a GAPS case, If you do want to proceed, "
            + "then please change the hearing route to List Assist";

    private final RegionalProcessingCenterService regionalProcessingCenterService;
    private final HearingRequestHandler hearingRequestHandler;


    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT) && callback.getEvent() == EventType.READY_TO_LIST;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback.");
        }

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        if (!sscsCaseData.isIbcCase() && HearingRoute.GAPS == sscsCaseData.getSchedulingAndListingFields().getHearingRoute()) {

            if (warningsShouldNotBeIgnored(callback)) {
                var response = new PreSubmitCallbackResponse<>(callback.getCaseDetails().getCaseData());
                response.addWarning(GAPS_CASE_WARNING);
                log.warn("Warning: {}", GAPS_CASE_WARNING);
                return response;
            }

            return HearingHandler.GAPS.handle(sscsCaseData, hearingRequestHandler);
        }

        String region = sscsCaseData.getRegion();

        if (sscsCaseData.isIbcCase()) {
            SscsUtil.setListAssistRoutes(sscsCaseData);
            return HearingHandler.valueOf(HearingRoute.LIST_ASSIST.name()).handle(sscsCaseData, hearingRequestHandler);
        }
        Map<String, RegionalProcessingCenter> regionalProcessingCenterMap = regionalProcessingCenterService
                .getRegionalProcessingCenterMap();

        HearingRoute route = regionalProcessingCenterMap.values().stream()
                .filter(rpc -> rpc.getName().equalsIgnoreCase(region))
                .map(RegionalProcessingCenter::getHearingRoute)
                .findFirst().orElse(HearingRoute.GAPS);

        // clear the ignore warnings field after use
        sscsCaseData.setIgnoreCallbackWarnings(null);

        log.info("Calling hearing handler for route {} for case ID: {}", route, sscsCaseData.getCcdCaseId());
        return HearingHandler.valueOf(route.name()).handle(sscsCaseData, hearingRequestHandler);
    }

    public static boolean warningsShouldNotBeIgnored(Callback<SscsCaseData> callback) {
        return !callback.isIgnoreWarnings() && !YesNo.YES.equals(callback.getCaseDetails().getCaseData().getIgnoreCallbackWarnings());
    }
}
