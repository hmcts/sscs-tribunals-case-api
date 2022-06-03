package uk.gov.hmcts.reform.sscs.ccd.presubmit.postponementrequest;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.POSTPONEMENTS_NOT_POSSIBLE_FOR_GAPS;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
public class PostponementRequestAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
                && callback.getEvent() == EventType.POSTPONEMENT_REQUEST
                && callback.getCaseDetails() != null;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        final Optional<Hearing> optionalHearing = emptyIfNull(sscsCaseData.getHearings()).stream()
                .filter(h -> h.getValue().getHearingDateTime().isAfter(LocalDateTime.now()))
                .distinct()
                .findFirst();

        final PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);

        if (HearingRoute.GAPS.equals(sscsCaseData.getRegionalProcessingCenter().getHearingRoute())) {
            response.addError(POSTPONEMENTS_NOT_POSSIBLE_FOR_GAPS);
        }

        optionalHearing.ifPresentOrElse(hearing -> setPostponementRequest(hearing, sscsCaseData),
                () -> response.addError("There are no hearing to postpone"));

        return response;
    }

    private void setPostponementRequest(Hearing hearing, SscsCaseData sscsCaseData) {
        sscsCaseData.getPostponementRequest().setPostponementRequestHearingDateAndTime(hearing.getValue().getHearingDateTime().toString());
        sscsCaseData.getPostponementRequest().setPostponementRequestHearingVenue(hearing.getValue().getVenue().getName());
    }
}
