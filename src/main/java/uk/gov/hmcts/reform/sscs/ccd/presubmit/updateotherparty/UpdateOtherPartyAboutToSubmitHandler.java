package uk.gov.hmcts.reform.sscs.ccd.presubmit.updateotherparty;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPDATE_OTHER_PARTY_DATA;
import static uk.gov.hmcts.reform.sscs.helper.SscsHelper.updateDirectionDueDateByAnAmountOfDays;
import static uk.gov.hmcts.reform.sscs.idam.UserRole.SYSTEM_USER;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.*;

import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil;


@Component
@Slf4j
public class UpdateOtherPartyAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private IdamService idamService;

    @Autowired
    UpdateOtherPartyAboutToSubmitHandler(IdamService idamService) {
        this.idamService = idamService;
    }

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
        assignNewOtherPartyData(otherParties);
        clearOtherPartyIfEmpty(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);
        final UserDetails userDetails = idamService.getUserDetails(userAuthorisation);
        final boolean hasSystemUserRole = userDetails.hasRole(SYSTEM_USER);
        updateHearingTypeForNonSscs1Case(sscsCaseData, response, hasSystemUserRole);

        if (sscsCaseData.getAppeal() != null && sscsCaseData.getAppeal().getBenefitType() != null
            && isBenefitTypeValidForOtherPartyValidation(sscsCaseData.getBenefitType())) {
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
        updateDirectionDueDateByAnAmountOfDays(sscsCaseData);
        return response;
    }

    private boolean isBenefitTypeValidForOtherPartyValidation(Optional<Benefit> benefitType) {
        return benefitType.filter(benefit -> SscsType.SSCS5.equals(benefit.getSscsType())).isPresent();
    }

    private void updateHearingTypeForNonSscs1Case(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> response, boolean hasSystemUserRole) {
        if (sscsCaseData.getAppeal().getHearingType() != null
            && HearingType.PAPER.getValue().equals(sscsCaseData.getAppeal().getHearingType())
            && isBenefitTypeValidForHearingTypeValidation(response.getData().getBenefitType())
            && OtherPartyDataUtil.otherPartyWantsToAttendHearing(response.getData().getOtherParties())) {

            response.getData().getAppeal().setHearingType(HearingType.ORAL.getValue());
            if (!hasSystemUserRole) {
                response.addWarning("The hearing type will be changed from Paper to Oral as at least one of the"
                    + " parties to the case would like to attend the hearing");
            }
        }
    }

    private boolean isBenefitTypeValidForHearingTypeValidation(Optional<Benefit> benefitType) {
        return benefitType.filter(benefit -> SscsType.SSCS2.equals(benefit.getSscsType())
            || SscsType.SSCS5.equals(benefit.getSscsType())).isPresent();
    }
}
