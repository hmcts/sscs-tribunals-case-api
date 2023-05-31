package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.*;
import static uk.gov.hmcts.reform.sscs.util.PartiesOnCaseUtil.getPartiesOnCaseWithDwpAndHmcts;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Service
@AllArgsConstructor
public class ActionFurtherEvidenceAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Value("${feature.postHearings.enabled}")
    private final boolean isPostHearingsEnabled;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
            && callback.getEvent() == EventType.ACTION_FURTHER_EVIDENCE;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        setFurtherEvidenceActionDropdown(sscsCaseData);
        setOriginalSenderDropdown(sscsCaseData);

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private void setFurtherEvidenceActionDropdown(SscsCaseData sscsCaseData) {
        List<DynamicListItem> listOptions = getFurtherActionEvidenceItems(sscsCaseData);
        sscsCaseData.setFurtherEvidenceAction(new DynamicList(listOptions.get(0), listOptions));
    }

    private List<DynamicListItem> getFurtherActionEvidenceItems(SscsCaseData sscsCaseData) {
        return Stream.of(values())
            .filter(item -> !item.equals(ADMIN_ACTION_CORRECTION)
                || (!SscsUtil.isGapsCase(sscsCaseData)
                && isPostHearingsEnabled))
            .map(item -> new DynamicListItem(item.getCode(), item.getLabel()))
            .collect(Collectors.toList());
    }

    private void setOriginalSenderDropdown(SscsCaseData sscsCaseData) {
        List<DynamicListItem> listOptions = getPartiesOnCaseWithDwpAndHmcts(sscsCaseData);

        sscsCaseData.setOriginalSender(new DynamicList(listOptions.get(0), listOptions));
    }
}
