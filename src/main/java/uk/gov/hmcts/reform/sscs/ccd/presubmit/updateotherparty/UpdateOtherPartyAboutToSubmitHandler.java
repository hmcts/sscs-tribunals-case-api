package uk.gov.hmcts.reform.sscs.ccd.presubmit.updateotherparty;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPDATE_OTHER_PARTY_DATA;
import static uk.gov.hmcts.reform.sscs.ccd.domain.SscsType.SSCS5;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.*;

import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
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
                && callback.getEvent() == UPDATE_OTHER_PARTY_DATA
                && nonNull(callback.getCaseDetails().getCaseData().getOtherParties());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        List<CcdValue<OtherParty>> otherParties = sscsCaseData.getOtherParties();
        updateOtherPartyUcb(sscsCaseData);
        checkConfidentiality(sscsCaseData);
        assignNewOtherPartyData(otherParties, UPDATE_OTHER_PARTY_DATA);
        clearOtherPartyIfEmpty(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);
        if (sscsCaseData.getAppeal() != null && sscsCaseData.getAppeal().getBenefitType() != null
            && isSscs5Case(sscsCaseData)) {
            if (callback.isIgnoreWarnings()) {
                validateOtherPartyForSscs5Case(sscsCaseData);
            } else {
                if (roleExistsForOtherParties(sscsCaseData.getOtherParties())) {
                    response.addWarning("You have entered a role for the Other Party which is not valid "
                        + "for an SSCS5 case. This role will be ignored when the event completes.");
                } else {
                    validateOtherPartyForSscs5Case(sscsCaseData);
                }
            }
            return response;
        }
        //Check if role is not entered for a Child support case
        if (roleAbsentForOtherParties(sscsCaseData.getOtherParties())) {
            response.addError("Role is required for the selected case");
        }
        return response;
    }

    private boolean isSscs5Case(SscsCaseData sscsCaseData) {
        return Optional.ofNullable(sscsCaseData.getAppeal().getBenefitType().getCode())
            .filter(b -> Benefit.findBenefitByShortName(b)
                .filter(benefit -> benefit.getSscsType().equals(SSCS5)).isPresent())
            .isPresent();
    }
}
