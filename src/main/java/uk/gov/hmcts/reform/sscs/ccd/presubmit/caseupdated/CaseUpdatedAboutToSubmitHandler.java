package uk.gov.hmcts.reform.sscs.ccd.presubmit.caseupdated;

import static java.util.Objects.*;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.helper.SscsHelper.validateHearingOptionsAndExcludeDates;
import static uk.gov.hmcts.reform.sscs.idam.UserRole.*;
import static uk.gov.hmcts.reform.sscs.idam.UserRole.SUPER_USER;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.isConfidential;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.handleBenefitType;

import java.util.*;
import java.util.stream.Collectors;
import javax.validation.ConstraintValidatorContext;
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
import uk.gov.hmcts.reform.sscs.ccd.validation.address.PostcodeValidator;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.model.CourtVenue;
import uk.gov.hmcts.reform.sscs.model.dwp.OfficeMapping;
import uk.gov.hmcts.reform.sscs.reference.data.service.SessionCategoryMapService;
import uk.gov.hmcts.reform.sscs.service.AirLookupService;
import uk.gov.hmcts.reform.sscs.service.DwpAddressLookupService;
import uk.gov.hmcts.reform.sscs.service.RefDataService;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;
import uk.gov.hmcts.reform.sscs.service.VenueService;

@Component
@Slf4j
public class CaseUpdatedAboutToSubmitHandler extends ResponseEventsAboutToSubmit implements PreSubmitCallbackHandler<SscsCaseData> {

    private final RegionalProcessingCenterService regionalProcessingCenterService;
    private final AssociatedCaseLinkHelper associatedCaseLinkHelper;
    private final AirLookupService airLookupService;
    private final DwpAddressLookupService dwpAddressLookupService;
    private final IdamService idamService;
    private final RefDataService refDataService;
    private final VenueService venueService;
    private final SessionCategoryMapService categoryMapService;
    private final boolean caseAccessManagementFeature;
    private final PostcodeValidator postcodeValidator = new PostcodeValidator();
    private static ConstraintValidatorContext context;


    private static final String WARNING_MESSAGE = "%s has not been provided for the %s, do you want to ignore this warning and proceed?";

    private static final String ERROR_MESSAGE = "%s has not been provided for the %s";

    private static final String REP_ERROR_MESSAGE = "Name/Organisation has not been provided for the Representative";

    private static final String FIRST_NAME = "First Name";

    private static final String LAST_NAME = "Last Name";


    @SuppressWarnings("squid:S107")
    CaseUpdatedAboutToSubmitHandler(RegionalProcessingCenterService regionalProcessingCenterService,
                                    AssociatedCaseLinkHelper associatedCaseLinkHelper,
                                    AirLookupService airLookupService,
                                    DwpAddressLookupService dwpAddressLookupService,
                                    IdamService idamService,
                                    RefDataService refDataService,
                                    VenueService venueService,
                                    SessionCategoryMapService categoryMapService,
                                    @Value("${feature.case-access-management.enabled}")  boolean caseAccessManagementFeature) {
        this.regionalProcessingCenterService = regionalProcessingCenterService;
        this.associatedCaseLinkHelper = associatedCaseLinkHelper;
        this.airLookupService = airLookupService;
        this.dwpAddressLookupService = dwpAddressLookupService;
        this.idamService = idamService;
        this.refDataService = refDataService;
        this.caseAccessManagementFeature = caseAccessManagementFeature;
        this.venueService = venueService;
        this.categoryMapService = categoryMapService;
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

        handleBenefitType(sscsCaseData);

        if (isNotEmpty(sscsCaseData.getBenefitCode())) {
            validateBenefitIssueCode(sscsCaseData, preSubmitCallbackResponse);
        }
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

        sscsCaseData.setIsConfidentialCase(isConfidential(sscsCaseData));
        updateCaseName(callback, sscsCaseData);
        updateCaseCategoriesIfBenefitTypeUpdated(callback, sscsCaseData, preSubmitCallbackResponse);
        updateLanguage(sscsCaseData);

        final boolean hasSystemUserRole = userDetails.hasRole(SYSTEM_USER);

        updateHearingTypeForNonSscs1Case(sscsCaseData, preSubmitCallbackResponse, hasSystemUserRole);

        YesNo isJointPartyAddressSameAsAppellant = sscsCaseData.getJointParty().getJointPartyAddressSameAsAppellant();
        if (sscsCaseData.isThereAJointParty() && !Objects.isNull(isJointPartyAddressSameAsAppellant) && isJointPartyAddressSameAsAppellant.toBoolean()) {
            sscsCaseData.getJointParty().setAddress(sscsCaseData.getAppeal().getAppellant().getAddress());
        }

        //validate benefit type and dwp issuing office for updateCaseData event triggered by user, which is not by CaseLoader
        if (!hasSystemUserRole) {
            validateAndUpdateDwpHandlingOffice(sscsCaseData, preSubmitCallbackResponse);
            validateHearingOptions(sscsCaseData, preSubmitCallbackResponse);
            validatingPartyAddresses(sscsCaseData, preSubmitCallbackResponse);
            validateAppellantCaseData(sscsCaseData, preSubmitCallbackResponse);
            validateAppointeeCaseData(sscsCaseData, preSubmitCallbackResponse);
            validateRepresentativeNameData(sscsCaseData, preSubmitCallbackResponse);
            validateJointPartyNameData(sscsCaseData, preSubmitCallbackResponse);
        }

        return preSubmitCallbackResponse;
    }

