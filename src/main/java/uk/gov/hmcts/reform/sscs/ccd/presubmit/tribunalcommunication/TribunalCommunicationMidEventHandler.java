package uk.gov.hmcts.reform.sscs.ccd.presubmit.tribunalcommunication;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isNoOrNull;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestUtil.getCommunicationRequestFromId;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestUtil.getRepliesWithoutReviews;

import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.FtaCommunicationFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.TribunalRequestType;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.util.CommunicationRequestUtil;
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

        FtaCommunicationFields communicationFields = Optional.ofNullable(sscsCaseData.getCommunicationFields())
            .orElse(FtaCommunicationFields.builder().build());

        if (callback.getPageId().equals("selectTribunalCommunicationAction")) {
            if (communicationFields.getTribunalRequestType().equals(TribunalRequestType.REVIEW_TRIBUNAL_REPLY)) {
                setFtaRequestRepliesDynamicList(preSubmitErrorCallbackResponse, communicationFields, sscsCaseData);
            } else 
            if (communicationFields.getTribunalRequestType().equals(TribunalRequestType.REPLY_TO_TRIBUNAL_QUERY)) {
                setTribunalCommunicationsDynamicList(communicationFields, sscsCaseData);
                handleReplyToTribunalQueryError(preSubmitErrorCallbackResponse, communicationFields);
            }
        } else if (callback.getPageId().equals("selectTribunalReply")) {
            setQueryReplyForReview(sscsCaseData, communicationFields);
        } else if (callback.getPageId().equals("reviewTribunalReply")) {
            YesNo actioned = communicationFields.getTribunalRequestRespondedActioned();
            if (isNoOrNull(actioned)) {
                preSubmitErrorCallbackResponse.addError("Please only select Yes if all actions to the response have been completed.");
            }
        } else if (callback.getPageId().equals("selectTribunalRequest")) {
            setQueryForReply(sscsCaseData, communicationFields);
        } else if (callback.getPageId().equals("replyToTribunalQuery")) {
            String textValue = communicationFields.getTribunalRequestNoResponseTextArea();
            List<String> noAction = communicationFields.getTribunalRequestNoResponseNoAction();
            if (StringUtils.isEmpty(textValue) && ObjectUtils.isEmpty(noAction)) {
                preSubmitErrorCallbackResponse.addError("Please provide a response to the Tribunal query or select No action required.");
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

    private void setFtaRequestRepliesDynamicList(PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse, FtaCommunicationFields ftaCommunicationFields, SscsCaseData sscsCaseData) {
        List<DynamicListItem> dynamicListItems = getRepliesWithoutReviews(ftaCommunicationFields.getTribunalCommunications())
            .stream()
            .map((CommunicationRequestUtil::getDlItemFromCommunicationRequest))
            .toList();
        if (dynamicListItems.isEmpty()) {
            preSubmitCallbackResponse.addError("There are no replies to review. Please select a different communication type.");
            return;
        }
        ftaCommunicationFields.setTribunalRequestRespondedDl(new DynamicList(null, dynamicListItems));
        sscsCaseData.setCommunicationFields(ftaCommunicationFields);
    }

    private void setQueryForReply(SscsCaseData sscsCaseData, FtaCommunicationFields tribunalCommunicationFields) {
        DynamicList tribunalRequestDl = tribunalCommunicationFields.getTribunalRequestNoResponseRadioDl();
        DynamicListItem chosenTribunalRequest = Optional.ofNullable(tribunalRequestDl.getValue())
            .orElseThrow(() -> new IllegalStateException("No chosen Tribunal request found"));
        String chosenTribunalRequestId = chosenTribunalRequest.getCode();
        CommunicationRequest communicationRequest = tribunalCommunicationFields.getFtaCommunications()
            .stream()
            .filter(request -> request.getId().equals(chosenTribunalRequestId))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No communication request found with id: " + chosenTribunalRequestId));
        tribunalCommunicationFields.setTribunalRequestNoResponseQuery(communicationRequest.getValue().getRequestMessage());
        sscsCaseData.setCommunicationFields(tribunalCommunicationFields);
    }

    private void handleReplyToTribunalQueryError(PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse,
                                            FtaCommunicationFields tribunalCommunicationFields) {
        if (tribunalCommunicationFields.getTribunalRequestNoResponseRadioDl() == null
            || tribunalCommunicationFields.getTribunalRequestNoResponseRadioDl().getListItems().isEmpty()) {
            preSubmitCallbackResponse.addError("There are no requests to reply to. Please select a different communication type.");
        }
    }

    private DynamicListItem getDlItemFromCommunicationRequest(CommunicationRequest communicationRequest) {
        return new DynamicListItem(communicationRequest.getId(),
            communicationRequest.getValue().getRequestTopic().getValue() + " - "
                + communicationRequest.getValue().getRequestDateTime()
                .format(DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm")) + " - "
                + communicationRequest.getValue().getRequestUserName());
    }

    private void setTribunalCommunicationsDynamicList(FtaCommunicationFields tribunalCommunicationFields, SscsCaseData sscsCaseData) {
        List<CommunicationRequest> ftaCommunicationRequests = Optional.ofNullable(tribunalCommunicationFields.getFtaCommunications())
            .orElse(Collections.emptyList());
        List<DynamicListItem> dynamicListItems = ftaCommunicationRequests.stream()
            .filter((communicationRequest -> communicationRequest.getValue().getRequestReply() == null))
            .map((this::getDlItemFromCommunicationRequest))
            .toList();
        tribunalCommunicationFields.setTribunalRequestNoResponseRadioDl(new DynamicList(null, dynamicListItems));
        sscsCaseData.setCommunicationFields(tribunalCommunicationFields);
    }
}
