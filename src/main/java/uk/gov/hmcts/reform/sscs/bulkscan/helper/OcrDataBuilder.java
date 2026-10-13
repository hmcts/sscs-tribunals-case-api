package uk.gov.hmcts.reform.sscs.bulkscan.helper;

import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain.OcrDataField;

public class OcrDataBuilder {

    private OcrDataBuilder() {
    }

    public static Map<String, Object> build(List<OcrDataField> exceptionCaseData) {
        Map<String, Object> pairs = new HashMap<>();

        if (exceptionCaseData != null) {
            for (OcrDataField ocrDataField : exceptionCaseData) {
                if (!StringUtils.isEmpty(ocrDataField.getName())) {
                    pairs.put(ocrDataField.getName(), trimToNull(ocrDataField.getValue()));
                }
            }
        }

        return pairs;
    }
}
