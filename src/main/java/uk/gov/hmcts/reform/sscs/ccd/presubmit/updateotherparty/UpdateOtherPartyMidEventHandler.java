package uk.gov.hmcts.reform.sscs.ccd.presubmit.updateotherparty;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPDATE_OTHER_PARTY_DATA;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.DateRange;
import uk.gov.hmcts.reform.sscs.ccd.domain.ExcludeDate;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.validation.address.PostcodeValidator;

@Service
@Slf4j
public class UpdateOtherPartyMidEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private static final String OTHER_PARTY = "other party";
    private static final String OTHER_PARTY_REPRESENTATIVE = "other party representative";
    private static final String OTHER_PARTY_APPOINTEE = "other party appointee";
    private static final String ERROR_ADDRESS_LINE_1 = "You must enter address line 1 for the %s";
    private static final String ERROR_COUNTRY = "You must enter a valid country for the %s";
    private static final String ERROR_POSTCODE = "You must enter a valid UK postcode for the %s";
    private static final String ERROR_MAINLAND_SELECTION = "You must select whether the address is in mainland UK for the %s";
    private static final String ERROR_ADDRESS_MISSING = "Address details are missing for the %s";
    private static final String ERROR_UNAVAILABILITY_START_AFTER_END = "unavailability start date must be before end date";
    private static final String ERROR_UNAVAILABILITY_NO_END_DATE = "Add an end date for unavailable dates";
    private static final String ERROR_UNAVAILABILITY_NO_START_DATE = "Add a start date for unavailable dates";

    private final PostcodeValidator postcodeValidator = new PostcodeValidator();

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.MID_EVENT)
            && callback.getEvent() == UPDATE_OTHER_PARTY_DATA
            && nonNull(callback.getCaseDetails().getCaseData().getOtherParties());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);

        caseData.getOtherParties()
            .forEach(party -> validateOtherParty(party.getValue(), caseData.isIbcCase(), response));

        return response;
    }

    private void validateOtherParty(OtherParty party, boolean isIbcCase, PreSubmitCallbackResponse<SscsCaseData> response) {
        response.addErrors(validateAddress(party.getAddress(), OTHER_PARTY, isIbcCase));
        response.addErrors(validateHearingUnavailabilityDates(Optional.ofNullable(party.getHearingOptions()).map(
            HearingOptions::getExcludeDates).orElse(emptyList())));

        if (party.hasRepresentative()) {
            response.addErrors(validateAddress(party.getRep().getAddress(), OTHER_PARTY_REPRESENTATIVE, isIbcCase));
        }

        if (party.hasAppointee()) {
            response.addErrors(validateAddress(party.getAppointee().getAddress(), OTHER_PARTY_APPOINTEE, isIbcCase));
        }
    }

    private Collection<String> validateHearingUnavailabilityDates(final List<ExcludeDate> excludeDates) {
        final List<DateRange> dateRanges = excludeDates.stream()
            .map(ExcludeDate::getValue)
            .filter(Objects::nonNull)
            .toList();

        if (dateRanges.stream().anyMatch(this::isStartDateAfterEndDate)) {
            return singletonList(ERROR_UNAVAILABILITY_START_AFTER_END);
        }
        if (dateRanges.stream().anyMatch(d -> isNotBlank(d.getStart()) && isBlank(d.getEnd()))) {
            return singletonList(ERROR_UNAVAILABILITY_NO_END_DATE);
        }
        if (dateRanges.stream().anyMatch(d -> isBlank(d.getStart()) && isNotBlank(d.getEnd()))) {
            return singletonList(ERROR_UNAVAILABILITY_NO_START_DATE);
        }
        return emptyList();
    }

    private boolean isStartDateAfterEndDate(final DateRange dateRange) {
        final LocalDate startDate = dateRange.getStartDate();
        final LocalDate endDate = dateRange.getEndDate();
        return nonNull(startDate) && nonNull(endDate) && startDate.isAfter(endDate);
    }

    private Set<String> validateAddress(Address address, String addressPrefix, boolean isIbcCase) {
        return isIbcCase ? validateIbcAddress(address, addressPrefix) : validateNonIbcAddress(address, addressPrefix);
    }

    private Set<String> validateIbcAddress(Address address, String addressPrefix) {
        Set<String> validationErrors = new HashSet<>();

        if (address == null) {
            validationErrors.add(String.format(ERROR_ADDRESS_MISSING, addressPrefix));
            return validationErrors;
        }

        if (isEmpty(address.getLine1())) {
            validationErrors.add(String.format(ERROR_ADDRESS_LINE_1, addressPrefix));
        }

        YesNo isInMainlandUk = address.getInMainlandUk();
        if (isEmpty(isInMainlandUk)) {
            validationErrors.add(String.format(ERROR_MAINLAND_SELECTION, addressPrefix));
        } else if (NO.equals(isInMainlandUk)) {
            if (isBlank(address.getCountry())) {
                validationErrors.add(String.format(ERROR_COUNTRY, addressPrefix));
            }
        } else if (YES.equals(isInMainlandUk)) {
            validatePostcode(address, addressPrefix, validationErrors);
        }
        return validationErrors;
    }

    private Set<String> validateNonIbcAddress(Address address, String addressPrefix) {
        Set<String> errors = new HashSet<>();
        if (isNull(address)) {
            return errors;
        }
        if (isNotBlank(address.getLine1())) {
            validatePostcode(address, addressPrefix, errors);
        }
        if (isNotBlank(address.getPostcode()) && isBlank(address.getLine1())) {
            errors.add(String.format(ERROR_ADDRESS_LINE_1, addressPrefix));
        }
        return errors;
    }

    private void validatePostcode(Address address, String addressPrefix, Set<String> errors) {
        if (isEmpty(address.getPostcode()) || !postcodeValidator.isValid(address.getPostcode(), null)) {
            errors.add(String.format(ERROR_POSTCODE, addressPrefix));
        }
    }

}
