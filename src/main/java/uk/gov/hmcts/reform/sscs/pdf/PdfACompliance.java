package uk.gov.hmcts.reform.sscs.pdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.transform.TransformerException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.graphics.color.PDOutputIntent;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.PDFAIdentificationSchema;
import org.apache.xmpbox.type.BadFieldValueException;
import org.apache.xmpbox.xml.XmpSerializer;

/**
 * Copied from https://github.com/keefmarshall/pdfpoc
 * For a new PDF, attempt to make it PDF/A-1a compliant.
 * We're not going to be able to do this with random PDFs submitted externally, but
 * if we follow a few simple rules when creating our own we should be able to get close.
 * NB currently only gets you to PDF/A-1b, to get to A-1a requires additional markup changes
 * on every element in the document, and we'd really need to purchase the ISO spec to fully
 * understand this.
 */
public class PdfACompliance {

    public enum PdfAPart {
        one(1);

        private int value;

        PdfAPart(int val) {
            value = val;
        }
    }

    public enum PdfAConformance {
        A, B
    }

    public void makeCompliant(PDDocument document) throws IOException {
        addXmpMetadata(document);
        setColourIntent(document);
    }

    private void addXmpMetadata(PDDocument document) throws IOException {

        // NB this is completely different from the examples on the PDFBox website
        // See instead the code example in Github:
        // https://github.com/apache/pdfbox/blob/trunk/examples/src/main/java/org/apache/pdfbox/examples/pdmodel/CreatePDFA.java
        XMPMetadata xmp = XMPMetadata.createXMPMetadata();
        try {
            // Not certain this is needed
            // DublinCoreSchema dc = xmp.createAndAddDublinCoreSchema();
            // dc.setTitle(file);

            PDFAIdentificationSchema id = xmp.createAndAddPFAIdentificationSchema();
            id.setPart(PdfAPart.one.value);
            id.setConformance(PdfAConformance.B.toString());

            XmpSerializer serializer = new XmpSerializer();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            serializer.serialize(xmp, baos, true);

            PDMetadata metadata = new PDMetadata(document);
            metadata.importXMPMetadata(baos.toByteArray());
            document.getDocumentCatalog().setMetadata(metadata);
        } catch (BadFieldValueException | TransformerException e) {
            // won't happen here, as the provided value is valid
            throw new IllegalArgumentException(e);
        }
    }

    private void setColourIntent(PDDocument document) throws IOException {
        // sRGB output intent - NOTE you need the actual ICC file in your resources
        // directory, it doesn't come with pdfbox's jar. You can download it from
        // the pdfbox examples repository.
        InputStream colorProfile =
                this.getClass().getResourceAsStream(
                        "/pdfa/sRGB.icc");
        PDOutputIntent intent = new PDOutputIntent(document, colorProfile);
        intent.setInfo("sRGB IEC61966-2.1");
        intent.setOutputCondition("sRGB IEC61966-2.1");
        intent.setOutputConditionIdentifier("sRGB IEC61966-2.1");
        intent.setRegistryName("http://www.color.org");
        document.getDocumentCatalog().addOutputIntent(intent);
    }
}
