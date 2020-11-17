package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence;

import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.util.concurrent.atomic.AtomicBoolean;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

class ThreadRunnable implements Runnable {

    int id;
    ActionFurtherEvidenceAboutToSubmitHandler handler;
    Callback<SscsCaseData> callback;
    String expectedCaseId;
    AtomicBoolean noOverwrite;

    public ThreadRunnable(int i, ActionFurtherEvidenceAboutToSubmitHandler handler, Callback<SscsCaseData> callback,
                          String expectedCaseId, AtomicBoolean noOverwrite) {
        this.id = i;
        this.handler = handler;
        this.callback = callback;
        this.expectedCaseId = expectedCaseId;
        this.noOverwrite = noOverwrite;
    }

    public void run() {
        System.out.println("Runnable id: " + id + " for case id " + callback.getCaseDetails().getCaseData().getCcdCaseId());
        PreSubmitCallbackResponse<SscsCaseData> response = null;

        response = handler.handle(ABOUT_TO_SUBMIT, callback, "Bearer token");

        System.out.println("response case id = " + response.getData().getCcdCaseId());

        if (!response.getData().getCcdCaseId().equals(expectedCaseId)) {
            System.out.println("Response  case id " + response.getData().getCcdCaseId() + " Doesnt Match expected " + expectedCaseId);
            noOverwrite.set(false);
        } else {
            System.out.println("Does Match");
        }
    }
}
