package uk.gov.hmcts.reform.sscs.email;

import static java.util.Objects.requireNonNull;

import lombok.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamSource;

@Value
public class EmailAttachment {

    private final InputStreamSource data;
    private final String contentType;
    private final String filename;

    public EmailAttachment(final InputStreamSource data, final String contentType,
                           final String filename) {
        requireNonNull(data);
        requireNonNull(contentType);
        requireNonNull(filename);
        this.data = data;
        this.contentType = contentType;
        this.filename = filename;
    }

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
}