    private void updateLanguage(SscsCaseData sscsCaseData) {
        HearingOptions hearingOptions = sscsCaseData.getAppeal().getHearingOptions();
        if (nonNull(hearingOptions)) {
            String language = null;

            if (isYes(hearingOptions.getLanguageInterpreter())) {
                DynamicList languageList = hearingOptions.getLanguagesList();

                if (nonNull(languageList)) {
                    DynamicListItem selectedValue = languageList.getValue();

                    if (nonNull(selectedValue)) {
                        language = selectedValue.getLabel();
                    }
                }
            }

            hearingOptions.setLanguages(language);
        }
    }

    private void validateBenefitIssueCode(SscsCaseData caseData,
                                          PreSubmitCallbackResponse<SscsCaseData> response) {
        boolean isSecondDoctorPresent = isNotBlank(caseData.getSscsIndustrialInjuriesData().getSecondPanelDoctorSpecialism());
        boolean fqpmRequired = isYes(caseData.getIsFqpmRequired());

        if (isNull(categoryMapService.getSessionCategory(caseData.getBenefitCode(), caseData.getIssueCode(),
                isSecondDoctorPresent, fqpmRequired))) {
            response.addError("Incorrect benefit/issue code combination");
        }
    }

    private void validatingPartyAddresses(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> response) {
        validateAddressAndPostcode(response, sscsCaseData.getAppeal().getAppellant(), "appellant");

        if (sscsCaseData.isThereAJointParty()) {
            YesNo isJointPartyAddressSameAsAppellant = sscsCaseData.getJointParty().getJointPartyAddressSameAsAppellant();
            if (Objects.isNull(isJointPartyAddressSameAsAppellant) || !isJointPartyAddressSameAsAppellant.toBoolean()) {
                validateAddressAndPostcode(response, sscsCaseData.getJointParty(), "joint party");
            }
        }

        String isAppointee = sscsCaseData.getAppeal().getAppellant().getIsAppointee();
        if (isYes(isAppointee)) {
            validateAddressAndPostcode(response, sscsCaseData.getAppeal().getAppellant().getAppointee(), "appointee");
        }

        if (sscsCaseData.isThereARepresentative()) {
            validateAddressAndPostcode(response, sscsCaseData.getAppeal().getRep(), "representative");
        }
    }

