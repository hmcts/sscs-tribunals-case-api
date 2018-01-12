package uk.gov.hmcts.sscs.service.xml;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import uk.gov.hmcts.sscs.domain.corecase.*;

public class RoboticsXmlUtil {

    public Document convertToXml(CcdCase ccdCase) {

        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("ccdCase");
            doc.appendChild(rootElement);

            if (ccdCase.getAppeal() != null) {
                rootElement.appendChild(buildAppealElement(doc, ccdCase.getAppeal()));
            }

            if (ccdCase.getAppellant() != null) {
                rootElement.appendChild(buildAppellantElement(doc, ccdCase.getAppellant()));
            }

            if (ccdCase.getAppointee() != null) {
                rootElement.appendChild(buildAppointeeElement(doc, ccdCase.getAppointee()));
            }

            if (ccdCase.getRepresentative() != null) {
                rootElement.appendChild(buildRepresentativeElement(doc, ccdCase.getRepresentative()));
            }

            if (ccdCase.getHearing() != null) {
                rootElement.appendChild(buildHearingElement(doc, ccdCase.getHearing()));
            }

            createXmlFile(doc);

            return doc;

        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        }
        return null;
    }

    private Element buildAppealElement(Document doc, Appeal appeal) {
        Element appealElement = doc.createElement("appeal");

        if (appeal.getBenefitAsCaseCode() != null) {
            Element caseCodeElement = doc.createElement("caseCode");
            caseCodeElement.appendChild(doc.createTextNode(appeal.getBenefitAsCaseCode()));
            appealElement.appendChild(caseCodeElement);
        }

        if (appeal.getDateAppealMade() != null) {
            Element dateAppealMadeElement = doc.createElement("dateAppealMade");
            dateAppealMadeElement.appendChild(doc.createTextNode(xmlDateToString(appeal.getDateAppealMade())));
            appealElement.appendChild(dateAppealMadeElement);
        }

        if (appeal.getDateOfDecision() != null) {
            Element dateOfDecisionElement = doc.createElement("dateOfDecision");
            dateOfDecisionElement.appendChild(doc.createTextNode(xmlDateToString(appeal.getDateOfDecision())));
            appealElement.appendChild(dateOfDecisionElement);
        }

        if (appeal.getOriginatingOffice() != null) {
            Element originatingOfficeElement = doc.createElement("originatingOffice");
            originatingOfficeElement.appendChild(doc.createTextNode(appeal.getOriginatingOffice()));
            appealElement.appendChild(originatingOfficeElement);
        }

        if (appeal.getOutOfTime() != null) {
            Element outOfTimeElement = doc.createElement("outOfTime");
            outOfTimeElement.appendChild(doc.createTextNode(appeal.getOutOfTime()));
            appealElement.appendChild(outOfTimeElement);
        }

        return appealElement;
    }

    private Element buildAppellantElement(Document doc, Appellant appellant) {
        Element appellantElement = doc.createElement("appellant");

        if (appellant.getAddress() != null) {
            appellantElement.appendChild(buildAddress(doc, appellant.getAddress()));
        }

        if (appellant.getName() != null) {
            appellantElement.appendChild(buildName(doc, appellant.getName()));
        }

        if (appellant.getEmail() != null) {
            Element emailElement = doc.createElement("email");
            emailElement.appendChild(doc.createTextNode(appellant.getEmail()));
            appellantElement.appendChild(emailElement);
        }

        if (appellant.getPhone() != null) {
            Element phoneElement = doc.createElement("phone");
            phoneElement.appendChild(doc.createTextNode(appellant.getPhone()));
            appellantElement.appendChild(phoneElement);
        }

        if (appellant.getNino() != null) {
            Element ninoElement = doc.createElement("nino");
            ninoElement.appendChild(doc.createTextNode(appellant.getNino()));
            appellantElement.appendChild(ninoElement);
        }

        if (appellant.getAdminGroup() != null) {
            Element adminGroupElement = doc.createElement("adminGroup");
            adminGroupElement.appendChild(doc.createTextNode(appellant.getAdminGroup()));
            appellantElement.appendChild(adminGroupElement);
        }

        return appellantElement;
    }

    private Element buildAppointeeElement(Document doc, Appointee appointee) {
        Element appointeeElement = doc.createElement("appointee");

        if (appointee.getAddress() != null) {
            appointeeElement.appendChild(buildAddress(doc, appointee.getAddress()));
        }

        if (appointee.getName() != null) {
            appointeeElement.appendChild(buildName(doc, appointee.getName()));
        }

        if (appointee.getEmail() != null) {
            Element emailElement = doc.createElement("email");
            emailElement.appendChild(doc.createTextNode(appointee.getEmail()));
            appointeeElement.appendChild(emailElement);
        }

        if (appointee.getPhone() != null) {
            Element phoneElement = doc.createElement("phone");
            phoneElement.appendChild(doc.createTextNode(appointee.getPhone()));
            appointeeElement.appendChild(phoneElement);
        }

        return appointeeElement;
    }

    private Element buildRepresentativeElement(Document doc, Representative representative) {
        Element representativeElement = doc.createElement("representative");

        if (representative.getAddress() != null) {
            representativeElement.appendChild(buildAddress(doc, representative.getAddress()));
        }

        if (representative.getName() != null) {
            representativeElement.appendChild(buildName(doc, representative.getName()));
        }

        if (representative.getEmail() != null) {
            Element emailElement = doc.createElement("email");
            emailElement.appendChild(doc.createTextNode(representative.getEmail()));
            representativeElement.appendChild(emailElement);
        }

        if (representative.getPhone() != null) {
            Element phoneElement = doc.createElement("phone");
            phoneElement.appendChild(doc.createTextNode(representative.getPhone()));
            representativeElement.appendChild(phoneElement);
        }

        if (representative.getOrganisation() != null) {
            Element organisationElement = doc.createElement("organisation");
            organisationElement.appendChild(doc.createTextNode(representative.getOrganisation()));
            representativeElement.appendChild(organisationElement);
        }

        return representativeElement;
    }

    private Element buildHearingElement(Document doc, Hearing hearing) {
        Element hearingElement = doc.createElement("hearing");

        if (hearing.getAdditionalInformation() != null) {
            Element additionalInformationElement = doc.createElement("additionalInformation");
            additionalInformationElement.appendChild(doc.createTextNode(hearing.getAdditionalInformation()));
            hearingElement.appendChild(additionalInformationElement);
        }

        if (hearing.getHasDisabilityNeedsForXml() != null) {
            Element hasDisabilityNeedsElement = doc.createElement("hasDisabilityNeeds");
            hasDisabilityNeedsElement.appendChild(doc.createTextNode(hearing.getHasDisabilityNeedsForXml()));
            hearingElement.appendChild(hasDisabilityNeedsElement);
        }

        if (hearing.getHearingLoopRequiredForXml() != null) {
            Element hearingLoopRequiredElement = doc.createElement("hearingLoopRequired");
            hearingLoopRequiredElement.appendChild(doc.createTextNode(hearing.getHearingLoopRequiredForXml()));
            hearingElement.appendChild(hearingLoopRequiredElement);
        }

        if (hearing.getLanguageInterpreterRequiredForXml() != null) {
            Element languageInterpreterRequiredElement = doc.createElement("languageInterpreterRequired");
            languageInterpreterRequiredElement.appendChild(doc.createTextNode(hearing.getLanguageInterpreterRequiredForXml()));
            hearingElement.appendChild(languageInterpreterRequiredElement);
        }

        if (hearing.getSignLanguageRequiredForXml() != null) {
            Element signLanguageRequiredElement = doc.createElement("signLanguageRequired");
            signLanguageRequiredElement.appendChild(doc.createTextNode(hearing.getSignLanguageRequiredForXml()));
            hearingElement.appendChild(signLanguageRequiredElement);
        }

        if (hearing.getTribunalType() != null) {
            Element tribunalTypeElement = doc.createElement("tribunalType");
            tribunalTypeElement.appendChild(doc.createTextNode(hearing.getTribunalType().toString()));
            hearingElement.appendChild(tribunalTypeElement);
        }

        if (hearing.getExcludeDates() != null) {
            hearingElement.appendChild(buildExcludeDates(doc, hearing.getExcludeDates()));
        }

        return hearingElement;
    }

    private Element buildExcludeDates(Document doc, ExcludeDates[] excludeDates) {
        Element excludeDatesElement = doc.createElement("excludeDates");

        for (ExcludeDates d : excludeDates) {
            Element excludeElement = doc.createElement("exclude");

            if (d.getStart() != null) {
                Element startElement = doc.createElement("start");
                startElement.appendChild(doc.createTextNode(d.getStart()));
                excludeElement.appendChild(startElement);
            }

            if (d.getEnd() != null) {
                Element endElement = doc.createElement("end");
                endElement.appendChild(doc.createTextNode(d.getEnd()));
                excludeElement.appendChild(endElement);
            }

            excludeDatesElement.appendChild(excludeElement);
        }

        return excludeDatesElement;
    }

    private Element buildAddress(Document doc, Address address) {
        Element addressElement = doc.createElement("address");

        if (address.getLine1() != null) {
            Element line1Element = doc.createElement("line1");
            line1Element.appendChild(doc.createTextNode(address.getLine1()));
            addressElement.appendChild(line1Element);
        }

        if (address.getLine2() != null) {
            Element line2Element = doc.createElement("line2");
            line2Element.appendChild(doc.createTextNode(address.getLine2()));
            addressElement.appendChild(line2Element);
        }

        if (address.getTown() != null) {
            Element townElement = doc.createElement("town");
            townElement.appendChild(doc.createTextNode(address.getTown()));
            addressElement.appendChild(townElement);
        }

        if (address.getCounty() != null) {
            Element countyElement = doc.createElement("county");
            countyElement.appendChild(doc.createTextNode(address.getCounty()));
            addressElement.appendChild(countyElement);
        }

        if (address.getPostcode() != null) {
            Element postcodeElement = doc.createElement("postcode");
            postcodeElement.appendChild(doc.createTextNode(address.getPostcode()));
            addressElement.appendChild(postcodeElement);
        }

        return addressElement;
    }

    private Element buildName(Document doc, Name name) {
        Element nameElement = doc.createElement("name");

        if (name.getTitle() != null) {
            Element titleElement = doc.createElement("title");
            titleElement.appendChild(doc.createTextNode(name.getTitle()));
            nameElement.appendChild(titleElement);
        }

        if (name.getInitial() != null) {
            Element initialElement = doc.createElement("initial");
            initialElement.appendChild(doc.createTextNode(name.getInitial()));
            nameElement.appendChild(initialElement);
        }

        if (name.getSurname() != null) {
            Element surnameElement = doc.createElement("surname");
            surnameElement.appendChild(doc.createTextNode(name.getSurname()));
            nameElement.appendChild(surnameElement);
        }

        return nameElement;
    }

    private String xmlDateToString(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    public void createXmlFile(Document doc) {

        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();

            DOMSource source = new DOMSource(doc);

            // TODO: Write this to a real file and send to robotics
            // StreamResult result = new StreamResult(new File("C:\\file.xml"));
            // transformer.transform(source, result);
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        }
    }

    public String extractValue(Document doc, String xpathExpression) {
        String actual;
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            documentBuilderFactory.setIgnoringElementContentWhitespace(true);

            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();

            actual = xpath.evaluate(xpathExpression, doc, XPathConstants.STRING).toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return actual;
    }
}
