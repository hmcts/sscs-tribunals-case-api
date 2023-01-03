package uk.gov.hmcts.reform.sscs.ccd.presubmit.issuegenericletter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentSelection;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentSelectionDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherPartySelection;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherPartySelectionDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static uk.gov.hmcts.reform.sscs.util.PartiesOnCaseUtil.addOtherPartiesToListOptions;

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
        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        log.info("IssueGenericLetterAboutToStartHandler::{} dwpDocuments {} sscsDocument {}",
                caseDetails.getId(), sscsCaseData.getDwpDocuments(), sscsCaseData.getSscsDocument().get(0).getValue().getDocumentFileName());

        setPartiesToSendLetter(sscsCaseData);
        setDocuments(sscsCaseData);


        log.info("IssueGenericLetterAboutToStartHandler:: hasJointParty {} hasRepresentative: {} hasOtherParties: {}",
                sscsCaseData.getHasJointParty().getValue(), sscsCaseData.getHasRepresentative().getValue(),
                sscsCaseData.getHasOtherParties().getValue());

        PreSubmitCallbackResponse<SscsCaseData> callbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        return callbackResponse;
    }

    private void setDocuments(SscsCaseData sscsCaseData) {
        List<DynamicListItem> listOptions = new ArrayList<>();

        var item1 = new DynamicListItem("1", "Document 1 Test");
        var item2 = new DynamicListItem("1", "Document 2 Test");
        var item3 = new DynamicListItem("1", "Document 3 Test");

        listOptions.add(item1);
        listOptions.add(item2);
        listOptions.add(item3);

        var l = new ArrayList<DocumentSelection>();
        l.add(new DocumentSelection(new DocumentSelectionDetails(new DynamicList(null, listOptions))));

        sscsCaseData.setDocumentSelection(l);
    }

    private void setPartiesToSendLetter(SscsCaseData sscsCaseData) {
        List<DynamicListItem> listOptions = new ArrayList<>();
        addOtherPartiesToListOptions(sscsCaseData, listOptions);

        var list = new ArrayList<OtherPartySelection>();
        list.add(new OtherPartySelection(new OtherPartySelectionDetails(new DynamicList(null, listOptions))));

        sscsCaseData.setOtherPartySelection(list);

        if (sscsCaseData.isThereAJointParty()) {
            sscsCaseData.setHasJointParty(YesNo.YES);
        }
        else {
            sscsCaseData.setHasJointParty(YesNo.NO);
        }

        if (sscsCaseData.getAppeal().getRep() != null
                && equalsIgnoreCase(sscsCaseData.getAppeal().getRep().getHasRepresentative(), "yes")) {
            sscsCaseData.setHasRepresentative(YesNo.YES);
        } else {
            sscsCaseData.setHasRepresentative(YesNo.NO);
        }

        if (isNotEmpty(sscsCaseData.getOtherParties())) {
            sscsCaseData.setHasOtherParties(YesNo.YES);
        } else {
            sscsCaseData.setHasOtherParties(YesNo.YES);
        }
    }
}
