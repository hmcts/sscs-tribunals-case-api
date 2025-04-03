package uk.gov.hmcts.reform.sscs.ccd.presubmit.ftacommunication;

import static java.util.Objects.requireNonNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CommunicationRequest;
import uk.gov.hmcts.reform.sscs.ccd.domain.CommunicationRequestDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.FtaCommunicationFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.FtaCommunicationFilter;
import uk.gov.hmcts.reform.sscs.ccd.domain.FtaRequestType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.TribunalCommunicationFilter;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;


@Service
@Slf4j
public class FtaCommunicationAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final boolean isFtaCommunicationEnabled;

    @Autowired
    public FtaCommunicationAboutToStartHandler(@Value("${feature.fta-communication.enabled}") boolean isFtaCommunicationEnabled) {
        this.isFtaCommunicationEnabled = isFtaCommunicationEnabled;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
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

        if (!isFtaCommunicationEnabled) {
            return new PreSubmitCallbackResponse<>(sscsCaseData);
        }

        FtaCommunicationFields ftaCommunicationFields = Optional.ofNullable(sscsCaseData.getCommunicationFields())
            .orElse(FtaCommunicationFields.builder().build());

        setFtaCommunicationsDynamicList(ftaCommunicationFields, sscsCaseData);

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private DynamicListItem getDlItemFromCommunicationRequest(CommunicationRequest communicationRequest) {
        return new DynamicListItem(communicationRequest.getId(),
            communicationRequest.getValue().getRequestTopic().getValue() + " - "
                + communicationRequest.getValue().getRequestDateTime()
                .format(DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm")) + " - "
                + communicationRequest.getValue().getRequestUserName());
    }

    private void setFtaCommunicationsDynamicList(FtaCommunicationFields ftaCommunicationFields, SscsCaseData sscsCaseData) {
        List<CommunicationRequest> ftaCommunicationRequests = ftaCommunicationFields.getFtaCommunications();
        List<DynamicListItem> dynamicListItems = ftaCommunicationRequests.stream()
            .filter((communicationRequest -> communicationRequest.getValue().getRequestReply() == null))
            .map((this::getDlItemFromCommunicationRequest))
            .collect(Collectors.toList());
        ftaCommunicationFields.setFtaRequestNoResponseRadioDl(new DynamicList(null, dynamicListItems));
        sscsCaseData.setCommunicationFields(ftaCommunicationFields);
    }
}
