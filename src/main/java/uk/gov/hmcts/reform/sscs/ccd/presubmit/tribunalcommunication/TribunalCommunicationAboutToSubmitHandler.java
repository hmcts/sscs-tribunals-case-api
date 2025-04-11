package uk.gov.hmcts.reform.sscs.ccd.presubmit.tribunalcommunication;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestUtil.addCommunicationRequest;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestUtil.getCommunicationRequestFromId;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestUtil.getOldestResponseDate;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestUtil.getOldestResponseProvidedDate;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestUtil.getRepliesWithoutReviews;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestUtil.getRequestsWithoutReplies;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestUtil.getRoleName;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestUtil.setCommRequestDateFilters;

import java.time.LocalDateTime;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;

@Service
@Slf4j
public class TribunalCommunicationAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final IdamService idamService;
    private final boolean isFtaCommunicationEnabled;

    @Autowired
    public TribunalCommunicationAboutToSubmitHandler(IdamService idamService,
                                                     @Value("${feature.fta-communication.enabled}") boolean isFtaCommunicationEnabled) {
        this.isFtaCommunicationEnabled = isFtaCommunicationEnabled;
        this.idamService = idamService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
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

        if (!isFtaCommunicationEnabled) {
            return new PreSubmitCallbackResponse<>(sscsCaseData);
        }

        FtaCommunicationFields communicationFields = Optional.ofNullable(sscsCaseData.getCommunicationFields())
            .orElse(FtaCommunicationFields.builder().build());

        if (TribunalRequestType.NEW_REQUEST.equals(communicationFields.getTribunalRequestType())) {
            CommunicationRequestTopic topic = communicationFields.getTribunalRequestTopic();
            String question = communicationFields.getTribunalRequestQuestion();
            List<CommunicationRequest> tribunalComms = Optional.ofNullable(communicationFields.getTribunalCommunications())
                .orElse(new ArrayList<>());

            final UserDetails userDetails = idamService.getUserDetails(userAuthorisation);
            addCommunicationRequest(tribunalComms, topic, question, userDetails);
            communicationFields.setTribunalCommunications(tribunalComms);
        } else if (TribunalRequestType.REPLY_TO_TRIBUNAL_QUERY.equals(communicationFields.getTribunalRequestType())) {
            handleReplyToTribunalQuery(communicationFields, userAuthorisation);
        } else if (TribunalRequestType.REVIEW_TRIBUNAL_REPLY.equals(communicationFields.getTribunalRequestType())) {
            handleReviewTribunalReply(communicationFields);
        }

        clearFields(communicationFields);
        setCommRequestDateFilters(communicationFields);
        sscsCaseData.setCommunicationFields(communicationFields);
        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private void handleReplyToTribunalQuery(FtaCommunicationFields communicationFields, String userAuthorisation) {
        DynamicList requestDl = communicationFields.getTribunalRequestNoResponseRadioDl();
        DynamicListItem chosenRequest = requestDl.getValue();
        String chosenTribunalRequestId = chosenRequest.getCode();
        CommunicationRequest communicationRequest = getCommunicationRequestFromId(chosenTribunalRequestId, communicationFields.getFtaCommunications());

        String replyText = communicationFields.getTribunalRequestNoResponseTextArea();
        boolean noActionRequired = !ObjectUtils.isEmpty(communicationFields.getTribunalRequestNoResponseNoAction());
        UserDetails userDetails = idamService.getUserDetails(userAuthorisation);
        CommunicationRequestReply reply = CommunicationRequestReply.builder()
            .replyDateTime(LocalDateTime.now())
            .replyUserName(userDetails.getName())
            .replyUserRole(getRoleName(userDetails))
            .replyMessage(noActionRequired ? "No action required" : replyText)
            .replyHasBeenActioned(noActionRequired ? null : YesNo.NO)
            .build();
        communicationRequest.getValue().setRequestReply(reply);
        communicationRequest.getValue().setRequestResponseDueDate(null);
    }

    private void clearFields(FtaCommunicationFields communicationFields) {
        communicationFields.setTribunalRequestQuestion(null);
        communicationFields.setTribunalRequestTopic(null);
        communicationFields.setTribunalRequestType(null);
        communicationFields.setTribunalRequestNoResponseQuery(null);
        communicationFields.setTribunalRequestNoResponseTextArea(null);
        communicationFields.setTribunalRequestNoResponseRadioDl(null);
        communicationFields.setTribunalRequestNoResponseNoAction(null);
        communicationFields.setTribunalRequestRespondedReply(null);
        communicationFields.setTribunalRequestRespondedQuery(null);
        communicationFields.setTribunalRequestRespondedDl(null);
        communicationFields.setTribunalRequestRespondedActioned(null);
    }

    private void handleReviewTribunalReply(FtaCommunicationFields ftaCommunicationFields) {
        DynamicList requestDl = ftaCommunicationFields.getTribunalRequestRespondedDl();
        String chosenFtaRequestId = Optional.ofNullable(requestDl.getValue()).orElse(new DynamicListItem(null, null)).getCode();
        CommunicationRequest communicationRequest = getCommunicationRequestFromId(chosenFtaRequestId, ftaCommunicationFields.getTribunalCommunications());
        communicationRequest.getValue().getRequestReply().setReplyHasBeenActioned(YesNo.YES);
    }
}

