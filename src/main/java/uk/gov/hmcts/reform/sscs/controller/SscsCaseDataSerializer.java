package uk.gov.hmcts.reform.sscs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Component
public class SscsCaseDataSerializer {
    private final ObjectMapper objectMapper;

    public SscsCaseDataSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> serialize(SscsCaseData sscsCaseData) {
        return objectMapper.convertValue(sscsCaseData, Map.class);
    }
}
