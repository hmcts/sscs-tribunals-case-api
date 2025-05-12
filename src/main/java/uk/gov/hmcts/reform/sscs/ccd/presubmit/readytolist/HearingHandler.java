package uk.gov.hmcts.reform.sscs.ccd.presubmit.readytolist;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.exception.GetCaseException;
import uk.gov.hmcts.reform.sscs.exception.TribunalsEventProcessingException;
import uk.gov.hmcts.reform.sscs.exception.UpdateCaseException;
import uk.gov.hmcts.reform.sscs.model.hearings.HearingRequest;
import uk.gov.hmcts.reform.sscs.service.hmc.topic.HearingRequestHandler;

@Slf4j
public enum HearingHandler {
    GAPS {
        @Override
        public PreSubmitCallbackResponse<SscsCaseData> handle(SscsCaseData sscsCaseData,
                                                              HearingRequestHandler hearingRequestHandler) {
            sscsCaseData.getSchedulingAndListingFields().setHearingRoute(HearingRoute.GAPS);
            sscsCaseData.getSchedulingAndListingFields().setHearingState(HearingState.CREATE_HEARING);
            PreSubmitCallbackResponse<SscsCaseData> callbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
            log.info(String.format("createdInGapsFrom is %s for caseId %s",
                    sscsCaseData.getCreatedInGapsFrom(), sscsCaseData.getCcdCaseId()));

            if (sscsCaseData.getCreatedInGapsFrom() == null
                    || StringUtils.equalsIgnoreCase(sscsCaseData.getCreatedInGapsFrom(), State.VALID_APPEAL.getId())) {
                callbackResponse.addError("Case already created in GAPS at valid appeal.");
                log.warn(String.format("Case already created in GAPS at valid appeal for caseId %s.",
                        sscsCaseData.getCcdCaseId()));
            }
            return callbackResponse;
        }
    },
    LIST_ASSIST {
        @Override
        public PreSubmitCallbackResponse<SscsCaseData> handle(SscsCaseData sscsCaseData,
                                                              HearingRequestHandler hearingRequestHandler) {

            PreSubmitCallbackResponse<SscsCaseData> callbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

            log.info(String.format("Handling List Assist request for case ID: %s", sscsCaseData.getCcdCaseId()));

            HearingRoute hearingRoute = HearingRoute.LIST_ASSIST;
            HearingState hearingState = HearingState.CREATE_HEARING;

            try {
                hearingRequestHandler.handleHearingRequest(
                        HearingRequest.builder(sscsCaseData.getCcdCaseId())
                                .hearingRoute(hearingRoute)
                                .hearingState(hearingState)
                                .build()
                );
                sscsCaseData.getSchedulingAndListingFields().setHearingRoute(hearingRoute);
                sscsCaseData.getSchedulingAndListingFields().setHearingState(hearingState);
            } catch (TribunalsEventProcessingException | GetCaseException | UpdateCaseException e) {
                callbackResponse.addError("An error occurred during message publish. Please try again.");
            }

            return callbackResponse;
        }
    };

    public abstract PreSubmitCallbackResponse<SscsCaseData> handle(SscsCaseData caseData,
                                                                   HearingRequestHandler hearingRequestHandler);
}
