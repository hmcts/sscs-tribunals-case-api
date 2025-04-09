package uk.gov.hmcts.reform.sscs.ccd.presubmit.tribunalcommunication;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.ftacommunication.FtaCommunicationAboutToSubmitHandler.addCommunicationRequest;

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

        log.info(userAuthorisation + "*!* - TribunalCommunicationAboutToSubmitHandler handle method called");                                                    
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

        if (communicationFields.getTribunalRequestType() == TribunalRequestType.NEW_REQUEST) {
            CommunicationRequestTopic topic = communicationFields.getTribunalRequestTopic();
            String question = communicationFields.getTribunalRequestQuestion();
            List<CommunicationRequest> tribunalComms = Optional.ofNullable(communicationFields.getTribunalCommunications())
                .orElse(new ArrayList<>());

            final UserDetails userDetails = idamService.getUserDetails(userAuthorisation);
            addCommunicationRequest(tribunalComms, topic, question, userDetails);
            setFieldsForNewRequest(sscsCaseData, communicationFields, tribunalComms);
        }
        else if (communicationFields.getTribunalRequestType() == TribunalRequestType.REPLY_TO_TRIBUNAL_QUERY) {
            log.info(userAuthorisation + "*!* - TribunalCommunicationAboutToSubmitHandler handle method called for reply to tribunal query");
            handleReplyToTribunalQuery(communicationFields, userAuthorisation, sscsCaseData);
        }

        clearFields(sscsCaseData, communicationFields);

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private void handleReplyToTribunalQuery(FtaCommunicationFields communicationFields, String userAuthorisation, SscsCaseData sscsCaseData) {
        DynamicList requestDl = communicationFields.getTribunalRequestNoResponseRadioDl();
        DynamicListItem chosenRequest = requestDl.getValue();
        String chosenTribunalRequestId = chosenRequest.getCode();
        CommunicationRequest communicationRequest = Optional.ofNullable(communicationFields.getTribunalCommunications())
            .orElse(Collections.emptyList())
            .stream()
            .filter(communicationRequest1 -> communicationRequest1.getId().equals(chosenTribunalRequestId))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No communication request found with id: " + chosenTribunalRequestId));
        String replyText = communicationFields.getTribunalRequestNoResponseTextArea();
        boolean noActionRequired = !ObjectUtils.isEmpty(communicationFields.getTribunalRequestNoResponseNoAction());
        CommunicationRequestReply reply = CommunicationRequestReply.builder()
            .replyDateTime(LocalDateTime.now())
            .replyUserName(idamService.getUserDetails(userAuthorisation).getName())
            .replyMessage(noActionRequired ? "No action required" : replyText)
            .build();
        communicationRequest.getValue().setRequestReply(reply);
        communicationRequest.getValue().setRequestResponseDueDate(null);
        communicationFields.setTribunalCommunicationFilter(noActionRequired ? null : TribunalCommunicationFilter.INFO_PROVIDED_BY_FTA);
        communicationFields.setFtaCommunicationFilter(null);
        sscsCaseData.setCommunicationFields(communicationFields);
    }

    private void setFieldsForNewRequest(SscsCaseData sscsCaseData, FtaCommunicationFields communicationFields, List<CommunicationRequest> comms) {
        communicationFields.setTribunalCommunications(comms);
        communicationFields.setTribunalCommunicationFilter(TribunalCommunicationFilter.INFO_REQUEST_FROM_FTA);
        communicationFields.setFtaCommunicationFilter(FtaCommunicationFilter.AWAITING_INFO_FROM_TRIBUNAL);
        communicationFields.setTribunalRequestTopic(null);
        communicationFields.setTribunalRequestQuestion(null);
        communicationFields.setTribunalRequestType(null);
        sscsCaseData.setCommunicationFields(communicationFields);
    }

    private void clearFields(SscsCaseData sscsCaseData, FtaCommunicationFields communicationFields) {
        communicationFields.setTribunalRequestQuestion(null);
        communicationFields.setTribunalRequestTopic(null);
        communicationFields.setTribunalRequestType(null);
        communicationFields.setTribunalRequestNoResponseQuery(null);
        communicationFields.setTribunalRequestNoResponseTextArea(null);
        communicationFields.setTribunalRequestNoResponseRadioDl(null);
        communicationFields.setTribunalRequestNoResponseNoAction(null);
        sscsCaseData.setCommunicationFields(communicationFields);
    }
}

