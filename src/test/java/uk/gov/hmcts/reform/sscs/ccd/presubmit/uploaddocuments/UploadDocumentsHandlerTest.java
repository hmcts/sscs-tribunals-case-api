package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments;

import static com.fasterxml.jackson.databind.DeserializationFeature.READ_ENUMS_USING_TO_STRING;
import static com.fasterxml.jackson.databind.DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_ENUMS_USING_TO_STRING;
import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@RunWith(JUnitParamsRunner.class)
public class UploadDocumentsHandlerTest {

    private UploadDocumentsHandler handler = new UploadDocumentsHandler();
    private ObjectMapper mapper;

    @Before
    public void setUp() {
        Jackson2ObjectMapperBuilder objectMapperBuilder =
            new Jackson2ObjectMapperBuilder()
                .featuresToEnable(READ_ENUMS_USING_TO_STRING)
                .featuresToEnable(READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
                .featuresToEnable(WRITE_ENUMS_USING_TO_STRING)
                .serializationInclusion(JsonInclude.Include.NON_ABSENT);

        mapper = objectMapperBuilder.createXmlMapper(false).build();
        mapper.registerModule(new JavaTimeModule());
    }

    @Test
    @Parameters({
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT,withDwp,true",
        "ABOUT_TO_START,UPLOAD_DOCUMENT,withDwp,false",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT,appealCreated,false",
        "ABOUT_TO_SUBMIT,APPEAL_RECEIVED,withDwp,false",
        "null,APPEAL_RECEIVED,withDwp,false",
        "ABOUT_TO_SUBMIT,null,withDwp,false",
    })
    public void canHandle(@Nullable CallbackType callbackType, @Nullable EventType eventType, String state, boolean expectedResult)
        throws IOException {
        boolean actualResult = handler.canHandle(callbackType, buildTestCallbackGivenEvent(eventType, state
        ));
        assertEquals(expectedResult, actualResult);
    }

    private Callback<SscsCaseData> buildTestCallbackGivenEvent(EventType eventType, String state)
        throws IOException {
        //custom condition to return a null callback needed for one test scenario above
        if (eventType == null) {
            return null;
        }
        String json = fetchData("callback/" + "uploadDocumentCallback.json");
        String eventIdPlaceholder = json.replace("EVENT_ID_PLACEHOLDER", eventType.getCcdType());
        String jsonCallback = eventIdPlaceholder.replace("STATE_ID_PLACEHOLDER", state);

        SscsCaseCallbackDeserializer sscsCaseCallbackDeserializer = new SscsCaseCallbackDeserializer(mapper);
        return sscsCaseCallbackDeserializer.deserialize(jsonCallback);
    }

    private String fetchData(String s) throws IOException {
        String file = Objects.requireNonNull(getClass().getClassLoader().getResource(s)).getFile();
        return FileUtils.readFileToString(new File(file), StandardCharsets.UTF_8.name());
    }

    @Test
    public void handle() {
    }
}