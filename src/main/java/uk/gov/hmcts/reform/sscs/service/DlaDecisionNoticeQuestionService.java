package uk.gov.hmcts.reform.sscs.service;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityQuestionLookup;

@Slf4j
@Service
public class DlaDecisionNoticeQuestionService extends DecisionNoticeQuestionService {

    @Autowired
    public DlaDecisionNoticeQuestionService() throws IOException {
        super("DLA");
    }

    @Override
    protected ActivityQuestionLookup getActivityQuestionLookup() {
        return key -> null;
    }
}
