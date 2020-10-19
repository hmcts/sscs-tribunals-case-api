package uk.gov.hmcts.reform.sscs.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaActivityQuestion;

@Slf4j
@Service
public class EsaDecisionNoticeQuestionService extends DecisionNoticeQuestionServiceBase {

    @Autowired
    public EsaDecisionNoticeQuestionService() throws IOException {
        super("ESA", EsaActivityQuestion::getByKey);
    }

    public int getTotalPoints(SscsCaseData sscsCaseData) {
        return Arrays.stream(EsaActivityQuestion.values())
            .map(q -> getAnswerForActivityQuestionKey(sscsCaseData, q.getKey()))
            .filter(Optional::isPresent).mapToInt(o -> o.get().getActivityAnswerPoints()).sum();
    }
}
