package uk.gov.hmcts.reform.sscs.ccd.presubmit.directionissued;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.directionissued.ExtensionNextEventItemList.*;
import static uk.gov.hmcts.reform.sscs.helper.SscsHelper.getPreValidStates;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil;

@Service
public class DirectionIssuedAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    private final boolean isPostHearingsEnabled;

    public DirectionIssuedAboutToStartHandler(@Value("${feature.postHearings.enabled}") boolean isPostHearingsEnabled) {
        this.isPostHearingsEnabled = isPostHearingsEnabled;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

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

        if (isPostHearingsEnabled) {
            sscsCaseData.setPrePostHearing(null);
        }

        clearFields(sscsCaseData);
        setPartiesToSendLetter(sscsCaseData);
        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private void setDirectionTypeDropDown(SscsCaseData sscsCaseData) {

        List<DynamicListItem> listOptions = new ArrayList<>();

        listOptions.add(new DynamicListItem(APPEAL_TO_PROCEED.toString(), APPEAL_TO_PROCEED.getLabel()));
        listOptions.add(new DynamicListItem(PROVIDE_INFORMATION.toString(), PROVIDE_INFORMATION.getLabel()));

        if (isYes(sscsCaseData.getSscsHearingRecordingCaseData().getHearingRecordingRequestOutstanding())) {
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

    private void clearFields(SscsCaseData sscsCaseData) {
        sscsCaseData.setConfidentialityType(null);
        sscsCaseData.setSendDirectionNoticeToFTA(null);
        sscsCaseData.setSendDirectionNoticeToRepresentative(null);
        sscsCaseData.setSendDirectionNoticeToOtherPartyRep(null);
        sscsCaseData.setSendDirectionNoticeToOtherPartyAppointee(null);
        sscsCaseData.setSendDirectionNoticeToOtherParty(null);
        sscsCaseData.setSendDirectionNoticeToJointParty(null);
        sscsCaseData.setSendDirectionNoticeToAppellantOrAppointee(null);
    }

    private void setPartiesToSendLetter(SscsCaseData sscsCaseData) {

        YesNo hasOtherParty = OtherPartyDataUtil.isOtherPartyPresent(sscsCaseData) ? YES : NO;
        YesNo hasOtherPartyRep = NO;
        YesNo hasOtherPartyAppointee = NO;

        if (isYes(hasOtherParty)) {
            boolean hasOtherPartyRepBoolean = sscsCaseData.getOtherParties().stream().map(CcdValue::getValue).anyMatch(OtherParty::hasRepresentative);
            boolean hasOtherPartyAppointeeBoolean = sscsCaseData.getOtherParties().stream().map(CcdValue::getValue).anyMatch(OtherParty::hasAppointee);

            if (hasOtherPartyRepBoolean) {
                hasOtherPartyRep = YES;
            }

            if (hasOtherPartyAppointeeBoolean) {
                hasOtherPartyAppointee = YES;
            }
        }
        YesNo hasRepresentative = sscsCaseData.isThereARepresentative() ? YES : NO;
        sscsCaseData.setHasRepresentative(hasRepresentative);

        sscsCaseData.setHasOtherPartyRep(hasOtherPartyRep);
        sscsCaseData.setHasOtherPartyAppointee(hasOtherPartyAppointee);
        sscsCaseData.setHasOtherParties(hasOtherParty);

        YesNo hasJointParty = sscsCaseData.isThereAJointParty() ? YES : NO;
        sscsCaseData.setHasJointParty(hasJointParty);
    }
}
