package uk.gov.hmcts.reform.sscs.ccd.presubmit.tribunalcommunication;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isNoOrNull;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getCommunicationRequestFromId;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getRepliesWithoutReviews;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CommunicationRequest;
import uk.gov.hmcts.reform.sscs.ccd.domain.CommunicationRequestDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CommunicationRequestReply;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.FtaCommunicationFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.FtaRequestType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.TribunalRequestType;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;


@Service
@Slf4j
public class TribunalCommunicationMidEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final boolean isFtaCommunicationEnabled;

    @Autowired
    public TribunalCommunicationMidEventHandler(@Value("${feature.fta-communication.enabled}") boolean isFtaCommunicationEnabled) {
        this.isFtaCommunicationEnabled = isFtaCommunicationEnabled;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.MID_EVENT)
            && callback.getEvent() == EventType.TRIBUNAL_COMMUNICATION;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {

        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        SscsCaseData sscsCaseData = caseDetails.getCaseData();
        PreSubmitCallbackResponse<SscsCaseData> preSubmitErrorCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
        if (!isFtaCommunicationEnabled) {
            return preSubmitErrorCallbackResponse;
        }

        FtaCommunicationFields ftaCommunicationFields = Optional.ofNullable(sscsCaseData.getCommunicationFields())
            .orElse(FtaCommunicationFields.builder().build());

        if (callback.getPageId().equals("selectTribunalCommunicationAction")) {
            if (ftaCommunicationFields.getTribunalRequestType().equals(TribunalRequestType.REVIEW_TRIBUNAL_REPLY)) {
                setFtaRequestRepliesDynamicList(ftaCommunicationFields, sscsCaseData);
                handleReviewTribunalReplyError(preSubmitErrorCallbackResponse, ftaCommunicationFields);
            }
        } else if (callback.getPageId().equals("selectTribunalReply")) {
            setQueryReplyForReview(sscsCaseData, ftaCommunicationFields);
        } else if (callback.getPageId().equals("reviewTribunalReply")) {
            YesNo actioned = ftaCommunicationFields.getTribunalRequestRespondedActioned();
            if (isNoOrNull(actioned)) {
                preSubmitErrorCallbackResponse.addError("Please only select Yes if all actions to the response have been completed.");
            }
        }

        if (!preSubmitErrorCallbackResponse.getErrors().isEmpty()) {
            return preSubmitErrorCallbackResponse;
        }

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private void setQueryReplyForReview(SscsCaseData sscsCaseData, FtaCommunicationFields ftaCommunicationFields) {
        DynamicList repliesDl = ftaCommunicationFields.getTribunalRequestRespondedDl();
        DynamicListItem chosenRequest = Optional.ofNullable(repliesDl.getValue())
            .orElseThrow(() -> new IllegalStateException("No chosen FTA request found"));
        String chosenRequestId = chosenRequest.getCode();
        CommunicationRequest communicationRequest = getCommunicationRequestFromId(chosenRequestId, ftaCommunicationFields.getTribunalCommunications());
        CommunicationRequestDetails requestDetails = communicationRequest.getValue();
        ftaCommunicationFields.setTribunalRequestRespondedQuery(requestDetails.getRequestMessage());
        ftaCommunicationFields.setTribunalRequestRespondedReply(requestDetails.getRequestReply().getReplyMessage());
        sscsCaseData.setCommunicationFields(ftaCommunicationFields);
    }

    private void handleReviewTribunalReplyError(PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse,
                                            FtaCommunicationFields ftaCommunicationFields) {
        if (ftaCommunicationFields.getTribunalRequestRespondedDl() == null
            || ftaCommunicationFields.getTribunalRequestRespondedDl().getListItems().isEmpty()) {
            preSubmitCallbackResponse.addError("There are no replies to review. Please select a different communication type.");
        }
    }

    private DynamicListItem getDlItemFromCommunicationRequest(CommunicationRequest communicationRequest) {
        return new DynamicListItem(communicationRequest.getId(),
            communicationRequest.getValue().getRequestTopic().getValue() + " - "
                + communicationRequest.getValue().getRequestDateTime()
                .format(DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm")) + " - "
                + communicationRequest.getValue().getRequestUserName());
    }

    private void setFtaRequestRepliesDynamicList(FtaCommunicationFields ftaCommunicationFields, SscsCaseData sscsCaseData) {
        List<DynamicListItem> dynamicListItems = getRepliesWithoutReviews(ftaCommunicationFields.getTribunalCommunications())
            .stream()
            .map((this::getDlItemFromCommunicationRequest))
            .toList();
        ftaCommunicationFields.setTribunalRequestRespondedDl(new DynamicList(null, dynamicListItems));
        sscsCaseData.setCommunicationFields(ftaCommunicationFields);
    }
}
