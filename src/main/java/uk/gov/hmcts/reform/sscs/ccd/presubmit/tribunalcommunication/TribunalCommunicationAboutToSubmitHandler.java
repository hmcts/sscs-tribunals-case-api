package uk.gov.hmcts.reform.sscs.ccd.presubmit.tribunalcommunication;

import static java.util.Objects.requireNonNull;

import java.time.LocalDateTime;
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
    private boolean isFtaCommunicationEnabled;

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

        FtaCommunicationFields ftaCommunicationFields = Optional.ofNullable(sscsCaseData.getFtaCommunicationFields()).orElse(FtaCommunicationFields.builder().build());
        if (ftaCommunicationFields.getTribunalRequestType() == TribunalRequestType.NEW_REQUEST) {
            String topic = ftaCommunicationFields.getTribunalRequestTopic();
            String question = ftaCommunicationFields.getTribunalRequestQuestion();
            LocalDateTime dueDate = dueDate(LocalDateTime.now());
            final UserDetails userDetails = idamService.getUserDetails(userAuthorisation);

            List<CommunicationRequest> tribunalComms = Optional.ofNullable(ftaCommunicationFields.getTribunalCommunications()).orElse(new ArrayList<>());
            tribunalComms.add(CommunicationRequest.builder()
                    .value(CommunicationRequestDetails.builder()
                            .requestMessage(question)
                            .requestTopic(topic)
                            .requestDateTime(LocalDateTime.now())
                            .requestUserName(userDetails.getName())
                            .requestResponseDue(dueDate)
                            .build())
                    .build());
            if (ftaCommunicationFields.getTribunalCommunications() != null) {
                tribunalComms.addAll(ftaCommunicationFields.getTribunalCommunications());
            }
            ftaCommunicationFields.setTribunalCommunications(tribunalComms);
            ftaCommunicationFields.setTribunalCommunicationFilter(TribunalCommunicationFilter.INFO_REQUEST_FROM_FTA);
            ftaCommunicationFields.setFtaCommunicationFilter(FtaCommunicationFilter.AWAITING_INFO_FROM_TRIBUNAL);
            tribunalComms.sort(Comparator.comparing(tribunalComm -> ((TribunalCommunication) tribunalComm).getValue().getRequestDateTime()).reversed());
            ftaCommunicationFields.setTribunalCommunications(tribunalComms);
            ftaCommunicationFields.setTribunalRequestTopic(null);
            ftaCommunicationFields.setTribunalRequestQuestion(null);
            ftaCommunicationFields.setTribunalRequestType(null);
            sscsCaseData.setFtaCommunicationFields(ftaCommunicationFields);
        }
        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    public LocalDateTime dueDate(LocalDateTime now) {
        LocalDateTime replyDueDate = now.plusDays(2);
        if (replyDueDate.getDayOfWeek().getValue() == 6) {
            replyDueDate = replyDueDate.plusDays(2);
        } else if (replyDueDate.getDayOfWeek().getValue() == 7) {
            replyDueDate = replyDueDate.plusDays(2);
        }
        return replyDueDate;
    }
}