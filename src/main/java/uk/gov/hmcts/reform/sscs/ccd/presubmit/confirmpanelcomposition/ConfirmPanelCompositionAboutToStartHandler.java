package uk.gov.hmcts.reform.sscs.ccd.presubmit.confirmpanelcomposition;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.readytolist.ReadyToListAboutToSubmitHandler.EXISTING_HEARING_WARNING;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus;
import uk.gov.hmcts.reform.sscs.model.multi.hearing.HearingsGetResponse;
import uk.gov.hmcts.reform.sscs.service.HmcHearingApiService;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConfirmPanelCompositionAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final HmcHearingApiService hmcHearingApiService;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
                && callback.getEvent() == EventType.CONFIRM_PANEL_COMPOSITION;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(callback.getCaseDetails().getCaseData());

        HearingsGetResponse hearingsGetResponse = hmcHearingApiService.getHearingsRequest(sscsCaseData.getCcdCaseId(), HmcStatus.LISTED);
        if (HearingRoute.LIST_ASSIST == sscsCaseData.getSchedulingAndListingFields().getHearingRoute()
                && nonNull(hearingsGetResponse.getCaseHearings())  && !hearingsGetResponse.getCaseHearings().isEmpty()) {
            response.addError(EXISTING_HEARING_WARNING);
            log.error("Error on case {}: There is a listed hearing request in List assist", sscsCaseData.getCcdCaseId());
            return response;
        }

        return response;
    }

}
