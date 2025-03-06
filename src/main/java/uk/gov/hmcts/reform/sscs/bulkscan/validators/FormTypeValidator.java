package uk.gov.hmcts.reform.sscs.bulkscan.validators;

import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.FORM_TYPE;
import static uk.gov.hmcts.reform.sscs.bulkscan.helper.OcrDataBuilder.build;
import static uk.gov.hmcts.reform.sscs.bulkscan.helper.SscsDataHelper.getValidationStatus;
import static uk.gov.hmcts.reform.sscs.bulkscan.util.SscsOcrDataUtil.getField;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.domain.CaseResponse;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain.ExceptionRecord;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain.ScannedData;
import uk.gov.hmcts.reform.sscs.ccd.domain.FormType;
import uk.gov.hmcts.reform.sscs.bulkscan.json.SscsJsonExtractor;

@Component
@Slf4j
public class FormTypeValidator {

    private final SscsJsonExtractor sscsJsonExtractor;
    private final Schema sscs1Schema = tryLoadSscsSchema("/config/schema/sscs-bulk-scan-schema.json");
    private final Schema sscs2Schema = tryLoadSscsSchema("/config/schema/sscs2-bulk-scan-schema.json");
    private final Schema sscs5Schema = tryLoadSscsSchema("/config/schema/sscs5-bulk-scan-schema.json");

    public FormTypeValidator(SscsJsonExtractor sscsJsonExtractor) {
        this.sscsJsonExtractor = sscsJsonExtractor;
    }

    public CaseResponse validate(String caseId, ExceptionRecord exceptionRecord) {
        List<String> errors = null;

        String formTypeData = getFormType(caseId, exceptionRecord);

        if (formTypeData == null) {
            errors = new ArrayList<>();
            errors.add("No valid form type was found. There needs to be a valid form_type on the OCR data or on the exception record.");
            log.info("No valid form type was found while transforming exception record caseId {}",
                caseId);

            return CaseResponse.builder().errors(errors).warnings(new ArrayList<>())
                .status(getValidationStatus(errors, null)).build();
        }

        try {
            FormType formType = FormType.getById(formTypeData);

            log.info("Validating against formType {}", formType);

            if (formType != null && formType.equals(FormType.SSCS2)) {
                sscs2Schema.validate(new JSONObject(build(exceptionRecord.getOcrDataFields())));
            } else if (formType != null && formType.equals(FormType.SSCS5)) {
                sscs5Schema.validate(new JSONObject(build(exceptionRecord.getOcrDataFields())));
            } else if (formType != null && (formType.equals(FormType.SSCS1U) || formType.equals(FormType.SSCS1)
                || formType.equals(FormType.SSCS1PE) || formType.equals(FormType.SSCS1PEU))) {
                sscs1Schema.validate(new JSONObject(build(exceptionRecord.getOcrDataFields())));
            }
        } catch (ValidationException ex) {
            log.error("Validation failed: {}", ex.getAllMessages());
            if (errors == null) {
                errors = new ArrayList<>();
            }

            for (String message : ex.getAllMessages()) {
                errors.add(message);
            }
        }

        return CaseResponse.builder().errors(errors).warnings(new ArrayList<>())
            .status(getValidationStatus(errors, null)).build();
    }

    private synchronized Schema tryLoadSscsSchema(String schemaLocation) {
        return SchemaLoader
            .load(new JSONObject(new JSONTokener(getClass().getResourceAsStream(schemaLocation))));
    }

    public String getFormType(String caseId, ExceptionRecord exceptionRecord) {
        ScannedData scannedData = sscsJsonExtractor.extractJson(exceptionRecord);
        String formType = getField(scannedData.getOcrCaseData(), FORM_TYPE);

        if (formType == null  || notAValidFormType(formType)) {
            formType = exceptionRecord.getFormType();

            if (formType != null && notAValidFormType(formType)) {
                formType = null;
            }
        }

        log.info("formtype for case {} is {}", caseId, formType);

        return formType;
    }

    public boolean notAValidFormType(String formType) {
        return FormType.UNKNOWN.equals(FormType.getById(formType));
    }
}
