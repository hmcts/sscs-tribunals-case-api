package uk.gov.hmcts.reform.sscs.ccd.presubmit.dwpraiseexception;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;


@Service
@Slf4j
public class DwpRaiseExceptionAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private boolean ucEnabled;

    public DwpRaiseExceptionAboutToSubmitHandler(@Value("${feature.universal-credit.enabled}") boolean ucEnabled) {
        this.ucEnabled = ucEnabled;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return ucEnabled && callbackType == CallbackType.ABOUT_TO_SUBMIT
                && callback.getEvent() == EventType.DWP_RAISE_EXCEPTION
                && Objects.nonNull(callback.getCaseDetails())
                && Objects.nonNull(callback.getCaseDetails().getCaseData());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        sscsCaseData.setIsProgressingViaGaps("Yes");
        sscsCaseData.setState(State.NOT_LISTABLE);
        sscsCaseData.setCreatedInGapsFrom(State.VALID_APPEAL.getId());

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        return preSubmitCallbackResponse;
    }
}
