package uk.gov.hmcts.reform.sscs.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityQuestionLookup;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaActivityQuestion;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaActivityQuestionKey;

@Slf4j
@Service
public class EsaDecisionNoticeQuestionService extends DecisionNoticeQuestionServiceBase {

    @Autowired
    public EsaDecisionNoticeQuestionService() throws IOException {
        super("ESA");
    }

    @Override
    protected ActivityQuestionLookup getActivityQuestionLookup() {
        return this::extractQuestionFromSelectedValue;
    }

    public int getTotalPoints(SscsCaseData sscsCaseData) {
        return Arrays.stream(EsaActivityQuestionKey.values())
            .map(q -> getAnswerForActivityQuestionKey(sscsCaseData, q.getKey()))
            .filter(Optional::isPresent).mapToInt(o -> o.get().getActivityAnswerPoints()).sum();
    }

    public EsaActivityQuestion extractQuestionFromSelectedValue(String selectedValue) {
        EsaActivityQuestionKey key = EsaActivityQuestionKey.getByKey(selectedValue);
        String questionText = findSelectedAnswerOrQuestionInJson(selectedValue);
        if (questionText != null) {
            @SuppressWarnings(value = "java:S4784")
            Pattern p = Pattern.compile("(\\d+)([a-z])\\. (.*)\\((\\d+)(?!.*\\d)");
            Matcher m = p.matcher(questionText);
            if (!m.find()) {
                return new EsaActivityQuestion(key, questionText);
            } else {
                throw new IllegalStateException("Text should be a question but it matches the format of an answer with points");
            }
        } else {
            throw new IllegalStateException("No text found for question with key:" + selectedValue);
        }
    }
}
