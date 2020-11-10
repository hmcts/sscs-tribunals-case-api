package uk.gov.hmcts.reform.sscs.service;

import java.io.IOException;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityQuestionLookup;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.PipActivityQuestion;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.PipPointsCondition;

@Slf4j
@Service
public class PipDecisionNoticeQuestionService extends DecisionNoticeQuestionService {

    @Autowired
    public PipDecisionNoticeQuestionService() throws IOException {
        super("PIP", Arrays.asList(PipPointsCondition.class));
    }

    @Override
    protected ActivityQuestionLookup getActivityQuestionLookup() {
        return PipActivityQuestion::getByKey;
    }

}
