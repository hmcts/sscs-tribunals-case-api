package uk.gov.hmcts.reform.sscs.ccd.presubmit.postponementrequest;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;


@Service
public class PostponementRequestAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    public static final String NOT_LIST_ASSIST_CASE_ERROR = "Postponement requests can only be made for list"
        + " assist cases";

    @Value("${feature.snl.enabled}")
    private boolean isScheduleListingEnabled;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType == CallbackType.ABOUT_TO_START
            && callback.getEvent() == EventType.POSTPONEMENT_REQUEST
            && isScheduleListingEnabled;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        final PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);

        if (!SscsUtil.isSAndLCase(sscsCaseData)) {
            response.addError(NOT_LIST_ASSIST_CASE_ERROR);
            return response;
        }

        Hearing hearing = sscsCaseData.getLatestHearing();

        if (isNull(hearing)) {
            response.addError("There is not a hearing to postpone");
            return response;
        }

        HearingStatus hearingStatus = Optional.ofNullable(hearing.getValue())
            .map(HearingDetails::getHearingStatus)
            .orElse(null);

        if (!HearingStatus.LISTED.equals(hearingStatus)) {
            response.addError("There not a listed hearing to postpone");
            return response;
        }

        setPostponementRequest(hearing, sscsCaseData);

        return response;
    }

    private void setPostponementRequest(Hearing hearing, SscsCaseData sscsCaseData) {
        sscsCaseData.getPostponementRequest().setPostponementRequestHearingDateAndTime(hearing.getValue().getStart().toString());
        sscsCaseData.getPostponementRequest().setPostponementRequestHearingVenue(hearing.getValue().getVenue().getName());
    }
}
