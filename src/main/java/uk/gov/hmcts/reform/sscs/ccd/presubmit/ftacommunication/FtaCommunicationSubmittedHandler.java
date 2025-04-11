package uk.gov.hmcts.reform.sscs.ccd.presubmit.ftacommunication;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_SEND_TO_INTERLOC;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import feign.FeignException;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CommunicationRequest;
import uk.gov.hmcts.reform.sscs.ccd.domain.CommunicationRequestDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.FtaCommunicationFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.AddNoteService;

@Component
@Slf4j
public class FtaCommunicationSubmittedHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private IdamService idamService;
    private AddNoteService addNoteService;
    private UpdateCcdCaseService updateCcdCaseService;
    private final boolean isFtaCommunicationEnabled;

    @Autowired
    public FtaCommunicationSubmittedHandler(IdamService idamService,
                                                AddNoteService addNoteService,
                                                UpdateCcdCaseService updateCcdCaseService,
                                                @Value("${feature.fta-communication.enabled}") boolean isFtaCommunicationEnabled) {
        this.isFtaCommunicationEnabled = isFtaCommunicationEnabled;
        this.idamService = idamService;
        this.addNoteService = addNoteService;
        this.updateCcdCaseService = updateCcdCaseService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
            && callback.getEvent() == EventType.FTA_COMMUNICATION;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        if (!isFtaCommunicationEnabled) {
            return new PreSubmitCallbackResponse<>(sscsCaseData);
        }

        Consumer<SscsCaseDetails> caseDataConsumer = sscsCaseDetails -> {
            SscsCaseData caseData = sscsCaseDetails.getData();
            FtaCommunicationFields communicationFields = caseData.getCommunicationFields();
            String note = "Request deleted: "
                + communicationFields.getDeleteCommRequestReadOnly().toString()
                + "\nReason for deletion: \n"
                + communicationFields.getDeleteCommRequestTextArea();
            addNoteService.addNote(userAuthorisation, caseData, note);
        };

        try {
            updateCcdCaseService.updateCaseV2(
                Long.valueOf(sscsCaseData.getCcdCaseId()),
                EventType.CASE_UPDATED.getCcdType(),
                "FTA communications updated",
                "FTA communication request deleted",
                idamService.getIdamTokens(),
                caseDataConsumer
            );
        } catch (FeignException e) {
            log.error(
                    "{}. CCD response: {}",
                    String.format("Could not add note from event %s for case %d", EventType.FTA_COMMUNICATION, caseDetails.getId()),
                    e.responseBody().isPresent() ? e.contentUTF8() : e.getMessage()
            );
            throw e;
        }

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }
}
