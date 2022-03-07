package uk.gov.hmcts.reform.sscs.ccd.presubmit.caseupdated;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.idam.UserRole.*;
import static uk.gov.hmcts.reform.sscs.idam.UserRole.SUPER_USER;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.checkConfidentiality;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.AssociatedCaseLinkHelper;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.ResponseEventsAboutToSubmit;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.isscottish.IsScottishHandler;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.model.CourtVenue;
import uk.gov.hmcts.reform.sscs.model.dwp.OfficeMapping;
import uk.gov.hmcts.reform.sscs.service.AirLookupService;
import uk.gov.hmcts.reform.sscs.service.DwpAddressLookupService;
import uk.gov.hmcts.reform.sscs.service.RefDataService;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;

@Component
@Slf4j
public class CaseUpdatedAboutToSubmitHandler extends ResponseEventsAboutToSubmit implements PreSubmitCallbackHandler<SscsCaseData> {

    private final RegionalProcessingCenterService regionalProcessingCenterService;
    private final AssociatedCaseLinkHelper associatedCaseLinkHelper;
    private final AirLookupService airLookupService;
    private final DwpAddressLookupService dwpAddressLookupService;
    private final IdamService idamService;
    private final RefDataService refDataService;
    private final boolean caseAccessManagementFeature;

    @SuppressWarnings("squid:S107")
    CaseUpdatedAboutToSubmitHandler(RegionalProcessingCenterService regionalProcessingCenterService,
                                    AssociatedCaseLinkHelper associatedCaseLinkHelper,
                                    AirLookupService airLookupService,
                                    DwpAddressLookupService dwpAddressLookupService,
                                    IdamService idamService,
                                    RefDataService refDataService,
                                    @Value("${feature.case-access-management.enabled}")  boolean caseAccessManagementFeature) {
        this.regionalProcessingCenterService = regionalProcessingCenterService;
        this.associatedCaseLinkHelper = associatedCaseLinkHelper;
        this.airLookupService = airLookupService;
        this.dwpAddressLookupService = dwpAddressLookupService;
        this.idamService = idamService;
        this.refDataService = refDataService;
        this.caseAccessManagementFeature = caseAccessManagementFeature;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && callback.getEvent() == EventType.CASE_UPDATED;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final Optional<CaseDetails<SscsCaseData>> caseDetailsBefore = callback.getCaseDetailsBefore();
        final SscsCaseData sscsCaseData = associatedCaseLinkHelper.linkCaseByNino(caseDetails.getCaseData(), caseDetailsBefore);

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        final UserDetails userDetails = idamService.getUserDetails(userAuthorisation);
        final boolean hasSuperUserRole = userDetails.hasRole(SUPER_USER);

        setCaseCode(preSubmitCallbackResponse, callback, hasSuperUserRole);
        validateBenefitForCase(preSubmitCallbackResponse, callback, hasSuperUserRole);
        if (!preSubmitCallbackResponse.getErrors().isEmpty()) {
            return preSubmitCallbackResponse;
        }

        Appellant appellant = sscsCaseData.getAppeal().getAppellant();
        if (appellant != null
            && appellant.getAddress() != null
            && isNotBlank(appellant.getAddress().getPostcode())) {

            String postCode = resolvePostCode(sscsCaseData);
            RegionalProcessingCenter newRpc = regionalProcessingCenterService.getByPostcode(postCode);

            maybeChangeIsScottish(sscsCaseData.getRegionalProcessingCenter(), newRpc, sscsCaseData);

            sscsCaseData.setRegionalProcessingCenter(newRpc);

            if (newRpc != null) {
                sscsCaseData.setRegion(newRpc.getName());
                updateProcessingVenueIfRequired(caseDetails, newRpc.getEpimsId());
            }
        }

        checkConfidentiality(sscsCaseData);
        updateCaseName(callback, sscsCaseData);
        updateCaseCategoriesIfBenefitTypeUpdated(callback, sscsCaseData, preSubmitCallbackResponse);

        final boolean hasSystemUserRole = userDetails.hasRole(SYSTEM_USER);

        updateHearingTypeForNonSscs1Case(sscsCaseData, preSubmitCallbackResponse, hasSystemUserRole);

        //validate benefit type and dwp issuing office for updateCaseData event triggered by user, which is not by CaseLoader
        if (!hasSystemUserRole) {
            validateAndUpdateDwpHandlingOffice(sscsCaseData, preSubmitCallbackResponse);
            validateHearingOptions(sscsCaseData, preSubmitCallbackResponse);
        }

        return preSubmitCallbackResponse;
    }

