package uk.gov.hmcts.reform.sscs.ccd.presubmit.issuehearingenquiryform;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static uk.gov.hmcts.reform.sscs.util.PartiesOnCaseUtil.addOtherPartiesToListOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.AbstractDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentSelectionDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherPartySelectionDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
@Slf4j
public class IssueHearingEnquiryFormAboutToStart implements PreSubmitCallbackHandler<SscsCaseData> {
    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START) && callback.getEvent() == EventType.ISSUE_HEARING_ENQUIRY_FORM;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
        String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback.");
        }

        final SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        sscsCaseData.clearNotificationFields();
        setParties(sscsCaseData);
        setDocuments(sscsCaseData);

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private void setParties(SscsCaseData sscsCaseData) {
        if (isNotEmpty(sscsCaseData.getOtherParties())) {
            List<DynamicListItem> listOptions = new ArrayList<>();
            addOtherPartiesToListOptions(sscsCaseData, listOptions, false);
            var list = new ArrayList<CcdValue<OtherPartySelectionDetails>>();
            list.add(new CcdValue<>(new OtherPartySelectionDetails(new DynamicList(null, listOptions))));
            sscsCaseData.setOtherPartySelection(list);
        }
    }

    private void setDocuments(SscsCaseData sscsCaseData) {
        final List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.addAll(getDocumentsListOptions(sscsCaseData.getDwpDocuments()));
        listOptions.addAll(getDocumentsListOptions(sscsCaseData.getSscsDocument()));
        var documentSelections = new ArrayList<CcdValue<DocumentSelectionDetails>>();
        var documentSelection = new CcdValue<>(new DocumentSelectionDetails(new DynamicList(null, listOptions)));
        documentSelections.add(documentSelection);
        sscsCaseData.setDocumentSelection(documentSelections);
    }

    private List<DynamicListItem> getDocumentsListOptions(List<? extends AbstractDocument<?>> documents) {
        final List<DynamicListItem> listOptions = new ArrayList<>();

        Optional.ofNullable(documents).orElse(emptyList()).forEach(document -> {
            String documentFileName = document.getValue().getDocumentFileName();
            listOptions.add(new DynamicListItem(documentFileName, documentFileName));
            if (document.getValue().getEditedDocumentLink() != null) {
                String editedDocumentFileName = document.getValue().getEditedDocumentLink().getDocumentFilename();
                listOptions.add(new DynamicListItem(editedDocumentFileName, editedDocumentFileName));
            }
        });

        return listOptions;
    }
}
