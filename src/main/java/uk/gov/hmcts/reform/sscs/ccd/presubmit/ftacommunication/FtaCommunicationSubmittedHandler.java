package uk.gov.hmcts.reform.sscs.ccd.presubmit.ftacommunication;

import static java.util.Objects.requireNonNull;

import feign.FeignException;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CommunicationRequestDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.FtaCommunicationFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.AddNoteService;

@Component
@Slf4j
public class FtaCommunicationSubmittedHandler implements CallbackHandler<SscsCaseData> {

    private IdamService idamService;
    private AddNoteService addNoteService;
    private UpdateCcdCaseService updateCcdCaseService;
    private final boolean isFtaCommunicationEnabled;

    @Autowired
    FtaCommunicationSubmittedHandler(IdamService idamService,
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
    public void handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();
        CommunicationRequestDetails readOnly = Optional.ofNullable(sscsCaseData.getCommunicationFields())
            .map(FtaCommunicationFields::getDeleteCommRequestReadOnly)
            .orElse(null);

        if (!isFtaCommunicationEnabled || readOnly == null) {
            return;
        }

        Consumer<SscsCaseDetails> caseDataConsumer = sscsCaseDetails -> {
            SscsCaseData caseData = sscsCaseDetails.getData();
            FtaCommunicationFields communicationFields = caseData.getCommunicationFields();
            String note = "Request deleted: "
                + communicationFields.getDeleteCommRequestReadOnly().toString()
                + "\nReason for deletion: \n"
                + communicationFields.getDeleteCommRequestTextArea();
            addNoteService.addNote(idamService.getIdamOauth2Token(), caseData, note);
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
    }

    @Override
    public DispatchPriority getPriority() {
        return DispatchPriority.LATEST;
    }
}
