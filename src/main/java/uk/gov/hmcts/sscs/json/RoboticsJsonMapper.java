package uk.gov.hmcts.sscs.json;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.json.JSONObject;
import org.json.simple.JSONArray;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.sscs.domain.wrapper.*;
import uk.gov.hmcts.sscs.model.robotics.RoboticsWrapper;

@Component
public class RoboticsJsonMapper {

    public JSONObject map(RoboticsWrapper wrapper) {

        SyaCaseWrapper appeal = wrapper.getSyaCaseWrapper();
        JSONObject obj = new JSONObject();

        obj = buildAppealDetails(obj, appeal);

        obj.put("caseId", wrapper.getCcdCaseId());
        obj.put("appellant", buildAppellantDetails(appeal.getAppellant()));

        if (appeal.getRepresentative() != null) {
            obj.put("representative", buildRepresentativeDetails(appeal.getRepresentative()));
        }

        if (appeal.getSyaHearingOptions() != null) {
            JSONObject hearingArrangements = buildHearingOptions(appeal.getSyaHearingOptions());
            if (hearingArrangements.length() > 0) {
                obj.put("hearingArrangements", hearingArrangements);
            }
        }

        return obj;
    }

    private static JSONObject buildAppealDetails(JSONObject obj, SyaCaseWrapper appeal) {
        obj.put("caseCode", "002DD");
        obj.put("appellantNino", appeal.getAppellant().getNino());
        //FIXME: To be implemented at a future date as part of another ticket
        obj.put("appellantPostCode", "Bedford");
        obj.put("appealDate", LocalDate.now().toString());

        if (appeal.getMrn() != null) {
            if (appeal.getMrn().getDate() != null) {
                obj.put("mrnDate", appeal.getMrn().getDate().toString());
            }
            if (appeal.getMrn().getReasonForBeingLate() != null) {
                obj.put("mrnReasonForBeingLate", appeal.getMrn().getReasonForBeingLate());
            }
        }

        if (appeal.getMrn().getDwpIssuingOffice() != null) {
            obj.put("pipNumber", appeal.getMrn().getDwpIssuingOffice());
        }

        obj.put("hearingType", convertBooleanToPaperOral(appeal.getSyaHearingOptions().getWantsToAttend()));

        if (appeal.getSyaHearingOptions().getWantsToAttend()) {
            obj.put("hearingRequestParty", appeal.getAppellant().getFullName());
        }

        return obj;
    }

    private static JSONObject buildAppellantDetails(SyaAppellant appellant) {
        JSONObject json = new JSONObject();

        json.put("title", appellant.getTitle());
        json.put("firstName", appellant.getFirstName());
        json.put("lastName", appellant.getLastName());

        return buildContactDetails(json, appellant.getContactDetails());
    }

    private static JSONObject buildRepresentativeDetails(SyaRepresentative rep) {
        JSONObject json = new JSONObject();

        json.put("firstName", rep.getFirstName());
        json.put("lastName", rep.getLastName());

        if (rep.getOrganisation() != null) {
            json.put("organisation", rep.getOrganisation());
        }

        return buildContactDetails(json, rep.getContactDetails());
    }

    @SuppressWarnings("unchecked")
    private static JSONObject buildHearingOptions(SyaHearingOptions hearingOptions) {
        JSONObject hearingArrangements = new JSONObject();

        if (hearingOptions.getArrangements() != null) {
            SyaArrangements arrangements = hearingOptions.getArrangements();
            hearingArrangements.put("languageInterpreter", convertBooleanToYesNo(arrangements != null && arrangements.getLanguageInterpreter() ? true : false));
            hearingArrangements.put("signLanguageInterpreter", convertBooleanToYesNo(arrangements != null && arrangements.getSignLanguageInterpreter() ? true : false));
            hearingArrangements.put("hearingLoop", convertBooleanToYesNo(arrangements != null && arrangements.getHearingLoop() ? true : false));
            hearingArrangements.put("accessibleHearingRoom", convertBooleanToYesNo(arrangements != null && arrangements.getAccessibleHearingRoom() ? true : false));
        }

        if (hearingOptions.getAnythingElse() != null) {
            hearingArrangements.put("other", hearingOptions.getAnythingElse());
        }

        if (hearingOptions.getDatesCantAttend() != null
            && hearingOptions.getDatesCantAttend().length > 0) {
            JSONArray datesCantAttendArray = new JSONArray();
            for (String a : hearingOptions.getDatesCantAttend()) {
                datesCantAttendArray.add(getLocalDate(a));
            }

            hearingArrangements.put("datesCantAttend", datesCantAttendArray);
        }

        return hearingArrangements;
    }

    private static JSONObject buildContactDetails(JSONObject json, SyaContactDetails contactDetails) {
        json.put("addressLine1", contactDetails.getAddressLine1());

        if (contactDetails.getAddressLine2() != null) {
            json.put("addressLine2", contactDetails.getAddressLine2());
        }

        json.put("townOrCity", contactDetails.getTownCity());
        json.put("county", contactDetails.getCounty());
        json.put("postCode", contactDetails.getPostCode());
        json.put("phoneNumber", contactDetails.getPhoneNumber());
        json.put("email", contactDetails.getEmailAddress());

        return json;
    }

    private static String convertBooleanToYesNo(Boolean value) {
        return value ? "Yes" : "No";
    }

    private static String convertBooleanToPaperOral(Boolean value) {
        return value ? "Oral" : "Paper";
    }

    private static String getLocalDate(String dateStr) {
        LocalDate localDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        return localDate.toString();
    }
}
