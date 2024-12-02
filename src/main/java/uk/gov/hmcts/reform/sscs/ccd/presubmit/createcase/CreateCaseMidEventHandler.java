package uk.gov.hmcts.reform.sscs.ccd.presubmit.createcase;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Entity;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.validation.address.PostcodeValidator;

@Component
@Slf4j
public class CreateCaseMidEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    public static final String IBCA_REFERENCE_VALIDATION_ERROR =
            "The IBCA reference must be 6 characters and match the format. The IBCA Reference format is 1 letter, 2 digits, 1 letter, 2 digits e.g. E24A45";

    private static final String HEARING_ROUTE_ERROR_MESSAGE = "Hearing route must be List Assist";

    private final PostcodeValidator postcodeValidator = new PostcodeValidator();

    private static final Pattern IBCA_REFERENCE_REGEX = Pattern.compile("^[A-Za-z]\\d{2}[A-HJKMNP-Z]\\d{2}$");

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType.equals(CallbackType.MID_EVENT)
                && (callback.getEvent() == EventType.VALID_APPEAL_CREATED
                || callback.getEvent() == EventType.NON_COMPLIANT
                || callback.getEvent() == EventType.INCOMPLETE_APPLICATION_RECEIVED
                || callback.getEvent() == EventType.CASE_UPDATED)
                && Objects.nonNull(callback.getCaseDetails())
                && Objects.nonNull(callback.getCaseDetails().getCaseData())
                && callback.getCaseDetails().getCaseData().isIbcCase();
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        PreSubmitCallbackResponse<SscsCaseData> errorResponse = new PreSubmitCallbackResponse<>(caseData);

        if (NO.equals(caseData.getAppeal().getAppellant().getAddress().getInMainlandUk())) {
            final String selectedPortOfEntryLocationCode = caseData.getAppeal().getAppellant().getAddress().getUkPortOfEntryList().getValue().getCode();
            caseData.getAppeal().getAppellant().getAddress().setPortOfEntry(selectedPortOfEntryLocationCode);
        }

        if (callback.getEvent() == EventType.CASE_UPDATED && caseData.getRegionalProcessingCenter() != null && HearingRoute.GAPS.equals(caseData.getRegionalProcessingCenter().getHearingRoute())) {
            errorResponse.addError(HEARING_ROUTE_ERROR_MESSAGE);
        }

        errorResponse.addErrors(validateAddress(caseData.getAppeal().getAppellant()));

        if (isYes(caseData.getAppeal().getRep().getHasRepresentative())
                && (isEmpty(caseData.getAppeal().getRep().getAddress())
                || isEmpty(caseData.getAppeal().getRep().getAddress().getInMainlandUk()))) {
            errorResponse.addError("You must enter Living in the UK for the representative");
        }

        errorResponse.addWarnings(validateIbcaReference(caseData.getAppeal().getAppellant()));

        return errorResponse;
    }

    private Collection<String> validateIbcaReference(Appellant appellant) {
        Set<String> validationWarnings = new HashSet<>();

        if (isEmpty(appellant.getIdentity()) || isEmpty(appellant.getIdentity().getIbcaReference())) {
            validationWarnings.add(IBCA_REFERENCE_VALIDATION_ERROR);
        } else if (!IBCA_REFERENCE_REGEX.matcher(appellant.getIdentity().getIbcaReference()).find()) {
            validationWarnings.add(IBCA_REFERENCE_VALIDATION_ERROR);
        }

        return validationWarnings;
    }

    private Collection<String> validateAddress(Entity party) {
        Set<String> validationErrors = new HashSet<>();
        String addressLine1 = party.getAddress().getLine1();
        String postcode = party.getAddress().getPostcode();
        String country = party.getAddress().getCountry();

        if (isBlank(addressLine1)) {
            validationErrors.add("You must enter address line 1 for the appellant");
        }

        if (NO.equals(party.getAddress().getInMainlandUk())) {
            if (isBlank(country)) {
                validationErrors.add("You must enter a valid country for the appellant");
            }
        } else {
            if (isBlank(postcode) || !postcodeValidator.isValid(postcode, null)) {
                validationErrors.add("You must enter a valid UK postcode for the appellant");
            }
        }

        return validationErrors;
    }
}
