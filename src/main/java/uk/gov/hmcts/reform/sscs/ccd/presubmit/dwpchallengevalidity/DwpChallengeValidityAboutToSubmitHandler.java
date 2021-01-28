package uk.gov.hmcts.reform.sscs.ccd.presubmit.dwpchallengevalidity;

import static java.util.Objects.requireNonNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpResponseDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.DwpDocumentService;

@Component
public class DwpChallengeValidityAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private DwpDocumentService dwpDocumentService;

    @Autowired
    public DwpChallengeValidityAboutToSubmitHandler(DwpDocumentService dwpDocumentService) {
        this.dwpDocumentService = dwpDocumentService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");

        return CallbackType.ABOUT_TO_SUBMIT == callbackType
            && EventType.DWP_CHALLENGE_VALIDITY == callback.getEvent();
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        DwpResponseDocument dwpChallengeValidityDocument = caseData.getDwpChallengeValidityDocument();
        dwpDocumentService.addToDwpDocuments(caseData, dwpChallengeValidityDocument, DwpDocumentType.DWP_CHALLENGE_VALIDITY);

        caseData.setDwpChallengeValidityDocument(null);

        return new PreSubmitCallbackResponse<>(caseData);
    }
}
