package uk.gov.hmcts.sscs.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import uk.gov.hmcts.sscs.tribunals.domain.corecase.*;

//@SuppressWarnings("serial")
public class CcdCaseDeserializer extends StdDeserializer<CcdCase> {

    public CcdCaseDeserializer() {
        this(null);
    }

    public CcdCaseDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public CcdCase deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        // TODO: Quite a few assumptions have been made on field names as front end is still in dev.
        // Will need to review once that work is finished

        CcdCase ccdCase = new CcdCase();
        @SuppressWarnings("unchecked")
        ObjectCodec oc = jp.getCodec();
        JsonNode node = oc.readTree(jp);

        ccdCase.setAppeal(buildAppeal(node));
        ccdCase.setAppellant(buildAppellant(node));
        ccdCase.setAppointee(buildAppointee(node));
        ccdCase.setRepresentative(buildRepresentative(node));
        ccdCase.setHearing(buildHearing(node));

        return ccdCase;
    }

    public Appeal buildAppeal(JsonNode node) {
        Appeal appeal = new Appeal();

        appeal.setBenefit(node.has("BenefitType_benefitType")
                ? Benefit.getBenefitByType(node.get("BenefitType_benefitType").asText()) : null);

        // TODO: Where is originating office?
        // appeal.setOriginatingOffice(values.containsKey("???")
        // ? values.get("???").toString() : null);

        if (node.has("MRNDate_day") && node.has("MRNDate_month") && node.has("MRNDate_year")) {
            appeal.setDateOfDecision(
                convertJsonStringIntoDate(node.get("MRNDate_day").asText(),
                        node.get("MRNDate_month").asText(),
                        node.get("MRNDate_year").asText()
                )
            );
        }

        // TODO: Where is the date of appeal made?
        // appeal.setDateAppealMade(values.containsKey("???")
        // ? values.get("???").toString() : null);

        return appeal;
    }

    public LocalDate convertJsonStringIntoDate(String day, String month, String year) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d M yyyy");
        StringBuilder date = new StringBuilder(day).append(" " + month).append(" " + year);
        return LocalDate.parse(date.toString(), formatter);
    }

    public Appellant buildAppellant(JsonNode node) {
        Name name = new Name();

        name.setTitle(node.has("AppellantName_title")
                ? node.get("AppellantName_title").asText() : null);
        name.setFirst(node.has("AppellantName_firstName")
                ? node.get("AppellantName_firstName").asText() : null);
        name.setSurname(node.has("AppellantName_lastName")
                ? node.get("AppellantName_lastName").asText() : null);

        Appellant appellant = new Appellant();
        appellant.setName(name);

        Address address = new Address();
        address.setLine1(node.has("AppellantDetails_addressLine1")
                ? node.get("AppellantDetails_addressLine1").asText() : null);
        address.setLine2(node.has("AppellantDetails_addressLine2")
                ? node.get("AppellantDetails_addressLine2").asText() : null);
        address.setTown(node.has("AppellantDetails_townCity")
                ? node.get("AppellantDetails_townCity").asText() : null);
        address.setCounty(node.has("AppellantDetails_county")
                ? node.get("AppellantDetails_county").asText() : null);
        address.setPostcode(node.has("AppellantDetails_postCode")
                ? node.get("AppellantDetails_postCode").asText() : null);

        appellant.setAddress(address);

        appellant.setPhone(node.has("AppellantDetails_appellantPhoneNumber")
                ? node.get("AppellantDetails_appellantPhoneNumber").asText() : null);
        appellant.setEmail(node.has("AppellantDetails_emailAddress")
                ? node.get("AppellantDetails_emailAddress").asText() : null);
        appellant.setNino(node.has("AppellantDetails_niNumber")
                ? node.get("AppellantDetails_niNumber").asText() : null);

        return appellant;
    }

    public Appointee buildAppointee(JsonNode node) {
        Name name = new Name();

        //TODO: Lots of assumptions here
        name.setTitle(node.has("AppointeeDetails_title")
                ? node.get("AppointeeDetails_title").asText() : null);
        name.setFirst(node.has("AppointeeDetails_firstName")
                ? node.get("AppointeeDetails_firstName").asText() : null);
        name.setSurname(node.has("AppointeeDetails_lastName")
                ? node.get("AppointeeDetails_lastName").asText() : null);

        Appointee appointee = new Appointee();
        appointee.setName(name);

        Address address = new Address();
        address.setLine1(node.has("AppointeeDetails_addressLine1")
                ? node.get("AppointeeDetails_addressLine1").asText() : null);
        address.setLine2(node.has("AppointeeDetails_addressLine2")
                ? node.get("AppointeeDetails_addressLine2").asText() : null);
        address.setTown(node.has("AppointeeDetails_townCity")
                ? node.get("AppointeeDetails_townCity").asText() : null);
        address.setCounty(node.has("AppointeeDetails_county")
                ? node.get("AppointeeDetails_county").asText() : null);
        address.setPostcode(node.has("AppointeeDetails_postCode")
                ? node.get("AppointeeDetails_postCode").asText() : null);

        appointee.setAddress(address);

        appointee.setPhone(node.has("AppointeeDetails_phoneNumber")
                ? node.get("AppointeeDetails_phoneNumber").asText() : null);
        appointee.setEmail(node.has("AppointeeDetails_emailAddress")
                ? node.get("AppointeeDetails_emailAddress").asText() : null);

        return appointee;
    }

    public Representative buildRepresentative(JsonNode node) {

        Name name = new Name();

        name.setTitle(node.has("RepresentativeDetails_title")
                ? node.get("RepresentativeDetails_title").asText() : null);
        name.setFirst(node.has("RepresentativeDetails_firstName")
                ? node.get("RepresentativeDetails_firstName").asText() : null);
        name.setSurname(node.has("RepresentativeDetails_lastName")
                ? node.get("RepresentativeDetails_lastName").asText() : null);

        Representative representative = new Representative();

        representative.setName(name);

        Address address = new Address();
        address.setLine1(node.has("RepresentativeDetails_addressLine1")
                ? node.get("RepresentativeDetails_addressLine1").asText() : null);
        address.setLine2(node.has("RepresentativeDetails_addressLine2")
                ? node.get("RepresentativeDetails_addressLine2").asText() : null);
        address.setTown(node.has("RepresentativeDetails_townCity")
                ? node.get("RepresentativeDetails_townCity").asText() : null);
        address.setCounty(node.has("RepresentativeDetails_county")
                ? node.get("RepresentativeDetails_county").asText() : null);
        address.setPostcode(node.has("RepresentativeDetails_postCode")
                ? node.get("RepresentativeDetails_postCode").asText() : null);

        representative.setAddress(address);

        representative.setPhone(node.has("RepresentativeDetails_phoneNumber")
                ? node.get("RepresentativeDetails_phoneNumber").asText() : null);
        representative.setEmail(node.has("RepresentativeDetails_emailAddress")
                ? node.get("RepresentativeDetails_emailAddress").asText() : null);

        representative.setOrganisation(node.has("RepresentativeDetails_organisation")
                ? node.get("RepresentativeDetails_organisation").asText() : null);

        return representative;
    }

    public Hearing buildHearing(JsonNode node) {
        Hearing hearing = new Hearing();
        // TODO: Lots of assumptions here

        hearing.setTribunalType(node.has("tribunal_type")
                ? TribunalType.getTribunalByKey(node.get("tribunal_type").asText()) : null);

        hearing.setLanguageInterpreterRequired(node.has("hearing_interpreter_required")
                ? node.get("hearing_interpreter_required").asText() : "No");
        hearing.setSignLanguageRequired(node.has("sign_interpreter_required")
                ? node.get("sign_interpreter_required").asText() : "No");
        hearing.setHearingLoopRequired(node.has("hearing_loop_required")
                ? node.get("hearing_loop_required").asText() : "No");
        hearing.setHasDisabilityNeeds(node.has("disabled_access_required")
                ? node.get("disabled_access_required").asText() : "No");
        hearing.setAdditionalInformation(node.has("other_details")
                ? node.get("other_details").asText() : null);

        // TODO: Add exclude dates when we know what front end looks like
        // hearing.setExcludeDates(values.has("disabled_access_required")
        // ? values.get("disabled_access_required").toString() : "No");

        return hearing;
    }
}