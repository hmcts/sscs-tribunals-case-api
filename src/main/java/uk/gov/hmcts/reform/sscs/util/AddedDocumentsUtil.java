package uk.gov.hmcts.reform.sscs.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Slf4j
@Component
public class AddedDocumentsUtil {

    private final boolean workAllocationFeature;

    public AddedDocumentsUtil(@Value("${feature.work-allocation.enabled}") boolean workAllocationFeature) {
        this.workAllocationFeature = workAllocationFeature;
    }

    public void clearAddedDocumentsBeforeEventSubmit(SscsCaseData sscsCaseData) {
        sscsCaseData.getWorkAllocationFields().setAddedDocuments(null);
    }

    public void computeDocumentsAddedThisEvent(SscsCaseData sscsCaseData,
                                               List<String> documentsAddedThisEvent,
                                               Enum<EventType> eventType) {
        if (workAllocationFeature) {
            Map<String, Integer> documentsAddedThisEventCounts = new HashMap<>();
            Set<String> scannedDocumentTypes = new HashSet<>();
            for (String type : documentsAddedThisEvent) {
                if (documentsAddedThisEventCounts.containsKey(type)) {
                    Integer count = documentsAddedThisEventCounts.get(type);
                    documentsAddedThisEventCounts.put(type, ++count);
                } else {
                    documentsAddedThisEventCounts.put(type, 1);
                }
                scannedDocumentTypes.add(type);
            }

            if (!scannedDocumentTypes.isEmpty()) {
                sscsCaseData.setScannedDocumentTypes(new ArrayList<String>(scannedDocumentTypes));
                logMessage(sscsCaseData, eventType);
            } else {
                sscsCaseData.setScannedDocumentTypes(null);
            }

            if (!documentsAddedThisEvent.isEmpty()) {
                try {
                    sscsCaseData.getWorkAllocationFields().setAddedDocuments(new ObjectMapper()
                        .writeValueAsString(documentsAddedThisEventCounts));
                } catch (JsonProcessingException e) {
                    throw new IllegalStateException(e);
                }

                logMessage(sscsCaseData, eventType);
            } else {
                sscsCaseData.getWorkAllocationFields().setAddedDocuments(null);
            }
        }
    }

    private void logMessage(SscsCaseData sscsCaseData, Enum<EventType> eventType) {
        if (eventType == EventType.UPLOAD_DOCUMENT) {
            log.info("Case {} with event {} added documents: {} from MYA.", sscsCaseData.getCcdCaseId(), eventType,
                sscsCaseData.getWorkAllocationFields().getAddedDocuments());
        } else {
            log.info("Case {} with event {} added documents: {}.", sscsCaseData.getCcdCaseId(), eventType,
                sscsCaseData.getWorkAllocationFields().getAddedDocuments());
        }
    }
}
