package uk.gov.hmcts.sscs.service.xml;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import org.junit.Test;

import uk.gov.hmcts.sscs.builder.CcdCaseBuilder;
import uk.gov.hmcts.sscs.domain.corecase.CcdCase;
import uk.gov.hmcts.sscs.domain.corecase.ExcludeDates;
import uk.gov.hmcts.sscs.domain.corecase.Hearing;

public class XmlUtilTest {

    @Test
    public void getCaseAsXml() {
        CcdCase ccdCase = CcdCaseBuilder.ccdCase();

        XmlUtil xmlUtil = new XmlUtil();
        String xml = xmlUtil.convertToXml(ccdCase, ccdCase.getClass());

        //appeal
        String xpathExpression = "/ccdCase/appeal";

        String actual = xmlUtil.extractValue(xml, xpathExpression + "/caseCode");
        assertThat(actual, is(ccdCase.getAppeal().getBenefitAsCaseCode()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/originatingOffice");
        assertThat(actual, is(ccdCase.getAppeal().getOriginatingOffice()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/dateOfDecision");
        assertThat(actual.toString(), is(ccdCase.getAppeal().getDateOfDecision().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy")).toString()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/dateAppealMade");
        assertThat(actual.toString(), is(ccdCase.getAppeal().getDateAppealMade().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy")).toString()));

        actual =  xmlUtil.extractValue(xml, xpathExpression + "/outOfTime");
        assertThat(actual, is(ccdCase.getAppeal().getOutOfTime()));

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

        //TODO: Assume there is only one hearing until business decides how to handle multiple
        actual = xmlUtil.extractValue(xml, xpathExpression + "/tribunalType");
        assertThat(actual, is(ccdCase.getHearings().get(0).getTribunalTypeText()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/languageInterpreterRequired");
        assertThat(actual, is(ccdCase.getHearings().get(0).getLanguageInterpreterRequiredForXml()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/signLanguageRequired");
        assertThat(actual, is(ccdCase.getHearings().get(0).getSignLanguageRequiredForXml()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/hearingLoopRequired");
        assertThat(actual, is(ccdCase.getHearings().get(0).getHearingLoopRequiredForXml()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/hasDisabilityNeeds");
        assertThat(actual, is(ccdCase.getHearings().get(0).getHasDisabilityNeedsForXml()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/additionalInformation");
        assertThat(actual, is(ccdCase.getHearings().get(0).getAdditionalInformation()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/excludeDates/exclude/start");
        assertThat(actual, is(ccdCase.getHearings().get(0).getExcludeDates()[0].getStart()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/excludeDates/exclude/end");
        assertThat(actual, is(ccdCase.getHearings().get(0).getExcludeDates()[0].getEnd()));
    }

    @Test
    public void excludeMultipleDates() {

        Hearing hearing = new Hearing(null, null, null, null, null, null, new ExcludeDates[]{
            new ExcludeDates("November 5th", "November 12th"),
            new ExcludeDates("December 1st", "December 2nd")});

        CcdCase ccdCase = new CcdCase(null, null, null, null,
                new ArrayList<Hearing>() {
                    {
                        add(hearing);
                    }
                });

        XmlUtil xmlUtil = new XmlUtil();
        String xpathExpression = "/ccdCase/hearing/excludeDates";

        String xml = xmlUtil.convertToXml(ccdCase, ccdCase.getClass());

        String actual = xmlUtil.extractValue(xml, xpathExpression + "/exclude/start");
        assertThat(actual, is(ccdCase.getHearings().get(0).getExcludeDates()[0].getStart()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/exclude/end");
        assertThat(actual, is(ccdCase.getHearings().get(0).getExcludeDates()[0].getEnd()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/exclude[2]/start");
        assertThat(actual, is(ccdCase.getHearings().get(0).getExcludeDates()[1].getStart()));

        actual = xmlUtil.extractValue(xml, xpathExpression + "/exclude[2]/end");
        assertThat(actual, is(ccdCase.getHearings().get(0).getExcludeDates()[1].getEnd()));
    }
}
