package uk.gov.hmcts.reform.sscs.ccd.presubmit.reissuedocument;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.*;
import static uk.gov.hmcts.reform.sscs.util.PartiesOnCaseUtil.getAllOtherPartiesOnCase;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
public class ReissueDocumentAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
                && callback.getEvent() == EventType.REISSUE_DOCUMENT;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);

        List<DynamicListItem> dropdownList = getDocumentDropdown(sscsCaseData);

        if (isEmpty(dropdownList)) {
            response.addError("There are no documents in this appeal available to reissue.");
        } else {
            sscsCaseData.setReissueFurtherEvidenceDocument(new DynamicList(dropdownList.get(0), dropdownList));
            sscsCaseData.setResendToAppellant(null);
            sscsCaseData.setResendToRepresentative(null);
            sscsCaseData.getTransientFields().setReissueDocumentOtherParty(getReissueDocumentOtherParty(sscsCaseData));
        }

        return response;
    }

    private List<CcdValue<ReissueDocumentOtherParty>> getReissueDocumentOtherParty(SscsCaseData sscsCaseData) {
        return getAllOtherPartiesOnCase(sscsCaseData).stream()
                .map(pair -> new CcdValue<>(ReissueDocumentOtherParty.builder().otherPartyName(pair.getRight()).otherPartyId(pair.getLeft()).build()))
                .collect(Collectors.toList());
    }

    private List<DynamicListItem> getDocumentDropdown(SscsCaseData sscsCaseData) {
        List<DynamicListItem> listCostOptions = new ArrayList<>();

        if (nonNull(sscsCaseData.getSscsDocument()) || nonNull(sscsCaseData.getSscsWelshDocuments())) {
            List<? extends AbstractDocument> filteredSscsDocuments = Stream.of(sscsCaseData.getSscsDocument(), sscsCaseData.getSscsWelshDocuments()).flatMap(x -> x == null ? null : x.stream()).filter(doc -> StringUtils.isNotBlank(doc.getValue().getDocumentType())).collect(Collectors.toList());
            if (filteredSscsDocuments.stream()
                    .anyMatch(doc -> doc.getValue().getDocumentType().equals(DECISION_NOTICE.getValue()))) {
                listCostOptions.add(new DynamicListItem(sscsCaseData.isLanguagePreferenceWelsh() ? EventType.DECISION_ISSUED_WELSH.getCcdType() : EventType.DECISION_ISSUED.getCcdType(), DECISION_NOTICE.getValue()));
            }
            if (filteredSscsDocuments.stream()
                    .anyMatch(doc -> doc.getValue().getDocumentType().equals(DIRECTION_NOTICE.getValue()))) {
                listCostOptions.add(new DynamicListItem(sscsCaseData.isLanguagePreferenceWelsh() ? EventType.DIRECTION_ISSUED_WELSH.getCcdType() : EventType.DIRECTION_ISSUED.getCcdType(), DIRECTION_NOTICE.getLabel()));
            }
            if (filteredSscsDocuments.stream()
                    .anyMatch(doc -> doc.getValue().getDocumentType().equals(FINAL_DECISION_NOTICE.getValue()))) {
                listCostOptions.add(new DynamicListItem(EventType.ISSUE_FINAL_DECISION.getCcdType(), FINAL_DECISION_NOTICE.getLabel()));
            }
            if (filteredSscsDocuments.stream()
                    .anyMatch(doc -> doc.getValue().getDocumentType().equals(ADJOURNMENT_NOTICE.getValue()))) {
                listCostOptions.add(new DynamicListItem(EventType.ISSUE_ADJOURNMENT_NOTICE.getCcdType(), ADJOURNMENT_NOTICE.getLabel()));
            }
        }

        return listCostOptions;

    }
}
