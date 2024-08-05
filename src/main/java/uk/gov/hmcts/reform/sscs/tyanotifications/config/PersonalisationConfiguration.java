package uk.gov.hmcts.reform.sscs.tyanotifications.config;

import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;


@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "text")
public class PersonalisationConfiguration {
    public Map<LanguagePreference, Map<String, String>> personalisation;

    public enum PersonalisationKey {
        ATTENDING_HEARING,
        YESSTRING,
        NOSTRING,
        DATES_NOT_ATTENDING,
        DATE_OF_MRN,
        REASON_FOR_LATE_APPEAL,
        REASON_FOR_NO_MRN,
        NAME,
        DATE_OF_BIRTH,
        NINO,
        ADDRESS,
        EMAIL,
        PHONE,
        CHILD_MAINTENANCE_NUMBER,
        RECEIVE_TEXT_MESSAGE_REMINDER,
        MOBILE,
        HAVE_AN_APPOINTEE,
        NOT_PROVIDED,
        HAVE_A_REPRESENTATIVE,
        ORGANISATION,
        WHAT_DISAGREE_WITH,
        WHY_DISAGREE_WITH,
        ANYTHING,
        LANGUAGE_INTERPRETER,
        SIGN_INTERPRETER,
        HEARING_LOOP,
        DISABLED_ACCESS,
        OTHER_ARRANGEMENTS,
        REQUIRED,
        NOT_REQUIRED,
        OTHER;

        public static String getYesNoKey(String value) {
            return Optional.ofNullable(value).filter(data -> "YES".equals(data.toUpperCase())).map(data -> YESSTRING.name()).orElse(NOSTRING.name());
        }
    }
}
