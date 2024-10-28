package uk.gov.hmcts.reform.sscs.ccd.presubmit.createcase;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.isIbcaCase;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Entity;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.validation.address.PostcodeValidator;

@Component
@Slf4j
public class CreateCaseMidEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final PostcodeValidator postcodeValidator = new PostcodeValidator();

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType.equals(CallbackType.MID_EVENT)
                && (callback.getEvent() == EventType.VALID_APPEAL_CREATED
                || callback.getEvent() == EventType.NON_COMPLIANT
                || callback.getEvent() == EventType.INCOMPLETE_APPLICATION_RECEIVED
                || callback.getEvent() == EventType.CASE_UPDATED)
                && Objects.nonNull(callback.getCaseDetails())
                && Objects.nonNull(callback.getCaseDetails().getCaseData())
                && isIbcaCase(callback.getCaseDetails().getCaseData()
        );
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        PreSubmitCallbackResponse<SscsCaseData> errorResponse = new PreSubmitCallbackResponse<>(caseData);

        if (NO.equals(caseData.getAppeal().getAppellant().getAddress().getInMainlandUk())) {
            final String selectedPortOfEntryLocationCode = caseData.getAppeal().getAppellant().getAddress().getUkPortOfEntryList().getValue().getCode();
            caseData.getAppeal().getAppellant().getAddress().setPortOfEntry(selectedPortOfEntryLocationCode);
        }

        errorResponse.addErrors(validateAddress(caseData.getAppeal().getAppellant()));

        if (isYes(caseData.getAppeal().getRep().getHasRepresentative())
                && isNotEmpty(caseData.getAppeal().getRep().getAddress())
                && isEmpty(caseData.getAppeal().getRep().getAddress().getInMainlandUk())) {
            errorResponse.addError("You must enter Living in the UK for the representative");
        }

        return errorResponse;
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
