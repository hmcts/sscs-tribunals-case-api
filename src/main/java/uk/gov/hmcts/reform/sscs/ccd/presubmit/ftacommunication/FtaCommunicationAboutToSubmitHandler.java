package uk.gov.hmcts.reform.sscs.ccd.presubmit.ftacommunication;

import static java.util.Objects.requireNonNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
public class FtaCommunicationAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    private IdamService idamService;

    @Value("${feature.fta-communication.enabled}")
    private final boolean isFtaCommuncationEnabled;

    @Autowired
    public FtaCommunicationAboutToSubmitHandler(IdamService idamService,boolean isFtaCommuncationEnabled) {
        this.isFtaCommuncationEnabled = isFtaCommuncationEnabled;
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

        if (!isFtaCommuncationEnabled) {
            return new PreSubmitCallbackResponse<>(sscsCaseData);
        }

        FtaCommunicationFields ftaCommunicationFields = Optional.ofNullable(sscsCaseData.getFtaCommunicationFields())
            .orElse(FtaCommunicationFields.builder().build());

        if (ftaCommunicationFields.getFtaRequestType() == FtaRequestType.NEW_REQUEST)  {  
            String topic = ftaCommunicationFields.getFtaRequestTopic();
            String question = ftaCommunicationFields.getFtaRequestQuestion();
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime dueDate = calculateDueDate(now);
            final UserDetails userDetails = idamService.getUserDetails(userAuthorisation);

            List<FtaCommunication> ftaComs = Optional.ofNullable(ftaCommunicationFields.getFtaCommunications())
                .orElse(new ArrayList<>());
            
            ftaComs.add(FtaCommunication.builder()
                .value(FtaCommunicationDetails.builder()
                        .requestText(question)
                        .requestTopic(topic)
                        .requestDateTime(now)
                        .requestUserName(userDetails.getName())
                        .requestDueDate(dueDate)
                    .build())
                .build());

            ftaComs.sort(Comparator.comparing(ftaCom ->
                ((FtaCommunication) ftaCom).getValue().getRequestDateTime()).reversed());

            ftaCommunicationFields.setFtaCommunications(ftaComs);
            ftaCommunicationFields.setFtaCommunicationFilter(FtaCommunicationFilter.AWAITING_INFO_FROM_FTA);
            ftaCommunicationFields.setTribunalCommunicationFilter(TribunalCommunicationFilter.NEW_FTA_FILTER);
            sscsCaseData.setFtaCommunicationFields(ftaCommunicationFields);
        }

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    public LocalDateTime calculateDueDate(LocalDateTime now) {
        //2 working days from now
        LocalDateTime dueDate = now.plusDays(2);
        if (dueDate.getDayOfWeek().getValue() == 6) {
            dueDate = dueDate.plusDays(2);
        } else if (dueDate.getDayOfWeek().getValue() == 7) {
            dueDate = dueDate.plusDays(1);
        }
        return dueDate;
    }

}
