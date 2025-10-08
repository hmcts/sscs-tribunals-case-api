package uk.gov.hmcts.reform.sscs.bulkscan.validators;

import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.*;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.WarningMessage.getMessageByCallbackType;
import static uk.gov.hmcts.reform.sscs.bulkscan.domain.CallbackType.EXCEPTION_CALLBACK;
import static uk.gov.hmcts.reform.sscs.bulkscan.domain.CallbackType.VALIDATION_CALLBACK;
import static uk.gov.hmcts.reform.sscs.bulkscan.helper.SscsDataHelper.getValidationStatus;
import static uk.gov.hmcts.reform.sscs.bulkscan.util.SscsOcrDataUtil.extractBooleanValueWarning;
import static uk.gov.hmcts.reform.sscs.bulkscan.util.SscsOcrDataUtil.findBooleanExists;
import static uk.gov.hmcts.reform.sscs.bulkscan.util.SscsOcrDataUtil.getBoolean;
import static uk.gov.hmcts.reform.sscs.bulkscan.util.SscsOcrDataUtil.getField;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_APPEAL;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain.ExceptionRecord;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain.ScannedData;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.validators.CaseValidator;
import uk.gov.hmcts.reform.sscs.bulkscan.constants.WarningMessage;
import uk.gov.hmcts.reform.sscs.bulkscan.domain.CallbackType;
import uk.gov.hmcts.reform.sscs.bulkscan.json.SscsJsonExtractor;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.AppealReasons;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.AppellantRole;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.Contact;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.ExcludeDate;
import uk.gov.hmcts.reform.sscs.ccd.domain.FormType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingSubtype;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.Role;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.UkPortOfEntry;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.domain.CaseResponse;
import uk.gov.hmcts.reform.sscs.model.dwp.OfficeMapping;
import uk.gov.hmcts.reform.sscs.service.DwpAddressLookupService;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;

@Component
@Slf4j
public class SscsCaseValidator implements CaseValidator {

    public static final String IS_NOT_A_VALID_POSTCODE = "is not a valid postcode";

    @SuppressWarnings("squid:S5843")
    private static final String PHONE_REGEX =
        "^((?:(?:\\(?(?:0(?:0|11)\\)?[\\s-]?\\(?|\\+)\\d{1,4}\\)?[\\s-]?(?:\\(?0\\)?[\\s-]?)?)|(?:\\(?0))(?:"
            + "(?:\\d{5}\\)?[\\s-]?\\d{4,5})|(?:\\d{4}\\)?[\\s-]?(?:\\d{5}|\\d{3}[\\s-]?\\d{3}))|(?:\\d{3}\\)"
            + "?[\\s-]?\\d{3}[\\s-]?\\d{3,4})|(?:\\d{2}\\)?[\\s-]?\\d{4}[\\s-]?\\d{4}))(?:[\\s-]?(?:x|ext\\.?|\\#)"
            + "\\d{3,4})?)?$";

    @SuppressWarnings("squid:S5843")
    private static final String UK_NUMBER_REGEX =
        "^\\(?(?:(?:0(?:0|11)\\)?[\\s-]?\\(?|\\+)44\\)?[\\s-]?\\(?(?:0\\)?[\\s-]?\\(?)?|0)(?:\\d{2}\\)?[\\s-]?\\d{4}"
            + "[\\s-]?\\d{4}|\\d{3}\\)?[\\s-]?\\d{3}[\\s-]?\\d{3,4}|\\d{4}\\)?[\\s-]?(?:\\d{5}|\\d{3}[\\s-]?\\d{3})|"
            +
            "\\d{5}\\)?[\\s-]?\\d{4,5}|8(?:00[\\s-]?11[\\s-]?11|45[\\s-]?46[\\s-]?4\\d))(?:(?:[\\s-]?(?:x|ext\\.?\\s?|"
            + "\\#)\\d+)?)$";

    @SuppressWarnings("squid:S5843")
    private static final String ADDRESS_REGEX =
        "^[a-zA-ZÀ-ž0-9]{1}[a-zA-ZÀ-ž0-9 \\r\\n\\.“”\",’\\?\\!\\[\\]\\(\\)/£:\\\\_+\\-%&;]{1,}$";

    @SuppressWarnings("squid:S5843")
    private static final String COUNTY_REGEX =
        "^\\.$|^[a-zA-ZÀ-ž0-9]{1}[a-zA-ZÀ-ž0-9 \\r\\n\\.“”\",’\\?\\!\\[\\]\\(\\)/£:\\\\_+\\-%&;]{1,}$";

    private final RegionalProcessingCenterService regionalProcessingCenterService;
    private final DwpAddressLookupService dwpAddressLookupService;
    private final PostcodeValidator postcodeValidator;
    private final SscsJsonExtractor sscsJsonExtractor;
    List<String> warnings;
    List<String> errors;
    private CallbackType callbackType;
    @Value("#{'${validation.titles}'.split(',')}")
    private List<String> titles;

    //TODO: Remove when uc-office-feature switched on
    @Setter
    private boolean ucOfficeFeatureActive;

    public SscsCaseValidator(RegionalProcessingCenterService regionalProcessingCenterService,
                             DwpAddressLookupService dwpAddressLookupService,
                             PostcodeValidator postcodeValidator,
                             SscsJsonExtractor sscsJsonExtractor,
                             @Value("${feature.uc-office-feature.enabled}") boolean ucOfficeFeatureActive) {
        this.regionalProcessingCenterService = regionalProcessingCenterService;
        this.dwpAddressLookupService = dwpAddressLookupService;
        this.postcodeValidator = postcodeValidator;
        this.sscsJsonExtractor = sscsJsonExtractor;
        this.ucOfficeFeatureActive = ucOfficeFeatureActive;
    }

