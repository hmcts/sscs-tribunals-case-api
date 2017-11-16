package uk.gov.hmcts.sscs.json;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.util.HashMap;
import java.util.Iterator;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

import uk.gov.hmcts.sscs.builder.CcdCaseBuilder;
import uk.gov.hmcts.sscs.builder.SubmitYourAppealJsonBuilder;
import uk.gov.hmcts.sscs.email.SubmitYourAppealEmail;
import uk.gov.hmcts.sscs.tribunals.domain.corecase.*;

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

        ccdCase = CcdCaseBuilder.ccdCase();
        appeal = ccdCase.getAppeal();
        appellant = ccdCase.getAppellant();
        appointee = ccdCase.getAppointee();
        representative = ccdCase.getRepresentative();
        hearing = ccdCase.getHearing();

        allJson = SubmitYourAppealJsonBuilder.buildAllCaseJson(ccdCase);

        appealJson = SubmitYourAppealJsonBuilder.convertAppeal(appeal);
        appellantJson = SubmitYourAppealJsonBuilder.convertAppellant(appellant);
        appointeeJson = SubmitYourAppealJsonBuilder.convertAppointee(appointee);
        representativeJson = SubmitYourAppealJsonBuilder.convertRepresentative(representative);
        hearingJson = SubmitYourAppealJsonBuilder.convertHearing(hearing);

        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(CcdCase.class, ccdCaseDeserializer);
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