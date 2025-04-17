package uk.gov.hmcts.reform.sscs.bulkscan.json;

import static uk.gov.hmcts.reform.sscs.bulkscan.helper.OcrDataBuilder.build;

import java.time.LocalDate;
import java.util.Map;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain.ExceptionRecord;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain.ScannedData;

@Component
public class SscsJsonExtractor {

    public ScannedData extractJson(ExceptionRecord exceptionCaseData) {
        Map<String, Object> ocrPairs = build(exceptionCaseData.getOcrDataFields());
        String openingDate = exceptionCaseData.getOpeningDate() != null ? exceptionCaseData.getOpeningDate().toLocalDate().toString() : LocalDate.now().toString();
        return ScannedData.builder().ocrCaseData(ocrPairs).records(exceptionCaseData.getScannedDocuments()).openingDate(openingDate).build();
    }
}
