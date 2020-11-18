package uk.gov.hmcts.reform.sscs.model.docassembly;

import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class TemplateComponent<C> {

    @JsonProperty("content")
    protected C content;

    @JsonProperty("id")
    private String id;

    @JsonProperty("is_descriptor_table")
    public abstract boolean isDescriptorTable();

    @JsonProperty("is_paragraph")
    public abstract boolean isParagraph();

    public TemplateComponent(String id, C content) {
        this.content = content;
        this.id = id;
    }

    public C getContent() {
        return content;
    }

    public String getId() {
        return id;
    }
}
