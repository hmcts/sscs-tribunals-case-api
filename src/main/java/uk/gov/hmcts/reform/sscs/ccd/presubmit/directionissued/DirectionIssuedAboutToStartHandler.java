package uk.gov.hmcts.reform.sscs.ccd.presubmit.directionissued;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType.*;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.directionissued.ExtensionNextEventItemList.*;
import static uk.gov.hmcts.reform.sscs.helper.SscsHelper.getPreValidStates;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
public class DirectionIssuedAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

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

        setDirectionTypeDropDown(sscsCaseData);
        setExtensionNextEventDropdown(callback.getCaseDetails().getState(), sscsCaseData);
        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private void setDirectionTypeDropDown(SscsCaseData sscsCaseData) {

        List<DynamicListItem> listOptions = new ArrayList<>();

        listOptions.add(new DynamicListItem(APPEAL_TO_PROCEED.toString(), APPEAL_TO_PROCEED.getLabel()));
        listOptions.add(new DynamicListItem(PROVIDE_INFORMATION.toString(), PROVIDE_INFORMATION.getLabel()));

        if (YesNo.YES.equals(sscsCaseData.getSscsHearingRecordingCaseData().getHearingRecordingRequestOutstanding())) {
            listOptions.add(new DynamicListItem(REFUSE_HEARING_RECORDING_REQUEST.toString(), REFUSE_HEARING_RECORDING_REQUEST.getLabel()));
        }

        if ("Yes".equalsIgnoreCase(sscsCaseData.getTimeExtensionRequested())) {
            listOptions.add(new DynamicListItem(GRANT_EXTENSION.toString(), GRANT_EXTENSION.getLabel()));
            listOptions.add(new DynamicListItem(REFUSE_EXTENSION.toString(), REFUSE_EXTENSION.getLabel()));
        }

        if (RequestOutcome.IN_PROGRESS.equals(sscsCaseData.getReinstatementOutcome())) {
            listOptions.add(new DynamicListItem(GRANT_REINSTATEMENT.toString(), GRANT_REINSTATEMENT.getLabel()));
            listOptions.add(new DynamicListItem(REFUSE_REINSTATEMENT.toString(), REFUSE_REINSTATEMENT.getLabel()));
        }

        if ("Yes".equalsIgnoreCase(sscsCaseData.getUrgentCase())) {
            listOptions.add(new DynamicListItem(GRANT_URGENT_HEARING.toString(), GRANT_URGENT_HEARING.getLabel()));
            listOptions.add(new DynamicListItem(REFUSE_URGENT_HEARING.toString(), REFUSE_URGENT_HEARING.getLabel()));
        }

        DynamicListItem selectedValue = null != sscsCaseData.getDirectionTypeDl() && sscsCaseData.getDirectionTypeDl().getValue() != null
                ? sscsCaseData.getDirectionTypeDl().getValue() : new DynamicListItem("", "");


        sscsCaseData.setDirectionTypeDl(new DynamicList(selectedValue, listOptions));
    }

    private void setExtensionNextEventDropdown(State state, SscsCaseData sscsCaseData) {
        List<DynamicListItem> listOptions = new ArrayList<>();

        listOptions.add(new DynamicListItem(SEND_TO_LISTING.getCode(), SEND_TO_LISTING.getLabel()));
        listOptions.add(new DynamicListItem(NO_FURTHER_ACTION.getCode(), NO_FURTHER_ACTION.getLabel()));

        if (getPreValidStates().contains(state)) {
            listOptions.add(new DynamicListItem(SEND_TO_VALID_APPEAL.getCode(), SEND_TO_VALID_APPEAL.getLabel()));
        }

        DynamicListItem selectedValue = null != sscsCaseData.getExtensionNextEventDl() && sscsCaseData.getExtensionNextEventDl().getValue() != null
                ? sscsCaseData.getExtensionNextEventDl().getValue() : new DynamicListItem("", "");
        sscsCaseData.setExtensionNextEventDl(new DynamicList(selectedValue, listOptions));
    }
}
