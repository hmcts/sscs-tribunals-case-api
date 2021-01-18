package uk.gov.hmcts.reform.sscs.model.docassembly;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class DescriptorTable extends TemplateComponent<List<Descriptor>> {

    private boolean hideAnswerColumns;
    private boolean prefixWithQuestionNumber;

    public DescriptorTable(String id, List<Descriptor> content, boolean hideAnswerColumns, boolean prefixWithQuestionNumber) {
        super(id, content);
        this.hideAnswerColumns = hideAnswerColumns;
        this.prefixWithQuestionNumber = prefixWithQuestionNumber;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Descriptor descriptor : content) {
            if (hideAnswerColumns) {
                sb.append((prefixWithQuestionNumber ? (descriptor.getActivityQuestionNumber() + ".") : "") + descriptor.getActivityQuestionValue());
            } else {
                sb.append((prefixWithQuestionNumber ? (descriptor.getActivityQuestionNumber() + ".") : "") + descriptor.getActivityQuestionValue());
                sb.append("\t");
                sb.append(descriptor.getActivityAnswerLetter());
                sb.append(".");
                sb.append(descriptor.getActivityAnswerValue());
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

    @JsonProperty("is_hide_answer_columns")
    public boolean isHideAnswerColumns() {
        return hideAnswerColumns;
    }
}
