package uk.gov.hmcts.sscs.service.xml;

import org.junit.Test;
import uk.gov.hmcts.sscs.tribunals.domain.corecase.*;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class XmlUtilTest {

    private CCDCase setupCCDCase() {
        Appeal appeal = new Appeal("001","Birmingham2 SSO", ZonedDateTime.now(), ZonedDateTime.now(),false, true, false);
        Appellant appellant = new Appellant(createName(), createAddress(), "01234 984585", "test@email.com", "JT123456F", "Bedford");
        Appointee appointee = new Appointee(createName(), createAddress(), "01987 232323", "my@email.com");
        Representative representative = new Representative(createName(), createAddress(), "01576 765456", "rep@email.com", "Computer Towers Ltd.")   ;
        Hearing hearing = new Hearing(TribunalType.PAPER, true, true, true, false, "Additional information", new ExcludeDates[]{new ExcludeDates("November 5th", "November 12th")});

        return new CCDCase(appeal, appellant, appointee, representative, hearing);
    }

    private Name createName() {
        return new Name("Mr", "Joe", "Bloggs");
    }

    private Address createAddress() {
        return new Address("My Road", "End of the path", "Applebury", "Surrey", "GU12 5JT");
    }

    @Test
    public void getCaseAsXml() {
        CCDCase ccdCase = setupCCDCase();

        XmlUtil xmlUtil = new XmlUtil();
        String xml = xmlUtil.convertToXml(ccdCase, ccdCase.getClass());

        //appeal
        String xpathExpression = "/ccdCase/appeal";

        String actual = xmlUtil.extractValue(xml, xpathExpression + "/caseCode");
        assertThat(actual, is(ccdCase.getAppeal().getCaseCodeWithSuffix()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/originatingOffice");
        assertThat(actual, is(ccdCase.getAppeal().getOriginatingOffice()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/dateOfDecision");
        assertThat(actual.toString(), is(ccdCase.getAppeal().getDateOfDecision().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")).toString()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/dateAppealMade");
        assertThat(actual.toString(), is(ccdCase.getAppeal().getDateAppealMade().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")).toString()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/ftaReconsiderationEnclosed");
        assertThat(actual, is(ccdCase.getAppeal().getFtaReconsiderationEnclosed().toString()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/ftaReconsiderationEnclosed");
        assertThat(actual, is(ccdCase.getAppeal().getFtaReconsiderationEnclosed().toString()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/admissable");
        assertThat(actual, is(ccdCase.getAppeal().isAdmissable().toString()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/furtherEvidenceRequired");
        assertThat(actual, is(ccdCase.getAppeal().isFurtherEvidenceRequired().toString()));

        //appellant
        xpathExpression = "/ccdCase/appellant/name";

        actual = xmlUtil.extractValue(xml, xpathExpression + "/title");
        assertThat(actual, is(ccdCase.getAppellant().getName().getTitle()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/surname");
        assertThat(actual, is(ccdCase.getAppellant().getName().getSurname()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/initial");
        assertThat(actual, is(ccdCase.getAppellant().getName().getInitial()));

        xpathExpression = "/ccdCase/appellant/address";

        actual = xmlUtil.extractValue(xml, xpathExpression + "/line1");
        assertThat(actual, is(ccdCase.getAppellant().getAddress().getLine1()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/line2");
        assertThat(actual, is(ccdCase.getAppellant().getAddress().getLine2()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/town");
        assertThat(actual, is(ccdCase.getAppellant().getAddress().getTown()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/county");
        assertThat(actual, is(ccdCase.getAppellant().getAddress().getCounty()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/postcode");
        assertThat(actual, is(ccdCase.getAppellant().getAddress().getPostcode()));

        xpathExpression = "/ccdCase/appellant";

        actual = xmlUtil.extractValue(xml, xpathExpression + "/phone");
        assertThat(actual, is(ccdCase.getAppellant().getPhone()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/email");
        assertThat(actual, is(ccdCase.getAppellant().getEmail()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/nino");
        assertThat(actual, is(ccdCase.getAppellant().getNino()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/adminGroup");
        assertThat(actual, is(ccdCase.getAppellant().getAdminGroup()));

        //appointee
        xpathExpression = "/ccdCase/appointee/name";

        actual = xmlUtil.extractValue(xml, xpathExpression + "/title");
        assertThat(actual, is(ccdCase.getAppointee().getName().getTitle()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/surname");
        assertThat(actual, is(ccdCase.getAppointee().getName().getSurname()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/initial");
        assertThat(actual, is(ccdCase.getAppointee().getName().getInitial()));

        xpathExpression = "/ccdCase/appointee/address";

        actual = xmlUtil.extractValue(xml, xpathExpression + "/line1");
        assertThat(actual, is(ccdCase.getAppointee().getAddress().getLine1()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/line2");
        assertThat(actual, is(ccdCase.getAppointee().getAddress().getLine2()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/town");
        assertThat(actual, is(ccdCase.getAppointee().getAddress().getTown()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/county");
        assertThat(actual, is(ccdCase.getAppointee().getAddress().getCounty()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/postcode");
        assertThat(actual, is(ccdCase.getAppointee().getAddress().getPostcode()));

        //representative
        xpathExpression = "/ccdCase/representative/name";

        actual = xmlUtil.extractValue(xml, xpathExpression + "/title");
        assertThat(actual, is(ccdCase.getRepresentative().getName().getTitle()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/surname");
        assertThat(actual, is(ccdCase.getRepresentative().getName().getSurname()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/initial");
        assertThat(actual, is(ccdCase.getRepresentative().getName().getInitial()));

        xpathExpression = "/ccdCase/representative/address";

        actual = xmlUtil.extractValue(xml, xpathExpression + "/line1");
        assertThat(actual, is(ccdCase.getRepresentative().getAddress().getLine1()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/line2");
        assertThat(actual, is(ccdCase.getRepresentative().getAddress().getLine2()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/town");
        assertThat(actual, is(ccdCase.getRepresentative().getAddress().getTown()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/county");
        assertThat(actual, is(ccdCase.getRepresentative().getAddress().getCounty()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/postcode");
        assertThat(actual, is(ccdCase.getRepresentative().getAddress().getPostcode()));

        xpathExpression = "/ccdCase/representative";

        actual = xmlUtil.extractValue(xml, xpathExpression + "/organisation");
        assertThat(actual, is(ccdCase.getRepresentative().getOrganisation()));

        //hearing
        xpathExpression = "/ccdCase/hearing";

        actual = xmlUtil.extractValue(xml, xpathExpression + "/tribunalType");
        assertThat(actual, is(ccdCase.getHearing().getTribunalType().toString()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/languageInterpreterRequired");
        assertThat(actual, is(ccdCase.getHearing().isLanguageInterpreterRequired().toString()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/signLanguageRequired");
        assertThat(actual, is(ccdCase.getHearing().isSignLanguageRequired().toString()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/hearingLoopRequired");
        assertThat(actual, is(ccdCase.getHearing().isHearingLoopRequired().toString()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/hasDisabilityNeeds");
        assertThat(actual, is(ccdCase.getHearing().getHasDisabilityNeeds().toString()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/additionalInformation");
        assertThat(actual, is(ccdCase.getHearing().getAdditionalInformation()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/excludeDates/exclude/start");
        assertThat(actual, is(ccdCase.getHearing().getExcludeDates()[0].getStart()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/excludeDates/exclude/end");
        assertThat(actual, is(ccdCase.getHearing().getExcludeDates()[0].getEnd()));
    }

    @Test
    public void excludeMultipleDates() {

        Hearing hearing = new Hearing(null, null, null, null, null, null, new ExcludeDates[]{new ExcludeDates("November 5th", "November 12th"), new ExcludeDates("December 1st", "December 2nd")});

        CCDCase ccdCase = new CCDCase(null, null, null, null, hearing);
        XmlUtil xmlUtil = new XmlUtil();
        String xpathExpression = "/ccdCase/hearing/excludeDates";

        String xml = xmlUtil.convertToXml(ccdCase, ccdCase.getClass());

        String actual = xmlUtil.extractValue(xml, xpathExpression + "/exclude/start");
        assertThat(actual, is(ccdCase.getHearing().getExcludeDates()[0].getStart()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/exclude/end");
        assertThat(actual, is(ccdCase.getHearing().getExcludeDates()[0].getEnd()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/exclude[2]/start");
        assertThat(actual, is(ccdCase.getHearing().getExcludeDates()[1].getStart()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/exclude[2]/end");
        assertThat(actual, is(ccdCase.getHearing().getExcludeDates()[1].getEnd()));
    }
}
