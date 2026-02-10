package uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.ftacommunication;

import static java.util.Objects.requireNonNull;

import feign.FeignException;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CommunicationRequestDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.FtaCommunicationFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.callback.handlers.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.AddNoteService;

@Component
@Slf4j
public class FtaCommunicationSubmittedHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final IdamService idamService;
    private final AddNoteService addNoteService;
    private final UpdateCcdCaseService updateCcdCaseService;

    @Autowired
    FtaCommunicationSubmittedHandler(IdamService idamService,
                                     AddNoteService addNoteService,
                                     UpdateCcdCaseService updateCcdCaseService) {
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
        CommunicationRequestDetails readOnly = Optional.ofNullable(sscsCaseData.getCommunicationFields())
            .map(FtaCommunicationFields::getDeleteCommRequestReadOnlyStored)
            .orElse(null);

        if (readOnly == null) {
            return new PreSubmitCallbackResponse<>(sscsCaseData);
        }

        Consumer<SscsCaseDetails> caseDataConsumer = sscsCaseDetails -> {
            SscsCaseData caseData = sscsCaseDetails.getData();
            FtaCommunicationFields communicationFields = caseData.getCommunicationFields();
            String note = "Request deleted: "
                + communicationFields.getDeleteCommRequestReadOnlyStored().toString()
                + "\nReason for deletion: \n"
                + communicationFields.getDeleteCommRequestTextAreaStored();
            addNoteService.addNote(userAuthorisation, caseData, note);
        };

        try {
            updateCcdCaseService.updateCaseV2(
                Long.valueOf(sscsCaseData.getCcdCaseId()),
                EventType.CASE_UPDATED.getCcdType(),
                "Tribunal/FTA communications updated",
                "Tribunal/FTA communication deleted",
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
