package uk.gov.hmcts.reform.sscs.docmosis.domain;

public class Template {

    private String templateName;
    private String hmctsDocName;

    public Template(String templateName, String hmctsDocName) {
        this.templateName = templateName;
        this.hmctsDocName = hmctsDocName;
    }

    public String getTemplateName() {
        return templateName;
    }

    public String getHmctsDocName() {
        return hmctsDocName;
    }
}
