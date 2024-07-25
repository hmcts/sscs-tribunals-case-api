package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.util.DateTimeUtils.isDateInTheFuture;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Component
@Slf4j
public class AdjournCaseMidEventDueDateService {

    public PreSubmitCallbackResponse<SscsCaseData> validateAdjournCaseDirectionsDueDateIsInFuture(Callback<SscsCaseData> callback) {

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        if (nonNull(sscsCaseData.getAdjournment().getDirectionsDueDate())
                && !isDateInTheFuture(sscsCaseData.getAdjournment().getDirectionsDueDate())
        ) {
            preSubmitCallbackResponse.addError("Directions due date must be in the future");
        }
        return preSubmitCallbackResponse;

    }

}
