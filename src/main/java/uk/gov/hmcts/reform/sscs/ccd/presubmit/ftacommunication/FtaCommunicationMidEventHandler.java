package uk.gov.hmcts.reform.sscs.ccd.presubmit.ftacommunication;

import static java.util.Objects.requireNonNull;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CommunicationRequest;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.FtaCommunicationFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.FtaRequestType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;


@Service
@Slf4j
public class FtaCommunicationMidEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final boolean isFtaCommunicationEnabled;

    @Autowired
    public FtaCommunicationMidEventHandler(@Value("${feature.fta-communication.enabled}") boolean isFtaCommunicationEnabled) {
        this.isFtaCommunicationEnabled = isFtaCommunicationEnabled;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.MID_EVENT)
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
        PreSubmitCallbackResponse<SscsCaseData> preSubmitErrorCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
        if (!isFtaCommunicationEnabled) {
            return preSubmitErrorCallbackResponse;
        }

        FtaCommunicationFields ftaCommunicationFields = Optional.ofNullable(sscsCaseData.getCommunicationFields())
            .orElse(FtaCommunicationFields.builder().build());

        if (callback.getPageId().equals("selectFtaCommunicationAction")) {
            if (ftaCommunicationFields.getFtaRequestType().equals(FtaRequestType.REPLY_TO_FTA_QUERY)) {
                setFtaCommunicationsDynamicList(ftaCommunicationFields, sscsCaseData);
                handleReplyToFtaQueryError(preSubmitErrorCallbackResponse, ftaCommunicationFields);
            }
        } else if (callback.getPageId().equals("selectFtaRequest")) {
            setQueryForReply(sscsCaseData, ftaCommunicationFields);
        } else if (callback.getPageId().equals("replyToFtaQuery")) {
            String textValue = ftaCommunicationFields.getFtaRequestNoResponseTextArea();
            List<String> noAction = ftaCommunicationFields.getFtaRequestNoResponseNoAction();
            if (StringUtils.isEmpty(textValue) && ObjectUtils.isEmpty(noAction)) {
                preSubmitErrorCallbackResponse.addError("Please provide a response to the FTA query or select No action required.");
            }
        }

        if (!preSubmitErrorCallbackResponse.getErrors().isEmpty()) {
            return preSubmitErrorCallbackResponse;
        }

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private void setQueryForReply(SscsCaseData sscsCaseData, FtaCommunicationFields ftaCommunicationFields) {
        DynamicList ftaRequestDl = ftaCommunicationFields.getFtaRequestNoResponseRadioDl();
        DynamicListItem chosenFtaRequest = Optional.ofNullable(ftaRequestDl.getValue())
            .orElseThrow(() -> new IllegalStateException("No chosen FTA request found"));
        String chosenFtaRequestId = chosenFtaRequest.getCode();
        CommunicationRequest communicationRequest = ftaCommunicationFields.getFtaCommunications()
            .stream()
            .filter(request -> request.getId().equals(chosenFtaRequestId))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No communication request found with id: " + chosenFtaRequestId));
        ftaCommunicationFields.setFtaRequestNoResponseQuery(communicationRequest.getValue().getRequestMessage());
        sscsCaseData.setCommunicationFields(ftaCommunicationFields);
    }

    private void handleReplyToFtaQueryError(PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse,
                                            FtaCommunicationFields ftaCommunicationFields) {
        if (ftaCommunicationFields.getFtaRequestNoResponseRadioDl() == null
            || ftaCommunicationFields.getFtaRequestNoResponseRadioDl().getListItems().isEmpty()) {
            preSubmitCallbackResponse.addError("There are no requests to reply to. Please select a different communication type.");
        }
    }

    private DynamicListItem getDlItemFromCommunicationRequest(CommunicationRequest communicationRequest) {
        return new DynamicListItem(communicationRequest.getId(),
            communicationRequest.getValue().getRequestTopic().getValue() + " - "
                + communicationRequest.getValue().getRequestDateTime()
                .format(DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm")) + " - "
                + communicationRequest.getValue().getRequestUserName());
    }

    private void setFtaCommunicationsDynamicList(FtaCommunicationFields ftaCommunicationFields, SscsCaseData sscsCaseData) {
        List<CommunicationRequest> ftaCommunicationRequests = Optional.ofNullable(ftaCommunicationFields.getFtaCommunications())
            .orElse(Collections.emptyList());
        List<DynamicListItem> dynamicListItems = ftaCommunicationRequests.stream()
            .filter((communicationRequest -> communicationRequest.getValue().getRequestReply() == null))
            .map((this::getDlItemFromCommunicationRequest))
            .collect(Collectors.toList());
        ftaCommunicationFields.setFtaRequestNoResponseRadioDl(new DynamicList(null, dynamicListItems));
        sscsCaseData.setCommunicationFields(ftaCommunicationFields);
    }
}
