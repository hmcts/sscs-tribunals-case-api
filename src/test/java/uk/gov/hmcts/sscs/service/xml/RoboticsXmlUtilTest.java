package uk.gov.hmcts.sscs.service.xml;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.junit.Test;
import org.w3c.dom.Document;
import uk.gov.hmcts.sscs.builder.CcdCaseBuilder;
import uk.gov.hmcts.sscs.domain.corecase.*;

public class RoboticsXmlUtilTest {

    RoboticsXmlUtil xmlUtil = new RoboticsXmlUtil();

    @Test
    public void getCaseAsXml() throws ParserConfigurationException, XPathExpressionException {
        CcdCase ccdCase = CcdCaseBuilder.ccdCase();

        Document xml = xmlUtil.convertToXml(ccdCase);

        String xpathExpression = "/ccdCase/appeal";

        String actual = extractValue(xml, xpathExpression + "/caseCode");
        assertThat(actual, is(ccdCase.getAppeal().getBenefitAsCaseCode()));

        actual = extractValue(xml, xpathExpression + "/originatingOffice");
        assertThat(actual, is(ccdCase.getAppeal().getOriginatingOffice()));

        actual = extractValue(xml, xpathExpression + "/dateOfDecision");
        assertThat(actual.toString(), is(ccdCase.getAppeal().getDateOfDecision().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy")).toString()));

        actual = extractValue(xml, xpathExpression + "/dateAppealMade");
        assertThat(actual.toString(), is(ccdCase.getAppeal().getDateAppealMade().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy")).toString()));

        actual =  extractValue(xml, xpathExpression + "/outOfTime");
        assertThat(actual, is(ccdCase.getAppeal().getOutOfTime()));

        //appellant
        xpathExpression = "/ccdCase/appellant/name";

        actual = extractValue(xml, xpathExpression + "/title");
        assertThat(actual, is(ccdCase.getAppellant().getName().getTitle()));

        actual = extractValue(xml, xpathExpression + "/surname");
        assertThat(actual, is(ccdCase.getAppellant().getName().getSurname()));

        actual = extractValue(xml, xpathExpression + "/initial");
        assertThat(actual, is(ccdCase.getAppellant().getName().getInitial()));

        xpathExpression = "/ccdCase/appellant/address";

        actual = extractValue(xml, xpathExpression + "/line1");
        assertThat(actual, is(ccdCase.getAppellant().getAddress().getLine1()));

        actual = extractValue(xml, xpathExpression + "/line2");
        assertThat(actual, is(ccdCase.getAppellant().getAddress().getLine2()));

        actual = extractValue(xml, xpathExpression + "/town");
        assertThat(actual, is(ccdCase.getAppellant().getAddress().getTown()));

        actual = extractValue(xml, xpathExpression + "/county");
        assertThat(actual, is(ccdCase.getAppellant().getAddress().getCounty()));

        actual = extractValue(xml, xpathExpression + "/postcode");
        assertThat(actual, is(ccdCase.getAppellant().getAddress().getPostcode()));

        xpathExpression = "/ccdCase/appellant";

        actual = extractValue(xml, xpathExpression + "/phone");
        assertThat(actual, is(ccdCase.getAppellant().getPhone()));

        actual = extractValue(xml, xpathExpression + "/email");
        assertThat(actual, is(ccdCase.getAppellant().getEmail()));

        actual = extractValue(xml, xpathExpression + "/nino");
        assertThat(actual, is(ccdCase.getAppellant().getNino()));

        actual = extractValue(xml, xpathExpression + "/adminGroup");
        assertThat(actual, is(ccdCase.getAppellant().getAdminGroup()));

        //appointee
        xpathExpression = "/ccdCase/appointee/name";

        actual = extractValue(xml, xpathExpression + "/title");
        assertThat(actual, is(ccdCase.getAppointee().getName().getTitle()));

        actual = extractValue(xml, xpathExpression + "/surname");
        assertThat(actual, is(ccdCase.getAppointee().getName().getSurname()));

        actual = extractValue(xml, xpathExpression + "/initial");
        assertThat(actual, is(ccdCase.getAppointee().getName().getInitial()));

        xpathExpression = "/ccdCase/appointee/address";

        actual = extractValue(xml, xpathExpression + "/line1");
        assertThat(actual, is(ccdCase.getAppointee().getAddress().getLine1()));

        actual = extractValue(xml, xpathExpression + "/line2");
        assertThat(actual, is(ccdCase.getAppointee().getAddress().getLine2()));

        actual = extractValue(xml, xpathExpression + "/town");
        assertThat(actual, is(ccdCase.getAppointee().getAddress().getTown()));

        actual = extractValue(xml, xpathExpression + "/county");
        assertThat(actual, is(ccdCase.getAppointee().getAddress().getCounty()));

        actual = extractValue(xml, xpathExpression + "/postcode");
        assertThat(actual, is(ccdCase.getAppointee().getAddress().getPostcode()));

        //representative
        xpathExpression = "/ccdCase/representative/name";

        actual = extractValue(xml, xpathExpression + "/title");
        assertThat(actual, is(ccdCase.getRepresentative().getName().getTitle()));

        actual = extractValue(xml, xpathExpression + "/surname");
        assertThat(actual, is(ccdCase.getRepresentative().getName().getSurname()));

        actual = extractValue(xml, xpathExpression + "/initial");
        assertThat(actual, is(ccdCase.getRepresentative().getName().getInitial()));

        xpathExpression = "/ccdCase/representative/address";

        actual = extractValue(xml, xpathExpression + "/line1");
        assertThat(actual, is(ccdCase.getRepresentative().getAddress().getLine1()));

        actual = extractValue(xml, xpathExpression + "/line2");
        assertThat(actual, is(ccdCase.getRepresentative().getAddress().getLine2()));

        actual = extractValue(xml, xpathExpression + "/town");
        assertThat(actual, is(ccdCase.getRepresentative().getAddress().getTown()));

        actual = extractValue(xml, xpathExpression + "/county");
        assertThat(actual, is(ccdCase.getRepresentative().getAddress().getCounty()));

        actual = extractValue(xml, xpathExpression + "/postcode");
        assertThat(actual, is(ccdCase.getRepresentative().getAddress().getPostcode()));

        xpathExpression = "/ccdCase/representative";

        actual = extractValue(xml, xpathExpression + "/organisation");
        assertThat(actual, is(ccdCase.getRepresentative().getOrganisation()));

        //hearing
        xpathExpression = "/ccdCase/hearing";

        //TODO: Assume there is only one hearing until business decides how to handle multiple
        actual = extractValue(xml, xpathExpression + "/tribunalType");
        assertThat(actual, is(ccdCase.getHearings().get(0).getTribunalTypeText()));

        actual = extractValue(xml, xpathExpression + "/languageInterpreterRequired");
        assertThat(actual, is(ccdCase.getHearings().get(0).getLanguageInterpreterRequiredForXml()));

        actual = extractValue(xml, xpathExpression + "/signLanguageRequired");
        assertThat(actual, is(ccdCase.getHearings().get(0).getSignLanguageRequiredForXml()));

        actual = extractValue(xml, xpathExpression + "/hearingLoopRequired");
        assertThat(actual, is(ccdCase.getHearings().get(0).getHearingLoopRequiredForXml()));

        actual = extractValue(xml, xpathExpression + "/hasDisabilityNeeds");
        assertThat(actual, is(ccdCase.getHearings().get(0).getHasDisabilityNeedsForXml()));

        actual = extractValue(xml, xpathExpression + "/additionalInformation");
        assertThat(actual, is(ccdCase.getHearings().get(0).getAdditionalInformation()));

        actual = extractValue(xml, xpathExpression + "/excludeDates/exclude/start");
        assertThat(actual, is(ccdCase.getHearings().get(0).getExcludeDates()[0].getStart()));

        actual = extractValue(xml, xpathExpression + "/excludeDates/exclude/end");
        assertThat(actual, is(ccdCase.getHearings().get(0).getExcludeDates()[0].getEnd()));
    }

    @Test
    public void ignoreNullExcludeStartDate() throws ParserConfigurationException, XPathExpressionException {
        Hearing hearing = new Hearing(null, null, null, null, null, null, new ExcludeDates[]{
            new ExcludeDates(null, "November 12th")});

        CcdCase ccdCase = new CcdCase(null, null, null, null,
                new ArrayList<Hearing>() {
                    {
                        add(hearing);
                    }
                });

        String xpathExpression = "/ccdCase/hearing/excludeDates";

        Document xml = xmlUtil.convertToXml(ccdCase);

        String actual = extractValue(xml, xpathExpression + "/exclude/start");
        assertThat(actual, is(""));

        actual = extractValue(xml, xpathExpression + "/exclude/end");
        assertThat(actual, is(ccdCase.getHearings().get(0).getExcludeDates()[0].getEnd()));
    }

    @Test
    public void ignoreNullExcludeEndDate() throws ParserConfigurationException, XPathExpressionException {

        Hearing hearing = new Hearing(null, null, null, null, null, null, new ExcludeDates[]{
            new ExcludeDates("November 5th", null)});

        CcdCase ccdCase = new CcdCase(null, null, null, null,
                new ArrayList<Hearing>() {
                    {
                        add(hearing);
                    }
                });

        String xpathExpression = "/ccdCase/hearing/excludeDates";

        Document xml = xmlUtil.convertToXml(ccdCase);


        String actual = extractValue(xml, xpathExpression + "/exclude/start");
        assertThat(actual, is(ccdCase.getHearings().get(0).getExcludeDates()[0].getStart()));

        actual = extractValue(xml, xpathExpression + "/exclude/end");
        assertThat(actual, is(""));
    }

    @Test
    public void excludeMultipleDates() throws ParserConfigurationException, XPathExpressionException {

        Hearing hearing = new Hearing(null, null, null, null, null, null, new ExcludeDates[]{
            new ExcludeDates("November 5th", "November 12th"),
            new ExcludeDates("December 1st", "December 2nd")});

        CcdCase ccdCase = new CcdCase(null, null, null, null,
                new ArrayList<Hearing>() {
                    {
                        add(hearing);
                    }
                });

        String xpathExpression = "/ccdCase/hearing/excludeDates";

        Document xml = xmlUtil.convertToXml(ccdCase);

        String actual = extractValue(xml, xpathExpression + "/exclude/start");
        assertThat(actual, is(ccdCase.getHearings().get(0).getExcludeDates()[0].getStart()));

        actual = extractValue(xml, xpathExpression + "/exclude/end");
        assertThat(actual, is(ccdCase.getHearings().get(0).getExcludeDates()[0].getEnd()));

        actual = extractValue(xml, xpathExpression + "/exclude[2]/start");
        assertThat(actual, is(ccdCase.getHearings().get(0).getExcludeDates()[1].getStart()));

        actual = extractValue(xml, xpathExpression + "/exclude[2]/end");
        assertThat(actual, is(ccdCase.getHearings().get(0).getExcludeDates()[1].getEnd()));
    }

    @Test
    public void checkNullHearingDoesNotAppendChild() throws ParserConfigurationException, XPathExpressionException {
        CcdCase ccdCase = new CcdCase(null, null, null, null, null);

        Document xml = xmlUtil.convertToXml(ccdCase);

        assertThat(extractValue(xml, "/ccdCase/hearing"), is(""));
    }

    @Test
    public void checkEmptyHearingDetailsDoesNotAppendHearing() throws ParserConfigurationException, XPathExpressionException {
        Hearing hearing = new Hearing(null, null, null, null, null, null, null);

        CcdCase ccdCase = new CcdCase(null, null, null, null, new ArrayList<Hearing>() {
            {
                add(hearing);
            }
        });

        Document xml = xmlUtil.convertToXml(ccdCase);

        assertThat(extractValue(xml, "/ccdCase/hearing/additionalInformation"), is(""));
        assertThat(extractValue(xml, "/ccdCase/hearing/tribunalType"), is(""));
    }

    @Test
    public void checkEmptyAppealDoesNotAppendAppeal() throws ParserConfigurationException, XPathExpressionException {
        CcdCase ccdCase = new CcdCase(new Appeal(), null, null, null, null);

        Document xml = xmlUtil.convertToXml(ccdCase);

        assertThat(extractValue(xml, "/ccdCase/appeal/caseCode"), is(""));
        assertThat(extractValue(xml, "/ccdCase/appeal/dateAppealMade"), is(""));
        assertThat(extractValue(xml, "/ccdCase/appeal/dateOfDecision"), is(""));
        assertThat(extractValue(xml, "/ccdCase/appeal/originatingOffice"), is(""));
        assertThat(extractValue(xml, "/ccdCase/appeal/outOfTime"), is(""));
    }

    @Test
    public void checkEmptyAppellantDoesNotAppendAppellant() throws ParserConfigurationException, XPathExpressionException {
        CcdCase ccdCase = new CcdCase(null, new Appellant(), null, null, null);

        Document xml = xmlUtil.convertToXml(ccdCase);

        assertThat(extractValue(xml, "/ccdCase/appellant/email"), is(""));
        assertThat(extractValue(xml, "/ccdCase/appellant/phone"), is(""));
        assertThat(extractValue(xml, "/ccdCase/appellant/name"), is(""));
        assertThat(extractValue(xml, "/ccdCase/appellant/phone"), is(""));
        assertThat(extractValue(xml, "/ccdCase/appellant/nino"), is(""));
        assertThat(extractValue(xml, "/ccdCase/appellant/adminGroup"), is(""));
    }

    @Test
    public void checkEmptyAppointeeDoesNotAppendAppointee() throws ParserConfigurationException, XPathExpressionException {
        CcdCase ccdCase = new CcdCase(null, null, new Appointee(), null, null);

        Document xml = xmlUtil.convertToXml(ccdCase);

        assertThat(extractValue(xml, "/ccdCase/appointee/email"), is(""));
        assertThat(extractValue(xml, "/ccdCase/appointee/phone"), is(""));
        assertThat(extractValue(xml, "/ccdCase/appointee/address"), is(""));
        assertThat(extractValue(xml, "/ccdCase/appointee/name"), is(""));
    }

    @Test
    public void checkEmptyRepresentativeDoesNotAppendRepresentative() throws ParserConfigurationException, XPathExpressionException {
        CcdCase ccdCase = new CcdCase(null, null, null, new Representative(), null);

        Document xml = xmlUtil.convertToXml(ccdCase);

        assertThat(extractValue(xml, "/ccdCase/representative/email"), is(""));
        assertThat(extractValue(xml, "/ccdCase/representative/phone"), is(""));
        assertThat(extractValue(xml, "/ccdCase/representative/address"), is(""));
        assertThat(extractValue(xml, "/ccdCase/representative/name"), is(""));
        assertThat(extractValue(xml, "/ccdCase/representative/organisation"), is(""));
    }

    @Test
    public void checkEmptyAddressDoesNotAppendAddress() throws ParserConfigurationException, XPathExpressionException {
        Representative rep = new Representative(null, new Address(null, null, null, null, null), null, null, null);

        CcdCase ccdCase = new CcdCase(null, null, null, rep, null);

        Document xml = xmlUtil.convertToXml(ccdCase);

        assertThat(extractValue(xml, "/ccdCase/representative/address/line1"), is(""));
        assertThat(extractValue(xml, "/ccdCase/representative/address/line2"), is(""));
        assertThat(extractValue(xml, "/ccdCase/representative/address/town"), is(""));
        assertThat(extractValue(xml, "/ccdCase/representative/address/county"), is(""));
        assertThat(extractValue(xml, "/ccdCase/representative/address/postcode"), is(""));
    }

    @Test
    public void checkEmptyNameDoesNotAppendName() throws ParserConfigurationException, XPathExpressionException {
        Representative rep = new Representative(new Name(null, null, null), null, null,null, null);

        CcdCase ccdCase = new CcdCase(null, null, null, rep, null);

        Document xml = xmlUtil.convertToXml(ccdCase);

        assertThat(extractValue(xml, "/ccdCase/representative/name/title"), is(""));
        assertThat(extractValue(xml, "/ccdCase/representative/name/initial"), is(""));
        assertThat(extractValue(xml, "/ccdCase/representative/name/surname"), is(""));
    }

    private String extractValue(Document doc, String xpathExpression) throws XPathExpressionException {
        String actual = "";

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        documentBuilderFactory.setIgnoringElementContentWhitespace(true);

        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();

        actual = xpath.evaluate(xpathExpression, doc, XPathConstants.STRING).toString();

        return actual;
    }
}
