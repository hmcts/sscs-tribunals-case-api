package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploadwelshdocument;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Component
public class UploadWelshDocumentsAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private List<DynamicListItem> listOptions;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
            && callback.getEvent() == EventType.UPLOAD_WELSH_DOCUMENT;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        setOriginalDocumentDropdown(sscsCaseData);

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private void setOriginalDocumentDropdown(SscsCaseData sscsCaseData) {
        listOptions = new ArrayList<>();

        List<SscsDocument> sscsDocuments = Optional.ofNullable(sscsCaseData).map(SscsCaseData::getSscsDocument)
            .orElse(Collections.emptyList())
            .stream()
            .filter(Objects::nonNull)
            .filter(a -> Objects.nonNull(a.getValue().getDocumentTranslationStatus())
                &&
                a.getValue().getDocumentTranslationStatus().equals(SscsDocumentTranslationStatus.TRANSLATION_REQUESTED))
            .filter(b -> !Arrays.asList(DocumentType.DECISION_NOTICE.getValue(), DocumentType.AUDIO_VIDEO_EVIDENCE_DIRECTION_NOTICE, DocumentType.DIRECTION_NOTICE.getValue())
                .contains(b.getValue().getDocumentType()))
            .collect(Collectors.toList());

        sscsDocuments.forEach(sscsDocument -> {
            listOptions.add(new DynamicListItem(sscsDocument.getValue().getDocumentLink().getDocumentFilename(),
                sscsDocument.getValue().getDocumentLink().getDocumentFilename()));
        });

        if (listOptions.size() > 0) {
            sscsCaseData.setOriginalDocuments(new DynamicList(listOptions.get(0), listOptions));
        } else {
            listOptions.add(new DynamicListItem("-", "No original file"));
            sscsCaseData.setOriginalDocuments(new DynamicList(listOptions.get(0), listOptions));
        }
    }
}
