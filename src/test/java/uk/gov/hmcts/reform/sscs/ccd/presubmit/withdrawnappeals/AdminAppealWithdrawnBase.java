package uk.gov.hmcts.reform.sscs.ccd.presubmit.withdrawnappeals;

import static com.fasterxml.jackson.databind.DeserializationFeature.READ_ENUMS_USING_TO_STRING;
import static com.fasterxml.jackson.databind.DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_ENUMS_USING_TO_STRING;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.apache.commons.io.FileUtils;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

public class AdminAppealWithdrawnBase {

    Callback<SscsCaseData> buildTestCallbackGivenEvent(EventType eventType, final String callbackName) throws IOException {
        var mapper = configureMapper();
        if (eventType == null) {
            return null;
        }
        String json = fetchData("callback/withdrawnappeals/" + callbackName);
        String jsonCallback = json.replace("EVENT_ID_PLACEHOLDER", eventType.getCcdType());
        SscsCaseCallbackDeserializer sscsCaseCallbackDeserializer = new SscsCaseCallbackDeserializer(mapper);
        return sscsCaseCallbackDeserializer.deserialize(jsonCallback);
    }

    private ObjectMapper configureMapper() {
        Jackson2ObjectMapperBuilder objectMapperBuilder = new Jackson2ObjectMapperBuilder()
                .featuresToEnable(READ_ENUMS_USING_TO_STRING)
                .featuresToEnable(READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
                .featuresToEnable(WRITE_ENUMS_USING_TO_STRING)
                .serializationInclusion(JsonInclude.Include.NON_ABSENT);

        var mapper = objectMapperBuilder.createXmlMapper(false).build();
        mapper.findAndRegisterModules();
        return mapper;
    }

    String fetchData(String s) throws IOException {
        String file = Objects.requireNonNull(getClass().getClassLoader().getResource(s)).getFile();
        return FileUtils.readFileToString(new File(file), StandardCharsets.UTF_8.name());
    }
}
