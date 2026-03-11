package uk.gov.hmcts.reform.sscs.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.thirdparty.pdfservice.ResourceManager;

@Service
@Slf4j
@RequiredArgsConstructor
public class I18nBuilder {

    private final ResourceManager resourceManager;
    private final ObjectMapper objectMapper;

    public Map build() throws IOException {
        return objectMapper.readValue(resourceManager.getResource("/json/en.json"), HashMap.class);
    }
}
