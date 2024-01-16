package uk.gov.hmcts.reform.sscs.ccd.presubmit.updatewelshpreference;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.AWAITING_ADMIN_ACTION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.WELSH_TRANSLATION;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.AddNoteService;

@Service
@Slf4j
public class UpdateWelshPreferenceAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    public AddNoteService addNoteService;

    @Autowired
    public UpdateWelshPreferenceAboutToSubmitHandler(AddNoteService addNoteService) {
        this.addNoteService = addNoteService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent().equals(EventType.UPDATE_WELSH_PREFERENCE);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        if (!caseData.isLanguagePreferenceWelsh()) {
            if (WELSH_TRANSLATION == caseData.getInterlocReviewState()) {
                caseData.setInterlocReviewState(AWAITING_ADMIN_ACTION);
                String note = "Assigned to admin - Case no longer Welsh. Please cancel any Welsh translations";
                addNoteService.addNote(userAuthorisation, caseData, note);
            }
            caseData.setTranslationWorkOutstanding("No");
        }
        return new PreSubmitCallbackResponse<>(caseData);
    }
}
