package uk.gov.hmcts.reform.sscs.ccd.presubmit.sendtofirsttier;

import static java.util.Objects.requireNonNull;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SendToFirstTierActions;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Service
@Slf4j
@RequiredArgsConstructor

public class SendToFirstTierAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Value("${feature.postHearings.enabled}")
    private final boolean isPostHearingsEnabled;

    @Value("${feature.postHearingsB.enabled}")
    private final boolean isPostHearingsBEnabled;

    private final FooterService footerService;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.SEND_TO_FIRST_TIER
                && isPostHearingsEnabled && isPostHearingsBEnabled;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType,
                                                          Callback<SscsCaseData> callback,
                                                          String userAuthorisation
    ) {
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        PreSubmitCallbackResponse response = new PreSubmitCallbackResponse<>(caseData);
        DocumentLink previewDocument = caseData.getDocumentStaging().getPreviewDocument();
        if (previewDocument == null) {
            response.addError("Preview document must be provided");
        }
//        if (response.getErrors().isEmpty()) {
//            SscsUtil.addDocumentToDocumentTabAndBundle(footerService, caseData, getDocumentType(caseData));
//        }
        return response;
    }

    private DocumentType getDocumentType(SscsCaseData caseData) {
        SendToFirstTierActions action = caseData.getPostHearing().getSendToFirstTier().getAction();
        switch (action) {
            case DECISION_REMADE:
                return DocumentType.UPPER_TRIBUNALS_DECISION_REMADE;
            case DECISION_REFUSED:
                return DocumentType.UPPER_TRIBUNALS_DECISION_REFUSED;
            default:
                throw new IllegalArgumentException("Unexpected decision type: " + action);
        }
    }
}
