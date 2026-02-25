package uk.gov.hmcts.reform.sscs.ccd.presubmit.validsendtointerloc;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.util.PartiesOnCaseUtil.getPartiesOnCase;
import static uk.gov.hmcts.reform.sscs.util.PartiesOnCaseUtil.isChildSupportAppeal;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Component
public class NonCompliantSendToInterlocAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final boolean cmInterlocConfidentialityPartyEnabled;

    public NonCompliantSendToInterlocAboutToStartHandler(
            @Value("${feature.cm-interloc-confidentiality-party.enabled}") boolean cmInterlocConfidentialityPartyEnabled) {
        this.cmInterlocConfidentialityPartyEnabled = cmInterlocConfidentialityPartyEnabled;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
                && callback.getEvent() == EventType.NON_COMPLIANT_SEND_TO_INTERLOC;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType,
                                                          Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();
        List<DynamicListItem> listOptions = getPartiesOnCase(sscsCaseData);
        DynamicListItem selectedParty = cmInterlocConfidentialityPartyEnabled && isChildSupportAppeal(sscsCaseData)
                ? new DynamicListItem("", "")
                : listOptions.get(0);

        sscsCaseData.setOriginalSender(new DynamicList(selectedParty, listOptions));
        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }
}
