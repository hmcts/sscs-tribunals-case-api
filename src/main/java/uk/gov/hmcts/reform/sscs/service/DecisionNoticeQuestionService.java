package uk.gov.hmcts.reform.sscs.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityAnswer;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityQuestion;

@Slf4j
@Service
public class DecisionNoticeQuestionService {

    private JSONArray decisionNoticeJson;

    @Autowired
    public DecisionNoticeQuestionService() throws IOException {
        String decisionNoticeQuestions = IOUtils.resourceToString("reference-data/decision-notice-questions.txt",
            StandardCharsets.UTF_8, Thread.currentThread().getContextClassLoader());

        decisionNoticeJson = new JSONArray("[" + decisionNoticeQuestions + "]");
    }

    /**
     * Obtain the answer details for an activity question, given an SscsCaseData instance.
     *
     * @param sscsCaseData The SscsCaseData
     * @param activityQuestionKey The key of an activity question.
     * @return The answer to that question, given the SscsCaseData instance provided.
     */
    public Optional<ActivityAnswer> getAnswerForActivityQuestionKey(SscsCaseData sscsCaseData, String activityQuestionKey) {

        Function<SscsCaseData, String> answerExtractor =
            ActivityQuestion.getByKey(activityQuestionKey).getAnswerExtractor();
        return extractAnswerFromSelectedValue(answerExtractor.apply(sscsCaseData));
    }

    public Optional<ActivityAnswer> extractAnswerFromSelectedValue(String selectedValue) {
        String answerText = findSelectedAnswerInJson(selectedValue);

        if (answerText != null) {
            @SuppressWarnings(value = "java:S4784")
            Pattern p = Pattern.compile("(\\d+)([a-z])\\. (.*)\\((\\d+)(?!.*\\d)");
            Matcher m = p.matcher(answerText);
            if (m.find()) {
                ActivityAnswer answer = ActivityAnswer.builder()
                    .activityAnswerNumber(m.group(1))
                    .activityAnswerLetter(m.group(2))
                    .activityAnswerValue(m.group(3).trim())
                    .activityAnswerPoints(Integer.parseInt(m.group(4))).build();
                return Optional.of(answer);
            }
        }
        return Optional.empty();
    }

    private String findSelectedAnswerInJson(String selectedValue) {
        for (int i = 0; i < decisionNoticeJson.length(); ++i) {
            JSONObject obj = decisionNoticeJson.getJSONObject(i);
            String id = obj.getString("ListElementCode");
            if (id.equals(selectedValue)) {
                return obj.getString("ListElement");
            }
        }
        return null;
    }


}
