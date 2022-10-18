package uk.gov.hmcts.reform.sscs.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityAnswer;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityQuestionLookup;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.PointsCondition;

@Slf4j
public abstract class DecisionNoticeQuestionService {

    public static final String REFERENCE_DECISION_NOTICE_QUESTIONS_JSON = "reference/%s-decision-notice-questions.json";
    private JSONArray decisionNoticeJson;
    private String benefitType;
    private List<Class<? extends PointsCondition<?>>> pointsConditionClasses;

    protected DecisionNoticeQuestionService(String benefitType) {
        this.benefitType = benefitType;
    }

    protected DecisionNoticeQuestionService(String benefitType, List<Class<? extends PointsCondition<?>>> pointsConditionClasses) throws IOException {
        String path = String.format(REFERENCE_DECISION_NOTICE_QUESTIONS_JSON, benefitType.toLowerCase());
        String decisionNoticeQuestions =
            IOUtils.resourceToString(path, StandardCharsets.UTF_8, Thread.currentThread().getContextClassLoader());

        decisionNoticeJson = new JSONArray(decisionNoticeQuestions);
        this.benefitType = benefitType;
        this.pointsConditionClasses = pointsConditionClasses;
    }

    protected abstract ActivityQuestionLookup getActivityQuestionLookup();

    public List<Class<? extends PointsCondition<?>>> getPointsConditionEnumClasses() {
        return pointsConditionClasses;
    }

    public String getBenefitType() {
        return benefitType;
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
            getActivityQuestionLookup().getByKey(activityQuestionKey).getAnswerExtractor();
        return extractAnswerFromSelectedValue(answerExtractor.apply(sscsCaseData));
    }

    public Optional<ActivityAnswer> extractAnswerFromSelectedValue(String selectedValue) {
        String answerText = findSelectedAnswerOrQuestionInJson(selectedValue);

        if (answerText != null) {
            @SuppressWarnings(value = "java:S4784")
            Pattern p = Pattern.compile("(\\d+)([a-z])\\. (.*)\\((\\d+) (points|point)\\)");
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

    protected String findSelectedAnswerOrQuestionInJson(String selectedValue) {
        for (int i = 0; i < decisionNoticeJson.length(); ++i) {
            JSONObject obj = decisionNoticeJson.getJSONObject(i);
            String id = obj.getString("ListElementCode");
            if (id.equals(selectedValue)) {
                return obj.getString("ListElement");
            }
        }
        return null;
    }

    public int getTotalPoints(SscsCaseData sscsCaseData, Collection<String> answerKeys) {
        return answerKeys.stream().map(answerText -> getAnswerForActivityQuestionKey(sscsCaseData,
            answerText)).filter(Optional::isPresent).map(Optional::get).mapToInt(ActivityAnswer::getActivityAnswerPoints).sum();
    }


}
