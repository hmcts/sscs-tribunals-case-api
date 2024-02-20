package uk.gov.hmcts.reform.sscs.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LanguageService {

    private static final String SIGN_LANGUAGE_NAME_PREFIX_1 = "Sign (";
    private static final String SIGN_LANGUAGE_NAME_PREFIX_2 = "Sign Language (";
    public static final String REFERENCE_LANGUAGES_JSON = "reference/languages.json";


    private JSONArray languagesJson;

    @Autowired
    public LanguageService() throws IOException {
        String languages = IOUtils.resourceToString(REFERENCE_LANGUAGES_JSON,
            StandardCharsets.UTF_8, Thread.currentThread().getContextClassLoader());

        languagesJson = new JSONArray(languages);
    }

    public String getLanguageNameFromLanguageKey(String languageKey) {
        for (int i = 0; i < languagesJson.length(); ++i) {
            JSONObject obj = languagesJson.getJSONObject(i);
            String id = obj.getString("ListElementCode");
            if (id.equals(languageKey)) {
                return obj.getString("ListElement");
            }
        }
        return null;
    }
}
