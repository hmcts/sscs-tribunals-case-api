package uk.gov.hmcts.reform.sscs.ccd.presubmit.markdocsfortranslation;

import static java.util.Objects.requireNonNull;

import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
@Slf4j
public class MarkDocumentsForTranslationAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && callback.getEvent().equals(EventType.MARK_DOCS_FOR_TRANSATION);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        setTranslationRequiredStatus.accept(caseData);
        log.info("Set the TranslationWorkOutstanding flag to YES,  for case id : {}", caseData.getCcdCaseId());
        caseData.setTranslationWorkOutstanding("Yes");
        return new PreSubmitCallbackResponse<>(caseData);
    }

    private Consumer<SscsCaseData> setTranslationRequiredStatus = sscsCaseData -> {
        sscsCaseData.getSscsDocument().stream().filter(sd -> sd.getValue().getDocumentType()
                .equals(DocumentType.APPELLANT_EVIDENCE.getValue()) || sd.getValue().getDocumentType()
                .equals(DocumentType.SSCS1.getValue()))
                .forEach(caseData -> caseData.getValue().setDocumentTranslationStatus(SscsDocumentTranslationStatus.TRANSLATION_REQUIRED));
    };
}
