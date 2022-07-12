package uk.gov.hmcts.reform.sscs.util;

import java.util.Arrays;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.reference.data.model.JudicialMemberType;

@Component
public class UpdateListingRequirementsUtil {

    private UpdateListingRequirementsUtil(){
    }

    public static boolean isValidJudicialMemberType(String appointment) {
        if (appointment == null) {
            return false;
        }

        JudicialMemberType type = getJudicialMemberType(appointment);
        return JudicialMemberType.TRIBUNAL_PRESIDENT == type
            || JudicialMemberType.TRIBUNAL_JUDGE == type
            || JudicialMemberType.REGIONAL_TRIBUNAL_JUDGE == type;
    }

    public static JudicialMemberType getJudicialMemberType(String appointment) {
        return Arrays.stream(JudicialMemberType.values())
            .filter(x -> x.getDescriptionEn().equals(appointment))
            .findFirst()
            .orElse(null);
    }
}
