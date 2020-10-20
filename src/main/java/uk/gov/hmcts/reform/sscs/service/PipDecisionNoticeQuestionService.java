package uk.gov.hmcts.reform.sscs.service;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.PipActivityQuestion;

@Slf4j
@Service
public class PipDecisionNoticeQuestionService extends DecisionNoticeQuestionServiceBase {

    @Autowired
    public PipDecisionNoticeQuestionService() throws IOException {
        super("PIP", PipActivityQuestion::getByKey);
    }
}
