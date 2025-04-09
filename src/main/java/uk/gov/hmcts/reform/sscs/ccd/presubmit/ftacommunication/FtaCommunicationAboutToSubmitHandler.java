package uk.gov.hmcts.reform.sscs.ccd.presubmit.ftacommunication;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.calculateDueDateWorkingDays;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getCommunicationRequestFromId;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getOldestResponseDate;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getOldestResponseProvidedDate;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getRepliesWithoutReviews;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getRequestsWithoutReplies;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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

        if (ftaCommunicationFields.getFtaRequestType() == FtaRequestType.NEW_REQUEST) {
            CommunicationRequestTopic topic = ftaCommunicationFields.getFtaRequestTopic();
            String question = ftaCommunicationFields.getFtaRequestQuestion();
            List<CommunicationRequest> ftaComms = Optional.ofNullable(ftaCommunicationFields.getFtaCommunications())
                .orElse(new ArrayList<>());

            final UserDetails userDetails = idamService.getUserDetails(userAuthorisation);
            addCommunicationRequest(ftaComms, topic, question, userDetails);
            setFieldsForNewRequest(sscsCaseData, ftaCommunicationFields, ftaComms);
        } else if (ftaCommunicationFields.getFtaRequestType() == FtaRequestType.REPLY_TO_FTA_QUERY) {
            handleReplyToFtaQuery(ftaCommunicationFields, userAuthorisation, sscsCaseData);
        }
        clearFields(sscsCaseData, ftaCommunicationFields);
        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    public static void addCommunicationRequest(List<CommunicationRequest> comms, CommunicationRequestTopic topic, String question, UserDetails userDetails) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate dueDate = calculateDueDateWorkingDays(now.toLocalDate(), 2);
        comms.add(CommunicationRequest.builder()
            .value(CommunicationRequestDetails.builder()
                .requestMessage(question)
                .requestTopic(topic)
                .requestDateTime(now)
                .requestUserName(userDetails.getName())
                .requestResponseDueDate(dueDate)
                .build())
            .build());
        comms.sort(Comparator.comparing(communicationRequest ->
            ((CommunicationRequest) communicationRequest).getValue().getRequestDateTime()).reversed());
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
        if (requestsWithoutReplies.isEmpty()) {
            ftaCommunicationFields.setTribunalResponseDueDate(null);
        } else {
            ftaCommunicationFields.setTribunalResponseDueDate(getOldestResponseDate(requestsWithoutReplies));
        }
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