    private List<String> combineWarnings() {
        List<String> mergedWarnings = new ArrayList<>();

        mergedWarnings.addAll(warnings);
        mergedWarnings.addAll(errors);
        errors.clear();

        return mergedWarnings;
    }

    @Override
    public CaseResponse validateExceptionRecord(CaseResponse transformResponse, ExceptionRecord exceptionRecord,
                                                Map<String, Object> caseData, boolean combineWarnings) {
        warnings =
            transformResponse != null && transformResponse.getWarnings() != null ? transformResponse.getWarnings() :
                new ArrayList<>();
        errors = new ArrayList<>();
        callbackType = EXCEPTION_CALLBACK;

        ScannedData ocrCaseData = sscsJsonExtractor.extractJson(exceptionRecord);

        boolean ignoreWarningsValue = exceptionRecord.getIgnoreWarnings() != null ? exceptionRecord.getIgnoreWarnings() : false;
        validateAppeal(ocrCaseData.getOcrCaseData(), caseData, false, ignoreWarningsValue, true, null);

        if (combineWarnings) {
            warnings = combineWarnings();
        }

        return CaseResponse.builder()
            .errors(errors)
            .warnings(warnings)
            .transformedCase(caseData)
            .status(getValidationStatus(errors, warnings))
            .build();
    }

    @Override
    public CaseResponse validateValidationRecord(Map<String, Object> caseData, boolean ignoreMrnValidation) {
        return validateValidationRecord(caseData, ignoreMrnValidation, null);
    }

    @Override
    public CaseResponse validateValidationRecord(Map<String, Object> caseData, boolean ignoreMrnValidation, EventType eventType) {
        warnings = new ArrayList<>();
        errors = new ArrayList<>();
        callbackType = VALIDATION_CALLBACK;

        Map<String, Object> ocrCaseData = new HashMap<>();

        validateAppeal(ocrCaseData, caseData, ignoreMrnValidation, false, false, eventType);

        return CaseResponse.builder()
            .errors(errors)
            .warnings(warnings)
            .transformedCase(caseData)
            .build();
    }

    private List<String> validateAppeal(Map<String, Object> ocrCaseData, Map<String, Object> caseData,
                                        boolean ignoreMrnValidation, boolean ignoreWarnings, boolean ignorePartyRoleValidation, EventType eventType) {

        FormType formType = (FormType) caseData.get("formType");
        Appeal appeal = (Appeal) caseData.get("appeal");
        String appellantPersonType = getPerson1OrPerson2(appeal.getAppellant());

        final boolean isIbcOrSscs8 = isIbcOrSscs8(formType,
            Optional.ofNullable(appeal.getBenefitType()).orElse(BenefitType.builder().build()).getCode());

        boolean validateIbcRoleField = FormType.SSCS8.equals(formType) && VALID_APPEAL.equals(eventType);
        checkAppellant(
            appeal,
            ocrCaseData,
            caseData,
            appellantPersonType,
            formType,
            validateIbcRoleField,
            ignorePartyRoleValidation,
            ignoreWarnings,
            isIbcOrSscs8
        );
        boolean isFromValidateAppealEvent = VALID_APPEAL.equals(eventType);
        if (isIbcOrSscs8 && !isFromValidateAppealEvent) {
            checkAppealReasons(appeal, ocrCaseData);
        }

        checkRepresentative(appeal, ocrCaseData, caseData, isIbcOrSscs8);
        checkMrnDetails(appeal, ocrCaseData, ignoreMrnValidation, formType);

        if (formType != null && formType.equals(FormType.SSCS2)) {
            checkChildMaintenance(caseData, ignoreWarnings);

            checkOtherParty(caseData, ignoreWarnings);
        }

        checkExcludedDates(appeal);

        isBenefitTypeValid(appeal, formType);

        isHearingTypeValid(appeal);

        checkHearingSubTypeIfHearingIsOral(appeal, caseData);

        if (caseData.get("sscsDocument") != null) {
            @SuppressWarnings("unchecked")
            List<SscsDocument> lists = ((List<SscsDocument>) caseData.get("sscsDocument"));
            checkAdditionalEvidence(lists);
        }

        return warnings;
    }

    private void checkChildMaintenance(Map<String, Object> caseData, boolean ignoreWarnings) {
        String childMaintenanceNumber = (String) caseData.get("childMaintenanceNumber");
        if (!ignoreWarnings && StringUtils.isBlank(childMaintenanceNumber)) {
            warnings.add(getMessageByCallbackType(callbackType, "", PERSON_1_CHILD_MAINTENANCE_NUMBER, IS_EMPTY));
        } else if (ignoreWarnings) {
            caseData.remove("childMaintenanceNumber");
        }
    }

    private void checkAdditionalEvidence(List<SscsDocument> sscsDocuments) {
        sscsDocuments.stream().filter(sscsDocument -> sscsDocument.getValue().getDocumentFileName() == null)
            .forEach(sscsDocument -> {
                errors.add(
                    "There is a file attached to the case that does not have a filename, add a filename, e.g. filename.pdf");
            });

        sscsDocuments.stream().filter(sscsDocument -> sscsDocument.getValue().getDocumentLink() != null
                && sscsDocument.getValue().getDocumentLink().getDocumentFilename() != null
                && sscsDocument.getValue().getDocumentLink().getDocumentFilename().indexOf('.') == -1)
            .forEach(sscsDocument -> {
                errors.add("There is a file attached to the case called "
                    + sscsDocument.getValue().getDocumentLink().getDocumentFilename()
                    + ", filenames must have extension, e.g. filename.pdf");
            });
    }

