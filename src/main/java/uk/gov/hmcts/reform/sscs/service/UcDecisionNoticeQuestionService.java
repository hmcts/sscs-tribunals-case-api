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
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityAnswer;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityQuestionLookup;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.UcActivityQuestion;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.UcActivityQuestionKey;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.UcAllowedOrRefusedCondition;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.UcPointsRegulationsAndSchedule7ActivitiesCondition;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.UcQuestionKey;

@Slf4j
@Service
public class UcDecisionNoticeQuestionService extends DecisionNoticeQuestionService {

    @Autowired
    public UcDecisionNoticeQuestionService() throws IOException {
        super("UC", Arrays.asList(UcPointsRegulationsAndSchedule7ActivitiesCondition.class, UcAllowedOrRefusedCondition.class));
    }

    @Override
    protected ActivityQuestionLookup getActivityQuestionLookup() {
        return key -> extractQuestionFromKey(UcActivityQuestionKey.getByKey(key));
    }

    @Override
    public Optional<ActivityAnswer> getAnswerForActivityQuestionKey(SscsCaseData sscsCaseData, String activityQuestionKey) {
        if (activityQuestionKey != null && activityQuestionKey.startsWith("schedule7")) {
            return extractSchedule7AnswerFromSelectedValue(activityQuestionKey);
        } else {
            return super.getAnswerForActivityQuestionKey(sscsCaseData, activityQuestionKey);
        }
    }

    public Optional<ActivityAnswer> extractSchedule7AnswerFromSelectedValue(String selectedValue) {
        String answerText = findSelectedAnswerOrQuestionInJson(selectedValue);

        if (answerText != null) {
            @SuppressWarnings(value = "java:S4784")
            Pattern p = Pattern.compile("^(\\d+)\\. ");
            Matcher m = p.matcher(answerText);
            if (m.find()) {
                ActivityAnswer answer = ActivityAnswer.builder()
                    .activityAnswerNumber(m.group(1)).build();
                return Optional.of(answer);
            }
        }
        return Optional.empty();
    }

    public UcActivityQuestion extractQuestionFromKey(UcQuestionKey key) {
        String questionText = findSelectedAnswerOrQuestionInJson(key.getKey());
        if (questionText != null) {
            @SuppressWarnings(value = "java:S4784")
            Pattern p = Pattern.compile("(\\d+)([a-z])\\. (.*)\\((\\d+) (points|point)\\)");
            Matcher m = p.matcher(questionText);
            if (!m.find()) {
                return new UcActivityQuestion(key, questionText);
            } else {
                throw new IllegalStateException("Text should be a question but it matches the format of an answer with points");
            }
        } else {
            throw new IllegalStateException("No text found for question with key:" + key.getKey());
        }
    }
}
