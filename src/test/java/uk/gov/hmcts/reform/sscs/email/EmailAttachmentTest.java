package uk.gov.hmcts.reform.sscs.email;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.domain.email.EmailAttachment;
import uk.gov.hmcts.reform.sscs.exception.UnknownFileTypeException;

@RunWith(MockitoJUnitRunner.class)
public class EmailAttachmentTest {
    @Test
    public void file() {
        Map<String, String> allowedContentTypes = new HashMap();

        allowedContentTypes.put("doc", "application/msword");
        allowedContentTypes.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        allowedContentTypes.put("jpeg", "image/jpeg");
        allowedContentTypes.put("jpg", "image/jpeg");
        allowedContentTypes.put("pdf", "application/pdf");
        allowedContentTypes.put("png", "image/png");
        allowedContentTypes.put("ppt", "application/vnd.ms-powerpoint");
        allowedContentTypes.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        allowedContentTypes.put("txt", "text/plain");
        allowedContentTypes.put("xls", "application/vnd.ms-excel");
        allowedContentTypes.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        allowedContentTypes.put("bmp", "image/bmp");
        allowedContentTypes.put("tiff", "image/tiff");
        allowedContentTypes.put("tif", "image/tiff");
        allowedContentTypes.put("dotx", "application/vnd.openxmlformats-officedocument.wordprocessingml.template");
        allowedContentTypes.put("xltx", "application/vnd.openxmlformats-officedocument.spreadsheetml.template");
        allowedContentTypes.put("potx", "application/vnd.openxmlformats-officedocument.presentationml.template");
        allowedContentTypes.put("ppsx", "application/vnd.openxmlformats-officedocument.presentationml.slideshow");
        allowedContentTypes.put("xlt", "application/vnd.ms-excel");
        allowedContentTypes.put("dot", "application/msword");
        allowedContentTypes.put("xla", "application/vnd.ms-excel");
        allowedContentTypes.put("xlsb", "application/vnd.ms-excel.sheet.binary.macroEnabled.12");
        allowedContentTypes.put("pot", "application/vnd.ms-powerpoint");
        allowedContentTypes.put("pps", "application/vnd.ms-powerpoint");
        allowedContentTypes.put("ppa", "application/vnd.ms-powerpoint");

        byte[] data = {};

        for (String contentType : allowedContentTypes.keySet()) {
            String filename = "somefile." + contentType;

            EmailAttachment actual = EmailAttachment.file(data, filename);

            assertEquals(filename, actual.getFilename());
            assertEquals(allowedContentTypes.get(contentType), actual.getContentType());
            assertNotNull(actual.getData());
        }
    }

    @Test(expected = RuntimeException.class)
    public void unknownContentTypeForFile() {
        byte[] data = {};

        EmailAttachment.file(data, "somefile.unknown");
    }

    @Test(expected = UnknownFileTypeException.class)
    public void noExtensionInFileName() {
        byte[] data = {};

        EmailAttachment.file(data, "somefile");
    }

    @Test
    public void pdf() {
        byte[] data = {};
        String filename = "somefile.pdf";

        EmailAttachment actual = EmailAttachment.pdf(data, filename);

        assertEquals(filename, actual.getFilename());
        assertEquals("application/pdf", actual.getContentType());
        assertNotNull(actual.getData());
    }

    @Test
    public void json() {
        byte[] data = {};
        String filename = "somefile.json";

        EmailAttachment actual = EmailAttachment.json(data, filename);

        assertEquals(filename, actual.getFilename());
        assertEquals("application/json", actual.getContentType());
        assertNotNull(actual.getData());
    }
}
