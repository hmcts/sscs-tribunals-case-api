package uk.gov.hmcts.reform.sscs.ccd.presubmit.tribunalcommunication;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

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
        Objects.requireNonNull(callback, "callback must not be null");
        Objects.requireNonNull(callbackType, "callbackType must not be null");

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

        List<TribunalCommunicationFields> tribunalComms = sscsCaseData.getTribunalCommunications().getTribunalCommunicationFields();

        String topic = sscsCaseData.getTribunalCommunications().getTribunalRequestTopic();
        String question = sscsCaseData.getTribunalCommunications().getTribunalRequestQuestion();
        LocalDateTime now = LocalDateTime.now();
        final UserDetails userDetails = idamService.getUserDetails(userAuthorisation);
        tribunalComms.add(TribunalCommunicationFields.builder()
                .requestMessage(question)
                .requestTopic(topic)
                .requestDateTime(now)
                .requestUserName(userDetails.getName())
                .requestResponseDue(now.plusDays(2))
                .build());

        tribunalComms.sort(Comparator.comparing(TribunalCommunicationFields::getRequestDateTime).reversed());

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }
}