    private void checkAppellant(Appeal appeal, Map<String, Object> ocrCaseData, Map<String, Object> caseData,
                                String personType, FormType formType, boolean validateIbcRoleField,
                                boolean ignorePartyRoleValidation, boolean ignoreWarnings, boolean isIbcOrSscs8) {
        Appellant appellant = appeal.getAppellant();

        if (appellant == null) {
            warnings.add(
                getMessageByCallbackType(callbackType, personType, getWarningMessageName(personType, appellant) + TITLE,
                    IS_EMPTY));
            warnings.add(getMessageByCallbackType(callbackType, personType,
                getWarningMessageName(personType, appellant) + FIRST_NAME, IS_EMPTY));
            warnings.add(getMessageByCallbackType(callbackType, personType,
                getWarningMessageName(personType, appellant) + LAST_NAME, IS_EMPTY));
            warnings.add(getMessageByCallbackType(callbackType, personType,
                getWarningMessageName(personType, appellant) + ADDRESS_LINE1, IS_EMPTY));
            warnings.add(getMessageByCallbackType(callbackType, personType,
                getWarningMessageName(personType, appellant) + ADDRESS_LINE3, IS_EMPTY));
            warnings.add(getMessageByCallbackType(callbackType, personType,
                getWarningMessageName(personType, appellant) + ADDRESS_LINE4, IS_EMPTY));
            warnings.add(getMessageByCallbackType(callbackType, personType,
                getWarningMessageName(personType, appellant) + ADDRESS_POSTCODE, IS_EMPTY));
            warnings.add(
                getMessageByCallbackType(callbackType, personType, getWarningMessageName(personType, appellant) + (isIbcOrSscs8 ? IBCA_REFERENCE : NINO),
                    IS_EMPTY));
        } else {
            checkAppointee(appellant, ocrCaseData, caseData, isIbcOrSscs8);

            checkPersonName(appellant.getName(), personType, appellant, isIbcOrSscs8);

            checkPersonAddressAndDob(appellant.getAddress(), appellant.getIdentity(), personType, ocrCaseData, caseData,
                appellant, isIbcOrSscs8);

            if (isIbcOrSscs8) {
                checkAppellantIbcaReference(appellant, personType);
            } else {
                checkAppellantNino(appellant, personType);
            }

            if (FormType.SSCS8.equals(formType)) {
                checkIbcRole(personType, ocrCaseData, appellant, validateIbcRoleField);
            }

            checkMobileNumber(appellant.getContact(), personType);

            checkHearingSubtypeDetails(appeal.getHearingSubtype());
            if (!ignorePartyRoleValidation && formType != null && formType.equals(FormType.SSCS2)) {
                checkAppellantRole(appellant.getRole(), ignoreWarnings);
            }
        }
    }

    private void checkAppealReasons(Appeal appeal, Map<String, Object> ocrCaseData) {
        AppealReasons appealReasons = appeal.getAppealReasons();
        if (appealReasons == null || appealReasons.getReasons() == null || appealReasons.getReasons().isEmpty()) {
            warnings.add(getMessageByCallbackType(callbackType, "", APPEAL_GROUNDS,
                IS_EMPTY));
        } else if (!ocrCaseData.isEmpty()) {
            boolean appealReasonBool = getBoolean(ocrCaseData, Collections.emptySet(), APPEAL_GROUNDS)
                || getBoolean(ocrCaseData, Collections.emptySet(), APPEAL_GROUNDS_2);
            if (!appealReasonBool) {
                warnings.add(getMessageByCallbackType(callbackType, "", APPEAL_GROUNDS,
                    IS_MISSING));
            }
        }
    }

    private void checkAppellantRole(Role role, boolean ignoreWarnings) {
        if (role == null && !ignoreWarnings) {
            warnings.add(getMessageByCallbackType(callbackType, "", WarningMessage.APPELLANT_PARTY_NAME.toString(),
                IS_MISSING));
        } else if (!ignoreWarnings) {
            String name = role.getName();
            String description = role.getDescription();
            if (StringUtils.isEmpty(name)) {
                warnings.add(getMessageByCallbackType(callbackType, "", WarningMessage.APPELLANT_PARTY_NAME.toString(),
                    IS_MISSING));
            } else if (AppellantRole.OTHER.getName().equalsIgnoreCase(name) && StringUtils.isEmpty(description)) {
                warnings.add(getMessageByCallbackType(callbackType, "", WarningMessage.APPELLANT_PARTY_DESCRIPTION.toString(),
                    IS_MISSING));
            }
        }
    }

    private void checkHearingSubtypeDetails(HearingSubtype hearingSubtype) {
        if (hearingSubtype != null) {
            if (YES_LITERAL.equals(hearingSubtype.getWantsHearingTypeTelephone())
                && hearingSubtype.getHearingTelephoneNumber() == null) {

                warnings.add(getMessageByCallbackType(callbackType, "", HEARING_TYPE_TELEPHONE_LITERAL,
                    PHONE_SELECTED_NOT_PROVIDED));

            } else if (hearingSubtype.getHearingTelephoneNumber() != null
                && !isUkNumberValid(hearingSubtype.getHearingTelephoneNumber())) {

                warnings
                    .add(getMessageByCallbackType(callbackType, "", HEARING_TELEPHONE_NUMBER_MULTIPLE_LITERAL, null));
            }

            if (YES_LITERAL.equals(hearingSubtype.getWantsHearingTypeVideo())
                && hearingSubtype.getHearingVideoEmail() == null) {

                warnings.add(getMessageByCallbackType(callbackType, "", HEARING_TYPE_VIDEO_LITERAL,
                    EMAIL_SELECTED_NOT_PROVIDED));
            }
        }
    }

