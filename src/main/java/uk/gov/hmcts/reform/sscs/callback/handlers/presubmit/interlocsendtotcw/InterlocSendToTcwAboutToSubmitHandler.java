package uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.interlocsendtotcw;

import static java.util.Objects.requireNonNull;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.callback.handlers.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.AddNoteService;

@Service
@Slf4j
public class InterlocSendToTcwAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final AddNoteService addNoteService;

    public InterlocSendToTcwAboutToSubmitHandler(AddNoteService addNoteService) {
        this.addNoteService = addNoteService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && (callback.getEvent() == EventType.INTERLOC_SEND_TO_TCW);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        log.info("Handling {} event for case id {}", callback.getEvent(), callback.getCaseDetails().getId());

        caseData.setDirectionDueDate(null);
        addNoteService.addNote(userAuthorisation, caseData, caseData.getTempNoteDetail());

        return new PreSubmitCallbackResponse<>(caseData);
    }
}
