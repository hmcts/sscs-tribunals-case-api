package uk.gov.hmcts.reform.sscs.model.docassembly;

public class Paragraph extends TemplateComponent<String> {

    @Override
    public boolean isDescriptorTable() {
        return false;
    }

    @Override
    public boolean isParagraph() {
        return true;
    }

    public String toString() {
        return content;
    }

    public Paragraph(String id, String content) {
        super(id, content);
    }
}