    private void checkAppointee(Appellant appellant, Map<String, Object> ocrCaseData, Map<String, Object> caseData, boolean isIbcOrSscs8) {
        if (appellant != null && !isAppointeeDetailsEmpty(appellant.getAppointee())) {
            checkPersonName(appellant.getAppointee().getName(), PERSON1_VALUE, appellant, isIbcOrSscs8);
            checkPersonAddressAndDob(appellant.getAppointee().getAddress(), appellant.getAppointee().getIdentity(),
                PERSON1_VALUE, ocrCaseData, caseData, appellant, isIbcOrSscs8);
            checkMobileNumber(appellant.getAppointee().getContact(), PERSON1_VALUE);
        }
    }

    private void checkRepresentative(Appeal appeal, Map<String, Object> ocrCaseData, Map<String, Object> caseData, boolean isIbcOrSscs8) {
        if (appeal.getRep() == null || StringUtils.isBlank(appeal.getRep().getHasRepresentative())) {
            errors.add(HAS_REPRESENTATIVE_FIELD_MISSING);
        }
        if (appeal.getRep() != null && StringUtils.equals(appeal.getRep().getHasRepresentative(), YES_LITERAL)) {
            final Contact repsContact = appeal.getRep().getContact();
            checkPersonAddressAndDob(appeal.getRep().getAddress(), null, REPRESENTATIVE_VALUE, ocrCaseData, caseData,
                appeal.getAppellant(), isIbcOrSscs8);

            Name name = appeal.getRep().getName();

            if (!isTitleValid(name.getTitle())) {
                warnings.add(getMessageByCallbackType(callbackType, REPRESENTATIVE_VALUE,
                    getWarningMessageName(REPRESENTATIVE_VALUE, null) + TITLE, IS_INVALID));
            }

            if (!doesFirstNameExist(name) && !doesLastNameExist(name) && appeal.getRep().getOrganisation() == null) {
                warnings.add(getMessageByCallbackType(callbackType, "", REPRESENTATIVE_NAME_OR_ORGANISATION_DESCRIPTION,
                    ARE_EMPTY));
            }

            checkMobileNumber(repsContact, REPRESENTATIVE_VALUE);
        }
    }

    private void checkOtherParty(Map<String, Object> caseData, boolean ignoreWarnings) {
        @SuppressWarnings("unchecked")
        List<CcdValue<OtherParty>> otherParties = ((List<CcdValue<OtherParty>>) caseData.get("otherParties"));

        OtherParty otherParty;
        if (otherParties != null && !otherParties.isEmpty()) {
            otherParty = otherParties.get(0).getValue();

            Name name = otherParty.getName();
            Address address = otherParty.getAddress();

            if (!ignoreWarnings) {
                checkOtherPartyDataValid(name, address);
            } else {
                if (!doesFirstNameExist(name) || !doesLastNameExist(name)) {
                    caseData.remove("otherParties");
                } else if (!doesAddressLine1Exist(address) || !doesAddressTownExist(address)
                    || !doesAddressPostcodeExist(address)) {
                    otherParty.setAddress(null);
                }
            }
        }
    }

    private void checkOtherPartyDataValid(Name name, Address address) {
        if (name != null && !isTitleValid(name.getTitle())) {
            warnings.add(
                getMessageByCallbackType(callbackType, OTHER_PARTY_VALUE,
                    getWarningMessageName(OTHER_PARTY_VALUE, null) + TITLE,
                    IS_INVALID));
        }

        boolean hasNoName = !doesFirstNameExist(name) && !doesLastNameExist(name);

        if (!hasNoName) {
            otherPartyNameValidation(name);
        }

        boolean hasNoAddress = !doesAddressLine1Exist(address)
            && !doesAddressTownExist(address)
            && !doesAddressPostcodeExist(address);

        if (!hasNoAddress) {
            otherPartyAddressValidation(address);
        }
    }

    private void otherPartyAddressValidation(Address address) {
        if (!doesAddressLine1Exist(address)) {
            warnings.add(getMessageByCallbackType(callbackType, OTHER_PARTY_VALUE,
                getWarningMessageName(OTHER_PARTY_VALUE, null) + ADDRESS_LINE1, IS_EMPTY));
        }

        if (!doesAddressTownExist(address)) {
            warnings.add(getMessageByCallbackType(callbackType, OTHER_PARTY_VALUE,
                getWarningMessageName(OTHER_PARTY_VALUE, null) + ADDRESS_LINE2, IS_EMPTY));
        }

        if (!doesAddressPostcodeExist(address)) {
            warnings.add(getMessageByCallbackType(callbackType, OTHER_PARTY_VALUE,
                getWarningMessageName(OTHER_PARTY_VALUE, null) + ADDRESS_POSTCODE, IS_EMPTY));
        }
    }

    private void otherPartyNameValidation(Name name) {
        if (!doesFirstNameExist(name)) {
            warnings.add(getMessageByCallbackType(callbackType, OTHER_PARTY_VALUE,
                getWarningMessageName(OTHER_PARTY_VALUE, null) + FIRST_NAME, IS_EMPTY));
        }

        if (!doesLastNameExist(name)) {
            warnings.add(getMessageByCallbackType(callbackType, OTHER_PARTY_VALUE,
                getWarningMessageName(OTHER_PARTY_VALUE, null) + LAST_NAME, IS_EMPTY));
        }
    }

