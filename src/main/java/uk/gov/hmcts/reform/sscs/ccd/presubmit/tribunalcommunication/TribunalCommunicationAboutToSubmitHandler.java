package uk.gov.hmcts.reform.sscs.ccd.presubmit.tribunalcommunication;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestUtil.addCommunicationRequest;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestUtil.getCommunicationRequestFromId;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestUtil.getOldestResponseDate;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestUtil.getOldestResponseProvidedDate;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestUtil.getRepliesWithoutReviews;

import java.util.*;
import lombok.extern.slf4j.Slf4j;
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

        FtaCommunicationFields ftaCommunicationFields = Optional.ofNullable(sscsCaseData.getCommunicationFields())
            .orElse(FtaCommunicationFields.builder().build());

        if (TribunalRequestType.NEW_REQUEST.equals(ftaCommunicationFields.getTribunalRequestType())) {
            CommunicationRequestTopic topic = ftaCommunicationFields.getTribunalRequestTopic();
            String question = ftaCommunicationFields.getTribunalRequestQuestion();
            List<CommunicationRequest> tribunalComms = Optional.ofNullable(ftaCommunicationFields.getTribunalCommunications())
                .orElse(new ArrayList<>());

            final UserDetails userDetails = idamService.getUserDetails(userAuthorisation);
            addCommunicationRequest(tribunalComms, topic, question, userDetails);
            setFieldsForNewRequest(sscsCaseData, ftaCommunicationFields, tribunalComms);
        } else if (TribunalRequestType.REVIEW_TRIBUNAL_REPLY.equals(ftaCommunicationFields.getTribunalRequestType())) {
            handleReviewTribunalReply(ftaCommunicationFields, sscsCaseData);
        }
        clearFields(sscsCaseData, ftaCommunicationFields);

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private void clearFields(SscsCaseData sscsCaseData, FtaCommunicationFields communicationFields) {
        communicationFields.setTribunalRequestTopic(null);
        communicationFields.setTribunalRequestQuestion(null);
        communicationFields.setTribunalRequestType(null);
        communicationFields.setTribunalRequestRespondedReply(null);
        communicationFields.setTribunalRequestRespondedQuery(null);
        communicationFields.setTribunalRequestRespondedDl(null);
        communicationFields.setTribunalRequestRespondedActioned(null);
        sscsCaseData.setCommunicationFields(communicationFields);
    }

    private void setFieldsForNewRequest(SscsCaseData sscsCaseData, FtaCommunicationFields communicationFields, List<CommunicationRequest> comms) {
        communicationFields.setTribunalCommunications(comms);
        communicationFields.setTribunalResponseDueDate(getOldestResponseDate(comms));
        sscsCaseData.setCommunicationFields(communicationFields);
    }

    private void handleReviewTribunalReply(FtaCommunicationFields ftaCommunicationFields, SscsCaseData sscsCaseData) {
        DynamicList requestDl = ftaCommunicationFields.getTribunalRequestRespondedDl();
        String chosenFtaRequestId = Optional.ofNullable(requestDl.getValue()).orElse(new DynamicListItem(null, null)).getCode();
        CommunicationRequest communicationRequest = getCommunicationRequestFromId(chosenFtaRequestId, ftaCommunicationFields.getTribunalCommunications());
        communicationRequest.getValue().getRequestReply().setReplyHasBeenActioned(YesNo.YES);
        List<CommunicationRequest> repliesWithoutReviews = getRepliesWithoutReviews(ftaCommunicationFields.getTribunalCommunications());

        if (repliesWithoutReviews.isEmpty()) {
            ftaCommunicationFields.setFtaResponseProvidedDate(null);
        } else {
            ftaCommunicationFields.setFtaResponseProvidedDate(getOldestResponseProvidedDate(repliesWithoutReviews));
        }
        sscsCaseData.setCommunicationFields(ftaCommunicationFields);
    }
}

