package uk.gov.hmcts.reform.sscs.ccd.presubmit.tribunalcommunication;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestUtil.addCommunicationRequest;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestUtil.getCommunicationRequestFromId;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestUtil.getRoleName;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestUtil.setCommRequestFilters;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
import uk.gov.hmcts.reform.sscs.utility.calendar.BusinessDaysCalculatorService;

@Service
@Slf4j
public class TribunalCommunicationAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final IdamService idamService;
    private final BusinessDaysCalculatorService businessDaysCalculatorService;
    private final boolean isWorkAllocationEnabled;

    @Autowired
    public TribunalCommunicationAboutToSubmitHandler(IdamService idamService,
                                                     BusinessDaysCalculatorService businessDaysCalculatorService,
                                                     @Value("${feature.work-allocation.enabled}") boolean isWorkAllocationEnabled
    ) {
        this.idamService = idamService;
        this.businessDaysCalculatorService = businessDaysCalculatorService;
        this.isWorkAllocationEnabled = isWorkAllocationEnabled;
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

        FtaCommunicationFields communicationFields = Optional.ofNullable(sscsCaseData.getCommunicationFields())
            .orElse(FtaCommunicationFields.builder().build());
        final UserDetails userDetails = idamService.getUserDetails(userAuthorisation);

        if (TribunalRequestType.NEW_REQUEST.equals(communicationFields.getTribunalRequestType())) {
            CommunicationRequestTopic topic = communicationFields.getCommRequestTopic();
            String question = communicationFields.getCommRequestQuestion();
            List<CommunicationRequest> tribunalComms = Optional.ofNullable(communicationFields.getTribunalCommunications())
                .orElse(new ArrayList<>());

            try {
                addCommunicationRequest(businessDaysCalculatorService, tribunalComms, topic, question, userDetails);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            communicationFields.setTribunalCommunications(tribunalComms);
        } else if (TribunalRequestType.REPLY_TO_TRIBUNAL_QUERY.equals(communicationFields.getTribunalRequestType())) {
            handleReplyToTribunalQuery(communicationFields, userDetails);
        } else if (TribunalRequestType.REVIEW_TRIBUNAL_REPLY.equals(communicationFields.getTribunalRequestType())) {
            handleReviewTribunalReply(communicationFields);
        }

        clearFields(communicationFields);
        setCommRequestFilters(communicationFields);
        sscsCaseData.setCommunicationFields(communicationFields);
        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private void handleReplyToTribunalQuery(FtaCommunicationFields communicationFields, UserDetails userDetails) {
        DynamicList requestDl = communicationFields.getTribunalRequestsDl();
        DynamicListItem chosenRequest = requestDl.getValue();
        String chosenTribunalRequestId = chosenRequest.getCode();
        CommunicationRequest communicationRequest = getCommunicationRequestFromId(chosenTribunalRequestId, communicationFields.getFtaCommunications());

        String replyText = communicationFields.getCommRequestResponseTextArea();
        boolean noActionRequired = !ObjectUtils.isEmpty(communicationFields.getCommRequestResponseNoAction());
        CommunicationRequestReply reply = CommunicationRequestReply.builder()
            .replyDateTime(ZonedDateTime.now(ZoneId.of("Europe/London")).toLocalDateTime())
            .replyUserName(userDetails.getName())
            .replyUserRole(getRoleName(userDetails))
            .replyMessage(noActionRequired ? "No reply required" : replyText)
            .replyHasBeenActionedByTribunal(noActionRequired ? null : YesNo.NO)
            .build();
        communicationRequest.getValue().setRequestReply(reply);
        communicationRequest.getValue().setRequestResponseDueDate(null);
        if (isWorkAllocationEnabled) {
            communicationFields.setWaTaskFtaCommunicationId(chosenTribunalRequestId);
        }
    }

    private void clearFields(FtaCommunicationFields communicationFields) {
        communicationFields.setCommRequestQuestion(null);
        communicationFields.setCommRequestTopic(null);
        communicationFields.setTribunalRequestNoResponseQuery(null);
        communicationFields.setCommRequestResponseTextArea(null);
        communicationFields.setFtaRequestsDl(null);
        communicationFields.setCommRequestResponseNoAction(null);
        communicationFields.setFtaRequestsToReviewDl(null);
        if (!isWorkAllocationEnabled) {
            communicationFields.setTribunalRequestType(null);
        }
    }

    private void handleReviewTribunalReply(FtaCommunicationFields ftaCommunicationFields) {
        DynamicMixedChoiceList requestDl = ftaCommunicationFields.getFtaRequestsToReviewDl();
        List<DynamicListItem> actionedRequests = Optional.ofNullable(requestDl.getValue()).orElse(Collections.emptyList());
        actionedRequests
            .forEach(request -> {
                CommunicationRequest communicationRequest = getCommunicationRequestFromId(request.getCode(), ftaCommunicationFields.getTribunalCommunications());
                communicationRequest.getValue().getRequestReply().setReplyHasBeenActionedByFta(YesNo.YES);
            });
    }
}

