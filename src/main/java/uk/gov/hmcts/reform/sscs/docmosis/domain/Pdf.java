package uk.gov.hmcts.reform.sscs.docmosis.domain;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Pdf {
    private final byte[] content;
    private final String name;

    public Pdf(byte[] content, String name) {
        this.content = content;
        this.name = name;
    }

    public byte[] getContent() {
        return ArrayUtils.clone(content);
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Pdf)) {
            return false;
        }
        return EqualsBuilder.reflectionEquals(this, other);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
