package uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.managedocuments;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.AUDIO_DOCUMENT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.AUDIO_VIDEO_EVIDENCE_DIRECTION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.VIDEO_DOCUMENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DocumentTabChoice.INTERNAL;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.callback.handlers.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicMixedChoiceList;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.InternalCaseDocumentData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;

@Component
@Slf4j
@AllArgsConstructor
public class UploadDocumentMidEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Value("${feature.tribunal-internal-documents.enabled}")
    private final boolean isTribunalInternalDocumentsEnabled;
    private static final String MOVE_DOCUMENT_TO = "moveDocumentTo";

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

        SscsCaseData sscsCaseDataBefore = callback.getCaseDetailsBefore().map(CaseDetails::getCaseData).orElse(callback.getCaseDetails().getCaseData());
        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
        if (!isTribunalInternalDocumentsEnabled || !MOVE_DOCUMENT_TO.equals(callback.getPageId())) {
            return preSubmitCallbackResponse;
        }
        List<String> invalidDocumentTypes = List.of(AUDIO_DOCUMENT.getValue(), VIDEO_DOCUMENT.getValue(), AUDIO_VIDEO_EVIDENCE_DIRECTION_NOTICE.getValue());
        List<SscsDocument> regularDocuments = Optional.ofNullable(sscsCaseDataBefore.getSscsDocument())
            .orElse(Collections.emptyList())
            .stream().filter(doc -> (!invalidDocumentTypes.contains(doc.getValue().getDocumentType())))
            .toList();
        List<SscsDocument> internalDocuments = Optional.ofNullable(sscsCaseDataBefore.getInternalCaseDocumentData())
            .map(InternalCaseDocumentData::getSscsInternalDocument)
            .orElse(Collections.emptyList())
            .stream().filter(doc -> (!invalidDocumentTypes.contains(doc.getValue().getDocumentType())))
            .toList();
        InternalCaseDocumentData internalCaseDocumentData = Optional.ofNullable(sscsCaseData.getInternalCaseDocumentData())
            .orElse(InternalCaseDocumentData.builder().build());
        if (INTERNAL.equals(internalCaseDocumentData.getMoveDocumentTo())) {
            List<DynamicListItem> regularDlItems = createDynamicListItems(regularDocuments);
            if (regularDlItems.isEmpty()) {
                preSubmitCallbackResponse.addError("No documents available to move");
            } else {
                internalCaseDocumentData
                    .setMoveDocumentToInternalDocumentsTabDL(
                        new DynamicMixedChoiceList(Collections.emptyList(), regularDlItems));
            }
        } else {
            List<DynamicListItem> internalDlItems = createDynamicListItems(internalDocuments);
            if (internalDlItems.isEmpty()) {
                preSubmitCallbackResponse.addError("No Tribunal Internal documents available to move");
            } else {
                internalCaseDocumentData
                    .setMoveDocumentToDocumentsTabDL(
                        new DynamicMixedChoiceList(Collections.emptyList(), internalDlItems));
            }
        }
        sscsCaseData.setInternalCaseDocumentData(internalCaseDocumentData);
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
