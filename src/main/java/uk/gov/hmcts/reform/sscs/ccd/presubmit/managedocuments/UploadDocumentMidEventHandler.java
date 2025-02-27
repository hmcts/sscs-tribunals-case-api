package uk.gov.hmcts.reform.sscs.ccd.presubmit.managedocuments;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.AUDIO_DOCUMENT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.AUDIO_VIDEO_EVIDENCE_DIRECTION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.VIDEO_DOCUMENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DocumentTabChoice.INTERNAL;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicMixedChoiceList;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.InternalCaseDocumentData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Component
@Slf4j
@AllArgsConstructor
public class UploadDocumentMidEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Value("${feature.tribunal-internal-documents.enabled}")
    private final boolean isTribunalInternalDocumentsEnabled;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.MID_EVENT)
            && callback.getEvent() == EventType.UPLOAD_DOCUMENT;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData sscsCaseDataBefore = callback.getCaseDetailsBefore().orElse(callback.getCaseDetails()).getCaseData();
        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
        if (isTribunalInternalDocumentsEnabled && "moveDocumentTo".equals(callback.getPageId())) {
            List<String> invalidDocumentTypes = Stream.of(AUDIO_DOCUMENT, VIDEO_DOCUMENT, AUDIO_VIDEO_EVIDENCE_DIRECTION_NOTICE).map(DocumentType::getValue).toList();
            List<SscsDocument> regularDocuments = Optional.ofNullable(sscsCaseDataBefore.getSscsDocument())
                .orElse(Collections.emptyList())
                .stream().filter(doc -> (!invalidDocumentTypes.contains(doc.getValue().getDocumentType())))
                .toList();
            List<SscsDocument> internalDocuments = Optional.ofNullable(sscsCaseDataBefore.getInternalCaseDocumentData())
                .map(InternalCaseDocumentData::getSscsInternalDocument)
                .orElse(Collections.emptyList())
                .stream().filter(doc -> (!invalidDocumentTypes.contains(doc.getValue().getDocumentType())))
                .toList();
            boolean moveToInternal = INTERNAL.equals(sscsCaseData.getInternalCaseDocumentData().getMoveDocumentTo());
            List<DynamicListItem> dynamicListItems = createDynamicListItems(moveToInternal ? regularDocuments : internalDocuments);
            DynamicMixedChoiceList dynamicMixedChoiceList = new DynamicMixedChoiceList(Collections.emptyList(), dynamicListItems);
            if (dynamicMixedChoiceList.getListItems().isEmpty()) {
                preSubmitCallbackResponse.addError("No " + (moveToInternal ? "" : "Tribunal Internal ") + "documents available to move");
            }
            sscsCaseData.getInternalCaseDocumentData().setDynamicList(moveToInternal, dynamicMixedChoiceList);
        }
        return preSubmitCallbackResponse;
    }

    private List<DynamicListItem> createDynamicListItems(List<SscsDocument> documents) {
        return documents.stream()
            .filter(doc -> isNotBlank(doc.getValue().getDocumentFileName()) || isNotBlank(doc.getValue().getDocumentLink().getDocumentFilename()))
            .map(doc -> new DynamicListItem(getDocumentIdFromUrl(doc),
                isNotBlank(doc.getValue().getDocumentFileName())
                    ? doc.getValue().getDocumentFileName()
                    : doc.getValue().getDocumentLink().getDocumentFilename()))
            .toList();
    }

    public static String getDocumentIdFromUrl(SscsDocument doc) {
        return Optional.ofNullable(doc)
            .map(SscsDocument::getValue)
            .map(SscsDocumentDetails::getDocumentLink)
            .map(DocumentLink::getDocumentUrl)
            .map(url -> url.split("/"))
            .map(splitUrl -> splitUrl[splitUrl.length - 1])
            .orElse("");
    }
}
