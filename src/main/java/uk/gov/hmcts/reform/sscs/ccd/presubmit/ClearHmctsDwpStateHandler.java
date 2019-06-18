package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static java.util.Objects.requireNonNull;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Component
public class ClearHmctsDwpStateHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    public boolean canHandle(Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");

        return callback.getEvent() == EventType.SEND_TO_DWP_OFFLINE;
    }

    public PreSubmitCallbackResponse<SscsCaseData> handle(Callback<SscsCaseData> callback) {
        if (!canHandle(callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        sscsCaseData.setHmctsDwpState(null);

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }
}
