package uk.gov.hmcts.reform.sscs.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Slf4j
@Component
public class AddedDocumentsUtil {

    private final boolean workAllocationFeature;

    public AddedDocumentsUtil(@Value("${feature.workAllocation.enabled}") boolean workAllocationFeature) {
        this.workAllocationFeature = workAllocationFeature;
    }

    public void computeDocumentsAddedThisEvent(SscsCaseData sscsCaseData, List<String> documentsAddedThisEvent,
                                               Enum<EventType> eventType) {
        if (workAllocationFeature) {
            Map<String, Integer> documentsAddedThisEventCounts = new HashMap<>();
            for (String type : documentsAddedThisEvent) {
                if (documentsAddedThisEventCounts.containsKey(type)) {
                    Integer count = documentsAddedThisEventCounts.get(type);
                    documentsAddedThisEventCounts.put(type, ++count);
                } else {
                    documentsAddedThisEventCounts.put(type, 1);
                }
            }

            if (!documentsAddedThisEvent.isEmpty()) {
                try {
                    sscsCaseData.setAddedDocuments(new ObjectMapper().writeValueAsString(documentsAddedThisEventCounts));
                } catch (JsonProcessingException e) {
                    throw new IllegalStateException(e);
                }
                log.info("Case {} with event {} added documents: {}.", sscsCaseData.getCcdCaseId(), eventType,
                    sscsCaseData.getAddedDocuments());
            }
        }
    }
}
