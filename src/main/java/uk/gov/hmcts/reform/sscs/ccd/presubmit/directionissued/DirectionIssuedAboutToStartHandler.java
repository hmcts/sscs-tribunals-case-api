package uk.gov.hmcts.reform.sscs.ccd.presubmit.directionissued;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.*;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.directionissued.DirectionTypeItemList.*;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.directionissued.ExtensionNextEventItemList.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
public class DirectionIssuedAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    List<State> preValidStates = new ArrayList<>(Arrays.asList(INCOMPLETE_APPLICATION, INCOMPLETE_APPLICATION_INFORMATION_REQUESTED, INTERLOCUTORY_REVIEW_STATE, PENDING_APPEAL, INCOMPLETE_APPLICATION_VOID_STATE, VOID_STATE));

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return (callbackType.equals(CallbackType.ABOUT_TO_START)
                || callbackType.equals(CallbackType.MID_EVENT))
            && callback.getEvent() == EventType.DIRECTION_ISSUED;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        setDirectionTypeDropDown(callback.getCaseDetails().getState(), sscsCaseData);
        setExtensionNextEventDropdown(callback.getCaseDetails().getState(), sscsCaseData);
        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private void setDirectionTypeDropDown(State state, SscsCaseData sscsCaseData) {
        List<DynamicListItem> listOptions = new ArrayList<>();

        if (preValidStates.contains(state)) {
            listOptions.add(new DynamicListItem(APPEAL_TO_PROCEED.getCode(), APPEAL_TO_PROCEED.getLabel()));
        }

        listOptions.add(new DynamicListItem(PROVIDE_INFORMATION.getCode(), PROVIDE_INFORMATION.getLabel()));

        if ("Yes".equalsIgnoreCase(sscsCaseData.getTimeExtensionRequested())) {
            listOptions.add(new DynamicListItem(GRANT_EXTENSION.getCode(), GRANT_EXTENSION.getLabel()));
            listOptions.add(new DynamicListItem(REFUSE_EXTENSION.getCode(), REFUSE_EXTENSION.getLabel()));
        }

        DynamicListItem selectedValue = null != sscsCaseData.getDirectionTypeDl() && sscsCaseData.getDirectionTypeDl().getValue() != null
                ? sscsCaseData.getDirectionTypeDl().getValue() : new DynamicListItem("", "");
        sscsCaseData.setDirectionTypeDl(new DynamicList(selectedValue, listOptions));
    }

    private void setExtensionNextEventDropdown(State state, SscsCaseData sscsCaseData) {
        List<DynamicListItem> listOptions = new ArrayList<>();

        listOptions.add(new DynamicListItem(SEND_TO_LISTING.getCode(), SEND_TO_LISTING.getLabel()));
        listOptions.add(new DynamicListItem(NO_FURTHER_ACTION.getCode(), NO_FURTHER_ACTION.getLabel()));

        if (preValidStates.contains(state)) {
            listOptions.add(new DynamicListItem(SEND_TO_VALID_APPEAL.getCode(), SEND_TO_VALID_APPEAL.getLabel()));
        }

        DynamicListItem selectedValue = null != sscsCaseData.getExtensionNextEventDl() && sscsCaseData.getExtensionNextEventDl().getValue() != null
                ? sscsCaseData.getExtensionNextEventDl().getValue() : new DynamicListItem("", "");
        sscsCaseData.setExtensionNextEventDl(new DynamicList(selectedValue, listOptions));
    }
}
