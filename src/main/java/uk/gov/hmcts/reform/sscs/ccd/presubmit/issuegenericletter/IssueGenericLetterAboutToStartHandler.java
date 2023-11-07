package uk.gov.hmcts.reform.sscs.ccd.presubmit.issuegenericletter;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static uk.gov.hmcts.reform.sscs.util.PartiesOnCaseUtil.addOtherPartiesToListOptions;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.AbstractDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentSelectionDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherPartySelectionDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
@Slf4j
public class IssueGenericLetterAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
                && callback.getEvent() == EventType.ISSUE_GENERIC_LETTER;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback.");
        }
        CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        SscsCaseData sscsCaseData = caseDetails.getCaseData();

        clearFields(sscsCaseData);

        setPartiesToSendLetter(sscsCaseData);
        setDocuments(sscsCaseData);

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private void clearFields(SscsCaseData sscsCaseData) {
        sscsCaseData.setGenericLetterText("");
        sscsCaseData.setSendToAllParties(null);
        sscsCaseData.setSendToApellant(null);
        sscsCaseData.setSendToJointParty(null);
        sscsCaseData.setSendToOtherParties(null);
        sscsCaseData.setSendToRepresentative(null);
        sscsCaseData.setAddDocuments(null);

        List<CcdValue<DocumentSelectionDetails>> documentSelection = sscsCaseData.getDocumentSelection();
        if (isNotEmpty(documentSelection)) {
            documentSelection.clear();
        }

        List<CcdValue<OtherPartySelectionDetails>> otherPartySelection = sscsCaseData.getOtherPartySelection();
        if (isNotEmpty(otherPartySelection)) {
            otherPartySelection.clear();
        }
    }

    private void setDocuments(SscsCaseData sscsCaseData) {
        List<DynamicListItem> listOptions = new ArrayList<>();

        listOptions.addAll(getDocumentsListOptions(sscsCaseData.getDwpDocuments()));
        listOptions.addAll(getDocumentsListOptions(sscsCaseData.getSscsDocument()));

        var documentSelectionDetails = new DocumentSelectionDetails(new DynamicList(null, listOptions));
        var documentSelections = new ArrayList<CcdValue<DocumentSelectionDetails>>();
        var documentSelection = new CcdValue<>(documentSelectionDetails);
        documentSelections.add(documentSelection);

        sscsCaseData.setDocumentSelection(documentSelections);
    }

    private List<DynamicListItem> getDocumentsListOptions(List<? extends AbstractDocument<?>> documents) {
        List<DynamicListItem> listOptions = new ArrayList<>();

        if (isNotEmpty(documents)) {
            for (var document : documents) {
                String documentFileName = document.getValue().getDocumentFileName();
                listOptions.add(new DynamicListItem(documentFileName, documentFileName));
                if (document.getValue().getEditedDocumentLink() != null) {
                    String editedDocumentFileName = document.getValue().getEditedDocumentLink().getDocumentFilename();
                    listOptions.add(new DynamicListItem(editedDocumentFileName, editedDocumentFileName));
                }
            }
        }

        return listOptions;
    }

    private void setPartiesToSendLetter(SscsCaseData sscsCaseData) {
        YesNo hasJointParty = sscsCaseData.isThereAJointParty() ? YesNo.YES : YesNo.NO;
        sscsCaseData.setHasJointParty(hasJointParty);

        YesNo hasRepresentative = sscsCaseData.isThereARepresentative() ? YesNo.YES : YesNo.NO;
        sscsCaseData.setHasRepresentative(hasRepresentative);

        if (isNotEmpty(sscsCaseData.getOtherParties())) {
            sscsCaseData.setHasOtherParties(YesNo.YES);

            List<DynamicListItem> listOptions = new ArrayList<>();
            addOtherPartiesToListOptions(sscsCaseData, listOptions);

            var list = new ArrayList<CcdValue<OtherPartySelectionDetails>>();
            list.add(new CcdValue<>(new OtherPartySelectionDetails(new DynamicList(null, listOptions))));

            sscsCaseData.setOtherPartySelection(list);
        } else {
            sscsCaseData.setHasOtherParties(YesNo.NO);
        }
    }
}