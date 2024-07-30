package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.util.DateTimeUtils.isDateInTheFuture;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Component
@Slf4j
public class AdjournCaseMidEventDueDateService {

    public Boolean validateAdjournCaseDirectionsDueDateIsInFuture(SscsCaseData sscsCaseData) {
        if (nonNull(sscsCaseData.getAdjournment().getDirectionsDueDate()) && !isDateInTheFuture(sscsCaseData.getAdjournment().getDirectionsDueDate())) {
            return false;
        }
        return true;
    }
}
