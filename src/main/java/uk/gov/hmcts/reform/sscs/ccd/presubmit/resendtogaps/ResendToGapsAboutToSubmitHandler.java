package uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps;

import static java.util.Objects.requireNonNull;

import com.networknt.schema.ValidationMessage;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.robotics.RoboticsJsonMapper;
import uk.gov.hmcts.reform.sscs.robotics.RoboticsJsonValidator;
import uk.gov.hmcts.reform.sscs.robotics.RoboticsValidationException;
import uk.gov.hmcts.reform.sscs.robotics.RoboticsWrapper;

@Component
@Slf4j
public class ResendToGapsAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse;

    private RoboticsJsonMapper roboticsJsonMapper;

    private RoboticsJsonValidator roboticsJsonValidator;

    private String missingFieldMessage = " is missing/not populated - please correct.";
    private String malformedFieldMessage = " is invalid - please correct.";

    @Autowired
    public ResendToGapsAboutToSubmitHandler(RoboticsJsonMapper roboticsMapper, RoboticsJsonValidator jsonValidator) {
        this.roboticsJsonMapper = roboticsMapper;
        this.roboticsJsonValidator = jsonValidator;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && callback.getEvent() == EventType.RESEND_CASE_TO_GAPS2;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {

        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        try {
            isValid(sscsCaseData, caseDetails.getId(), caseDetails.getState());
        } catch (RoboticsValidationException roboticsValidationException) {

            Set<ValidationMessage> validationErrors = roboticsValidationException.getValidationErrors();
            preSubmitCallbackResponse = addErrors(preSubmitCallbackResponse, validationErrors,
                    sscsCaseData.getCcdCaseId(), roboticsValidationException);
        }

        return preSubmitCallbackResponse;
    }

    public PreSubmitCallbackResponse<SscsCaseData> addErrors(PreSubmitCallbackResponse callbackResponse,
                                               Set<ValidationMessage> validationErrors,
                                               String caseId,
                                               RoboticsValidationException roboticsValidationException) {
        if (validationErrors != null) {
            for (ValidationMessage validationError : validationErrors) {

                log.error("Unable to validate robotics json for case id" + caseId + " with error: "
                        + validationError.getMessage() + " and type " + validationError.getType() + " and code "
                        + validationError.getCode());
                callbackResponse.addError(toCcdError(validationError));
            }
        } else {
            callbackResponse.addError(roboticsValidationException.getMessage());
        }
        return  callbackResponse;
    }

    public String toCcdError(ValidationMessage error) {

        String ccdError;

        String errorType = error.getType();
        String path = toPath(error.getPath());

        if (errorType.equals("required")) {

            ccdError = requiredErrorMessage(error, path);

        } else if (errorType.equals("minLength")) {
            ccdError = path + missingFieldMessage;

        } else if (errorType.equals("pattern")) {
            ccdError = path + malformedFieldMessage;
        } else {
            ccdError = "An unexpected error has occurred. Please raise a ServiceNow ticket"
                    + " - the following field has caused the issue: " + path;
        }

        return ccdError;
    }


    private String requiredErrorMessage(ValidationMessage error, String path) {
        String ccdError;
        if (error.getArguments().length > 0) {
            String field = error.getArguments()[0];
            if (path.length() > 0) {
                ccdError = path + "." + field + missingFieldMessage;
            } else {
                ccdError = field + missingFieldMessage;
            }
        } else {
            ccdError = path + missingFieldMessage;
        }
        return ccdError;
    }

    public String toPath(String input) {
        if (input != null && input.length() > 2) {
            return input.substring(2);
        } else {
            return  "";
        }
    }

    public void isValid(SscsCaseData caseData, Long caseId, State caseState) throws RoboticsValidationException {

        RoboticsWrapper roboticsWrapper = RoboticsWrapper
                .builder()
                .sscsCaseData(caseData)
                .ccdCaseId(caseId)
                .evidencePresent(caseData.getEvidencePresent())
                .state(caseState).build();

        JSONObject roboticsJson = toJsonObject(roboticsWrapper);
        roboticsJsonValidator.validate(roboticsJson);
    }

    public JSONObject toJsonObject(RoboticsWrapper roboticsWrapper) throws RoboticsValidationException  {
        JSONObject roboticsJson;
        try {
            roboticsJson = roboticsJsonMapper.map(roboticsWrapper);
        } catch  (NullPointerException e) {
            log.error("Json Mapper throws NPE", e);
            throw new RoboticsValidationException("Json Mapper Unable to build robotics json due to missing fields", null);
        }
        return roboticsJson;
    }
}
