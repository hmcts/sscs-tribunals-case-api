package uk.gov.hmcts.reform.sscs.ccd.presubmit.overdueftaresponse;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestUtil.getCommunicationRequestFromId;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestUtil.getRequestsWithoutReplies;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CommunicationRequest;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.FtaCommunicationFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.utility.calendar.BusinessDaysCalculatorService;


@Service
@Slf4j
@RequiredArgsConstructor
public class OverdueFtaResponseAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final BusinessDaysCalculatorService businessDaysCalculatorService;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.OVERDUE_FTA_RESPONSE;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        SscsCaseData sscsCaseData = caseDetails.getCaseData();
        String caseId = sscsCaseData.getCcdCaseId();

        FtaCommunicationFields communicationFields = Optional.ofNullable(sscsCaseData.getCommunicationFields())
                .orElse(FtaCommunicationFields.builder().build());

        List<CommunicationRequest> communicationRequestList = communicationFields.getFtaCommunications();
        log.info("Communication request List size is: {} for case id: {}", communicationRequestList.size(), caseId);

        CommunicationRequest overdueCommunicationRequest =  getRequestsWithoutReplies(communicationRequestList).stream()
                     .filter(request -> request.getValue().getTaskCreatedForRequest() != YesNo.YES
                            && isDateOverdue(request, caseId)).findFirst().orElse(null);

        if (overdueCommunicationRequest != null) {
            log.info("Found overdue communication request with id: {} for case id: {}", overdueCommunicationRequest.getId(), caseId);
            sscsCaseData.getCommunicationFields().setWaTaskFtaCommunicationId(overdueCommunicationRequest.getId());
            getCommunicationRequestFromId(overdueCommunicationRequest.getId(), communicationFields.getFtaCommunications()).getValue().setTaskCreatedForRequest(YesNo.YES);
        }
        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    public boolean isDateOverdue(CommunicationRequest communicationRequest, String caseId) {
        try {
            LocalDate overdueDate = businessDaysCalculatorService
                    .getBusinessDayInPast(LocalDate.now(), 2);
            log.info("Overdue date for case id: {} calculated as: {} for communication request id: {}", caseId,
                    overdueDate, communicationRequest.getId());
            return nonNull(communicationRequest.getValue().getRequestDateTime())
                    && nonNull(overdueDate)
                    && (communicationRequest.getValue().getRequestDateTime().toLocalDate().isEqual(overdueDate)
                         || communicationRequest.getValue().getRequestDateTime().toLocalDate().isBefore(overdueDate));
        } catch (IOException e) {
            log.error("Error calculating overdue date for case id: {}. Falling back to default date check.", caseId, e);
            return communicationRequest.getValue().getRequestDateTime() != null
                    && communicationRequest.getValue().getRequestDateTime().toLocalDate().isEqual(LocalDate.now().minusDays(2));
        }
    }
}
