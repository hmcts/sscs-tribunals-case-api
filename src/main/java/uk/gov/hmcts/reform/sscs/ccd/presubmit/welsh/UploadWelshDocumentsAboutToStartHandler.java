package uk.gov.hmcts.reform.sscs.ccd.presubmit.welsh;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@Component
@Slf4j
public class UploadWelshDocumentsAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
                && callback.getEvent() == EventType.UPLOAD_WELSH_DOCUMENT;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        setOriginalDocumentDropdown(sscsCaseData);

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private void setOriginalDocumentDropdown(SscsCaseData sscsCaseData) {
        log.debug("invoking custom class");
        SscsDocumentDetails sscsDocumentDetails;
        List<DynamicListItem> listOptions = new ArrayList<>();
        List<SscsDocument> sscsDocuments =  Optional.ofNullable(sscsCaseData).map(SscsCaseData::getSscsDocument)
                .orElse(Collections.emptyList())
                .stream()
                .filter(a -> a.getValue().getDocumentTranslationStatus().equals(SscsDocumentTranslationStatus.TRANSLATION_REQUESTED))
                .collect(Collectors.toList());

        for (SscsDocument sscsDocument: sscsDocuments){
            sscsDocumentDetails = sscsDocument.getValue();
            listOptions.add(new DynamicListItem(sscsDocumentDetails.getDocumentLink().getDocumentFilename(),
                    sscsDocumentDetails.getDocumentLink().getDocumentFilename()));
        }

        if(listOptions.size() > 0) {
            sscsCaseData.setOriginalDocuments(new DynamicList(listOptions.get(0), listOptions));
        } else {
            listOptions.add(new DynamicListItem("-", "Not file available in desired status"));
            sscsCaseData.setOriginalDocuments(new DynamicList(listOptions.get(0), listOptions));
        }

    }
}
