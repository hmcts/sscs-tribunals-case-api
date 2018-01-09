package uk.gov.hmcts.sscs.builder;

import java.util.HashMap;
import java.util.Iterator;
import org.json.simple.JSONObject;
import uk.gov.hmcts.sscs.domain.corecase.*;

public class SubmitYourAppealJsonBuilder {

    public static JSONObject convertAppeal(Appeal appeal) {
        return new JSONObject(new HashMap<String, Object>() {{
                put("BenefitType_benefitType", appeal.getBenefit().getType());
                put("MRNDate_day", appeal.getDateOfDecision().getDayOfMonth());
                put("MRNDate_month",  appeal.getDateOfDecision().getMonthValue());
                put("MRNDate_year",  appeal.getDateOfDecision().getYear());
            }
        });
    }

    public static JSONObject convertAppellant(Appellant appellant) {
        return new JSONObject(new HashMap<String, Object>() {{
                put("AppellantName_title", appellant.getName().getTitle());
                put("AppellantName_firstName", appellant.getName().getFirst());
                put("AppellantName_lastName", appellant.getName().getSurname());
                put("AppellantDetails_addressLine1", appellant.getAddress().getLine1());
                put("AppellantDetails_addressLine2", appellant.getAddress().getLine2());
                put("AppellantDetails_townCity", appellant.getAddress().getTown());
                put("AppellantDetails_county", appellant.getAddress().getCounty());
                put("AppellantDetails_postCode", appellant.getAddress().getPostcode());
                put("AppellantDetails_appellantPhoneNumber", appellant.getPhone());
                put("AppellantDetails_emailAddress", appellant.getEmail());
                put("AppellantDetails_niNumber", appellant.getNino());
            }
        });
    }

    public static JSONObject convertAppointee(Appointee appointee) {
        return new JSONObject(new HashMap<String, Object>() {{
                put("AppointeeDetails_title", appointee.getName().getTitle());
                put("AppointeeDetails_firstName", appointee.getName().getFirst());
                put("AppointeeDetails_lastName", appointee.getName().getSurname());
                put("AppointeeDetails_addressLine1", appointee.getAddress().getLine1());
                put("AppointeeDetails_addressLine2", appointee.getAddress().getLine2());
                put("AppointeeDetails_townCity", appointee.getAddress().getTown());
                put("AppointeeDetails_county", appointee.getAddress().getCounty());
                put("AppointeeDetails_postCode", appointee.getAddress().getPostcode());
                put("AppointeeDetails_phoneNumber", appointee.getPhone());
                put("AppointeeDetails_emailAddress", appointee.getEmail());
            }
        });
    }

    public static JSONObject convertRepresentative(Representative representative) {
        return new JSONObject(new HashMap<String, Object>() {{
                put("RepresentativeDetails_title", representative.getName().getTitle());
                put("RepresentativeDetails_firstName", representative.getName().getFirst());
                put("RepresentativeDetails_lastName", representative.getName().getSurname());
                put("RepresentativeDetails_addressLine1", representative.getAddress().getLine1());
                put("RepresentativeDetails_addressLine2", representative.getAddress().getLine2());
                put("RepresentativeDetails_townCity", representative.getAddress().getTown());
                put("RepresentativeDetails_county", representative.getAddress().getCounty());
                put("RepresentativeDetails_postCode", representative.getAddress().getPostcode());
                put("RepresentativeDetails_phoneNumber", representative.getPhone());
                put("RepresentativeDetails_emailAddress", representative.getEmail());
                put("RepresentativeDetails_organisation", representative.getOrganisation());
            }
        });
    }

    public static JSONObject convertHearing(Hearing hearing) {
        return new JSONObject(new HashMap<String, Object>() {{
                put("tribunal_type", hearing.getTribunalType().toString());
                put("hearing_interpreter_required", hearing.getLanguageInterpreterRequired());
                put("sign_interpreter_required", hearing.getSignLanguageRequired());
                put("hearing_loop_required", hearing.getHearingLoopRequired());
                put("disabled_access_required", hearing.getHasDisabilityNeeds());
                put("other_details", hearing.getAdditionalInformation());
            }
        });
    }

    public static JSONObject buildAllCaseJson(CcdCase ccdCase) {
        return merge(convertAppeal(ccdCase.getAppeal()), convertAppellant(ccdCase.getAppellant()),
                convertAppointee(ccdCase.getAppointee()),
                convertRepresentative(ccdCase.getRepresentative()),
                convertHearing(ccdCase.getHearings().get(0)));
    }

    private static JSONObject merge(JSONObject... jsonObjects) {

        JSONObject jsonObject = new JSONObject();

        for (JSONObject temp : jsonObjects) {
            Iterator<String> keys = temp.keySet().iterator();
            while (keys.hasNext()) {
                String key = keys.next();
                jsonObject.put(key, temp.get(key));
            }
        }
        return jsonObject;
    }
}
