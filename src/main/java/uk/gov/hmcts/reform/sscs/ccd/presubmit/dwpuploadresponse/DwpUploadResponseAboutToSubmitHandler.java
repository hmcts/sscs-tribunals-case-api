package uk.gov.hmcts.reform.sscs.ccd.presubmit.dwpuploadresponse;

import static java.util.Objects.requireNonNull;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Component
@Slf4j
public class DwpUploadResponseAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse;

    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && callback.getEvent() == EventType.DWP_UPLOAD_RESPONSE;
    }

    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        if (sscsCaseData.getBenefitCode() == null) {
            preSubmitCallbackResponse.addError("Benefit code cannot be empty");
        }
        if (sscsCaseData.getIssueCode() == null) {
            preSubmitCallbackResponse.addError("Issue code cannot be empty");
        }

        sscsCaseData.setCaseCode(buildCaseCode(sscsCaseData));

        return preSubmitCallbackResponse;
    }

    private String buildCaseCode(SscsCaseData sscsCaseData) {
        return sscsCaseData.getBenefitCode() + sscsCaseData.getIssueCode();
    }
}
