package uk.gov.hmcts.reform.sscs.ccd.presubmit.updateotherparty;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPDATE_OTHER_PARTY_DATA;
import static uk.gov.hmcts.reform.sscs.helper.SscsHelper.getUpdatedDirectionDueDate;
import static uk.gov.hmcts.reform.sscs.helper.SscsHelper.validateHearingOptionsAndExcludeDates;
import static uk.gov.hmcts.reform.sscs.idam.UserRole.SYSTEM_USER;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.clearOtherPartiesIfEmpty;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.getOtherPartiesWithClearedRoles;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.getOtherPartyUcb;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.isConfidential;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.otherPartyWantsToAttendHearing;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.roleAbsentForOtherParties;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.roleExistsForOtherParties;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.sendNewOtherPartyNotification;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingType;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsType;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;


@Component
@Slf4j
public class UpdateOtherPartyAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private static final String WARN_NON_SSCS1_PAPER_TO_ORAL = "The hearing type will be changed from Paper to Oral as "
            + "at least one of the parties to the case would like to attend the hearing";

    private static final String WARN_INVALID_OTHER_PARTY_ROLE_FOR_SSCS5 = "You have entered a role for the Other Party "
            + "which is not valid for an SSCS5 case. This role will be ignored when the event completes.";

    private static final String ERR_ROLE_REQUIRED = "Role is required for the selected case";

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
        sscsCaseData.setOtherPartyUcb(getOtherPartyUcb(sscsCaseData.getOtherParties()));
        sscsCaseData.setIsConfidentialCase(isConfidential(sscsCaseData));
        sscsCaseData.getOtherParties().forEach(otherPartyCcdValue -> otherPartyCcdValue.getValue()
                .setSendNewOtherPartyNotification(sendNewOtherPartyNotification(otherPartyCcdValue)));
        sscsCaseData.setOtherParties(clearOtherPartiesIfEmpty(sscsCaseData));

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);
        response.addErrors(verifyHearingUnavailableDates(sscsCaseData.getOtherParties()));

        if (isNonSscs1Case(sscsCaseData, response)) {
            response.getData().getAppeal().setHearingType(HearingType.ORAL.getValue());
            final UserDetails user = idamService.getUserDetails(userAuthorisation);
            response.addWarnings(!user.hasRole(SYSTEM_USER) ? List.of(WARN_NON_SSCS1_PAPER_TO_ORAL) : List.of());
        }

        sscsCaseData.setDirectionDueDate(getUpdatedDirectionDueDate(sscsCaseData));

        if (sscsCaseData.getAppeal() != null && sscsCaseData.getAppeal().getBenefitType() != null
            && isBenefitTypeValidForOtherPartyValidation(sscsCaseData.getBenefitType())) {
            if (callback.isIgnoreWarnings()) {
                sscsCaseData.setOtherParties(getOtherPartiesWithClearedRoles(sscsCaseData.getOtherParties()));
            } else {
                if (roleExistsForOtherParties(sscsCaseData.getOtherParties())) {
                    response.addWarning(WARN_INVALID_OTHER_PARTY_ROLE_FOR_SSCS5);
                } else {
                    sscsCaseData.setOtherParties(getOtherPartiesWithClearedRoles(sscsCaseData.getOtherParties()));
                }
            }
            return response;
        }
        //Check if role is not entered for a Child support case
        if (roleAbsentForOtherParties(sscsCaseData.getOtherParties())) {
            response.addError(ERR_ROLE_REQUIRED);
        }
        return response;
    }

    private List<String> verifyHearingUnavailableDates(final List<CcdValue<OtherParty>> otherParties) {
        List<String> errors = new ArrayList<>();
        if (!isNull(otherParties)) {
            otherParties.stream()
                    .map(CcdValue::getValue)
                    .filter(otherParty -> hasValidHearingOptionsAndWantsToExcludeDates(otherParty))
                    .forEach(otherParty -> errors.addAll(
                            validateHearingOptionsAndExcludeDates(otherParty.getHearingOptions().getExcludeDates())
                    ));
        }
        return errors;
    }

    private boolean hasValidHearingOptionsAndWantsToExcludeDates(final OtherParty otherParty) {
        return otherParty.getHearingOptions() != null
                && YesNo.isYes(otherParty.getHearingOptions().getWantsToAttend())
                && YesNo.isYes(otherParty.getHearingOptions().getScheduleHearing());
    }

    private boolean isNonSscs1Case(final SscsCaseData sscsCaseData,
                                                     final PreSubmitCallbackResponse<SscsCaseData> response) {
        return sscsCaseData.getAppeal().getHearingType() != null
                && HearingType.PAPER.getValue().equals(sscsCaseData.getAppeal().getHearingType())
                && isBenefitTypeValidForHearingTypeValidation(response.getData().getBenefitType())
                && otherPartyWantsToAttendHearing(response.getData().getOtherParties());
    }

    private boolean isBenefitTypeValidForHearingTypeValidation(final Optional<Benefit> benefitType) {
        return benefitType.filter(benefit -> SscsType.SSCS2.equals(benefit.getSscsType())
                || SscsType.SSCS5.equals(benefit.getSscsType())).isPresent();
    }

    private boolean isBenefitTypeValidForOtherPartyValidation(final Optional<Benefit> benefitType) {
        return benefitType.filter(benefit -> SscsType.SSCS5.equals(benefit.getSscsType())).isPresent();
    }
}
