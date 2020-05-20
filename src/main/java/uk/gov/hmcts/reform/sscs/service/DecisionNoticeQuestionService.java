package uk.gov.hmcts.reform.sscs.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public int extractPointsFromSelectedValue(String selectedValue) {
        String answerText = findSelectedAnswerInJson(selectedValue);

        if (answerText != null) {
            Pattern p = Pattern.compile("(\\d+)(?!.*\\d)");
            Matcher m = p.matcher(answerText);
            if (m.find()) {
                return Integer.parseInt(m.group());
            }
        }
        return 0;
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
