package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class CallbackValidationService {

    public boolean isDueDateInvalid(String dueDate) {
        if (!isBlank(dueDate)) {
            LocalDate localDueDate = LocalDate.parse(dueDate);
            LocalDate now = LocalDate.now();
            return !localDueDate.isAfter(now);
        }
        return false;
    }
}
