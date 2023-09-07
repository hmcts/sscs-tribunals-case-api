package uk.gov.hmcts.reform.sscs.ccd.presubmit.remitfromuppertribunal;

import static java.util.Objects.requireNonNull;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Service
@Slf4j
@RequiredArgsConstructor
public class RemitFromUpperTribunalAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final FooterService footerService;

    @Value("${feature.postHearingsB.enabled}")
    private final boolean isPostHearingsBEnabled;


    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.REMIT_FROM_UPPER_TRIBUNAL
                && isPostHearingsBEnabled;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType,
                                                          Callback<SscsCaseData> callback,
                                                          String userAuthorisation
    ) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        String caseId = caseData.getCcdCaseId();
        log.info("Remit To First Tier: handling for case {}", caseId);
        PreSubmitCallbackResponse<SscsCaseData> response = validateRequest(caseData);

        if (response.getErrors().isEmpty()) {
            SscsUtil.addDocumentToDocumentTabAndBundle(footerService, caseData,
                caseData.getPostHearing().getRemitFromUpperTribunal().getRemittanceDocument(),
                DocumentType.UPPER_TRIBUNALS_DECISION_REMITTED,
                callback.getEvent());
        }
        return response;
    }

    private PreSubmitCallbackResponse<SscsCaseData> validateRequest(SscsCaseData caseData) {
        final PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);
        RemitFromUpperTribunal remitFromUpperTribunal = caseData.getPostHearing().getRemitFromUpperTribunal();
        if (remitFromUpperTribunal.getRemittanceDocument() == null) {
            response.addError("There is no remittance document");
        }
        return response;
    }
}
