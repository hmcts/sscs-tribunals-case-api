package uk.gov.hmcts.reform.sscs.ccd.presubmit.updateotherparty;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPDATE_OTHER_PARTY_DATA;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;

import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.validation.address.PostcodeValidator;

@Service
public class UpdateOtherPartyMidEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final PostcodeValidator postcodeValidator = new PostcodeValidator();

    private static final String OTHER_PARTY = "other party";
    private static final String OTHER_PARTY_REPRESENTATIVE = "other party representative";
    private static final String OTHER_PARTY_APPOINTEE = "other party appointee";
    private static final String ERROR_ADDRESS_LINE_1 = "You must enter address line 1 for the %s";
    private static final String ERROR_COUNTRY = "You must enter a valid country for the %s";
    private static final String ERROR_POSTCODE = "You must enter a valid UK postcode for the %s";
    private static final String ERROR_MAINLAND_SELECTION = "You must select whether the address is in mainland UK for the %s";
    private static final String ERROR_ADDRESS_MISSING = "Address details are missing for the %s";

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
        boolean isIbcCase = caseData.isIbcCase();

        caseData.getOtherParties().forEach(party -> validateOtherPartyAddresses(party, isIbcCase, response));

        return response;
    }

    private void validateOtherPartyAddresses(CcdValue<OtherParty> party, boolean isIbcCase, PreSubmitCallbackResponse<SscsCaseData> response) {
        response.addErrors(validateAddress(party.getValue().getAddress(), OTHER_PARTY, isIbcCase));

        if (party.getValue().hasRepresentative()) {
            response.addErrors(
                validateAddress(party.getValue().getRep().getAddress(), OTHER_PARTY_REPRESENTATIVE, isIbcCase));
        }

        if (party.getValue().hasAppointee()) {
            response.addErrors(
                validateAddress(party.getValue().getAppointee().getAddress(), OTHER_PARTY_APPOINTEE, isIbcCase));
        }
    }

    private Set<String> validateAddress(Address address, String addressPrefix, boolean isIbcCase) {
        Set<String> validationErrors = new HashSet<>();

        if (address == null) {
            validationErrors.add(String.format(ERROR_ADDRESS_MISSING, addressPrefix));
            return validationErrors;
        }

        if (isEmpty(address.getLine1())) {
            validationErrors.add(String.format(ERROR_ADDRESS_LINE_1, addressPrefix));
        }

        if (isIbcCase) {
            validateIbcCountryAndPostcode(address, addressPrefix, validationErrors);
        } else {
            validateUkPostcode(address, addressPrefix, validationErrors);
        }

        return validationErrors;
    }

    private void validateIbcCountryAndPostcode(Address address, String partyRoleLabel, Set<String> errors) {
        YesNo isInMainlandUk = address.getInMainlandUk();

        if (isEmpty(isInMainlandUk)) {
            errors.add(String.format(ERROR_MAINLAND_SELECTION, partyRoleLabel));
            return;
        }

        if (NO.equals(isInMainlandUk)) {
            if (isBlank(address.getCountry())) {
                errors.add(String.format(ERROR_COUNTRY, partyRoleLabel));
            }
            return;
        }

        validateUkPostcode(address, partyRoleLabel, errors);
    }

    private void validateUkPostcode(Address address, String partyRoleLabel, Set<String> errors) {
        if (isEmpty(address.getPostcode()) || !postcodeValidator.isValid(address.getPostcode(), null)) {
            errors.add(String.format(ERROR_POSTCODE, partyRoleLabel));
        }
    }
}
