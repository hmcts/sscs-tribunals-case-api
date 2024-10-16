package uk.gov.hmcts.reform.sscs.ccd.presubmit.createcase;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.IBCA_BENEFIT_CODE;

import java.util.Objects;
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
                && ((callback.getEvent() == EventType.VALID_APPEAL_CREATED
                || callback.getEvent() == EventType.NON_COMPLIANT
                || callback.getEvent() == EventType.INCOMPLETE_APPLICATION_RECEIVED))
                && Objects.nonNull(callback.getCaseDetails())
                && Objects.nonNull(callback.getCaseDetails().getCaseData());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        PreSubmitCallbackResponse<SscsCaseData> errorResponse = new PreSubmitCallbackResponse<>(caseData);

        if (IBCA_BENEFIT_CODE.equals(caseData.getBenefitCode())) {
            if (NO.equals(caseData.getAppeal().getAppellant().getAddress().getIsInUk())) {
                final String selectedPortOfEntryLocationCode = caseData.getAppeal().getAppellant().getAddress().getUkPortOfEntryList().getValue().getCode();
                caseData.getAppeal().getAppellant().getAddress().setPortOfEntry(selectedPortOfEntryLocationCode);
            }
            validateAddress(errorResponse, caseData.getAppeal().getAppellant());

            if (YES.equals(caseData.getHasRepresentative())
                    && isNull(caseData.getAppeal().getRep().getAddress())
                    && isNull(caseData.getAppeal().getRep().getAddress().getIsInUk())) {
                errorResponse.addError("You must enter Living in the UK for the representative");
            }
        }

        return errorResponse;
    }

    private void validateAddress(PreSubmitCallbackResponse<SscsCaseData> response, Entity party) {
        String addressLine1 = party.getAddress().getLine1();
        String postcode = party.getAddress().getPostcode();
        String country = party.getAddress().getCountry();

        if (isBlank(addressLine1)) {
            response.addError("You must enter address line 1 for the appellant");
        }

        if (NO.equals(party.getAddress().getIsInUk())) {
            if (isBlank(country)) {
                response.addError("You must enter a valid country for the appellant");
            }
        } else {
            if (isBlank(postcode) || !postcodeValidator.isValid(postcode, null)) {
                response.addError("You must enter a valid UK postcode for the appellant");
            }
        }
    }
}
