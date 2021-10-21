package uk.gov.hmcts.reform.sscs.ccd.presubmit.caseupdated;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.sscs.idam.UserRole.SYSTEM_USER;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
import uk.gov.hmcts.reform.sscs.model.dwp.OfficeMapping;
import uk.gov.hmcts.reform.sscs.service.AirLookupService;
import uk.gov.hmcts.reform.sscs.service.DwpAddressLookupService;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;

@Component
@Slf4j
public class CaseUpdatedAboutToSubmitHandler extends ResponseEventsAboutToSubmit implements PreSubmitCallbackHandler<SscsCaseData> {

    private final RegionalProcessingCenterService regionalProcessingCenterService;
    private final AssociatedCaseLinkHelper associatedCaseLinkHelper;
    private final AirLookupService airLookupService;
    private final DwpAddressLookupService dwpAddressLookupService;
    private IdamService idamService;

    @Autowired
    CaseUpdatedAboutToSubmitHandler(RegionalProcessingCenterService regionalProcessingCenterService,
                                    AssociatedCaseLinkHelper associatedCaseLinkHelper,
                                    AirLookupService airLookupService,
                                    DwpAddressLookupService dwpAddressLookupService,
                                    IdamService idamService) {
        this.regionalProcessingCenterService = regionalProcessingCenterService;
        this.associatedCaseLinkHelper = associatedCaseLinkHelper;
        this.airLookupService = airLookupService;
        this.dwpAddressLookupService = dwpAddressLookupService;
        this.idamService = idamService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

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

        setCaseCode(preSubmitCallbackResponse, callback);

        if (sscsCaseData.getAppeal().getAppellant() != null
                && sscsCaseData.getAppeal().getAppellant().getAddress() != null
                && isNotBlank(sscsCaseData.getAppeal().getAppellant().getAddress().getPostcode())) {

            RegionalProcessingCenter newRpc =
                    regionalProcessingCenterService.getByPostcode(sscsCaseData.getAppeal().getAppellant().getAddress().getPostcode());

            maybeChangeIsScottish(sscsCaseData.getRegionalProcessingCenter(), newRpc, sscsCaseData);

            sscsCaseData.setRegionalProcessingCenter(newRpc);

            if (newRpc != null) {
                sscsCaseData.setRegion(newRpc.getName());
            }

            updateProcessingVenueIfRequired(caseDetails);

        }

        final UserDetails userDetails = idamService.getUserDetails(userAuthorisation);
        final boolean hasSystemUserRole = userDetails.hasRole(SYSTEM_USER);

        //validate benefit type and dwp issuing office for updateCaseData event triggered by user, which is not by CaseLoader
        if (!hasSystemUserRole) {
            validateAndUpdateDwpHandlingOffice(sscsCaseData,preSubmitCallbackResponse);
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

    private boolean validateBenefitType(BenefitType benefitType, PreSubmitCallbackResponse<SscsCaseData> response) {
        if (benefitType == null || StringUtils.isEmpty(benefitType.getCode())) {
            response.addWarning("Benefit type code is empty");
            return false;
        } else if (Benefit.findBenefitByShortName(benefitType.getCode()).isEmpty()) {
            String validBenefitTypes = Arrays.stream(Benefit.values()).sequential().map(Benefit::getShortName).collect(Collectors.joining(", "));
            response.addWarning("Benefit type code is invalid, should be one of: " + validBenefitTypes);
            return false;
        }
        return true;
    }

    private boolean validateDwpIssuingOffice(MrnDetails mrnDetails, BenefitType benefitType, PreSubmitCallbackResponse<SscsCaseData> response) {
        if (mrnDetails != null) {
            if (StringUtils.isEmpty(mrnDetails.getDwpIssuingOffice())) {
                response.addWarning("DWP issuing office is empty");
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
            String isScottishCase = IsScottishHandler.isScottishCase(newRpc, caseData);
            caseData.setIsScottishCase(isScottishCase);
        }
    }

    private void updateProcessingVenueIfRequired(CaseDetails<SscsCaseData> caseDetails) {

        SscsCaseData sscsCaseData = caseDetails.getCaseData();

        String postCode = "yes".equalsIgnoreCase(sscsCaseData.getAppeal().getAppellant().getIsAppointee())
            && null != sscsCaseData.getAppeal().getAppellant().getAppointee()
            && null != sscsCaseData.getAppeal().getAppellant().getAppointee().getAddress()
            && null != sscsCaseData.getAppeal().getAppellant().getAppointee().getAddress().getPostcode()
                ? sscsCaseData.getAppeal().getAppellant().getAppointee().getAddress().getPostcode()
                : sscsCaseData.getAppeal().getAppellant().getAddress().getPostcode();

        String venue = airLookupService.lookupAirVenueNameByPostCode(postCode, sscsCaseData.getAppeal().getBenefitType());

        if (venue != null && !venue.equalsIgnoreCase(sscsCaseData.getProcessingVenue())) {
            log.info("Case id: {} - setting venue name to {} from {}", caseDetails.getId(), venue, sscsCaseData.getProcessingVenue());

            sscsCaseData.setProcessingVenue(venue);
        }

    }

}
