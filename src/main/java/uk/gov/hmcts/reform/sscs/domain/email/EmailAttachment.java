package uk.gov.hmcts.reform.sscs.domain.email;

import static uk.gov.hmcts.reform.sscs.model.AllowedFileTypes.getContentTypeForFileName;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamSource;

@Data
@Builder
@Slf4j
public class EmailAttachment {

    private final InputStreamSource data;
    private final String contentType;
    private final String filename;

    public static EmailAttachment pdf(byte[] content, String fileName) {
        return new EmailAttachment(
            new ByteArrayResource(content),
            "application/pdf",
            fileName
        );
    }

    public static EmailAttachment json(byte[] content, String fileName) {
        return new EmailAttachment(
            new ByteArrayResource(content),
            "application/json",
            fileName
        );
    }

    public static EmailAttachment file(byte[] content, String fileName) {
        return new EmailAttachment(
            new ByteArrayResource(content),
            getContentTypeForFileName(fileName),
            fileName
        );
    }
}
