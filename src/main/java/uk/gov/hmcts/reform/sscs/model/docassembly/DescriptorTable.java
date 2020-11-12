package uk.gov.hmcts.reform.sscs.model.docassembly;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class DescriptorTable extends TemplateComponent<List<Descriptor>> {

    private boolean showOnlyFirstColumn;

    public DescriptorTable(String id, List<Descriptor> content, boolean showOnlyFirstColumn) {
        super(id, content);
        this.showOnlyFirstColumn = showOnlyFirstColumn;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Descriptor descriptor : content) {
            if (showOnlyFirstColumn) {
                sb.append(descriptor.getActivityQuestionValue());
            } else {
                sb.append(descriptor.getActivityQuestionValue());
                sb.append("\t");
                sb.append(descriptor.getActivityAnswerValue());
                sb.append(".");
                sb.append(descriptor.getActivityAnswerLetter());
                sb.append("\t");
                sb.append(descriptor.getActivityAnswerPoints());
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    @Override
    public boolean isDescriptorTable() {
        return true;
    }

    @Override
    public boolean isParagraph() {
        return false;
    }

    @JsonProperty("is_show_only_first_column")
    public boolean isShowOnlyFirstColumn() {
        return showOnlyFirstColumn;
    }
}
