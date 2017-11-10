package uk.gov.hmcts.sscs.service.json;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.util.HashMap;
import java.util.Iterator;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import uk.gov.hmcts.sscs.tribunals.domain.corecase.*;

public class CcdCaseDeserializerTest {

    private CcdCaseDeserializer ccdCaseDeserializer;
    private ObjectMapper mapper;
    private Appeal appeal;
    private Appellant appellant;
    private Appointee appointee;
    private Representative representative;
    private Hearing hearing;
    private CcdCase ccdCase;
    private JSONObject appealJson;
    private JSONObject appellantJson;
    private JSONObject appointeeJson;
    private JSONObject representativeJson;
    private JSONObject hearingJson;
    private JSONObject allJson;

    @Before
    public void setup() {

        ccdCaseDeserializer = new CcdCaseDeserializer();

        appeal = new Appeal(Benefit.UNIVERSAL_CREDIT, null,
                ccdCaseDeserializer.convertJsonStringIntoDate("2", "9", "2017"), null);

        appellant = new Appellant(new Name("Dr", "Kenny", "Rodgers"),
                new Address("My Road", "Village", "Bedrock", "Bedford", "BF12 1HF"),
                "01234 123456", "m@test.com", "JT0123456H", null);

        appointee = new Appointee(new Name("Mrs", "Benny", "Dodgers"),
                new Address("My House", "Village", "Bedrock", "Bedford", "BF12 1HF"),
                "01234 765432", "appointee@test.com");

        representative = new Representative(new Name("Mr", "Benny", "Dodgers"),
                new Address("My House", "Village", "Bedrock", "Bedford", "BF12 1HF"),
                "01234 765432", "appointee@test.com", "Monsters Inc.");

        hearing = new Hearing(TribunalType.PAPER, "Yes", "Yes", "No", "No",
                "Additional info", null);

        ccdCase = new CcdCase(appeal, appellant, appointee, representative, hearing);

        appealJson = new JSONObject(new HashMap<String, Object>() {{
                put("BenefitType_benefitType", appeal.getBenefit().getType());
                put("MRNDate_day", "2");
                put("MRNDate_month", "9");
                put("MRNDate_year", "2017");
                }
            }
        );

        appellantJson = new JSONObject(new HashMap<String, Object>() {{
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
            }
        );

        appointeeJson = new JSONObject(new HashMap<String, Object>() {{
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
            }
        );

        representativeJson = new JSONObject(new HashMap<String, Object>() {{
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
            }
        );

        hearingJson = new JSONObject(new HashMap<String, Object>() {{
                put("tribunal_type", hearing.getTribunalType().getKey());
                put("hearing_interpreter_required", hearing.getLanguageInterpreterRequired());
                put("sign_interpreter_required", hearing.getSignLanguageRequired());
                put("hearing_loop_required", hearing.getHearingLoopRequired());
                put("disabled_access_required", hearing.getHasDisabilityNeeds());
                put("other_details", hearing.getAdditionalInformation());
                }
            }
        );

        allJson = merge(appealJson, appellantJson, appointeeJson, representativeJson, hearingJson);

        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(CcdCase.class, ccdCaseDeserializer);
    }

    private static JSONObject merge(JSONObject... jsonObjects) {

        JSONObject jsonObject = new JSONObject();

        for (JSONObject temp : jsonObjects) {
            Iterator<String> keys = temp.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                try {
                    jsonObject.put(key, temp.get(key));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return jsonObject;
    }

    @Test
    public void buildAppeal() throws IOException {
        final ObjectNode jsonNode = mapper.readValue(appealJson.toString(), ObjectNode.class);
        assertEquals(ccdCaseDeserializer.buildAppeal(jsonNode), appeal);
    }

    @Test
    public void convertJsonStringIntoDate() {
        LocalDate ldt = LocalDate.of(2017, Month.SEPTEMBER, 2);
        assertEquals(ccdCaseDeserializer.convertJsonStringIntoDate("2", "9", "2017"), ldt);
    }

    @Test
    public void buildAppellant() throws IOException {
        final ObjectNode jsonNode = mapper.readValue(appellantJson.toString(), ObjectNode.class);
        assertEquals(ccdCaseDeserializer.buildAppellant(jsonNode), appellant);
    }

    @Test
    public void buildAppointee() throws IOException {
        final ObjectNode jsonNode = mapper.readValue(appointeeJson.toString(), ObjectNode.class);
        assertEquals(ccdCaseDeserializer.buildAppointee(jsonNode), appointee);
    }

    @Test
    public void buildRepresentative() throws IOException {
        final ObjectNode jsonNode = mapper.readValue(
                representativeJson.toString(), ObjectNode.class);
        assertEquals(ccdCaseDeserializer.buildRepresentative(jsonNode), representative);
    }

    @Test
    public void buildHearing() throws IOException {
        final ObjectNode jsonNode = mapper.readValue(hearingJson.toString(), ObjectNode.class);
        assertEquals(ccdCaseDeserializer.buildHearing(jsonNode), hearing);
    }

    @Test
    public void deserializeAllCcdCaseJson() throws Exception {
        assertEquals(mapper.readValue(allJson.toString(), CcdCase.class), ccdCase);
    }
}