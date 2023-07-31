package uk.gov.hmcts.reform.sscs.ccd.presubmit.sendtofirsttier;

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

import static java.util.Objects.requireNonNull;

@Service
@Slf4j
@RequiredArgsConstructor
public class SendToFirstTierAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final FooterService footerService;

    @Value("${feature.postHearingsB.enabled}")
    private final boolean isPostHearingsBEnabled;


    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.SEND_TO_FIRST_TIER
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
        PostHearingRequestType requestType = caseData.getPostHearing().getRequestType();
        log.info("Send To First Tier: handling action {} for case {}", requestType,  caseId);
        PreSubmitCallbackResponse<SscsCaseData> response = validateRequest(caseData);

        if (response.getErrors().isEmpty()) {
            SscsUtil.addDocumentToDocumentTabAndBundle(footerService, caseData,
                caseData.getPostHearing().getSendToFirstTier().getDecisionDocument(),
                getSendToFirstTierDocumentType(caseData.getPostHearing().getSendToFirstTier().getAction()));
        }
        return response;
    }

    private PreSubmitCallbackResponse<SscsCaseData> validateRequest(SscsCaseData caseData) {
        final PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);
        SendToFirstTier sendToFirstTier = caseData.getPostHearing().getSendToFirstTier();
        if (sendToFirstTier.getAction() == null) {
            response.addError("There is no decision type");
        }
        if (sendToFirstTier.getDecisionDocument() == null) {
            response.addError("There is no decision document");
        }
        return response;
    }

    private DocumentType getSendToFirstTierDocumentType(SendToFirstTierActions action) {
        switch (action) {
            case DECISION_REMADE:
                return DocumentType.UPPER_TRIBUNALS_DECISION_REMADE;
            case DECISION_REFUSED:
                return DocumentType.UPPER_TRIBUNALS_DECISION_REFUSED;
            default:
                throw new IllegalArgumentException("Unexpected decision action: " + action);
        }
    }
}
