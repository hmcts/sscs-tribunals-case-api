package uk.gov.hmcts.reform.sscs.ccd.presubmit.updateotherparty;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.assignOtherPartyId;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.getIdComparator;

import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;


@Component
@Slf4j
public class UpdateOtherPartyAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.UPDATE_OTHER_PARTY_DATA
                && nonNull(callback.getCaseDetails().getCaseData().getOtherParties());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        List<CcdValue<OtherParty>> otherParties = sscsCaseData.getOtherParties();
        otherParties.sort(getIdComparator());
        assignOtherPartyId(otherParties);

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }
}
