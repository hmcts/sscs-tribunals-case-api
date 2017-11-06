package uk.gov.hmcts.sscs.service.xml;

import org.w3c.dom.Document;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;

public class XmlUtil {
    public String convertToXml(Object source, Class... type) {
        String result;
        StringWriter sw = new StringWriter();
        try {
            JAXBContext caseContext = JAXBContext.newInstance(type);
            Marshaller caseMarshaller = caseContext.createMarshaller();
            caseMarshaller.marshal(source, sw);
            result = sw.toString();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    public String extractValue(String xml, String xpathExpression) {
        String actual;
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            documentBuilderFactory.setIgnoringElementContentWhitespace(true);
            DocumentBuilder docBuilder = documentBuilderFactory.newDocumentBuilder();

            byte[] bytes = xml.getBytes("UTF-8");
            InputStream inputStream = new ByteArrayInputStream(bytes);
            Document doc = docBuilder.parse(inputStream);
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();

            actual = xpath.evaluate(xpathExpression, doc, XPathConstants.STRING).toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return actual;
    }
}