package uk.gov.hmcts.reform.sscs.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

public class AddedDocumentsUtilTest {

    private AddedDocumentsUtil addedDocumentsUtil;

    private static final String CASE_ID = "123456";

    private SscsCaseData sscsCaseData;

    @Before
    public void setup() {
        addedDocumentsUtil = new AddedDocumentsUtil(true);
        sscsCaseData = new SscsCaseData();
        sscsCaseData.setCcdCaseId(CASE_ID);
    }

    @Test
    public void givenWorkAllocationFeatureFlagIsOff_shouldNotAddDocuments() {
        addedDocumentsUtil = new AddedDocumentsUtil(false);
        List<String> documentsAddedThisEvent = Collections.singletonList("reinstatementRequest");

        addedDocumentsUtil.computeDocumentsAddedThisEvent(sscsCaseData, documentsAddedThisEvent,
            EventType.ACTION_FURTHER_EVIDENCE);

        org.assertj.core.api.Assertions.assertThat(sscsCaseData.getWorkAllocationFields().getAddedDocuments())
            .as("The feature flag is off. No documents should be added.")
            .isNull();
    }


    @Test
    public void givenACaseWithScannedDocuments_shouldGenerateAMapOfSscsDocuments() throws JsonProcessingException {
        List<String> documentsAddedThisEvent = Collections.singletonList("reinstatementRequest");

        addedDocumentsUtil.computeDocumentsAddedThisEvent(sscsCaseData, documentsAddedThisEvent,
            EventType.ACTION_FURTHER_EVIDENCE);

        Map<String, Integer> addedDocuments = new ObjectMapper().readerFor(Map.class)
            .readValue(sscsCaseData.getWorkAllocationFields().getAddedDocuments());

        org.assertj.core.api.Assertions.assertThat(addedDocuments)
            .as("One document has been added to the case and should be added to added documents.")
            .containsOnly(org.assertj.core.api.Assertions.entry("reinstatementRequest", 1));
    }


    @Test
    public void givenACaseWithMultipleScannedDocuments_shouldGenerateAMapOfSscsDocumentsWithCorrectSums()
        throws JsonProcessingException {
        List<String> documentsAddedThisEvent = Arrays.asList("reinstatementRequest", "reinstatementRequest",
            "appellantEvidence", "confidentialityRequest");

        addedDocumentsUtil.computeDocumentsAddedThisEvent(sscsCaseData, documentsAddedThisEvent,
            EventType.ACTION_FURTHER_EVIDENCE);


        Map<String, Integer> addedDocuments = new ObjectMapper().readerFor(Map.class)
            .readValue(sscsCaseData.getWorkAllocationFields().getAddedDocuments());

        org.assertj.core.api.Assertions.assertThat(addedDocuments)
            .as("Multiple document have been added to the case and should be added to added documents.")
            .containsOnly(org.assertj.core.api.Assertions.entry("reinstatementRequest", 2),
                org.assertj.core.api.Assertions.entry("appellantEvidence", 1),
                org.assertj.core.api.Assertions.entry("confidentialityRequest", 1));
    }


    @Test
    public void givenACaseWiNoScannedDocuments_shouldGenerateAnEmptyMapOfSscsDocuments() {
        addedDocumentsUtil.computeDocumentsAddedThisEvent(sscsCaseData, new ArrayList<>(),
            EventType.ACTION_FURTHER_EVIDENCE);

        org.assertj.core.api.Assertions.assertThat(sscsCaseData.getWorkAllocationFields().getAddedDocuments())
            .as("No documents have been attached - map of added documents should be empty.")
            .isNull();
    }

}
