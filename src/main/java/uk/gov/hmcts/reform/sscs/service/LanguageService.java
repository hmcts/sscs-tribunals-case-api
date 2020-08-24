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


    private JSONArray languagesJson;

    @Autowired
    public LanguageService() throws IOException {
        String languages = IOUtils.resourceToString("reference-data/languages.txt",
            StandardCharsets.UTF_8, Thread.currentThread().getContextClassLoader());

        languagesJson = new JSONArray("[" + languages + "]");
    }


    public String getInterpreterDescriptionForLanguageKey(String languageKey) {
        if (languageKey != null) {
            String languageName = getLanguageNameFromLanguageKey(languageKey);
            if (languageName != null) {
                if (languageKey.startsWith("sign")) {
                    String signLanguageType = getSignLanguageType(languageName);
                    return "a sign language interpreter (" + signLanguageType + ")";
                } else {
                    return "an interpreter in " + languageName;
                }
            }

        }
        return null;
    }

    private String getSignLanguageType(String languageName) {

        if (languageName.startsWith(SIGN_LANGUAGE_NAME_PREFIX_1) && languageName.endsWith(")")) {
            return languageName.substring(SIGN_LANGUAGE_NAME_PREFIX_1.length(), languageName.length() - 1);
        } else  if (languageName.startsWith(SIGN_LANGUAGE_NAME_PREFIX_2) && languageName.endsWith(")")) {
            return languageName.substring(SIGN_LANGUAGE_NAME_PREFIX_2.length(), languageName.length() - 1);
        } else {
            return languageName;
        }
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
