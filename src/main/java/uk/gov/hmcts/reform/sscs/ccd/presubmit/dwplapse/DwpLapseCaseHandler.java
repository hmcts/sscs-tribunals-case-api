package uk.gov.hmcts.reform.sscs.ccd.presubmit.dwplapse;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.AWAITING_ADMIN_ACTION;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.DwpDocumentService;


@Service
@Slf4j
public class DwpLapseCaseHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private DwpDocumentService dwpDocumentService;

    @Autowired
    public DwpLapseCaseHandler(DwpDocumentService dwpDocumentService) {
        this.dwpDocumentService = dwpDocumentService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.DWP_LAPSE_CASE;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        log.info("Setting interloc review field to " + AWAITING_ADMIN_ACTION + " for case id " + caseData.getCcdCaseId());

        caseData.setInterlocReviewState(AWAITING_ADMIN_ACTION);
        caseData.setDwpState(DwpState.LAPSED);

        if (caseData.getDwpLapseLetter() != null) {
            dwpDocumentService.addToDwpDocuments(caseData, caseData.getDwpLapseLetter(), DwpDocumentType.DWP_LAPSE_LETTER);
            caseData.setDwpLapseLetter(null);
        }

        if (caseData.getDwpLT203() != null) {
            dwpDocumentService.addToDwpDocuments(caseData, caseData.getDwpLT203(), DwpDocumentType.DWP_LT_203);
            caseData.setDwpLT203(null);
        }

        PreSubmitCallbackResponse<SscsCaseData> sscsCaseDataPreSubmitCallbackResponse = new PreSubmitCallbackResponse<>(caseData);
        log.info("Handled DWP lapse case " + caseData.getCcdCaseId());

        return sscsCaseDataPreSubmitCallbackResponse;
    }
}