    private void validateAndUpdateDwpHandlingOffice(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> response) {
        MrnDetails mrnDetails = sscsCaseData.getAppeal().getMrnDetails();
        BenefitType benefitType = sscsCaseData.getAppeal().getBenefitType();
        boolean validBenefitType = validateBenefitType(benefitType,response);
        boolean validDwpIssuingOffice = validateDwpIssuingOffice(mrnDetails, benefitType, response);

        if (validBenefitType && validDwpIssuingOffice) {
            String regionalCenter = dwpAddressLookupService.getDwpRegionalCenterByBenefitTypeAndOffice(benefitType.getCode(), mrnDetails.getDwpIssuingOffice());
            sscsCaseData.setDwpRegionalCentre(regionalCenter);
        }
    }

    private void validateHearingOptions(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> response) {
        HearingOptions hearingOptions = sscsCaseData.getAppeal().getHearingOptions();
        if (hearingOptions != null
            && sscsCaseData.getAppeal().getHearingType() != null
            && HearingType.ORAL.getValue().equals(sscsCaseData.getAppeal().getHearingType())
            && Boolean.FALSE.equals(hearingOptions.isWantsToAttendHearing())) {
            response.addWarning("There is a mismatch between the hearing type and the wants to attend field, "
                + "all hearing options will be cleared please check if this is correct");
        }
    }

