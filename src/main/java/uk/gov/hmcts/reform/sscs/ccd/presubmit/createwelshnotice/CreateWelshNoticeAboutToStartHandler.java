package uk.gov.hmcts.reform.sscs.ccd.presubmit.createwelshnotice;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
public class CreateWelshNoticeAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
                && callback.getEvent() == EventType.CREATE_WELSH_NOTICE;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();
        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse =
                new PreSubmitCallbackResponse<>(sscsCaseData);

        if (!sscsCaseData.isLanguagePreferenceWelsh()) {
            preSubmitCallbackResponse.addError("Error: This action is only available for Welsh cases.");
        }
        setDocumentTypesAndOriginalNoticeDocumentsDropdown(sscsCaseData);
        return preSubmitCallbackResponse;
    }


    private void setDocumentTypesAndOriginalNoticeDocumentsDropdown(SscsCaseData sscsCaseData) {
        List<DynamicListItem> listNoticeDocumentOptions = new ArrayList<>();
        List<DynamicListItem> listDocumentTypeOptions = new ArrayList<>();
        List<SscsDocument> sscsDocuments =  Optional.ofNullable(sscsCaseData).map(SscsCaseData::getSscsDocument)
                .orElse(Collections.emptyList())
                .stream()
                .filter(Objects::nonNull)
                .filter(a -> Objects.nonNull(a.getValue().getDocumentTranslationStatus())
                        && a.getValue().getDocumentTranslationStatus().equals(SscsDocumentTranslationStatus.TRANSLATION_REQUESTED))
                .filter(a -> Objects.nonNull(a.getValue().getDocumentType())
                        && (a.getValue().getDocumentType().equals(DECISION_NOTICE.getValue())
                        || a.getValue().getDocumentType().equals(ADJOURNMENT_NOTICE.getValue())
                        || a.getValue().getDocumentType().equals(AUDIO_VIDEO_EVIDENCE_DIRECTION_NOTICE.getValue())
                        || a.getValue().getDocumentType().equals(POSTPONEMENT_REQUEST_DIRECTION_NOTICE.getValue())
                        || a.getValue().getDocumentType().equals(DIRECTION_NOTICE.getValue())))
                .toList();

        sscsDocuments.forEach(sscsDocument -> listNoticeDocumentOptions.add(new DynamicListItem(sscsDocument.getValue().getDocumentLink().getDocumentFilename(),
                sscsDocument.getValue().getDocumentLink().getDocumentFilename())));

        sscsDocuments.forEach(sscsDocument -> listDocumentTypeOptions.add(new DynamicListItem(sscsDocument.getValue().getDocumentType(),
                DocumentType.fromValue(sscsDocument.getValue().getDocumentType()).getLabel())));

        if (listNoticeDocumentOptions.size() > 0) {
            requireNonNull(sscsCaseData).setOriginalNoticeDocuments(new DynamicList(listNoticeDocumentOptions.get(0), listNoticeDocumentOptions));
        } else {
            listNoticeDocumentOptions.add(new DynamicListItem("-", "-"));
            requireNonNull(sscsCaseData).setOriginalNoticeDocuments(new DynamicList(listNoticeDocumentOptions.get(0), listNoticeDocumentOptions));
        }

        if (listDocumentTypeOptions.size() > 0) {
            requireNonNull(sscsCaseData).setDocumentTypes(new DynamicList(listDocumentTypeOptions.get(0), listDocumentTypeOptions));
        } else {
            listDocumentTypeOptions.add(new DynamicListItem("-", "-"));
            requireNonNull(sscsCaseData).setDocumentTypes(new DynamicList(listDocumentTypeOptions.get(0), listDocumentTypeOptions));
        }
    }
}
