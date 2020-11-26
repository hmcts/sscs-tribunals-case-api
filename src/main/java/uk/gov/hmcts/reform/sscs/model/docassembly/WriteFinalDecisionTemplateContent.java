package uk.gov.hmcts.reform.sscs.model.docassembly;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class WriteFinalDecisionTemplateContent {

    @JsonProperty("template_content")
    private List<TemplateComponent<?>> components;

    public WriteFinalDecisionTemplateContent() {
        this.components = new ArrayList<>();
    }

    public List<TemplateComponent<?>> getComponents() {
        return components;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (TemplateComponent<?> component : components) {
            if (!component.toString().isBlank()) {
                sb.append(component.toString());
                sb.append("\n\n");
            }

        }
        return sb.toString();
    }

    public void addDescriptorTableIfPopulated(DescriptorTable desciptorTable) {
        if (desciptorTable.getContent() != null && !desciptorTable.getContent().isEmpty()) {
            this.components.add(desciptorTable);
        }
    }

    public void addComponent(TemplateComponent<?> component) {
        this.components.add(component);
    }
}
