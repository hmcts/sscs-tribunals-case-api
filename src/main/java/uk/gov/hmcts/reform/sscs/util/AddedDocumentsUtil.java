package uk.gov.hmcts.reform.sscs.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.AbstractDocument;
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
        if (workAllocationFeature) {
            sscsCaseData.getWorkAllocationFields().setAddedDocuments(null);
        }
    }

    public void computeDocumentsAddedThisEvent(SscsCaseData sscsCaseData,
                                               List<String> documentsAddedThisEvent,
                                               Enum<EventType> eventType) {
        if (workAllocationFeature) {
            updateScannedDocumentTypes(sscsCaseData, documentsAddedThisEvent);
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

    public void updateScannedDocumentTypes(SscsCaseData sscsCaseData, List<String> documentsAddedThisEvent) {
        if (workAllocationFeature) {
            if (documentsAddedThisEvent != null) {
                sscsCaseData.getWorkAllocationFields().setScannedDocumentTypes(documentsAddedThisEvent.stream().distinct().collect(Collectors.toList()));
            } else {
                sscsCaseData.getWorkAllocationFields().setScannedDocumentTypes(null);
            }
        }
    }

    public List<String> addedDocumentTypes(List<? extends AbstractDocument> previousDocuments, List<? extends AbstractDocument> documents) {
        Map<String, Optional<String>> existingDocumentTypes = null;
        if (previousDocuments != null) {
            existingDocumentTypes = previousDocuments.stream().collect(
                    Collectors.toMap(d -> d.getId(), d -> Optional.ofNullable(d.getValue().getDocumentType())));
        }

        return addedDocumentTypes(existingDocumentTypes, documents);
    }

    public List<String> addedDocumentTypes(Map<String, Optional<String>> existingDocumentTypes, List<? extends AbstractDocument> documents) {
        if (documents != null) {
            return documents.stream()
                    .filter(d -> isNewDocumentOrTypeChanged(existingDocumentTypes, d))
                    .map(d -> d.getValue().getDocumentType())
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private boolean isNewDocumentOrTypeChanged(Map<String, Optional<String>> existingDocumentTypes, AbstractDocument document) {
        if (existingDocumentTypes != null) {
            if (existingDocumentTypes.containsKey(document.getId())) {
                return !StringUtils.equals(document.getValue().getDocumentType(),
                        existingDocumentTypes.get(document.getId()).orElse(null));
            }
        }
        return true;
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
