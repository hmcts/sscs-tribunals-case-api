package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments;

import static com.fasterxml.jackson.databind.DeserializationFeature.READ_ENUMS_USING_TO_STRING;
import static com.fasterxml.jackson.databind.DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_ENUMS_USING_TO_STRING;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsFurtherEvidenceDoc;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsFurtherEvidenceDocDetails;

public class BaseHandlerTest {
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

    protected Callback<SscsCaseData> buildTestCallbackGivenData(EventType eventType, String state,
                                                                String documentType, String documentType2,
                                                                String filePath)
        throws IOException {
        //edge case test scenario: callback is null
        if (eventType == null) {
            return null;
        }
        String json = fetchData(filePath);
        json = json.replace("EVENT_ID_PLACEHOLDER", eventType.getCcdType());
        json = json.replace("STATE_ID_PLACEHOLDER", state);
        json = json.replace("DOCUMENT_TYPE2_PLACEHOLDER", documentType2);
        json = json.replace("DOCUMENT_TYPE_PLACEHOLDER",
            (!documentType.equals("nullSscsDocuments")) ? documentType : "it will be null");

        SscsCaseCallbackDeserializer sscsCaseCallbackDeserializer = new SscsCaseCallbackDeserializer(mapper);
        Callback<SscsCaseData> sscsCaseDataCallback = sscsCaseCallbackDeserializer.deserialize(json);

        configureTestDataForEdgeCaseScenarios(documentType, sscsCaseDataCallback);
        return sscsCaseDataCallback;
    }

    private void configureTestDataForEdgeCaseScenarios(String documentType,
                                                       Callback<SscsCaseData> sscsCaseDataCallback) {
        if (documentType.equals("nullSscsDocuments")) {
            sscsCaseDataCallback.getCaseDetails().getCaseData().setDraftSscsFurtherEvidenceDocument(null);
        }
        if (documentType.equals("nullDocumentType")) {
            List<SscsFurtherEvidenceDoc> sscsDocumentsWithNullDocTypes = sscsCaseDataCallback.getCaseDetails()
                .getCaseData().getDraftSscsFurtherEvidenceDocument().stream()
                .map(doc -> new SscsFurtherEvidenceDoc(SscsFurtherEvidenceDocDetails.builder().build()))
                .collect(Collectors.toList());
            sscsCaseDataCallback.getCaseDetails().getCaseData()
                .setDraftSscsFurtherEvidenceDocument(sscsDocumentsWithNullDocTypes);
        }
        if (documentType.equals("nullSscsDocument")) {
            List<SscsFurtherEvidenceDoc> sscsFurtherEvidenceDocs = new ArrayList<>();
            sscsFurtherEvidenceDocs.add(SscsFurtherEvidenceDoc.builder().build());
            sscsCaseDataCallback.getCaseDetails().getCaseData()
                .setDraftSscsFurtherEvidenceDocument(sscsFurtherEvidenceDocs);
        }
    }

    protected String fetchData(final String filePath) throws IOException {
        String file = Objects.requireNonNull(getClass().getClassLoader()
            .getResource("callback/" + filePath)).getFile();
        return FileUtils.readFileToString(new File(file), StandardCharsets.UTF_8.name());
    }
}
