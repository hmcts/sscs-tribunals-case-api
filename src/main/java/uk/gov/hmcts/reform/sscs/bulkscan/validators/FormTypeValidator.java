package uk.gov.hmcts.reform.sscs.bulkscan.validators;

import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.FORM_TYPE;
import static uk.gov.hmcts.reform.sscs.bulkscan.helper.OcrDataBuilder.build;
import static uk.gov.hmcts.reform.sscs.bulkscan.helper.SscsDataHelper.getValidationStatus;
import static uk.gov.hmcts.reform.sscs.bulkscan.util.SscsOcrDataUtil.getField;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain.ExceptionRecord;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain.ScannedData;
import uk.gov.hmcts.reform.sscs.bulkscan.json.SscsJsonExtractor;
import uk.gov.hmcts.reform.sscs.ccd.domain.FormType;
import uk.gov.hmcts.reform.sscs.domain.CaseResponse;

@Component
@Slf4j
public class FormTypeValidator {

    private final SscsJsonExtractor sscsJsonExtractor;
    private final ObjectMapper mapper = new ObjectMapper();
    private final JsonSchema sscs1Schema = tryLoadSscsSchema("/config/schema/sscs-bulk-scan-schema.json");
    private final JsonSchema sscs2Schema = tryLoadSscsSchema("/config/schema/sscs2-bulk-scan-schema.json");
    private final JsonSchema sscs5Schema = tryLoadSscsSchema("/config/schema/sscs5-bulk-scan-schema.json");

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

        FormType formType = FormType.getById(formTypeData);

        log.info("Validating against formType {}", formType);

        JsonNode jsonNode = mapper.valueToTree(build(exceptionRecord.getOcrDataFields()));
        Set<ValidationMessage> validationErrors = null;

        if (formType != null && formType.equals(FormType.SSCS2)) {
            validationErrors = sscs2Schema.validate(jsonNode);
        } else if (formType != null && formType.equals(FormType.SSCS5)) {
            validationErrors = sscs5Schema.validate(jsonNode);
        } else if (formType != null && (formType.equals(FormType.SSCS1U) || formType.equals(FormType.SSCS1)
            || formType.equals(FormType.SSCS1PE) || formType.equals(FormType.SSCS1PEU))) {
            validationErrors = sscs1Schema.validate(jsonNode);
        }

        if (validationErrors != null && !validationErrors.isEmpty()) {
            errors = new ArrayList<>();
            for (ValidationMessage validationError : validationErrors) {
                errors.add(validationError.getMessage());
            }
            log.error("Validation failed: {}", errors);
        }

        return CaseResponse.builder().errors(errors).warnings(new ArrayList<>())
            .status(getValidationStatus(errors, null)).build();
    }

    private synchronized JsonSchema tryLoadSscsSchema(String schemaLocation) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        InputStream inputStream = getClass().getResourceAsStream(schemaLocation);
        return factory.getSchema(inputStream);
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
