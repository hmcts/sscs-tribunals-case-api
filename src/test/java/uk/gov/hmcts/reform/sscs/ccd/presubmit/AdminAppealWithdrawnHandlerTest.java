package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static com.fasterxml.jackson.databind.DeserializationFeature.READ_ENUMS_USING_TO_STRING;
import static com.fasterxml.jackson.databind.DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_ENUMS_USING_TO_STRING;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
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
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@RunWith(JUnitParamsRunner.class)
public class AdminAppealWithdrawnHandlerTest {

    private final AdminAppealWithdrawnHandler adminAppealWithdrawnHandler = new AdminAppealWithdrawnHandler();
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
        "ABOUT_TO_SUBMIT,ADMIN_APPEAL_WITHDRAWN,true",
        "ABOUT_TO_START,ADMIN_APPEAL_WITHDRAWN,false",
        "SUBMITTED,ADMIN_APPEAL_WITHDRAWN,false",
        "MID_EVENT,ADMIN_APPEAL_WITHDRAWN,false",
        "ABOUT_TO_SUBMIT,ISSUE_FURTHER_EVIDENCE,false",
        "null,ADMIN_APPEAL_WITHDRAWN,false",
        "ABOUT_TO_SUBMIT,null,false",
    })
    public void canHandle(@Nullable CallbackType callbackType, @Nullable EventType eventType, boolean expectedResult)
        throws IOException {
        Callback<SscsCaseData> callback = buildTestCallback(eventType);

        boolean actualResult = adminAppealWithdrawnHandler.canHandle(callbackType, callback);

        assertEquals(expectedResult, actualResult);
    }

    private Callback<SscsCaseData> buildTestCallback(EventType eventType) throws IOException {
        if (eventType == null) return null;
        String json = fetchData("callback/adminAppealWithdrawnCallback.json");
        String jsonCallback = json.replace("EVENT_ID_PLACEHOLDER", eventType.getCcdType());
        SscsCaseCallbackDeserializer sscsCaseCallbackDeserializer = new SscsCaseCallbackDeserializer(mapper);
        return sscsCaseCallbackDeserializer.deserialize(jsonCallback);
    }

    @Test
    public void handle() throws IOException {
        PreSubmitCallbackResponse<SscsCaseData> actualResult = adminAppealWithdrawnHandler.handle(
            CallbackType.ABOUT_TO_SUBMIT, buildTestCallback(EventType.ADMIN_APPEAL_WITHDRAWN));

        String expectedCaseData = fetchData("callback/adminAppealWithdrawnExpectedCaseData.json");
        assertEquals("withdrawalReceived", actualResult.getData().getDwpState());
        assertThatJson(actualResult.getData()).isEqualTo(expectedCaseData);
    }

    private String fetchData(String s) throws IOException {
        String file = Objects.requireNonNull(getClass().getClassLoader().getResource(s)).getFile();
        return FileUtils.readFileToString(new File(file), StandardCharsets.UTF_8.name());
    }
}