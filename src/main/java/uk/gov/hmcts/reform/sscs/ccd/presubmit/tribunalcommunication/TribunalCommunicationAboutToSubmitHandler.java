package uk.gov.hmcts.reform.sscs.ccd.presubmit.tribunalcommunication;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.ftacommunication.FtaCommunicationAboutToSubmitHandler.addCommunicationRequest;

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

        if (ftaCommunicationFields.getTribunalRequestType() == TribunalRequestType.NEW_REQUEST) {
            CommunicationRequestTopic topic = ftaCommunicationFields.getTribunalRequestTopic();
            String question = ftaCommunicationFields.getTribunalRequestQuestion();
            List<CommunicationRequest> tribunalComms = Optional.ofNullable(ftaCommunicationFields.getTribunalCommunications())
                .orElse(new ArrayList<>());

            final UserDetails userDetails = idamService.getUserDetails(userAuthorisation);
            addCommunicationRequest(tribunalComms, topic, question, userDetails);
            setFieldsForNewRequest(sscsCaseData, ftaCommunicationFields, tribunalComms);
        }

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private void setFieldsForNewRequest(SscsCaseData sscsCaseData, FtaCommunicationFields communicationFields, List<CommunicationRequest> comms) {
        communicationFields.setTribunalCommunications(comms);
        communicationFields.setInfoRequestFromFta(YesNo.YES);
        communicationFields.setAwaitingInfoFromTribunal(YesNo.YES);
        communicationFields.setTribunalRequestTopic(null);
        communicationFields.setTribunalRequestQuestion(null);
        communicationFields.setTribunalRequestType(null);
        sscsCaseData.setCommunicationFields(communicationFields);
    }
}