    private void checkMrnDetails(Appeal appeal, Map<String, Object> ocrCaseData, boolean ignoreMrnValidation, FormType formType) {

        String dwpIssuingOffice = getDwpIssuingOffice(appeal, ocrCaseData);

        // if Appeal to Proceed direction type for direction Issue event and mrn date is blank then ignore mrn date validation
        if (!ignoreMrnValidation && !doesMrnDateExist(appeal)) {
            warnings.add(getMessageByCallbackType(callbackType, "", MRN_DATE, IS_EMPTY));
        } else if (!ignoreMrnValidation) {
            checkDateValidDate(appeal.getMrnDetails().getMrnDate(), MRN_DATE, "", true);
        }

        if (dwpIssuingOffice != null && appeal.getBenefitType() != null && appeal.getBenefitType().getCode() != null) {

            Optional<OfficeMapping> officeMapping = Optional.empty();
            //TODO: remove when ucOfficeFeatureActive fully enabled.
            if (!ucOfficeFeatureActive && Benefit.UC.getShortName().equals(appeal.getBenefitType().getCode())) {
                officeMapping = dwpAddressLookupService.getDefaultDwpMappingByBenefitType(Benefit.UC.getShortName());
            } else {
                officeMapping =
                    dwpAddressLookupService.getDwpMappingByOffice(appeal.getBenefitType().getCode(), dwpIssuingOffice);
            }

            if (!officeMapping.isPresent()) {
                log.info("DwpHandling handling office is not valid");
                warnings.add(getMessageByCallbackType(callbackType, "", ISSUING_OFFICE, IS_INVALID));
            }
        } else if (dwpIssuingOffice == null && !FormType.SSCS2.equals(formType) && !FormType.SSCS5.equals(formType) && !FormType.SSCS8.equals(formType)) {
            warnings.add(getMessageByCallbackType(callbackType, "", ISSUING_OFFICE, IS_EMPTY));
        }
    }

    private String getDwpIssuingOffice(Appeal appeal, Map<String, Object> ocrCaseData) {
        if (Boolean.TRUE.equals(doesIssuingOfficeExist(appeal))) {
            return appeal.getMrnDetails().getDwpIssuingOffice();
        } else if (!ocrCaseData.isEmpty()) {
            return getField(ocrCaseData, "office");
        } else {
            return null;
        }
    }

    private void checkPersonName(Name name, String personType, Appellant appellant, boolean isIbcOrSscs8) {
        if (!isIbcOrSscs8) {
            if (!doesTitleExist(name)) {
                warnings.add(
                    getMessageByCallbackType(callbackType, personType, getWarningMessageName(personType, appellant) + TITLE,
                        IS_EMPTY));
            } else if (name != null && !isTitleValid(name.getTitle())) {
                warnings.add(
                    getMessageByCallbackType(callbackType, personType, getWarningMessageName(personType, appellant) + TITLE,
                        IS_INVALID));
            }
        }

        if (!doesFirstNameExist(name)) {
            warnings.add(getMessageByCallbackType(callbackType, personType,
                getWarningMessageName(personType, appellant) + FIRST_NAME, IS_EMPTY));
        }
        if (!doesLastNameExist(name)) {
            warnings.add(getMessageByCallbackType(callbackType, personType,
                getWarningMessageName(personType, appellant) + LAST_NAME, IS_EMPTY));
        }
    }

    private Map<String, Boolean> getIbcRoleMapping(Map<String, Object> ocrCaseData) {
        return Map.of(
            IBC_ROLE_FOR_SELF, extractBooleanValueWarning(ocrCaseData, warnings, IBC_ROLE_FOR_SELF),
            IBC_ROLE_FOR_U18, extractBooleanValueWarning(ocrCaseData, warnings, IBC_ROLE_FOR_U18),
            IBC_ROLE_FOR_LACKING_CAPACITY, extractBooleanValueWarning(ocrCaseData, warnings, IBC_ROLE_FOR_LACKING_CAPACITY),
            IBC_ROLE_FOR_POA, extractBooleanValueWarning(ocrCaseData, warnings, IBC_ROLE_FOR_POA),
            IBC_ROLE_FOR_DECEASED, extractBooleanValueWarning(ocrCaseData, warnings, IBC_ROLE_FOR_DECEASED)
        );
    }

    private void checkIbcRole(String personType, Map<String, Object> ocrCaseData, Appellant appellant, boolean validateIbcRoleField) {
        if (!validateIbcRoleField && personType.equalsIgnoreCase("person1")) {
            Map<String, Boolean> ibcRoles = getIbcRoleMapping(ocrCaseData);

            long trueCount = ibcRoles.values().stream().filter(bool -> bool).count();

            if (trueCount > 1) {
                List<String> trueValues = ibcRoles.keySet().stream().filter(ibcRoles::get).toList();
                errors.add(String.join(", ", trueValues) + " cannot all be True");
            } else if (trueCount == 0) {
                errors.add("One of the following must be True: " + String.join(", ", ibcRoles.keySet()));
            }
        }

        if (validateIbcRoleField && appellant.getIbcRole() == null) {
            errors.add(IBC_ROLE + " " + IS_EMPTY);
        }
    }

