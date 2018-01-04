package uk.gov.hmcts.sscs.json;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.sscs.builder.CcdCaseBuilder;
import uk.gov.hmcts.sscs.builder.SubmitYourAppealJsonBuilder;
import uk.gov.hmcts.sscs.domain.corecase.*;

public class CcdCaseDeserializerTest {

    private CcdCaseDeserializer ccdCaseDeserializer;
    private ObjectMapper mapper;
    private CcdCase ccdCase;
    private Appeal appeal;
    private Appellant appellant;
    private Appointee appointee;
    private Representative representative;
    private Hearing hearing;
    private JSONObject appealJson;
    private JSONObject appellantJson;
    private JSONObject appointeeJson;
    private JSONObject representativeJson;
    private JSONObject hearingJson;
    private JSONObject allJson;

    @Before
    public void setup() {

        ccdCaseDeserializer = new CcdCaseDeserializer();
        mapper = new ObjectMapper();

        ccdCase = CcdCaseBuilder.ccdCase();
        appeal = ccdCase.getAppeal();
        appellant = ccdCase.getAppellant();
        appointee = ccdCase.getAppointee();
        representative = ccdCase.getRepresentative();
        hearing = ccdCase.getHearings().get(0);

        allJson = SubmitYourAppealJsonBuilder.buildAllCaseJson(ccdCase);

        appealJson = SubmitYourAppealJsonBuilder.convertAppeal(appeal);
        appellantJson = SubmitYourAppealJsonBuilder.convertAppellant(appellant);
        appointeeJson = SubmitYourAppealJsonBuilder.convertAppointee(appointee);
        representativeJson = SubmitYourAppealJsonBuilder.convertRepresentative(representative);
        hearingJson = SubmitYourAppealJsonBuilder.convertHearing(hearing);
    }

    @Test
    public void deserializeAppealJson() throws IOException {
        final ObjectNode jsonNode = mapper.readValue(appealJson.toString(), ObjectNode.class);
        Appeal result = ccdCaseDeserializer.deserializeAppeal(jsonNode);

        assertEquals(appeal.getBenefit(), result.getBenefit());
        assertEquals(appeal.getDateOfDecision(), result.getDateOfDecision());
    }

    @Test
    public void convertJsonStringIntoDate() {
        LocalDate ldt = LocalDate.of(2017, Month.SEPTEMBER, 2);
        assertEquals(ccdCaseDeserializer.convertJsonStringIntoDate("2", "9", "2017"), ldt);
    }

    @Test
    public void deserializeAppellantJson() throws IOException {
        final ObjectNode jsonNode = mapper.readValue(appellantJson.toString(), ObjectNode.class);
        Appellant result = ccdCaseDeserializer.deserializeAppellant(jsonNode);

        assertEquals(appellant.getNino(), result.getNino());
        assertEquals(appellant.getName(), result.getName());
        assertEquals(appellant.getAddress(), result.getAddress());
        assertEquals(appellant.getPhone(), result.getPhone());
        assertEquals(appellant.getEmail(), result.getEmail());
    }

    @Test
    public void deserializeAppointeeJson() throws IOException {
        final ObjectNode jsonNode = mapper.readValue(appointeeJson.toString(), ObjectNode.class);
        assertEquals(appointee, ccdCaseDeserializer.deserializeAppointee(jsonNode));
    }

    @Test
    public void deserializeRepresentativeJson() throws IOException {
        final ObjectNode jsonNode = mapper.readValue(
                representativeJson.toString(), ObjectNode.class);
        assertEquals(representative, ccdCaseDeserializer.deserializeRepresentative(jsonNode));
    }

    @Test
    public void deserializeHearingJson() throws IOException {
        final ObjectNode jsonNode = mapper.readValue(hearingJson.toString(), ObjectNode.class);
        Hearing result = ccdCaseDeserializer.deserializeHearing(jsonNode);

        assertEquals(hearing.getTribunalType(), result.getTribunalType());
        assertEquals(hearing.getLanguageInterpreterRequired(),
                result.getLanguageInterpreterRequired());
        assertEquals(hearing.getSignLanguageRequired(), result.getSignLanguageRequired());
        assertEquals(hearing.getHearingLoopRequired(), result.getHearingLoopRequired());
        assertEquals(hearing.getHasDisabilityNeeds(), result.getHasDisabilityNeeds());
        assertEquals(hearing.getAdditionalInformation(), result.getAdditionalInformation());
    }

    @Test
    public void deserializeAllCcdCaseJson() throws Exception {
        CcdCase result = mapper.readValue(allJson.toString(), CcdCase.class);

        Appeal a = result.getAppeal();
        assertEquals(appeal.getBenefit(), a.getBenefit());
        assertEquals(appeal.getDateOfDecision(), a.getDateOfDecision());

        Appellant ap = result.getAppellant();
        assertEquals(appellant.getNino(), ap.getNino());
        assertEquals(appellant.getName(), ap.getName());
        assertEquals(appellant.getAddress(), ap.getAddress());
        assertEquals(appellant.getPhone(), ap.getPhone());
        assertEquals(appellant.getEmail(), ap.getEmail());

        assertEquals(appointee, result.getAppointee());
        assertEquals(representative, result.getRepresentative());

        Hearing h = result.getHearings().get(0);
        assertEquals(hearing.getTribunalType(), h.getTribunalType());
        assertEquals(hearing.getLanguageInterpreterRequired(), h.getLanguageInterpreterRequired());
        assertEquals(hearing.getSignLanguageRequired(), h.getSignLanguageRequired());
        assertEquals(hearing.getHearingLoopRequired(), h.getHearingLoopRequired());
        assertEquals(hearing.getHasDisabilityNeeds(), h.getHasDisabilityNeeds());
        assertEquals(hearing.getAdditionalInformation(), h.getAdditionalInformation());
    }
}