package uk.gov.hmcts.reform.sscs.ccd.presubmit.ftacommunication;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestUtil.addCommunicationRequest;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestUtil.getCommunicationRequestFromId;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestUtil.getOldestResponseDate;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestUtil.getOldestResponseProvidedDate;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestUtil.getRepliesWithoutReviews;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestUtil.getRequestsWithoutReplies;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
public class FtaCommunicationAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private IdamService idamService;
    private final boolean isFtaCommunicationEnabled;

    @Autowired
    public FtaCommunicationAboutToSubmitHandler(IdamService idamService,
                                                @Value("${feature.fta-communication.enabled}") boolean isFtaCommunicationEnabled) {
        this.isFtaCommunicationEnabled = isFtaCommunicationEnabled;
        this.idamService = idamService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
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

        if (!isFtaCommunicationEnabled) {
            return new PreSubmitCallbackResponse<>(sscsCaseData);
        }

        FtaCommunicationFields ftaCommunicationFields = Optional.ofNullable(sscsCaseData.getCommunicationFields())
            .orElse(FtaCommunicationFields.builder().build());

        if (FtaRequestType.NEW_REQUEST.equals(ftaCommunicationFields.getFtaRequestType())) {
            CommunicationRequestTopic topic = ftaCommunicationFields.getFtaRequestTopic();
            String question = ftaCommunicationFields.getFtaRequestQuestion();
            List<CommunicationRequest> ftaComms = Optional.ofNullable(ftaCommunicationFields.getFtaCommunications())
                .orElse(new ArrayList<>());

            final UserDetails userDetails = idamService.getUserDetails(userAuthorisation);
            addCommunicationRequest(ftaComms, topic, question, userDetails);
            setFieldsForNewRequest(sscsCaseData, ftaCommunicationFields, ftaComms);
        } else if (FtaRequestType.REPLY_TO_FTA_QUERY.equals(ftaCommunicationFields.getFtaRequestType())) {
            handleReplyToFtaQuery(ftaCommunicationFields, userAuthorisation, sscsCaseData);
        }
        clearFields(sscsCaseData, ftaCommunicationFields);
        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private void setFieldsForNewRequest(SscsCaseData sscsCaseData, FtaCommunicationFields communicationFields, List<CommunicationRequest> comms) {
        communicationFields.setFtaCommunications(comms);
        communicationFields.setFtaResponseDueDate(getOldestResponseDate(comms));
        sscsCaseData.setCommunicationFields(communicationFields);
    }

    private void handleReplyToFtaQuery(FtaCommunicationFields ftaCommunicationFields, String userAuthorisation, SscsCaseData sscsCaseData) {
        DynamicList ftaRequestDl = ftaCommunicationFields.getFtaRequestNoResponseRadioDl();
        DynamicListItem chosenFtaRequest = ftaRequestDl.getValue();
        String chosenFtaRequestId = chosenFtaRequest.getCode();
        CommunicationRequest communicationRequest = getCommunicationRequestFromId(chosenFtaRequestId, ftaCommunicationFields.getTribunalCommunications());

        String replyText = ftaCommunicationFields.getFtaRequestNoResponseTextArea();
        boolean noActionRequired = !ObjectUtils.isEmpty(ftaCommunicationFields.getFtaRequestNoResponseNoAction());
        CommunicationRequestReply reply = CommunicationRequestReply.builder()
            .replyDateTime(LocalDateTime.now())
            .replyUserName(idamService.getUserDetails(userAuthorisation).getName())
            .replyMessage(noActionRequired ? "No action required" : replyText)
            .replyHasBeenActioned(noActionRequired ? null : YesNo.NO)
            .build();
        communicationRequest.getValue().setRequestReply(reply);
        communicationRequest.getValue().setRequestResponseDueDate(null);

        List<CommunicationRequest> requestsWithoutReplies = getRequestsWithoutReplies(ftaCommunicationFields.getTribunalCommunications());
        ftaCommunicationFields.setTribunalResponseDueDate(getOldestResponseDate(requestsWithoutReplies));

        List<CommunicationRequest> repliesWithoutReviews = getRepliesWithoutReviews(ftaCommunicationFields.getTribunalCommunications());
        ftaCommunicationFields.setFtaResponseProvidedDate(getOldestResponseProvidedDate(repliesWithoutReviews));
        sscsCaseData.setCommunicationFields(ftaCommunicationFields);
    }

    private void clearFields(SscsCaseData sscsCaseData, FtaCommunicationFields communicationFields) {
        communicationFields.setFtaRequestQuestion(null);
        communicationFields.setFtaRequestTopic(null);
        communicationFields.setFtaRequestType(null);
        communicationFields.setFtaRequestNoResponseQuery(null);
        communicationFields.setFtaRequestNoResponseTextArea(null);
        communicationFields.setFtaRequestNoResponseRadioDl(null);
        communicationFields.setFtaRequestNoResponseNoAction(null);
        sscsCaseData.setCommunicationFields(communicationFields);
    }
}