    private void checkPersonAddressAndDob(Address address, Identity identity, String personType,
                                          Map<String, Object> ocrCaseData, Map<String, Object> caseData,
                                          Appellant appellant, boolean isIbcOrSscs8) {

        boolean isAddressLine4Present = findBooleanExists(getField(ocrCaseData, personType + "_address_line4"));

        // Remove this part if/when the mainland UK question is added to sscs8 form
        if (isIbcOrSscs8 && address != null && address.getInMainlandUk() == null) {
            address.setInMainlandUk(Boolean.TRUE.equals(doesAddressPortOfEntryExist(address)) ? YesNo.NO : YesNo.YES);
        }

        if (!doesAddressLine1Exist(address)) {
            warnings.add(getMessageByCallbackType(callbackType, personType,
                getWarningMessageName(personType, appellant) + ADDRESS_LINE1, IS_EMPTY));
        } else if (!address.getLine1().matches(ADDRESS_REGEX)) {
            warnings.add(getMessageByCallbackType(callbackType, personType,
                getWarningMessageName(personType, appellant) + ADDRESS_LINE1, HAS_INVALID_ADDRESS));
        }

        if (!isIbcOrSscs8) {
            String townLine = (isAddressLine4Present) ? ADDRESS_LINE3 : ADDRESS_LINE2;
            if (!doesAddressTownExist(address)) {

                warnings.add(getMessageByCallbackType(callbackType, personType,
                    getWarningMessageName(personType, appellant) + townLine, IS_EMPTY));
            } else if (!address.getTown().matches(ADDRESS_REGEX)) {
                warnings.add(getMessageByCallbackType(callbackType, personType,
                    getWarningMessageName(personType, appellant) + townLine, HAS_INVALID_ADDRESS));
            }
            // Removed from IBC as it's not on the SSCS8 form
            String countyLine = (isAddressLine4Present) ? ADDRESS_LINE4 : "_ADDRESS_LINE3_COUNTY";
            if (!doesAddressCountyExist(address)) {
                warnings.add(getMessageByCallbackType(callbackType, personType,
                    getWarningMessageName(personType, appellant) + countyLine, IS_EMPTY));
            } else if (!address.getCounty().matches(COUNTY_REGEX)) {
                warnings.add(getMessageByCallbackType(callbackType, personType,
                    getWarningMessageName(personType, appellant) + countyLine, HAS_INVALID_ADDRESS));
            }
        } else {
            boolean hasLine3 = findBooleanExists(getField(ocrCaseData, personType + ADDRESS_LINE3));
            String townLine = (hasLine3) ? ADDRESS_LINE3 : ADDRESS_LINE2;
            if (!doesAddressTownExist(address)) {
                warnings.add(getMessageByCallbackType(callbackType, personType,
                    getWarningMessageName(personType, appellant) + townLine, IS_EMPTY));
            } else if (!address.getTown().matches(ADDRESS_REGEX)) {
                warnings.add(getMessageByCallbackType(callbackType, personType,
                    getWarningMessageName(personType, appellant) + townLine, HAS_INVALID_ADDRESS));
            }
        }

        if (isAddressPostcodeValid(address, personType, appellant) && address != null) {
            if (personType.equals(getPerson1OrPerson2(appellant))) {
                boolean isPort = YesNo.NO.equals(address.getInMainlandUk());
                String postCodeOrPort = isPort ? address.getPortOfEntry() : address.getPostcode();

                RegionalProcessingCenter rpc = regionalProcessingCenterService.getByPostcode(postCodeOrPort, isIbcOrSscs8);

                if (rpc != null) {
                    if (isIbcOrSscs8) {
                        rpc = rpc.toBuilder().hearingRoute(HearingRoute.LIST_ASSIST).build();
                    }
                    caseData.put("region", rpc.getName());
                    caseData.put("regionalProcessingCenter", rpc);
                } else if (!isPort) {
                    warnings.add(getMessageByCallbackType(callbackType, personType,
                        getWarningMessageName(personType, appellant) + ADDRESS_POSTCODE,
                        "is not a postcode that maps to a regional processing center"));
                }
            }
        }
        if (identity != null) {
            checkDateValidDate(identity.getDob(), getWarningMessageName(personType, appellant) + DOB, personType, true);
        }
    }

    private Boolean doesTitleExist(Name name) {
        if (name != null) {
            return StringUtils.isNotEmpty(name.getTitle());
        }
        return false;
    }

    private boolean isTitleValid(String title) {
        if (StringUtils.isNotBlank(title)) {
            String strippedTitle = title.replaceAll("[-+.^:,'_]", "");
            return titles.stream().anyMatch(strippedTitle::equalsIgnoreCase);
        }
        return true;
    }

    private Boolean doesFirstNameExist(Name name) {
        if (name != null) {
            return StringUtils.isNotEmpty(name.getFirstName());
        }
        return false;
    }

    private Boolean doesLastNameExist(Name name) {
        if (name != null) {
            return StringUtils.isNotEmpty(name.getLastName());
        }
        return false;
    }

    private Boolean doesAddressLine1Exist(Address address) {
        if (address != null) {
            return StringUtils.isNotEmpty(address.getLine1());
        }
        return false;
    }

    private boolean isInMainlandUk(Address address) {
        return address == null || address.getInMainlandUk() == null || YesNo.YES.equals(address.getInMainlandUk());
    }

    private Boolean doesAddressPostcodeExist(Address address) {
        if (address != null) {
            return StringUtils.isNotEmpty(address.getPostcode());
        }
        return false;
    }

    private Boolean doesAddressTownExist(Address address) {
        if (address != null) {
            return StringUtils.isNotEmpty(address.getTown());
        }
        return false;
    }