    private void validateAddressAndPostcode(PreSubmitCallbackResponse<SscsCaseData> response, Entity party, String partyName) {
        if (isNull(party.getAddress())) {
            response.addError("You must enter address line 1 for the " + partyName);
            response.addError("You must enter a valid UK postcode for the " + partyName);
        } else {
            String addressLine1 = party.getAddress().getLine1();
            String postcode = party.getAddress().getPostcode();

            if (isBlank(addressLine1)) {
                response.addError("You must enter address line 1 for the " + partyName);
            }

            if (isBlank(postcode) || !postcodeValidator.isValid(postcode, context)) {
                response.addError("You must enter a valid UK postcode for the " + partyName);
            }
        }
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

    private boolean hasValidHearingOptionsAndWantsToExcludeDates(HearingOptions hearingOptions) {
        return hearingOptions != null
            && isYes(hearingOptions.getWantsToAttend())
            && isYes(hearingOptions.getScheduleHearing());
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

        if (hasValidHearingOptionsAndWantsToExcludeDates(hearingOptions)) {
            response.addErrors(
                    validateHearingOptionsAndExcludeDates(hearingOptions.getExcludeDates())
            );
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
        if (benefitType == null || isEmpty(benefitType.getCode())) {
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
            if (isEmpty(mrnDetails.getDwpIssuingOffice())) {
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
            String isScottishCase = IsScottishHandler.isScottishCase(newRpc, caseData);
            caseData.setIsScottishCase(isScottishCase);
        }
    }

    private void updateProcessingVenueIfRequired(CaseDetails<SscsCaseData> caseDetails, String rpcEpimsId) {
        SscsCaseData sscsCaseData = caseDetails.getCaseData();
        String postCode = resolvePostCode(sscsCaseData);
        log.info("Checking whether processing venue requires updating for post code {}, case {}", postCode, caseDetails.getId());

        String venue = airLookupService.lookupAirVenueNameByPostCode(postCode, sscsCaseData.getAppeal().getBenefitType());

        if (venue != null && !venue.equalsIgnoreCase(sscsCaseData.getProcessingVenue())) {
            log.info("Processing venue requires updating for case {}: setting venue name to {} from {}", caseDetails.getId(), venue,
                sscsCaseData.getProcessingVenue());

            sscsCaseData.setProcessingVenue(venue);

            if (caseAccessManagementFeature && isNotEmpty(venue)) {
                String venueEpimsId = venueService.getEpimsIdForVenue(venue);
                CourtVenue courtVenue = refDataService.getCourtVenueRefDataByEpimsId(venueEpimsId);

                sscsCaseData.setCaseManagementLocation(CaseManagementLocation.builder()
                    .baseLocation(rpcEpimsId)
                    .region(courtVenue.getRegionId()).build());

                log.info("Successfully updated case management location details for case {}. Processing venue {}, epimsId {}",
                    caseDetails.getId(), venue, venueEpimsId);

            }
        } else {
            log.info("Processing venue has not changed or is null, skipping update for case {}, venue: {}",
                caseDetails.getId(), venue);
        }
    }

    private List<String> validatePartyCaseData(Entity entity, String partyType) {
        List<String> listOfWarnings = new ArrayList<>();

        if (entity != null) {
            if (entity.getName() != null) {
                if (isBlank(entity.getName().getFirstName())) {
                    listOfWarnings.add(String.format(WARNING_MESSAGE, FIRST_NAME, partyType));
                }
                if (isBlank(entity.getName().getLastName())) {
                    listOfWarnings.add(String.format(WARNING_MESSAGE, LAST_NAME, partyType));
                }
            }
            if (entity.getIdentity() != null) {
                if (isBlank(entity.getIdentity().getDob())) {
                    listOfWarnings.add(String.format(WARNING_MESSAGE, "Date of Birth", partyType));
                }
                if (isBlank(entity.getIdentity().getNino())) {
                    listOfWarnings.add(String.format(WARNING_MESSAGE, "National Insurance Number", partyType));
                }
            }
        }
        return listOfWarnings;
    }


    private void validateAppellantCaseData(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> response) {
        Appellant appellantInfo = sscsCaseData.getAppeal().getAppellant();

        List<String> warnings = validatePartyCaseData(appellantInfo, "Appellant");

        if (!warnings.isEmpty()) {
            response.addWarnings(warnings);
        }
    }

    private void validateAppointeeCaseData(SscsCaseData sscsCaseData, PreSubmitCallbackResponse response) {
        Appointee appointeeInfo = sscsCaseData.getAppeal().getAppellant().getAppointee();
        String isAppointee = sscsCaseData.getAppeal().getAppellant().getIsAppointee();

        if (isAppointee != null && isAppointee.equals("Yes") && appointeeInfo != null) {
            List<String> warnings = validatePartyCaseData(appointeeInfo, "Appointee");

            if (!warnings.isEmpty()) {
                response.addWarnings(warnings);
            }
        }
    }

    private List<String> validateRepAndJointPartyCaseData(Entity entity, String entityType) {
        List<String> listOfErrors = new ArrayList<>();

        if (entity != null && entity.getName() != null) {
            if (isBlank(entity.getName().getFirstName())) {
                listOfErrors.add(String.format(ERROR_MESSAGE, FIRST_NAME, entityType));
            }
            if (isBlank(entity.getName().getLastName())) {
                listOfErrors.add(String.format(ERROR_MESSAGE, LAST_NAME, entityType));
            }
        } else {
            if (entityType.equals("Representative")) {
                listOfErrors.add(REP_ERROR_MESSAGE);
            } else {
                listOfErrors.add(String.format(ERROR_MESSAGE, FIRST_NAME, entityType));
                listOfErrors.add(String.format(ERROR_MESSAGE, LAST_NAME, entityType));
            }
        }
        return listOfErrors;
    }

    private void validateRepresentativeNameData(SscsCaseData sscsCaseData, PreSubmitCallbackResponse response) {
        final boolean hasRepresentative = sscsCaseData.isThereARepresentative();
        if (hasRepresentative) {
            Representative representativeInfo = sscsCaseData.getAppeal().getRep();
            if (isBlank(representativeInfo.getOrganisation())) {
                List<String> warnings = validateRepAndJointPartyCaseData(representativeInfo, "Representative");
                if (!warnings.isEmpty()) {
                    response.addErrors(warnings);
                }
            }
        }
    }

    private void validateJointPartyNameData(SscsCaseData sscsCaseData, PreSubmitCallbackResponse response) {
        JointParty jointPartyInfo = sscsCaseData.getJointParty();
        final boolean hasJointParty = sscsCaseData.isThereAJointParty();

        if (hasJointParty) {
            List<String> warnings = validateRepAndJointPartyCaseData(jointPartyInfo, "Joint Party");
            if (!warnings.isEmpty()) {
                response.addErrors(warnings);
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

    private static String resolvePostCode(SscsCaseData sscsCaseData) {
        if (YES.getValue().equalsIgnoreCase(sscsCaseData.getAppeal().getAppellant().getIsAppointee())) {
            return Optional.ofNullable(sscsCaseData.getAppeal().getAppellant().getAppointee())
                .map(Appointee::getAddress)
                .map(Address::getPostcode)
                .map(String::trim)
                .filter(StringUtils::isNotEmpty)
                .orElse(sscsCaseData.getAppeal().getAppellant().getAddress().getPostcode());
        }

        return sscsCaseData.getAppeal().getAppellant().getAddress().getPostcode();
    }

}
