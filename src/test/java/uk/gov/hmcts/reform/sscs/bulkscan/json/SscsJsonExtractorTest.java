package uk.gov.hmcts.reform.sscs.bulkscan.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static uk.gov.hmcts.reform.sscs.bulkscan.helper.OcrDataBuilderTest.buildScannedValidationOcrData;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain.ExceptionRecord;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain.InputScannedDoc;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain.ScannedData;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;

public class SscsJsonExtractorTest {

    @Autowired
    private SscsJsonExtractor sscsJsonExtractor;

    @Before
    public void setup() {
        sscsJsonExtractor = new SscsJsonExtractor();
    }

    final LocalDateTime now = LocalDateTime.now();

    @Test
    public void givenExceptionCaseData_thenExtractIntoKeyValuePairs() {

        Map<String, Object> valueMap = new HashMap<>();

        valueMap.put("name", "person1_first_name");
        valueMap.put("value", "Bob");

        ScannedData result = sscsJsonExtractor.extractJson(buildExceptionRecordFromOcrData(valueMap));

        assertEquals("Bob", result.getOcrCaseData().get("person1_first_name"));
    }

    @Test
    public void givenDocumentData_thenExtractIntoSscsDocumentObject() {

        List<InputScannedDoc> inputScannedDocs = new ArrayList<>();
        InputScannedDoc scannedDoc = new InputScannedDoc("1",
            "my subtype", DocumentLink.builder().documentUrl("www.test.com").build(),
            "4", "Test_doc", now, now);

        inputScannedDocs.add(scannedDoc);

        ScannedData result = sscsJsonExtractor.extractJson(ExceptionRecord.builder().scannedDocuments(inputScannedDocs).build());

        assertEquals(scannedDoc, result.getRecords().getFirst());
    }

    @Test
    public void givenMultipleDocumentData_thenExtractIntoSscsDocumentObject() {

        List<InputScannedDoc> inputScannedDocs = new ArrayList<>();
        InputScannedDoc scannedDoc1 = new InputScannedDoc("1",
            "my subtype1", DocumentLink.builder().documentUrl("www.test.com").build(),
            "4", "Test_doc", now, now);

        InputScannedDoc scannedDoc2 = new InputScannedDoc("2",
            "my subtype2", DocumentLink.builder().documentUrl("www.test.com").build(),
            "4", "Test_doc", now, now);

        inputScannedDocs.add(scannedDoc1);
        inputScannedDocs.add(scannedDoc2);

        ScannedData result = sscsJsonExtractor.extractJson(ExceptionRecord.builder().scannedDocuments(inputScannedDocs).build());

        assertEquals(scannedDoc1, result.getRecords().get(0));
        assertEquals(scannedDoc2, result.getRecords().get(1));
    }

    @Test
    public void givenExceptionCaseDataWithEmptyData_thenExtractIntoKeyValuePairs() {

        Map<String, Object> valueMap = new HashMap<>();

        valueMap.put("name", "appellant_first_name");
        valueMap.put("value", null);

        ScannedData result = sscsJsonExtractor.extractJson(buildExceptionRecordFromOcrData(valueMap));

        assertNull(result.getOcrCaseData().get("appellant_first_name"));
    }

    @Test
    public void givenExceptionCaseDataWithValidOpeningDate_thenSetCorrectOpeningDate() {

        ScannedData result = sscsJsonExtractor.extractJson(ExceptionRecord.builder().openingDate(LocalDateTime.now().minusYears(3)).build());

        assertEquals(LocalDateTime.now().minusYears(3).toLocalDate().toString(), result.getOpeningDate());
    }

    @Test
    public void givenExceptionCaseDataWithNullOpeningDate_thenSetOpeningDateToToday() {

        ScannedData result = sscsJsonExtractor.extractJson(ExceptionRecord.builder().openingDate(null).build());

        assertEquals(LocalDateTime.now().toLocalDate().toString(), result.getOpeningDate());
    }

    @SafeVarargs
    public static ExceptionRecord buildExceptionRecordFromOcrData(Map<String, Object>... valueMap) {
        return ExceptionRecord.builder().ocrDataFields(buildScannedValidationOcrData(valueMap)).build();
    }
}