    private Boolean doesAddressCountyExist(Address address) {
        if (address != null) {
            return StringUtils.isNotEmpty(address.getCounty());
        }
        return false;
    }

    private Boolean doesAddressPortOfEntryExist(Address address) {
        if (address != null) {
            return StringUtils.isNotEmpty(address.getPortOfEntry());
        }
        return false;
    }

    private Boolean isPortOfEntryValid(Address address) {
        if (address != null && address.getPortOfEntry() != null) {
            // TODO get actual wording for warning message below
            String portOfEntry = address.getPortOfEntry();
            List<String> validPortCodes = Arrays.stream(UkPortOfEntry.values()).map((UkPortOfEntry::getLocationCode)).toList();
            if (!validPortCodes.contains(portOfEntry)) {
                errors.add(PORT_OF_ENTRY_INVALID_ERROR);
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    private Boolean isAddressPostcodeValid(Address address, String personType, Appellant appellant) {
        if (address != null && YesNo.NO.equals(address.getInMainlandUk()) && personType.equals(PERSON1_VALUE)) {
            return isPortOfEntryValid(address);
        }
        if (address != null && address.getPostcode() != null) {
            if (postcodeValidator.isValidPostcodeFormat(address.getPostcode())) {
                boolean isValidPostcode = postcodeValidator.isValid(address.getPostcode());
                if (!isValidPostcode) {
                    warnings.add(getMessageByCallbackType(callbackType, personType, getWarningMessageName(personType, appellant) + ADDRESS_POSTCODE, IS_NOT_A_VALID_POSTCODE));
                }
                return isValidPostcode;
            }
            errors.add(getMessageByCallbackType(callbackType, personType, getWarningMessageName(personType, appellant) + ADDRESS_POSTCODE, "is not in a valid format"));
            return false;
        }
        warnings.add(getMessageByCallbackType(callbackType, personType,
            getWarningMessageName(personType, appellant) + ADDRESS_POSTCODE, IS_EMPTY));
        return false;
    }

    private void checkAppellantNino(Appellant appellant, String personType) {
        if (appellant != null && appellant.getIdentity() != null && appellant.getIdentity().getNino() != null) {
            if (!appellant.getIdentity().getNino().matches(
                "^(?!BG)(?!GB)(?!NK)(?!KN)(?!TN)(?!NT)(?!ZZ)\\s?(?:[A-CEGHJ-PR-TW-Z]\\s?[A-CEGHJ-NPR-TW-Z])\\s?(?:\\d\\s?){6}([A-D]|\\s)\\s?$")) {
                warnings.add(getMessageByCallbackType(callbackType, personType,
                    getWarningMessageName(personType, appellant) + NINO, IS_INVALID));
            }
        } else {
            warnings.add(
                getMessageByCallbackType(callbackType, personType, getWarningMessageName(personType, appellant) + NINO,
                    IS_EMPTY));
        }
    }

    private void checkAppellantIbcaReference(Appellant appellant, String personType) {
        if (appellant != null && appellant.getIdentity() != null && appellant.getIdentity().getIbcaReference() != null) {
            if (!String.join("", appellant.getIdentity().getIbcaReference().split(" ")).matches(
                "^[A-z]\\d{2}[A-z]\\d{2}$")) {
                warnings.add(getMessageByCallbackType(callbackType, personType,
                    getWarningMessageName(personType, appellant) + IBCA_REFERENCE, IS_INVALID));
            }
        } else {
            warnings.add(
                getMessageByCallbackType(callbackType, personType, getWarningMessageName(personType, appellant) + IBCA_REFERENCE,
                    IS_EMPTY));
        }
    }

    private void checkDateValidDate(String dateField, String fieldName, String personType, Boolean isInFutureCheck) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        if (!StringUtils.isEmpty(dateField)) {
            try {
                LocalDate date = LocalDate.parse(dateField, formatter);

                if (isInFutureCheck && date.isAfter(LocalDate.now())) {
                    warnings.add(getMessageByCallbackType(callbackType, personType, fieldName, IS_IN_FUTURE));
                } else if (!isInFutureCheck && date.isBefore(LocalDate.now())) {
                    warnings.add(getMessageByCallbackType(callbackType, personType, fieldName, IS_IN_PAST));

                }
            } catch (DateTimeParseException ex) {
                log.error("Date time error", ex);
            }
        }
    }

    private Boolean doesMrnDateExist(Appeal appeal) {
        if (appeal.getMrnDetails() != null) {
            return appeal.getMrnDetails().getMrnDate() != null;
        }
        return false;
    }

    private Boolean doesIssuingOfficeExist(Appeal appeal) {
        if (appeal.getMrnDetails() != null) {
            return StringUtils.isNotEmpty(appeal.getMrnDetails().getDwpIssuingOffice());
        }
        return false;
    }

    private String getPerson1OrPerson2(Appellant appellant) {
        if (appellant == null || isAppointeeDetailsEmpty(appellant.getAppointee())) {
            return PERSON1_VALUE;
        } else {
            return PERSON2_VALUE;
        }
    }

    private Boolean isAppointeeDetailsEmpty(Appointee appointee) {
        return appointee == null
            || (isAddressEmpty(appointee.getAddress())
            && isContactEmpty(appointee.getContact())
            && isIdentityEmpty(appointee.getIdentity())
            && isNameEmpty(appointee.getName()));
    }

    private Boolean isAddressEmpty(Address address) {
        return address == null
            || (address.getLine1() == null
            && address.getLine2() == null
            && address.getTown() == null
            && address.getCounty() == null
            && address.getPostcode() == null);
    }

    private Boolean isContactEmpty(Contact contact) {
        return contact == null
            || (contact.getEmail() == null
            && contact.getPhone() == null
            && contact.getMobile() == null);
    }

    private Boolean isIdentityEmpty(Identity identity) {
        return identity == null
            || (identity.getDob() == null
            && identity.getNino() == null);
    }

    private Boolean isNameEmpty(Name name) {
        return name == null
            || (name.getFirstName() == null
            && name.getLastName() == null
            && name.getTitle() == null);
    }

    private void isBenefitTypeValid(Appeal appeal, FormType formType) {
        BenefitType benefitType = appeal.getBenefitType();
        if (benefitType != null && benefitType.getCode() != null) {
            final Optional<Benefit> benefitOptional = Benefit.findBenefitByShortName(benefitType.getCode());
            if (benefitOptional.isEmpty()) {
                List<String> benefitNameList = new ArrayList<>();
                for (Benefit be : Benefit.values()) {
                    benefitNameList.add(be.getShortName());
                }
                errors.add(getMessageByCallbackType(callbackType, "", BENEFIT_TYPE_DESCRIPTION,
                    "invalid. Should be one of: " + String.join(", ", benefitNameList)));
            } else {
                Benefit benefit = benefitOptional.get();
                appeal.setBenefitType(BenefitType.builder()
                    .code(benefit.getShortName())
                    .description(benefit.getDescription())
                    .build());
            }
        } else {
            if (FormType.SSCS8.equals(formType)) {
                appeal.setBenefitType(BenefitType.builder()
                    .description(Benefit.INFECTED_BLOOD_COMPENSATION.getDescription())
                    .code(Benefit.INFECTED_BLOOD_COMPENSATION.getShortName())
                    .build());
            }
            if (formType == null || (!formType.equals(FormType.SSCS1U) && !formType.equals(FormType.SSCS5) && !formType.equals(FormType.SSCS8))) {
                warnings.add(getMessageByCallbackType(callbackType, "", BENEFIT_TYPE_DESCRIPTION, IS_EMPTY));
            }
        }
    }

    private void isHearingTypeValid(Appeal appeal) {
        String hearingType = appeal.getHearingType();

        if (hearingType == null
            || (!hearingType.equals(HEARING_TYPE_ORAL) && !hearingType.equals(HEARING_TYPE_PAPER))) {
            warnings.add(getMessageByCallbackType(callbackType, "", HEARING_TYPE_DESCRIPTION, IS_INVALID));
        }
    }

    private void checkHearingSubTypeIfHearingIsOral(Appeal appeal, Map<String, Object> caseData) {
        String hearingType = appeal.getHearingType();
        FormType formType = (FormType) caseData.get("formType");
        log.info("Bulk-scan form type: {}", formType != null ? formType.toString() : null);
        if ((FormType.SSCS1PEU.equals(formType) || FormType.SSCS2.equals(formType) || FormType.SSCS5.equals(formType) || FormType.SSCS8.equals(formType))
            && hearingType != null && hearingType.equals(HEARING_TYPE_ORAL)
            && !isValidHearingSubType(appeal)) {
            warnings.add(
                getMessageByCallbackType(callbackType, "", HEARING_SUB_TYPE_TELEPHONE_OR_VIDEO_FACE_TO_FACE_DESCRIPTION,
                    ARE_EMPTY));
        }
    }

    private boolean isValidHearingSubType(Appeal appeal) {
        boolean isValid = true;
        HearingSubtype hearingSubType = appeal.getHearingSubtype();
        if (hearingSubType == null
            || !(hearingSubType.isWantsHearingTypeTelephone() || hearingSubType.isWantsHearingTypeVideo()
            || hearingSubType.isWantsHearingTypeFaceToFace())) {
            isValid = false;
        }
        return isValid;
    }

    private void checkExcludedDates(Appeal appeal) {
        if (appeal.getHearingOptions() != null && appeal.getHearingOptions().getExcludeDates() != null) {
            for (ExcludeDate excludeDate : appeal.getHearingOptions().getExcludeDates()) {
                checkDateValidDate(excludeDate.getValue().getStart(), HEARING_OPTIONS_EXCLUDE_DATES_LITERAL, "", false);
            }
        }
    }

    private void checkMobileNumber(Contact contact, String personType) {
        if (contact != null && contact.getMobile() != null && !isMobileNumberValid(contact.getMobile())) {
            errors.add(
                getMessageByCallbackType(callbackType, personType, getWarningMessageName(personType, null) + MOBILE,
                    IS_INVALID));
        }
    }

    private boolean isMobileNumberValid(String number) {
        if (number != null) {
            return number.matches(PHONE_REGEX);
        }
        return true;
    }

    private boolean isUkNumberValid(String number) {
        if (number != null) {
            return number.matches(UK_NUMBER_REGEX);
        }
        return true;
    }

    private String getWarningMessageName(String personType, Appellant appellant) {
        if (personType.equals(REPRESENTATIVE_VALUE)) {
            return "REPRESENTATIVE";
        } else if (personType.equals(OTHER_PARTY_VALUE)) {
            return "OTHER_PARTY";
        } else if (personType.equals(PERSON2_VALUE) || appellant == null
            || isAppointeeDetailsEmpty(appellant.getAppointee())) {
            return "APPELLANT";
        } else {
            return "APPOINTEE";
        }
    }

    private boolean isIbcOrSscs8(FormType formType, String benefitTypeCode) {
        return Benefit.INFECTED_BLOOD_COMPENSATION.getShortName().equals(benefitTypeCode)
            || FormType.SSCS8.equals(formType);
    }
}
