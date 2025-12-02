package uk.gov.hmcts.reform.sscs.ccd.presubmit.ftacommunication;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestUtil.addCommunicationRequest;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestUtil.getCommunicationRequestFromId;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestUtil.getRoleName;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestUtil.setCommRequestFilters;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.service.BusinessDaysCalculatorService;

@Service
@Slf4j
public class FtaCommunicationAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final IdamService idamService;
    private final BusinessDaysCalculatorService businessDaysCalculatorService;

    @Autowired
    public FtaCommunicationAboutToSubmitHandler(IdamService idamService,
                                                BusinessDaysCalculatorService businessDaysCalculatorService) {
        this.idamService = idamService;
        this.businessDaysCalculatorService = businessDaysCalculatorService;
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

        FtaCommunicationFields ftaCommunicationFields = Optional.ofNullable(sscsCaseData.getCommunicationFields())
            .orElse(FtaCommunicationFields.builder().build());
        final UserDetails userDetails = idamService.getUserDetails(userAuthorisation);

        if (FtaRequestType.NEW_REQUEST.equals(ftaCommunicationFields.getFtaRequestType())) {
            CommunicationRequestTopic topic = ftaCommunicationFields.getCommRequestTopic();
            String question = ftaCommunicationFields.getCommRequestQuestion();
            List<CommunicationRequest> ftaComms = Optional.ofNullable(ftaCommunicationFields.getFtaCommunications())
                .orElse(new ArrayList<>());

            try {
                addCommunicationRequest(businessDaysCalculatorService,
                    ftaComms, topic, question, userDetails);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            ftaCommunicationFields.setFtaCommunications(ftaComms);
            ftaCommunicationFields.setLatestFtaCommunicationId(ftaCommunicationFields.getFtaCommunications().getFirst().getId());
        } else if (FtaRequestType.REPLY_TO_FTA_QUERY.equals(ftaCommunicationFields.getFtaRequestType())) {
            handleReplyToFtaQuery(ftaCommunicationFields, userDetails);
        } else if (ftaCommunicationFields.getFtaRequestType() == FtaRequestType.REVIEW_FTA_REPLY) {
            handleReviewFtaReply(ftaCommunicationFields);
        } else if (FtaRequestType.DELETE_REQUEST_REPLY.equals(ftaCommunicationFields.getFtaRequestType())) {
            handleDeleteRequestReply(ftaCommunicationFields);
        }
        clearFields(ftaCommunicationFields);
        setCommRequestFilters(ftaCommunicationFields);
        ftaCommunicationFields.setDeleteCommRequestReadOnlyStored(ftaCommunicationFields.getDeleteCommRequestReadOnly());
        ftaCommunicationFields.setDeleteCommRequestTextAreaStored(ftaCommunicationFields.getDeleteCommRequestTextArea());
        ftaCommunicationFields.setDeleteCommRequestReadOnly(null);
        ftaCommunicationFields.setDeleteCommRequestTextArea(null);
        sscsCaseData.setCommunicationFields(ftaCommunicationFields);
        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private void handleReplyToFtaQuery(FtaCommunicationFields ftaCommunicationFields, UserDetails userDetails) {
        DynamicList ftaRequestDl = ftaCommunicationFields.getFtaRequestsDl();
        DynamicListItem chosenFtaRequest = ftaRequestDl.getValue();
        String chosenFtaRequestId = chosenFtaRequest.getCode();
        CommunicationRequest communicationRequest = getCommunicationRequestFromId(chosenFtaRequestId, ftaCommunicationFields.getTribunalCommunications());

        String replyText = ftaCommunicationFields.getCommRequestResponseTextArea();
        boolean noActionRequired = !ObjectUtils.isEmpty(ftaCommunicationFields.getCommRequestResponseNoAction());
        CommunicationRequestReply reply = CommunicationRequestReply.builder()
            .replyDateTime(ZonedDateTime.now(ZoneId.of("Europe/London")).toLocalDateTime())
            .replyUserName(userDetails.getName())
            .replyUserRole(getRoleName(userDetails))
            .replyMessage(noActionRequired ? "No reply required" : replyText)
            .replyHasBeenActionedByFta(noActionRequired ? null : YesNo.NO)
            .build();
        communicationRequest.getValue().setRequestReply(reply);
        communicationRequest.getValue().setRequestResponseDueDate(null);
    }

    private void handleReviewFtaReply(FtaCommunicationFields ftaCommunicationFields) {
        DynamicMixedChoiceList requestDl = ftaCommunicationFields.getTribunalRequestsToReviewDl();
        List<DynamicListItem> actionedRequests = Optional.ofNullable(requestDl.getValue()).orElse(Collections.emptyList());
        actionedRequests
            .forEach(request -> {
                CommunicationRequest communicationRequest = getCommunicationRequestFromId(request.getCode(), ftaCommunicationFields.getFtaCommunications());
                communicationRequest.getValue().getRequestReply().setReplyHasBeenActionedByTribunal(YesNo.YES);
            });
    }

    private void handleDeleteRequestReply(FtaCommunicationFields ftaCommunicationFields) {
        String requestIdToDelete = ftaCommunicationFields.getDeleteCommRequestRadioDl().getValue().getCode();
        ftaCommunicationFields.setFtaCommunications(
            Optional.ofNullable(ftaCommunicationFields.getFtaCommunications())
                .orElse(Collections.emptyList())
                .stream()
                .filter(request -> !request.getId().equals(requestIdToDelete))
                .toList()
        );
        ftaCommunicationFields.setTribunalCommunications(
            Optional.ofNullable(ftaCommunicationFields.getTribunalCommunications())
                .orElse(Collections.emptyList())
                .stream()
                .filter(request -> !request.getId().equals(requestIdToDelete))
                .toList()
        );
    }

    private void clearFields(FtaCommunicationFields communicationFields) {
        communicationFields.setCommRequestQuestion(null);
        communicationFields.setCommRequestTopic(null);
        communicationFields.setFtaRequestType(null);
        communicationFields.setFtaRequestNoResponseQuery(null);
        communicationFields.setCommRequestResponseTextArea(null);
        communicationFields.setFtaRequestsDl(null);
        communicationFields.setCommRequestResponseNoAction(null);
        communicationFields.setTribunalRequestsToReviewDl(null);
        communicationFields.setDeleteCommRequestRadioDl(null);
    }
}
