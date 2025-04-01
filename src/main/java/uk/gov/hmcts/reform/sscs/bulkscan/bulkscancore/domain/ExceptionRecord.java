package uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExceptionRecord {

    private final String id;
    private final String caseTypeId;
    private final String poBox;
    private final String jurisdiction;
    private final String formType;
    private final JourneyClassification journeyClassification;
    private final LocalDateTime deliveryDate;
    private final LocalDateTime openingDate;
    private final List<InputScannedDoc> scannedDocuments;
    private final List<OcrDataField> ocrDataFields;
    private final String envelopeId;
    private final Boolean isAutomatedProcess;
    private final String exceptionRecordId;
    private final String exceptionRecordCaseTypeId;
    private final Boolean ignoreWarnings;

    public ExceptionRecord(
        @JsonProperty("id") String id,
        @JsonProperty("case_type_id") String caseTypeId,
        @JsonProperty("po_box") String poBox,
        @JsonProperty("po_box_jurisdiction") String jurisdiction,
        @JsonProperty("form_type") String formType,
        @JsonProperty("journey_classification") JourneyClassification journeyClassification,
        @JsonProperty("delivery_date") LocalDateTime deliveryDate,
        @JsonProperty("opening_date") LocalDateTime openingDate,
        @JsonProperty("scanned_documents") List<InputScannedDoc> scannedDocuments,
        @JsonProperty("ocr_data_fields") List<OcrDataField> ocrDataFields,
        // Auto Case creation request fields
        @JsonProperty("envelope_id") String envelopeId,
        @JsonProperty("is_automated_process") Boolean isAutomatedProcess,
        @JsonProperty("exception_record_id") String exceptionRecordId,
        @JsonProperty("exception_record_case_type_id") String exceptionRecordCaseTypeId,
        @JsonProperty("ignore_warnings") Boolean ignoreWarnings
    ) {
        this.id = id;
        this.caseTypeId = caseTypeId;
        this.poBox = poBox;
        this.jurisdiction = jurisdiction;
        this.formType = formType;
        this.journeyClassification = journeyClassification;
        this.deliveryDate = deliveryDate;
        this.openingDate = openingDate;
        this.scannedDocuments = scannedDocuments;
        this.ocrDataFields = ocrDataFields;
        this.envelopeId = envelopeId;
        this.isAutomatedProcess = isAutomatedProcess;
        this.exceptionRecordId = exceptionRecordId;
        this.exceptionRecordCaseTypeId = exceptionRecordCaseTypeId;
        this.ignoreWarnings = ignoreWarnings;
    }
}
