package uk.gov.hmcts.reform.sscs.ccd.presubmit.readytolist;

import static java.util.Objects.requireNonNull;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.helper.SscsHelper;

@Service
@Slf4j
public class ReadyToListAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    static final String EXISTING_HEARING_WARNING = "There is already a hearing request in List assist, "
            + "are you sure you want to send another request? If you do proceed, then please cancel the existing hearing request first";
    static final String GAPS_CASE_WARNING = "This is a GAPS case, If you do want to proceed, "
            + "then please change the hearing route to List Assist";

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
        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(callback.getCaseDetails().getCaseData());

        if (!sscsCaseData.isIbcCase() && HearingRoute.GAPS == sscsCaseData.getSchedulingAndListingFields().getHearingRoute()) {
            if (warningsShouldNotBeIgnored(callback)) {
                response.addWarning(GAPS_CASE_WARNING);
                log.warn("Warning: {}", GAPS_CASE_WARNING);
                return response;
            }
        }

        if (SscsHelper.hasHearingScheduledInTheFuture(sscsCaseData) && warningsShouldNotBeIgnored(callback)) {
            response.addWarning(EXISTING_HEARING_WARNING);
            log.warn("Warning: {}", EXISTING_HEARING_WARNING);
            return response;
        }
        return response;
    }

    boolean warningsShouldNotBeIgnored(Callback<SscsCaseData> callback) {
        return !callback.isIgnoreWarnings() && !YesNo.YES.equals(callback.getCaseDetails().getCaseData().getIgnoreCallbackWarnings());
    };
}
