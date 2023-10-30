package uk.gov.hmcts.reform.sscs.ccd.presubmit.dwpuploadresponse;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;

import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.ResponseEventsAboutToSubmit;

@Component
@Slf4j
public class DwpUploadResponseAboutToStartHandler extends ResponseEventsAboutToSubmit implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START) && callback.getEvent() == EventType.DWP_UPLOAD_RESPONSE;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        if (!READY_TO_LIST.getId().equals(sscsCaseData.getCreatedInGapsFrom())) {
            preSubmitCallbackResponse.addError("This case cannot be updated by DWP");
        } else {
            updateDwpStateList(sscsCaseData);
        }

        return preSubmitCallbackResponse;
    }

    private void updateDwpStateList(SscsCaseData sscsCaseData) {
        List<DwpState> postHearingDwpStates = DwpState.getPostHearingDwpStates();
        List<DynamicListItem> dwpStatesExcludingPostHearing = Arrays.stream(DwpState.values())
                .filter(state -> !postHearingDwpStates.contains(state))
                .map(state -> new DynamicListItem(state.getCcdDefinition(), state.getDescription()))
                .toList();

        DwpState dwpState = sscsCaseData.getDwpState();
        if (nonNull(dwpState) && postHearingDwpStates.contains(dwpState)) {
            dwpState = null;
        }

        DynamicListItem selectedState = nonNull(dwpState) ? new DynamicListItem(dwpState.getCcdDefinition(), dwpState.getDescription()) : null;

        sscsCaseData.setDynamicDwpState(new DynamicList(selectedState, dwpStatesExcludingPostHearing));
    }

}