    private void updateHearingTypeForNonSscs1Case(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> response, boolean hasSystemUserRole) {
        if (sscsCaseData.getAppeal().getHearingType() != null
            && sscsCaseData.getAppeal().getHearingOptions() != null
            && HearingType.PAPER.getValue().equals(sscsCaseData.getAppeal().getHearingType())
            && isBenefitTypeValidForHearingTypeValidation(response.getData().getBenefitType())
            && sscsCaseData.getAppeal().getHearingOptions().isWantsToAttendHearing().equals(Boolean.TRUE)) {

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

    private boolean validateBenefitType(BenefitType benefitType, PreSubmitCallbackResponse<SscsCaseData> response) {
        if (benefitType == null || StringUtils.isEmpty(benefitType.getCode())) {
            response.addWarning("Benefit type code is empty");
            return false;
        } else if (Benefit.findBenefitByShortName(benefitType.getCode()).isEmpty()) {
            if (!caseAccessManagementFeature) {
                String validBenefitTypes = Arrays.stream(Benefit.values()).sequential().map(Benefit::getShortName).collect(Collectors.joining(", "));
                response.addWarning("Benefit type code is invalid, should be one of: " + validBenefitTypes);
            }
            return false;
        }
        return true;
    }

    private boolean validateDwpIssuingOffice(MrnDetails mrnDetails, BenefitType benefitType, PreSubmitCallbackResponse<SscsCaseData> response) {
        if (mrnDetails != null) {
            if (StringUtils.isEmpty(mrnDetails.getDwpIssuingOffice())) {
                response.addWarning("FTA issuing office is empty");
                return false;
            } else if (Benefit.findBenefitByShortName(benefitType.getCode()).isPresent()) {
                if (!dwpAddressLookupService.validateIssuingOffice(benefitType.getCode(), mrnDetails.getDwpIssuingOffice())) {
                    OfficeMapping[] officeMappings = dwpAddressLookupService.getDwpOfficeMappings(benefitType.getCode());
                    String validOffice = Arrays.stream(officeMappings).map(OfficeMapping::getCode).collect(Collectors.joining(", "));
                    response.addWarning("DWP issuing office is invalid, should one of: " + validOffice);
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    public void maybeChangeIsScottish(RegionalProcessingCenter oldRpc, RegionalProcessingCenter newRpc, SscsCaseData caseData) {
        if (oldRpc != newRpc) {
            YesNo isScottishCase = IsScottishHandler.isScottishCase(newRpc, caseData);
            caseData.setIsScottishCase(isScottishCase);
        }
    }

    private void updateProcessingVenueIfRequired(CaseDetails<SscsCaseData> caseDetails, String rpcEpimsId) {
        SscsCaseData sscsCaseData = caseDetails.getCaseData();
        String postCode = resolvePostCode(sscsCaseData);
        log.info("updateProcessingVenueIfRequired for post code " + postCode);
        String venue = airLookupService.lookupAirVenueNameByPostCode(postCode, sscsCaseData.getAppeal().getBenefitType());

        log.info("venue i s {}", venue);
        if (venue != null && !venue.equalsIgnoreCase(sscsCaseData.getProcessingVenue())) {
            log.info("Case id: {} - setting venue name to {} from {}", caseDetails.getId(), venue, sscsCaseData.getProcessingVenue());

            sscsCaseData.setProcessingVenue(venue);

            if (caseAccessManagementFeature && StringUtils.isNotEmpty(venue)) {
                CourtVenue courtVenue = refDataService.getVenueRefData(venue);
                if (courtVenue != null) {
                    sscsCaseData.setCaseManagementLocation(CaseManagementLocation.builder()
                            .baseLocation(rpcEpimsId)
                            .region(courtVenue.getRegionId()).build());
                }
            }
        }
    }

    private void updateCaseName(Callback<SscsCaseData> callback, SscsCaseData caseData) {
        if (!caseAccessManagementFeature) {
            return;
        }

        final String caseName = getCaseName(caseData.getAppeal().getAppellant());
        CaseDetails<SscsCaseData> oldCaseDetails = callback.getCaseDetailsBefore().orElse(null);

        if (oldCaseDetails != null
            && oldCaseDetails.getCaseData() != null
            && oldCaseDetails.getCaseData().getCaseAccessManagementFields() != null
            && oldCaseDetails.getCaseData().getCaseAccessManagementFields().getCaseNameHmctsInternal() != null
            && oldCaseDetails.getCaseData().getCaseAccessManagementFields().getCaseNameHmctsInternal().equals(caseName)) {
            return;
        }

        caseData.getCaseAccessManagementFields().setCaseNames(caseName);
    }

    private String getCaseName(Appellant appellant) {
        if (appellant != null
            && appellant.getName() != null) {
            return appellant.getName().getFullNameNoTitle();
        }
        return null;
    }

    private void updateCaseCategoriesIfBenefitTypeUpdated(Callback<SscsCaseData> callback,
                                                          SscsCaseData sscsCaseData,
                                                          PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        if (!caseAccessManagementFeature) {
            return;
        }

        Optional<Benefit> benefit = sscsCaseData.getBenefitType();

        CaseDetails<SscsCaseData> oldCaseDetails = callback.getCaseDetailsBefore().orElse(null);
        Optional<Benefit> oldBenefit = getOldBenefitCode(oldCaseDetails);

        if (benefit.isPresent()) {
            sscsCaseData.getCaseAccessManagementFields().setCategories(benefit.get());

        } else if (benefitCodeHasValue(sscsCaseData.getAppeal())) {
            String validBenefitTypes = Arrays.stream(Benefit.values())
                .map(Benefit::getShortName)
                .collect(Collectors.joining(", "));
            preSubmitCallbackResponse.addError("Benefit type code is invalid, should be one of: " + validBenefitTypes);

        } else if (oldBenefit.isPresent()) {
            preSubmitCallbackResponse.addError("Benefit type code is empty");
        }
    }

    private boolean benefitCodeHasValue(Appeal appeal) {
        return appeal != null
                && appeal.getBenefitType() != null
                && appeal.getBenefitType().getCode() != null
                && !isEmpty(appeal.getBenefitType().getCode());
    }

    private Optional<Benefit> getOldBenefitCode(CaseDetails<SscsCaseData> oldCaseDetails) {
        if (oldCaseDetails == null || oldCaseDetails.getCaseData() == null
                || oldCaseDetails.getCaseData().getBenefitType().isEmpty()) {
            return Optional.empty();
        } else {
            return oldCaseDetails.getCaseData().getBenefitType();
        }
    }

    private String resolvePostCode(SscsCaseData sscsCaseData) {
        String postCode;

        if (isYes(sscsCaseData.getAppeal().getAppellant().getIsAppointee())) {
            postCode = Optional.ofNullable(sscsCaseData.getAppeal().getAppellant().getAppointee())
                .map(Appointee::getAddress)
                .map(Address::getPostcode)
                .filter(appointeePostCode -> !"".equals(appointeePostCode))
                .orElse(sscsCaseData.getAppeal().getAppellant().getAddress().getPostcode());
        } else {
            postCode = sscsCaseData.getAppeal().getAppellant().getAddress().getPostcode();
        }

        return postCode;
    }

}
