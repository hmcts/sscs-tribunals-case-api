package uk.gov.hmcts.reform.sscs.ccd.presubmit.ftacommunication;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isNoOrNull;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestUtil.getAllRequests;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestUtil.getCommunicationRequestFromId;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestUtil.getRepliesWithoutReviews;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestUtil.getRequestsWithoutReplies;

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
import uk.gov.hmcts.reform.sscs.ccd.domain.FtaRequestType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.util.CommunicationRequestUtil;


@Service
@Slf4j
public class FtaCommunicationMidEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final boolean isFtaCommunicationEnabled;

    @Autowired
    public FtaCommunicationMidEventHandler(@Value("${feature.fta-communication.enabled}") boolean isFtaCommunicationEnabled) {
        this.isFtaCommunicationEnabled = isFtaCommunicationEnabled;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.MID_EVENT)
            && callback.getEvent() == EventType.FTA_COMMUNICATION;
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

        if (callback.getPageId().equals("selectFtaCommunicationAction")) {
            ftaCommunicationFields.setDeleteCommRequestReadOnly(null);
            ftaCommunicationFields.setDeleteCommRequestTextArea(null);
            if (FtaRequestType.REPLY_TO_FTA_QUERY.equals(ftaCommunicationFields.getFtaRequestType())) {
                setFtaCommunicationsDynamicList(preSubmitErrorCallbackResponse, ftaCommunicationFields);
            } else if (FtaRequestType.REVIEW_FTA_REPLY.equals(ftaCommunicationFields.getFtaRequestType())) {
                setTribunalRequestRepliesDynamicList(preSubmitErrorCallbackResponse, ftaCommunicationFields);
            } else if (FtaRequestType.DELETE_REQUEST_REPLY.equals(ftaCommunicationFields.getFtaRequestType())) {
                setRequestToDeleteDynamicList(preSubmitErrorCallbackResponse, ftaCommunicationFields);
            }
        } else if (callback.getPageId().equals("selectFtaRequest")) {
            setQueryForReply(ftaCommunicationFields);
        } else if (callback.getPageId().equals("selectFtaReply")) {
            setQueryReplyForReview(ftaCommunicationFields);
        } else if (callback.getPageId().equals("replyToFtaQuery")) {
            String textValue = ftaCommunicationFields.getFtaRequestNoResponseTextArea();
            List<String> noAction = ftaCommunicationFields.getFtaRequestNoResponseNoAction();
            if (StringUtils.isEmpty(textValue) && ObjectUtils.isEmpty(noAction)) {
                preSubmitErrorCallbackResponse.addError("Please provide a response to the FTA query or select No action required.");
            }
        } else if (callback.getPageId().equals("reviewFtaReply")) {
            YesNo actioned = ftaCommunicationFields.getFtaResponseActioned();
            if (isNoOrNull(actioned)) {
                preSubmitErrorCallbackResponse.addError("Please only select Yes if all actions to the response have been completed.");
            }
        } else if (callback.getPageId().equals("selectRequestToDelete")) {
            setRequestReadOnlyForDelete(ftaCommunicationFields);
        }

        if (!preSubmitErrorCallbackResponse.getErrors().isEmpty()) {
            return preSubmitErrorCallbackResponse;
        }

        sscsCaseData.setCommunicationFields(ftaCommunicationFields);
        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private void setQueryForReply(FtaCommunicationFields ftaCommunicationFields) {
        DynamicList ftaRequestDl = ftaCommunicationFields.getFtaRequestNoResponseRadioDl();
        DynamicListItem chosenFtaRequest = Optional.ofNullable(ftaRequestDl.getValue())
            .orElseThrow(() -> new IllegalStateException("No chosen FTA request found"));
        String chosenFtaRequestId = chosenFtaRequest.getCode();
        CommunicationRequest communicationRequest = getCommunicationRequestFromId(chosenFtaRequestId, ftaCommunicationFields.getTribunalCommunications());
        ftaCommunicationFields.setFtaRequestNoResponseQuery(communicationRequest.getValue().getRequestMessage());
    }

    private void setFtaCommunicationsDynamicList(PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse, FtaCommunicationFields ftaCommunicationFields) {
        List<DynamicListItem> dynamicListItems = getRequestsWithoutReplies(ftaCommunicationFields.getTribunalCommunications())
            .stream()
            .map((CommunicationRequestUtil::getDlItemFromCommunicationRequest))
            .toList();
        if (dynamicListItems.isEmpty()) {
            preSubmitCallbackResponse.addError("There are no requests to reply to. Please select a different communication type.");
            return;
        }
        ftaCommunicationFields.setFtaRequestNoResponseRadioDl(new DynamicList(null, dynamicListItems));
    }

    private void setQueryReplyForReview(FtaCommunicationFields ftaCommunicationFields) {
        DynamicList ftaRequestDl = ftaCommunicationFields.getFtaResponseNoActionedRadioDl();
        DynamicListItem chosenFtaRequest = Optional.ofNullable(ftaRequestDl.getValue())
            .orElseThrow(() -> new IllegalStateException("No chosen request found"));
        String chosenFtaRequestId = chosenFtaRequest.getCode();
        CommunicationRequest communicationRequest = getCommunicationRequestFromId(chosenFtaRequestId, ftaCommunicationFields.getFtaCommunications());
        CommunicationRequestDetails requestDetails = communicationRequest.getValue();
        ftaCommunicationFields.setFtaResponseNoActionedQuery(requestDetails.getRequestMessage());
        ftaCommunicationFields.setFtaResponseNoActionedReply(requestDetails.getRequestReply().getReplyMessage());
    }

    private void setTribunalRequestRepliesDynamicList(PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse, FtaCommunicationFields ftaCommunicationFields) {
        List<DynamicListItem> dynamicListItems = getRepliesWithoutReviews(ftaCommunicationFields.getFtaCommunications())
            .stream()
            .map((CommunicationRequestUtil::getDlItemFromCommunicationRequest))
            .toList();
        if (dynamicListItems.isEmpty()) {
            preSubmitCallbackResponse.addError("There are no replies to review. Please select a different communication type.");
            return;
        }
        ftaCommunicationFields.setFtaResponseNoActionedRadioDl(new DynamicList(null, dynamicListItems));
    }

    private void setRequestToDeleteDynamicList(PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse, FtaCommunicationFields communicationFields) {
        List<DynamicListItem> dynamicListItems = getAllRequests(communicationFields)
            .stream()
            .map((CommunicationRequestUtil::getDlItemFromCommunicationRequest))
            .toList();
        if (dynamicListItems.isEmpty()) {
            preSubmitCallbackResponse.addError("There are no requests to delete. Please select a different communication type.");
            return;
        }
        communicationFields.setDeleteCommRequestRadioDl(new DynamicList(null, dynamicListItems));
    }

    private void setRequestReadOnlyForDelete(FtaCommunicationFields communicationFields) {
        DynamicList requestDl = communicationFields.getDeleteCommRequestRadioDl();
        DynamicListItem requestToDelete = Optional.ofNullable(requestDl.getValue())
            .orElseThrow(() -> new IllegalStateException("No chosen request found"));
        String requestToDeleteId = requestToDelete.getCode();
        CommunicationRequestDetails communicationRequest =
            getCommunicationRequestFromId(requestToDeleteId, getAllRequests(communicationFields)).getValue();
        communicationFields.setDeleteCommRequestReadOnly(communicationRequest);
        communicationFields.setDeleteCommRequestTextArea(null);
    }
}
