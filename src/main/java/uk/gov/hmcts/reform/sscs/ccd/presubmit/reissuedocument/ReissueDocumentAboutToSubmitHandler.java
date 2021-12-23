package uk.gov.hmcts.reform.sscs.ccd.presubmit.reissuedocument;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
public class ReissueDocumentAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.REISSUE_DOCUMENT;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback.");
        }
        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();
        ArrayList<String> errors = new ArrayList<>();

        boolean caseHasARepresentative = StringUtils.equalsIgnoreCase("Yes", Optional.ofNullable(sscsCaseData.getAppeal().getRep()).map(Representative::getHasRepresentative).orElse("No"));
        boolean isSendToOtherParty = emptyIfNull(sscsCaseData.getTransientFields().getReissueDocumentOtherParty()).stream()
                .map(CcdValue::getValue)
                .filter(f -> Objects.nonNull(f.getOtherPartyId()))
                .anyMatch(f -> YesNo.isYes(f.getReissue()));

        if (!sscsCaseData.isResendToRepresentative() && !sscsCaseData.isResendToAppellant() && !isSendToOtherParty) {
            errors.add("No party selected to reissue document");
        } else if (!caseHasARepresentative && sscsCaseData.isResendToRepresentative()) {
            errors.add("Cannot re-issue to the representative as there is no representative on the appeal");
        }

        PreSubmitCallbackResponse<SscsCaseData> callbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
        if (CollectionUtils.isNotEmpty(errors)) {
            callbackResponse.addErrors(errors);
        }
        return callbackResponse;
    }
}
