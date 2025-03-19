package uk.gov.hmcts.reform.sscs.ccd.presubmit.ftacommunication;

import static java.util.Objects.requireNonNull;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

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
public class FtaCommunicationAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    private IdamService idamService;

    @Autowired
    public FtaCommunicationAboutToSubmitHandler(IdamService idamService) {
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

        List<FtaCommunication> ftaComs = sscsCaseData.getFtaCommunicationFields().getFtaCommunications();

        String topic = sscsCaseData.getFtaCommunicationFields().getFtaRequestTopic();
        String question = sscsCaseData.getFtaCommunicationFields().getFtaRequestQuestion();
        LocalDateTime now = LocalDateTime.now();
        final UserDetails userDetails = idamService.getUserDetails(userAuthorisation);
        ftaComs.add(FtaCommunication.builder()
            .requestText(question)
            .requestTopic(topic)
            .requestDateTime(now)
            .requestUserName(userDetails.getName())
            .requestDueDate(now.plusDays(2))
            .build());
        
        ftaComs.sort(Comparator.comparing(FtaCommunication::getRequestDateTime).reversed());

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

}
