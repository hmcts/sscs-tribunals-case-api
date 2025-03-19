package uk.gov.hmcts.reform.sscs.ccd.presubmit.confirmpanelcomposition;

import static java.util.Objects.requireNonNull;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;


@Service
@Slf4j
public class ConfirmPanelCompositionAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {


    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.CONFIRM_PANEL_COMPOSITION;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {

        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        SscsCaseData sscsCaseData = caseDetails.getCaseData();

        FtaCommunicationFields ftaCommunicationFields = sscsCaseData.getFtaCommunicationFields();
        List<FtaCommunication> ftaComs = ftaCommunicationFields.getFtaCommunications();

        String topic = sscsCaseData.getFtaCommunicationFields().getFtaRequestTopic();
        String question = sscsCaseData.getFtaCommunicationFields().getFtaRequestQuestion();
        LocalDateTime now = LocalDateTime.now();
        ftaComs.add(FtaCommunication.builder()
            .requestText(question)
            .requestTopic(topic)
            .requestDateTime(now)
            .requestUserName("TBC")
            .requestDueDate(now.plusDays(2))
            .build());
        
        ftaComs.sort(Comparator.comparing(FtaCommunication::getRequestDateTime).reversed());

        //sscsCaseData.setFtaCommunicationFields(ftaCommunicationFields);

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);

        processInterloc(sscsCaseData);
        return response;
    }

    private void processInterloc(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getIsFqpmRequired() != null && sscsCaseData.getInterlocReviewState() != null
                && sscsCaseData.getInterlocReviewState().equals(InterlocReviewState.REVIEW_BY_JUDGE)) {
            sscsCaseData.setInterlocReferralReason(null);
            sscsCaseData.setInterlocReviewState(null);
        }
    }
}
