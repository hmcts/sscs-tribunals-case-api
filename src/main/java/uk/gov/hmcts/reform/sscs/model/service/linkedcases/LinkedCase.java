package uk.gov.hmcts.reform.sscs.model.service.linkedcases;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LinkedCase {
    private String caseReference;
    private String caseName;
    private List<String> reasonsForLink;
}
