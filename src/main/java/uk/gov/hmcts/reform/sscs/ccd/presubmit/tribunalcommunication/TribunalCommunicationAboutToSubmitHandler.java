package uk.gov.hmcts.reform.sscs.ccd.presubmit.tribunalcommunication;

import static java.util.Objects.requireNonNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    public TribunalCommunicationAboutToSubmitHandler(IdamService idamService) {
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

        //List<TribunalCommunicationFields> tribunalComms = sscsCaseData.getTribunalCommunications().getTribunalCommunicationFields();
        //        List<TribunalCommunicationFields> tribunalComms = Collections.emptyList();
        //        if (sscsCaseData.getTribunalCommunications() != null && sscsCaseData.getTribunalCommunications().getTribunalCommunicationFields() != null) {
        //            tribunalComms = sscsCaseData.getTribunalCommunications().getTribunalCommunicationFields();
        //        }
        TribunalCommunicationDetails tribunalCommunicationDetails = Optional.ofNullable(sscsCaseData.getTribunalCommunicationsDetails()).orElse(TribunalCommunicationDetails.builder().build());
        String topic = tribunalCommunicationDetails.getTribunalRequestTopic();
        String question = tribunalCommunicationDetails.getTribunalRequestQuestion();
        LocalDateTime dueDate = LocalDateTime.now().plusDays(2);
        final UserDetails userDetails = idamService.getUserDetails(userAuthorisation);

        List<TribunalCommunication> tribunalComms = Optional.ofNullable(tribunalCommunicationDetails.getTribunalCommunications()).orElse(new ArrayList<>());
        tribunalComms.add(TribunalCommunication.builder()
                .value(TribunalCommunicationFields.builder()
                    .requestMessage(question)
                    .requestTopic(topic)
                    .requestDateTime(LocalDateTime.now())
                    .requestUserName(userDetails.getName())
                    .requestResponseDue(dueDate)
                    .build())
                .build());
        if (tribunalCommunicationDetails.getTribunalCommunications() != null) {
            tribunalComms.addAll(tribunalCommunicationDetails.getTribunalCommunications());
        }

        tribunalComms.sort(Comparator.comparing(tribunalComm -> ((TribunalCommunication) tribunalComm).getValue().getRequestDateTime()).reversed());
        tribunalCommunicationDetails.setTribunalCommunications(tribunalComms);
        sscsCaseData.setTribunalCommunicationsDetails(tribunalCommunicationDetails);
        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }
}