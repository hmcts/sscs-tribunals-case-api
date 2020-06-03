package uk.gov.hmcts.reform.sscs.service.pdf.data;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

public class UploadedEvidence {
    private final Resource content;
    private final String name;
    private final String contentType;

    public static UploadedEvidence pdf(byte[] content, String name) {
        return new UploadedEvidence(new ByteArrayResource(content), name, "application/pdf");
    }

    public UploadedEvidence(Resource content, String name, String contentType) {
        this.content = content;
        this.name = name;
        this.contentType = contentType;
    }

    public Resource getContent() {
        return content;
    }

    public String getName() {
        return name;
    }

    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UploadedEvidence pdf = (UploadedEvidence) o;

        if (content != null ? !content.equals(pdf.content) : pdf.content != null) {
            return false;
        }
        if (name != null ? !name.equals(pdf.name) : pdf.name != null) {
            return false;
        }
        return contentType != null ? contentType.equals(pdf.contentType) : pdf.contentType == null;
    }

    @Override
    public int hashCode() {
        int result = content != null ? content.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (contentType != null ? contentType.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "UploadedEvidence{"
                + "content=" + content
                + ", name='" + name + '\''
                + ", contentType='" + contentType + '\''
                + '}';
    }
}
