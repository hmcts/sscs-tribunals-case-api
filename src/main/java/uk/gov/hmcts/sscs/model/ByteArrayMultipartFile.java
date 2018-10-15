package uk.gov.hmcts.sscs.model;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

public class ByteArrayMultipartFile implements MultipartFile {
    private final byte[] content;
    private final String name;
    private final MediaType contentType;

    public ByteArrayMultipartFile(byte[] content, String name, MediaType contentType) {
        this.content = content;
        this.name = name;
        this.contentType = contentType;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getOriginalFilename() {
        return name;
    }

    @Override
    public String getContentType() {
        return contentType.toString();
    }

    @Override
    public boolean isEmpty() {
        return content == null || content.length == 0;
    }

    @Override
    public long getSize() {
        return content.length;
    }

    @Override
    public byte[] getBytes() {
        return content;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(content);
    }

    @Override
    public void transferTo(File dest) throws IllegalStateException {
        throw new UnsupportedOperationException("Should only be used for byte array.");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ByteArrayMultipartFile that = (ByteArrayMultipartFile) o;
        return Arrays.equals(content, that.content)
                && Objects.equals(name, that.name)
                && Objects.equals(contentType, that.contentType);
    }

    @Override
    public int hashCode() {

        int result = Objects.hash(name, contentType);
        result = 31 * result + Arrays.hashCode(content);
        return result;
    }
}
